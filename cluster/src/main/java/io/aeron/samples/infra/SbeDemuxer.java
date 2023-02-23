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

package io.aeron.samples.infra;

import io.aeron.samples.cluster.protocol.AddAuctionBidCommandDecoder;
import io.aeron.samples.cluster.protocol.AddParticipantCommandDecoder;
import io.aeron.samples.cluster.protocol.CreateAuctionCommandDecoder;
import io.aeron.samples.cluster.protocol.ListAuctionsCommandDecoder;
import io.aeron.samples.cluster.protocol.ListParticipantsCommandDecoder;
import io.aeron.samples.cluster.protocol.MessageHeaderDecoder;
import io.aeron.samples.domain.auctions.Auction;
import io.aeron.samples.domain.auctions.Auctions;
import io.aeron.samples.domain.participants.Participant;
import io.aeron.samples.domain.participants.Participants;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Demultiplexes messages from the ingress stream to the appropriate domain handler.
 */
public class SbeDemuxer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SbeDemuxer.class);
    private final Participants participants;
    private final Auctions auctions;
    private final ClusterClientResponder responder;

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    private final AddParticipantCommandDecoder addParticipantDecoder = new AddParticipantCommandDecoder();
    private final AddAuctionBidCommandDecoder addAuctionBidDecoder = new AddAuctionBidCommandDecoder();
    private final CreateAuctionCommandDecoder createAuctionDecoder = new CreateAuctionCommandDecoder();
    private final ListAuctionsCommandDecoder listAuctionsDecoder = new ListAuctionsCommandDecoder();


    /**
     * Dispatches ingress messages to domain logic.
     *
     * @param participants          the participants domain model to which commands are dispatched
     * @param auctions              the auction domain model to which commands are dispatched
     * @param responder             the responder to which responses are sent
     */
    public SbeDemuxer(
        final Participants participants,
        final Auctions auctions,
        final ClusterClientResponder responder)
    {
        this.participants = participants;
        this.auctions = auctions;
        this.responder = responder;
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
                    addParticipantDecoder.correlationId(), addParticipantDecoder.name());
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
            case ListAuctionsCommandDecoder.TEMPLATE_ID ->
            {
                final List<Auction> auctionList = auctions.getAuctionList();
                responder.returnAuctionList(auctionList);
            }
            case ListParticipantsCommandDecoder.TEMPLATE_ID ->
            {
                final List<Participant> participantList = participants.getParticipantList();
                responder.returnParticipantList(participantList);
            }
            default -> LOGGER.error("Unknown message template {}, ignored.", headerDecoder.templateId());
        }
    }
}
