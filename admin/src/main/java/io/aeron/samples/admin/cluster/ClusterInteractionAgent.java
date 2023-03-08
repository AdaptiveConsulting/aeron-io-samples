/*
 * Copyright 2023 Adaptive Financial Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.aeron.samples.admin.cluster;

import io.aeron.Publication;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.samples.cluster.admin.protocol.AddAuctionBidDecoder;
import io.aeron.samples.cluster.admin.protocol.AddAuctionDecoder;
import io.aeron.samples.cluster.admin.protocol.AddParticipantDecoder;
import io.aeron.samples.cluster.admin.protocol.ConnectClusterDecoder;
import io.aeron.samples.cluster.admin.protocol.DisconnectClusterDecoder;
import io.aeron.samples.cluster.admin.protocol.ListAuctionsDecoder;
import io.aeron.samples.cluster.admin.protocol.ListParticipantsDecoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderDecoder;
import io.aeron.samples.cluster.protocol.AddAuctionBidCommandEncoder;
import io.aeron.samples.cluster.protocol.AddParticipantCommandEncoder;
import io.aeron.samples.cluster.protocol.CreateAuctionCommandEncoder;
import io.aeron.samples.cluster.protocol.ListAuctionsCommandEncoder;
import io.aeron.samples.cluster.protocol.ListParticipantsCommandEncoder;
import io.aeron.samples.cluster.protocol.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStyle;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent to interact with the cluster
 */
public class ClusterInteractionAgent implements Agent, MessageHandler
{
    private static final long HEARTBEAT_INTERVAL = 250;
    private static final long RETRY_COUNT = 10;
    private static final String INGRESS_CHANNEL = "aeron:udp?term-length=64k";
    private final MutableDirectBuffer sendBuffer = new ExpandableDirectByteBuffer(1024);
    private long lastHeartbeatTime = Long.MIN_VALUE;
    private final OneToOneRingBuffer adminClusterComms;
    private final IdleStrategy idleStrategy;
    private final AtomicBoolean runningFlag;
    private final PendingMessageManager pendingMessageManager;
    private AdminClientEgressListener adminClientEgressListener;
    private AeronCluster aeronCluster;
    private ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
    private LineReader lineReader;
    private MediaDriver mediaDriver;

    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final ConnectClusterDecoder connectClusterDecoder = new ConnectClusterDecoder();
    private final AddAuctionDecoder addAuctionDecoder = new AddAuctionDecoder();
    private final AddParticipantDecoder addParticipantDecoder = new AddParticipantDecoder();
    private final AddAuctionBidDecoder addAuctionBidDecoder = new AddAuctionBidDecoder();

    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final CreateAuctionCommandEncoder createAuctionCommandEncoder = new CreateAuctionCommandEncoder();
    private final AddParticipantCommandEncoder addParticipantCommandEncoder = new AddParticipantCommandEncoder();
    private final AddAuctionBidCommandEncoder addAuctionBidCommandEncoder = new AddAuctionBidCommandEncoder();
    private final ListParticipantsCommandEncoder listParticipantsCommandEncoder = new ListParticipantsCommandEncoder();
    private final ListAuctionsCommandEncoder listAuctionsCommandEncoder = new ListAuctionsCommandEncoder();

    /**
     * Creates a new agent to interact with the cluster
     * @param adminClusterChannel the channel to send messages to the cluster from the REPL
     * @param idleStrategy the idle strategy to use
     * @param runningFlag the flag to indicate if the REPL is still running
     */
    public ClusterInteractionAgent(
        final OneToOneRingBuffer adminClusterChannel,
        final IdleStrategy idleStrategy,
        final AtomicBoolean runningFlag)
    {
        this.adminClusterComms = adminClusterChannel;
        this.idleStrategy = idleStrategy;
        this.runningFlag = runningFlag;
        this.pendingMessageManager = new PendingMessageManager(SystemEpochClock.INSTANCE);
    }

    @Override
    public int doWork()
    {
        //send cluster heartbeat roughly every 250ms
        final long now = SystemEpochClock.INSTANCE.time();
        if (now >= (lastHeartbeatTime + HEARTBEAT_INTERVAL))
        {
            lastHeartbeatTime = now;
            if (connectionState == ConnectionState.CONNECTED)
            {
                aeronCluster.sendKeepAlive();
            }
        }

        //poll inbound to this agent messages (from the REPL)
        adminClusterComms.read(this);

        //poll outbound messages from the cluster
        if (null != aeronCluster && !aeronCluster.isClosed())
        {
            aeronCluster.pollEgress();
        }

        //check for timed-out messages
        pendingMessageManager.doWork();

        //always sleep
        return 0;
    }

    @Override
    public String roleName()
    {
        return "cluster-interaction-agent";
    }

    @Override
    public void onMessage(final int msgTypeId, final MutableDirectBuffer buffer, final int offset, final int length)
    {
        messageHeaderDecoder.wrap(buffer, offset);
        switch (messageHeaderDecoder.templateId())
        {
            case ConnectClusterDecoder.TEMPLATE_ID -> processConnectCluster(buffer, offset);
            case DisconnectClusterDecoder.TEMPLATE_ID -> processDisconnectCluster();
            case AddAuctionDecoder.TEMPLATE_ID -> processAddAuction(messageHeaderDecoder, buffer, offset);
            case AddParticipantDecoder.TEMPLATE_ID -> processAddParticipant(messageHeaderDecoder, buffer, offset);
            case AddAuctionBidDecoder.TEMPLATE_ID -> processAddAuctionBid(messageHeaderDecoder, buffer, offset);
            case ListAuctionsDecoder.TEMPLATE_ID -> processListAuctions();
            case ListParticipantsDecoder.TEMPLATE_ID -> processListParticipants();
            default -> log("Unknown message type: " + messageHeaderDecoder.templateId(), AttributedStyle.RED);
        }
    }


    /**
     * Opens the cluster connection
     * @param buffer the buffer containing the message
     * @param offset the offset of the message
     */
    private void processConnectCluster(final MutableDirectBuffer buffer, final int offset)
    {
        connectClusterDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        connectCluster(connectClusterDecoder.baseport(), connectClusterDecoder.port(),
            connectClusterDecoder.clusterHosts(), connectClusterDecoder.localhostName());
        connectionState = ConnectionState.CONNECTED;
    }

    /**
     * Closes the cluster connection
     */
    private void processDisconnectCluster()
    {
        log("Disconnecting from cluster", AttributedStyle.WHITE);
        disconnectCluster();
        connectionState = ConnectionState.NOT_CONNECTED;
        log("Cluster disconnected", AttributedStyle.GREEN);
    }

    /**
     * Marshals the CLI protocol to cluster protocol for Adding an Auction
     * @param messageHeaderDecoder the message header decoder
     * @param buffer the buffer containing the message
     * @param offset the offset of the message
     */
    private void processAddAuction(
        final MessageHeaderDecoder messageHeaderDecoder,
        final MutableDirectBuffer buffer,
        final int offset)
    {
        final String correlationId = UUID.randomUUID().toString();

        addAuctionDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        createAuctionCommandEncoder.wrapAndApplyHeader(sendBuffer, 0, messageHeaderEncoder);

        createAuctionCommandEncoder.createdByParticipantId(addAuctionDecoder.createdByParticipantId());
        createAuctionCommandEncoder.startTime(addAuctionDecoder.startTime());
        createAuctionCommandEncoder.endTime(addAuctionDecoder.endTime());
        createAuctionCommandEncoder.correlationId(correlationId);
        createAuctionCommandEncoder.name(addAuctionDecoder.name());
        createAuctionCommandEncoder.description(addAuctionDecoder.description());

        pendingMessageManager.addMessage(correlationId, "add-auction");

        retryingClusterOffer(sendBuffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            createAuctionCommandEncoder.encodedLength());
    }

    /**
     * Marshals the CLI protocol to cluster protocol for Adding a Participant
     * @param messageHeaderDecoder the message header decoder
     * @param buffer the buffer containing the message
     * @param offset the offset of the message
     */
    private void processAddParticipant(
        final MessageHeaderDecoder messageHeaderDecoder,
        final MutableDirectBuffer buffer,
        final int offset)
    {
        final String correlationId = UUID.randomUUID().toString();
        addParticipantDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        addParticipantCommandEncoder.wrapAndApplyHeader(sendBuffer, 0, messageHeaderEncoder);

        pendingMessageManager.addMessage(correlationId, "add-participant");
        addParticipantCommandEncoder.participantId(addParticipantDecoder.participantId());
        addParticipantCommandEncoder.correlationId(correlationId);
        addParticipantCommandEncoder.name(addParticipantDecoder.name());

        retryingClusterOffer(sendBuffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            addParticipantCommandEncoder.encodedLength());
    }

    /**
     * Marshals the CLI protocol to cluster protocol for Adding a Bid to an auction
     * @param messageHeaderDecoder the message header decoder
     * @param buffer the buffer containing the message
     * @param offset the offset of the message
     */
    private void processAddAuctionBid(
        final MessageHeaderDecoder messageHeaderDecoder,
        final MutableDirectBuffer buffer,
        final int offset)
    {
        final String correlationId = UUID.randomUUID().toString();
        addAuctionBidDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        addAuctionBidCommandEncoder.wrapAndApplyHeader(sendBuffer, 0, messageHeaderEncoder);

        addAuctionBidCommandEncoder.auctionId(addAuctionBidDecoder.auctionId());
        addAuctionBidCommandEncoder.addedByParticipantId(addAuctionBidDecoder.addedByParticipantId());
        addAuctionBidCommandEncoder.price(addAuctionBidDecoder.price());
        addAuctionBidCommandEncoder.correlationId(correlationId);
        pendingMessageManager.addMessage(correlationId, "add-auction-bid");

        retryingClusterOffer(sendBuffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            addAuctionBidCommandEncoder.encodedLength());
    }

    /**
     * Marshals the CLI protocol to cluster protocol for Listing all participants
     */
    private void processListParticipants()
    {
        final String correlationId = UUID.randomUUID().toString();
        listParticipantsCommandEncoder.wrapAndApplyHeader(sendBuffer, 0, messageHeaderEncoder);
        listParticipantsCommandEncoder.correlationId(correlationId);
        pendingMessageManager.addMessage(correlationId, "list-participants");
        retryingClusterOffer(sendBuffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            listParticipantsCommandEncoder.encodedLength());
    }

    /**
     * Marshals the CLI protocol to cluster protocol for Listing all auctions
     */
    private void processListAuctions()
    {
        final String correlationId = UUID.randomUUID().toString();
        listAuctionsCommandEncoder.wrapAndApplyHeader(sendBuffer, 0, messageHeaderEncoder);
        listAuctionsCommandEncoder.correlationId(correlationId);
        pendingMessageManager.addMessage(correlationId, "list-auctions");
        retryingClusterOffer(sendBuffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            listAuctionsCommandEncoder.encodedLength());
    }

    /**
     * Disconnects from the cluster
     */
    private void disconnectCluster()
    {
        adminClientEgressListener = null;
        if (aeronCluster != null)
        {
            aeronCluster.close();
        }
        if (mediaDriver != null)
        {
            mediaDriver.close();
        }
    }

    /**
     * Connects to the cluster
     *
     * @param basePort base port to use
     * @param port the port to use
     * @param clusterHosts list of cluster hosts
     * @param localHostName if empty, will be looked up
     */
    private void connectCluster(
        final int basePort,
        final int port,
        final String clusterHosts,
        final String localHostName)
    {
        final List<String> hostnames = Arrays.asList(clusterHosts.split(","));
        final String ingressEndpoints = ClusterConfig.ingressEndpoints(
            hostnames, basePort, ClusterConfig.CLIENT_FACING_PORT_OFFSET);
        final String egressChannel = "aeron:udp?endpoint=" + localHostName + ":" + port;
        adminClientEgressListener = new AdminClientEgressListener(pendingMessageManager);
        adminClientEgressListener.setLineReader(lineReader);
        mediaDriver = MediaDriver.launch(new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .dirDeleteOnStart(true)
            .errorHandler(this::logError)
            .dirDeleteOnShutdown(true));
        aeronCluster = AeronCluster.connect(
            new AeronCluster.Context()
                .egressListener(adminClientEgressListener)
                .egressChannel(egressChannel)
                .ingressChannel(INGRESS_CHANNEL)
                .ingressEndpoints(ingressEndpoints)
                .errorHandler(this::logError)
                .aeronDirectoryName(mediaDriver.aeronDirectoryName()));

        log("Connected to cluster leader, node " + aeronCluster.leaderMemberId(), AttributedStyle.GREEN);
    }

    private void logError(final Throwable throwable)
    {
        log("Error: " + throwable.getMessage(), AttributedStyle.RED);
    }

    /**
     * Sets the line reader to use for input saving while logging
     *
     * @param lineReader line reader to use
     */
    public void setLineReader(final LineReader lineReader)
    {
        this.lineReader = lineReader;
        pendingMessageManager.setLineReader(lineReader);
    }

    /**
     * Logs a message to the terminal if available or to the logger if not
     *
     * @param message message to log
     * @param color message color to use
     */
    private void log(final String message, final int color)
    {
        LineReaderHelper.log(lineReader, message, color);
    }

    /**
     * sends to cluster with retry as needed, up to the limit
     *
     * @param buffer buffer containing the message
     * @param offset offset of the message
     * @param length length of the message
     */
    private void retryingClusterOffer(final DirectBuffer buffer, final int offset, final int length)
    {
        if (connectionState == ConnectionState.CONNECTED)
        {
            int retries = 0;
            do
            {
                final long result = aeronCluster.offer(buffer, offset, length);
                if (result > 0L)
                {
                    return;
                }
                else if (result == Publication.ADMIN_ACTION || result == Publication.BACK_PRESSURED)
                {
                    log("backpressure or admin action on cluster offer", AttributedStyle.YELLOW);
                }
                else if (result == Publication.NOT_CONNECTED || result == Publication.MAX_POSITION_EXCEEDED)
                {
                    log("Cluster is not connected, or maximum position has been exceeded. Message lost.",
                        AttributedStyle.RED);
                    return;
                }

                idleStrategy.idle();
                retries += 1;
                log("failed to send message to cluster. Retrying (" + retries + " of " + RETRY_COUNT + ")",
                    AttributedStyle.YELLOW);
            }
            while (retries < RETRY_COUNT);

            log("Failed to send message to cluster. Message lost.", AttributedStyle.RED);
        }
        else
        {
            log("Not connected to cluster. Connect first", AttributedStyle.RED);
        }
    }

    @Override
    public void onClose()
    {
        if (aeronCluster != null)
        {
            aeronCluster.close();
        }
        if (mediaDriver != null)
        {
            mediaDriver.close();
        }
        runningFlag.set(false);
    }
}
