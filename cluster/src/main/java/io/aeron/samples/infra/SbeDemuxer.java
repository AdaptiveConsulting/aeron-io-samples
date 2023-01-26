/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import io.aeron.sample.cluster.protocol.AddAuctionBidDecoder;
import io.aeron.sample.cluster.protocol.AddParticipantDecoder;
import io.aeron.sample.cluster.protocol.CreateAuctionDecoder;
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
    private final SessionMessageContextImpl sessionMessageContext;
    private final Participants participants;
    private final Auctions auctions;
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    private final AddParticipantDecoder addParticipantDecoder = new AddParticipantDecoder();
    private final CreateAuctionDecoder createAuctionDecoder = new CreateAuctionDecoder();

    /**
     * Dispatches ingress messages to domain logic.
     *
     * @param sessionMessageContext the message context
     * @param participants          the participants domain model to which commands are dispatched
     * @param auctions              the auction domain model to which commands are dispatched
     */
    public SbeDemuxer(final SessionMessageContextImpl sessionMessageContext, final Participants participants,
        final Auctions auctions)
    {
        this.sessionMessageContext = sessionMessageContext;
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
            case AddParticipantDecoder.TEMPLATE_ID ->
            {
                addParticipantDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
                participants.addParticipant(addParticipantDecoder.participantId(),
                    addParticipantDecoder.name());
            }
            case CreateAuctionDecoder.TEMPLATE_ID ->
            {
                createAuctionDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
                auctions.addAuction(createAuctionDecoder.createdByParticipantId(),
                    createAuctionDecoder.startTime(),
                    createAuctionDecoder.endTime(),
                    createAuctionDecoder.name(),
                    createAuctionDecoder.description());
            }
            case AddAuctionBidDecoder.TEMPLATE_ID ->
            {
                //todo 1
            }
            default -> LOGGER.error("Unknown message template {}, ignored.", headerDecoder.templateId());
        }
    }
}
