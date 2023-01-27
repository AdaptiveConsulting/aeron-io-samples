/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domaininfra;

import io.aeron.sample.cluster.protocol.AddAuctionBidCommandDecoder;
import io.aeron.sample.cluster.protocol.AddParticipantCommandDecoder;
import io.aeron.sample.cluster.protocol.CreateAuctionCommandDecoder;
import io.aeron.sample.cluster.protocol.MessageHeaderDecoder;
import io.aeron.samples.domain.auctions.Auctions;
import io.aeron.samples.domain.participants.Participants;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demultiplexes messages from the ingress stream to the appropriate domain handler.
 */
public class SbeDemuxer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SbeDemuxer.class);
    private final Participants participants;
    private final Auctions auctions;
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    private final AddParticipantCommandDecoder addParticipantDecoder = new AddParticipantCommandDecoder();
    private final AddAuctionBidCommandDecoder addAuctionBidDecoder = new AddAuctionBidCommandDecoder();
    private final CreateAuctionCommandDecoder createAuctionDecoder = new CreateAuctionCommandDecoder();

    /**
     * Dispatches ingress messages to domain logic.
     *
     * @param participants          the participants domain model to which commands are dispatched
     * @param auctions              the auction domain model to which commands are dispatched
     */
    public SbeDemuxer(final Participants participants, final Auctions auctions)
    {
        this.participants = participants;
        this.auctions = auctions;
    }

    /**
     * Dispatch a message to the appropriate domain handler.
     *
     * @param buffer the buffer containing the inbound message, including a header
     * @param offset the offset to apply
     * @param length the length of the message
     */
    public void dispatch(final DirectBuffer buffer, final int offset, final int length)
    {
        if (length < MessageHeaderDecoder.ENCODED_LENGTH)
        {
            LOGGER.error("Message too short, ignored.");
            return;
        }
        headerDecoder.wrap(buffer, offset);

        switch (headerDecoder.templateId())
        {
            case AddParticipantCommandDecoder.TEMPLATE_ID ->
            {
                addParticipantDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
                participants.addParticipant(addParticipantDecoder.participantId(),
                    addParticipantDecoder.name());
            }
            case CreateAuctionCommandDecoder.TEMPLATE_ID ->
            {
                createAuctionDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
                auctions.addAuction(createAuctionDecoder.correlationId(),
                    createAuctionDecoder.createdByParticipantId(),
                    createAuctionDecoder.startTime(),
                    createAuctionDecoder.endTime(),
                    createAuctionDecoder.name(),
                    createAuctionDecoder.description());
            }
            case AddAuctionBidCommandDecoder.TEMPLATE_ID ->
            {
                addAuctionBidDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
                auctions.addBid(addAuctionBidDecoder.auctionId(),
                    addAuctionBidDecoder.addedByParticipantId(),
                    addAuctionBidDecoder.price(),
                    addAuctionBidDecoder.correlationId());
            }
            default -> LOGGER.error("Unknown message template {}, ignored.", headerDecoder.templateId());
        }
    }
}
