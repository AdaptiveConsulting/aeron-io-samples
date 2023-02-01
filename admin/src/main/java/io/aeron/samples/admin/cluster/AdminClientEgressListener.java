/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cluster;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.logbuffer.Header;
import io.aeron.samples.cluster.protocol.AddParticipantCommandEncoder;
import io.aeron.samples.cluster.protocol.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Admin client egress listener
 */
public class AdminClientEgressListener implements EgressListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminClientEgressListener.class);
    private final IdleStrategy idleStrategy = new SleepingMillisIdleStrategy();
    private final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final AddParticipantCommandEncoder addParticipantCommandEncoder = new AddParticipantCommandEncoder();
    private AeronCluster clusterClient;
    private Terminal terminal;

    @Override
    public void onMessage(final long clusterSessionId, final long timestamp, final DirectBuffer buffer,
        final int offset, final int length, final Header header)
    {

    }

    @Override
    public void onSessionEvent(
        final long correlationId,
        final long clusterSessionId,
        final long leadershipTermId,
        final int leaderMemberId,
        final EventCode code,
        final String detail)
    {
        LOGGER.info("Received session event {} for session {} with detail {}", code, clusterSessionId, detail);
    }

    /**
     * Sets the cluster client
     *
     * @param clusterClient the cluster client
     */
    public void setAeronCluster(final AeronCluster clusterClient)
    {
        this.clusterClient = clusterClient;
    }

    private void offer(final DirectBuffer buffer, final int offset, final int length)
    {
        while (clusterClient.offer(buffer, offset, length) < 0)
        {
            idleStrategy.idle(clusterClient.pollEgress());
        }
    }

    /**
     * Adds a participant
     *
     * @param participantId   the participant id
     * @param participantName the participant name
     */
    public void addParticipant(final Integer participantId, final String participantName)
    {
        final AttributedStringBuilder builder = new AttributedStringBuilder();
        builder
            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("Adding participant with id ")
            .append(participantId.toString())
            .append(" and name ")
            .append(participantName);
        terminal.writer().println(builder.toAnsi(terminal));
        messageHeaderEncoder.wrap(buffer, 0);
        addParticipantCommandEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        addParticipantCommandEncoder.participantId(participantId);
        addParticipantCommandEncoder.name(participantName);
        offer(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            addParticipantCommandEncoder.encodedLength());
    }

    /**
     * Sets the terminal
     *
     * @param terminal the terminal
     */
    public void setTerminal(final Terminal terminal)
    {
        this.terminal = terminal;
    }
}
