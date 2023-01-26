/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import io.aeron.sample.cluster.protocol.IdGeneratorSnapshotDecoder;
import io.aeron.sample.cluster.protocol.IdGeneratorSnapshotEncoder;
import io.aeron.sample.cluster.protocol.MessageHeaderDecoder;
import io.aeron.sample.cluster.protocol.MessageHeaderEncoder;
import io.aeron.sample.cluster.protocol.ParticipantSnapshotDecoder;
import io.aeron.sample.cluster.protocol.ParticipantSnapshotEncoder;
import io.aeron.samples.domain.IdGenerators;
import io.aeron.samples.domain.auctions.Auctions;
import io.aeron.samples.domain.participants.Participants;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Manages the loading and writing of domain data snapshots within the cluster
 */
public class SnapshotManager implements FragmentHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotManager.class);
    private static final int RETRY_COUNT = 3;
    private final Auctions auctions;
    private final Participants participants;
    private final IdGenerators idGenerators;
    private IdleStrategy idleStrategy;

    private final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(1024);
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final ParticipantSnapshotDecoder participantDecoder = new ParticipantSnapshotDecoder();
    private final ParticipantSnapshotEncoder participantEncoder = new ParticipantSnapshotEncoder();
    private final IdGeneratorSnapshotEncoder idGeneratorEncoder = new IdGeneratorSnapshotEncoder();
    private final IdGeneratorSnapshotDecoder idGeneratorDecoder = new IdGeneratorSnapshotDecoder();
    /**
     * Constructor
     *
     * @param auctions     the auction domain model to read and write with snapshot interactions
     * @param participants the participant domain model to read and write with snapshot interactions
     * @param idGenerators the id generator domain model to read and write with snapshot interactions
     */
    public SnapshotManager(final Auctions auctions, final Participants participants, final IdGenerators idGenerators)
    {
        this.auctions = auctions;
        this.participants = participants;
        this.idGenerators = idGenerators;
    }

    /**
     * Called by the clustered service once a snapshot needs to be taken
     * @param snapshotPublication the publication to write snapshot data to
     */
    public void takeSnapshot(final ExclusivePublication snapshotPublication)
    {
        LOGGER.info("Starting snapshot...");
        offerParticipants(snapshotPublication);
        offerIdGenerators(snapshotPublication);
        LOGGER.info("Snapshot complete");
    }


    /**
     * Called by the clustered service once a snapshot has been provided by the cluster
     * todo: Question do we want to add in start/end markers to the snapshots?
     * @param snapshotImage the image to read snapshot data from
     */
    public void loadSnapshot(final Image snapshotImage)
    {
        LOGGER.info("Loading snapshot...");
        Objects.requireNonNull(idleStrategy, "Idle strategy must be set before loading snapshot");
        idleStrategy.reset();
        while (!snapshotImage.isEndOfStream())
        {
            idleStrategy.idle(snapshotImage.poll(this, 20));
        }
        LOGGER.info("Snapshot load complete.");
    }

    /**
     * Provide an idle strategy for the snapshot load process
     * @param idleStrategy the idle strategy to use
     */
    public void setIdleStrategy(final IdleStrategy idleStrategy)
    {
        this.idleStrategy = idleStrategy;
    }

    /**
     *
     * @param buffer containing the data.
     * @param offset at which the data begins.
     * @param length of the data in bytes.
     * @param header representing the metadata for the data.
     */
    @Override
    public void onFragment(final DirectBuffer buffer, final int offset, final int length, final Header header)
    {
        if (length < MessageHeaderDecoder.ENCODED_LENGTH)
        {
            return;
        }

        headerDecoder.wrap(buffer, offset);

        switch (headerDecoder.templateId())
        {
            case ParticipantSnapshotDecoder.TEMPLATE_ID ->
            {
                participantDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
                participants.addParticipant(participantDecoder.participantId(),
                    participantDecoder.name());
            }
            case IdGeneratorSnapshotDecoder.TEMPLATE_ID ->
            {
                idGeneratorDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
                idGenerators.initializeAuctionId(idGeneratorDecoder.nextAuctionId());
            }
            default -> LOGGER.warn("Unknown snapshot message template id: {}", headerDecoder.templateId());
        }
    }

    /**
     * Offers the participants to the snapshot publication using the ParticipantSnapshotEncoder
     * @param snapshotPublication the publication to offer the snapshot data to
     */
    private void offerParticipants(final ExclusivePublication snapshotPublication)
    {
        headerEncoder.wrap(buffer, 0);
        participants.getParticipants().forEach(participant ->
        {
            participantEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder);
            participantEncoder.participantId(participant.participantId());
            participantEncoder.name(participant.name());
            retryingOffer(snapshotPublication, buffer, 0,
                headerEncoder.encodedLength() + participantEncoder.encodedLength());
        });
    }


    /**
     * Offers the id generators to the snapshot publication using the IdGeneratorSnapshotEncoder
     * @param snapshotPublication the publication to offer the snapshot data to
     */
    private void offerIdGenerators(final ExclusivePublication snapshotPublication)
    {
        headerEncoder.wrap(buffer, 0);
        idGeneratorEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder);
        idGeneratorEncoder.nextAuctionId(idGenerators.getAuctionId());
        retryingOffer(snapshotPublication, buffer, 0,
            headerEncoder.encodedLength() + idGeneratorEncoder.encodedLength());
    }


    /**
     * Retries the offer to the publication if it fails on back pressure or admin action
     * @param publication the publication to offer data to
     * @param buffer the buffer holding the source data
     * @param offset the offset to write from
     * @param length the length to write
     */
    private void retryingOffer(final ExclusivePublication publication, final DirectBuffer buffer,
        final int offset, final int length)
    {
        int retries = 0;
        do
        {
            final long result = publication.offer(buffer, offset, length);
            if (result > 0L)
            {
                return;
            }
            else if (result == Publication.ADMIN_ACTION || result == Publication.BACK_PRESSURED)
            {
                LOGGER.warn("backpressure or admin action on snapshot");
            }
            else if (result == Publication.NOT_CONNECTED || result == Publication.MAX_POSITION_EXCEEDED)
            {
                LOGGER.error("unexpected publication state on snapshot: {}", result);
                return;
            }
            idleStrategy.idle();
            retries += 1;
        }
        while (retries < RETRY_COUNT);

        LOGGER.error("failed to offer snapshot within {} retries", RETRY_COUNT);
    }
}
