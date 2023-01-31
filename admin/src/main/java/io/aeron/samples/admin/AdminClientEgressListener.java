/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.client.EgressListener;
import io.aeron.logbuffer.Header;
import io.aeron.samples.cluster.protocol.AddParticipantCommandEncoder;
import io.aeron.samples.cluster.protocol.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Admin client egress listener
 */
public class AdminClientEgressListener implements EgressListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminClientEgressListener.class);
    private final IdleStrategy idleStrategy = new SleepingMillisIdleStrategy();
    private AeronCluster clusterClient;
    private final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final AddParticipantCommandEncoder addParticipantCommandEncoder = new AddParticipantCommandEncoder();


    @Override
    public void onMessage(final long clusterSessionId, final long timestamp, final DirectBuffer buffer,
        final int offset, final int length, final Header header)
    {

    }


    /**
     * Sets the cluster client
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
        LOGGER.info("Offered message to cluster");
    }

    /**
     * Adds a participant
     * @param participantId the participant id
     * @param participantName the participant name
     */
    public void addParticipant(final Integer participantId, final String participantName)
    {
        LOGGER.info("Adding participant {} with name {}", participantId, participantName);
        messageHeaderEncoder.wrap(buffer, 0);
        addParticipantCommandEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        addParticipantCommandEncoder.participantId(participantId);
        addParticipantCommandEncoder.name(participantName);
        offer(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            addParticipantCommandEncoder.encodedLength());
    }
}
