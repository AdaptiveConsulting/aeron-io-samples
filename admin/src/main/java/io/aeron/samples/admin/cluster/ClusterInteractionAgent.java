/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cluster;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.samples.cluster.admin.protocol.ConnectClusterDecoder;
import io.aeron.samples.cluster.admin.protocol.DisconnectClusterDecoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderDecoder;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStyle;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import static io.aeron.samples.admin.cluster.MessageTypes.CLUSTER_CLIENT_CONTROL;
import static io.aeron.samples.admin.cluster.MessageTypes.CLUSTER_PASSTHROUGH;

/**
 * Agent to interact with the cluster
 */
public class ClusterInteractionAgent implements Agent, MessageHandler
{
    private static final long HEARTBEAT_INTERVAL = 250;
    public static final String INGRESS_CHANNEL = "aeron:udp?term-length=64k";
    private ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
    private long lastHeartbeatTime = Long.MIN_VALUE;
    private final OneToOneRingBuffer adminClusterComms;
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final ConnectClusterDecoder connectClusterDecoder = new ConnectClusterDecoder();
    private MediaDriver mediaDriver;
    private AeronCluster aeronCluster;
    private AdminClientEgressListener adminClientEgressListener;
    private LineReader lineReader;

    /**
     * Creates a new agent to interact with the cluster
     * @param adminClusterChannel the channel to send messages to the cluster from the REPL
     */
    public ClusterInteractionAgent(final OneToOneRingBuffer adminClusterChannel)
    {
        this.adminClusterComms = adminClusterChannel;
    }

    @Override
    public int doWork()
    {
        final long now = SystemEpochClock.INSTANCE.time();
        if (now > (lastHeartbeatTime + HEARTBEAT_INTERVAL))
        {
            lastHeartbeatTime = now;
            if (connectionState == ConnectionState.CONNECTED)
            {
                aeronCluster.sendKeepAlive();
            }
        }

        adminClusterComms.read(this);

        if (null != aeronCluster && !aeronCluster.isClosed())
        {
            aeronCluster.pollEgress();
        }

        return 0; //always sleep
    }

    @Override
    public String roleName()
    {
        return "cluster-interaction-agent";
    }

    @Override
    public void onMessage(final int msgTypeId, final MutableDirectBuffer buffer, final int offset, final int length)
    {
        if (msgTypeId == CLUSTER_CLIENT_CONTROL)
        {
            if (length < MessageHeaderDecoder.ENCODED_LENGTH)
            {
                log("Invalid message length", AttributedStyle.RED);
            }

            messageHeaderDecoder.wrap(buffer, offset);
            processInternalMessage(messageHeaderDecoder, buffer, offset);
        }
        else if (msgTypeId == CLUSTER_PASSTHROUGH)
        {
            if (connectionState == ConnectionState.CONNECTED)
            {
                aeronCluster.offer(buffer, offset, length);
            }
            else
            {
                log("Not connected to cluster. Connect first", AttributedStyle.RED);
            }
        }
    }

    private void processInternalMessage(
        final MessageHeaderDecoder messageHeaderDecoder,
        final MutableDirectBuffer buffer,
        final int offset)
    {
        final int templateId = messageHeaderDecoder.templateId();
        if (templateId == ConnectClusterDecoder.TEMPLATE_ID)
        {
            connectClusterDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
            log("Connecting to cluster", AttributedStyle.WHITE);
            connectCluster(connectClusterDecoder.baseport(), connectClusterDecoder.clusterHosts(),
                connectClusterDecoder.localhostName());
            connectionState = ConnectionState.CONNECTED;
            log("Cluster connected", AttributedStyle.GREEN);
        }
        else if (templateId == DisconnectClusterDecoder.TEMPLATE_ID)
        {
            log("Disconnecting from cluster", AttributedStyle.WHITE);
            disconnectCluster();
            connectionState = ConnectionState.NOT_CONNECTED;
            log("Cluster disconnected", AttributedStyle.GREEN);
        }
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
     * @param clusterHosts list of cluster hosts
     * @param localHostName if empty, will be looked up
     */
    private void connectCluster(final int basePort, final String clusterHosts, final String localHostName)
    {
        final List<String> hostnames = Arrays.asList(clusterHosts.split(","));
        final String ingressEndpoints = ClusterConfig.ingressEndpoints(
            hostnames, basePort, ClusterConfig.CLIENT_FACING_PORT_OFFSET);
        log("Connecting to cluster hosts using ingress endpoints: " + ingressEndpoints, AttributedStyle.WHITE);
        String hostName = "localhost";
        if (localHostName.isEmpty() || localHostName.isBlank())
        {
            try
            {
                hostName = InetAddress.getLocalHost().getHostAddress();
                log("Using hostname: " + hostName, AttributedStyle.WHITE);
            }
            catch (final Exception e)
            {
                log("Unable to get hostname", AttributedStyle.RED);
            }
        }
        else
        {
            hostName = localHostName;
        }
        final String egressChannel = "aeron:udp?endpoint=" + hostName + ":0";
        adminClientEgressListener = new AdminClientEgressListener();
        adminClientEgressListener.setLineReader(lineReader);
        mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true));
        aeronCluster = AeronCluster.connect(
            new AeronCluster.Context()
                .egressListener(adminClientEgressListener)
                .egressChannel(egressChannel)
                .ingressChannel(INGRESS_CHANNEL)
                .ingressEndpoints(ingressEndpoints)
                .aeronDirectoryName(mediaDriver.aeronDirectoryName()));
    }

    /**
     * Sets the line reader to use for input saving while logging
     *
     * @param lineReader line reader to use
     */
    public void setLineReader(final LineReader lineReader)
    {
        this.lineReader = lineReader;
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
    }
}
