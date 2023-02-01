/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cluster;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.samples.cluster.admin.protocol.ConnectClusterDecoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderDecoder;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Agent to interact with the cluster
 */
public class ClusterInteractionAgent implements Agent, MessageHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterInteractionAgent.class);
    private static final long HEARTBEAT_INTERVAL = 250;
    private final OneToOneRingBuffer clusterAdminComms;
    private ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
    private long lastHeartbeatTime = Long.MIN_VALUE;
    private final OneToOneRingBuffer adminClusterComms;
    private MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private ConnectClusterDecoder connectClusterDecoder = new ConnectClusterDecoder();
    private Terminal terminal;
    private MediaDriver mediaDriver;
    private AeronCluster aeronCluster;
    private AdminClientEgressListener adminClientEgressListener;
    private LineReader lineReader;

    /**
     * Creates a new agent to interact with the cluster
     * @param adminClusterChannel
     * @param clusterAdminChannel
     */
    public ClusterInteractionAgent(final OneToOneRingBuffer adminClusterChannel,
        final OneToOneRingBuffer clusterAdminChannel)
    {

        this.adminClusterComms = adminClusterChannel;
        this.clusterAdminComms = clusterAdminChannel;
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
        if (length < MessageHeaderDecoder.ENCODED_LENGTH)
        {
            log("Invalid message length");
        }

        messageHeaderDecoder.wrap(buffer, offset);
        if (messageHeaderDecoder.templateId() == ConnectClusterDecoder.TEMPLATE_ID)
        {
            connectClusterDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
            if (connectionState == ConnectionState.NOT_CONNECTED)
            {
                log("Connecting to cluster");
                connectCluster(connectClusterDecoder.baseport(), connectClusterDecoder.clusterHosts());
                connectionState = ConnectionState.CONNECTED;
            }
        }
    }

    /**
     * Connects to the cluster
     *
     * @param baseport base port to use
     * @param clusterHosts list of cluster hosts
     */
    private void connectCluster(final int baseport, final String clusterHosts)
    {
        final String ingressEndpoints = ingressEndpoints(baseport, Arrays.asList(clusterHosts));
        adminClientEgressListener = new AdminClientEgressListener();
        mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true));
        aeronCluster = AeronCluster.connect(
            new AeronCluster.Context()
                .egressListener(adminClientEgressListener)
                .egressChannel("aeron:udp?endpoint=localhost:0")
                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                .ingressChannel("aeron:udp")
                .ingressEndpoints(ingressEndpoints));
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
     * Ingress endpoint configuration
     *
     * @param hostnames list of hostnames
     * @param portBase port base to use
     * @return ingress endpoint configuration
     */
    private static String ingressEndpoints(final int portBase, final List<String> hostnames)
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostnames.size(); i++)
        {
            sb.append(i).append('=');
            sb.append(hostnames.get(i)).append(':').append(calculatePort(portBase, i, 10));
            sb.append(',');
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static int calculatePort(final int portBase, final int nodeId, final int offset)
    {
        return portBase + (nodeId * ClusterConfig.PORTS_PER_NODE) + offset;
    }

    /**
     * Logs a message to the terminal if available or to the logger if not
     *
     * @param message message to log
     */
    private void log(final String message)
    {
        if (lineReader == null)
        {
            LOGGER.info(message);
        }
        else
        {
            lineReader.getTerminal().puts(InfoCmp.Capability.carriage_return);
            lineReader.getTerminal().writer().println(message);
            lineReader.callWidget(LineReader.REDRAW_LINE);
            lineReader.callWidget(LineReader.REDISPLAY);
            lineReader.getTerminal().writer().flush();
        }
    }
}
