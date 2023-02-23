/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cluster;

import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.logbuffer.Header;
import io.aeron.samples.cluster.protocol.AddAuctionBidCommandResultDecoder;
import io.aeron.samples.cluster.protocol.AddAuctionBidResult;
import io.aeron.samples.cluster.protocol.AddAuctionResult;
import io.aeron.samples.cluster.protocol.AddParticipantCommandResultDecoder;
import io.aeron.samples.cluster.protocol.AuctionListDecoder;
import io.aeron.samples.cluster.protocol.AuctionStatus;
import io.aeron.samples.cluster.protocol.AuctionUpdateEventDecoder;
import io.aeron.samples.cluster.protocol.CreateAuctionCommandResultDecoder;
import io.aeron.samples.cluster.protocol.MessageHeaderDecoder;
import io.aeron.samples.cluster.protocol.NewAuctionEventDecoder;
import io.aeron.samples.cluster.protocol.ParticipantListDecoder;
import org.agrona.DirectBuffer;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Admin client egress listener
 */
public class AdminClientEgressListener implements EgressListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminClientEgressListener.class);
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final AuctionUpdateEventDecoder auctionUpdateEventDecoder = new AuctionUpdateEventDecoder();
    private final AddParticipantCommandResultDecoder addParticipantDecoder = new AddParticipantCommandResultDecoder();
    private final CreateAuctionCommandResultDecoder createAuctionResultDecoder =
        new CreateAuctionCommandResultDecoder();
    private final NewAuctionEventDecoder newAuctionEventDecoder = new NewAuctionEventDecoder();
    private final AddAuctionBidCommandResultDecoder addBidResultDecoder = new AddAuctionBidCommandResultDecoder();
    private final AuctionListDecoder auctionListDecoder = new AuctionListDecoder();
    private final ParticipantListDecoder participantListDecoder = new ParticipantListDecoder();
    private LineReader lineReader;

    @Override
    public void onMessage(
        final long clusterSessionId,
        final long timestamp,
        final DirectBuffer buffer,
        final int offset,
        final int length,
        final Header header)
    {
        if (length < MessageHeaderDecoder.ENCODED_LENGTH)
        {
            LOGGER.warn("Message too short");
            return;
        }
        messageHeaderDecoder.wrap(buffer, offset);

        switch (messageHeaderDecoder.templateId())
        {
            case AddParticipantCommandResultDecoder.TEMPLATE_ID ->
            {
                addParticipantDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                addParticipantDecoder.correlationId();
                final long addedId = addParticipantDecoder.participantId();
                log("Participant added with id " + addedId, AttributedStyle.GREEN);
            }
            case CreateAuctionCommandResultDecoder.TEMPLATE_ID ->
            {
                createAuctionResultDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                if (createAuctionResultDecoder.result().equals(AddAuctionResult.SUCCESS))
                {
                    log("Auction added with ID: " + createAuctionResultDecoder.auctionId(),
                        AttributedStyle.GREEN);
                }
                else
                {
                    log("Auction rejected with reason: " + createAuctionResultDecoder.result().name(),
                        AttributedStyle.RED);
                }
            }
            case NewAuctionEventDecoder.TEMPLATE_ID ->
            {
                newAuctionEventDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                log("New auction event: " + newAuctionEventDecoder.auctionId() + " with name " +
                    newAuctionEventDecoder.name(), AttributedStyle.CYAN);
            }
            case AddAuctionBidCommandResultDecoder.TEMPLATE_ID ->
            {
                addBidResultDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                if (addBidResultDecoder.result().equals(AddAuctionBidResult.SUCCESS))
                {
                    log("Bid added to auction " + addBidResultDecoder.auctionId(), AttributedStyle.GREEN);
                }
                else
                {
                    log("Bid rejected with reason: " + addBidResultDecoder.result().name(), AttributedStyle.RED);
                }
            }
            case AuctionUpdateEventDecoder.TEMPLATE_ID -> displayAuctionUpdate(buffer, offset);
            case AuctionListDecoder.TEMPLATE_ID -> displayAuctions(buffer, offset);
            case ParticipantListDecoder.TEMPLATE_ID -> displayParticipants(buffer, offset);
            default -> log("unknown message type: " + messageHeaderDecoder.templateId(), AttributedStyle.RED);
        }
    }

    private void displayAuctionUpdate(final DirectBuffer buffer, final int offset)
    {
        auctionUpdateEventDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        final long auctionId = auctionUpdateEventDecoder.auctionId();
        final AuctionStatus auctionStatus = auctionUpdateEventDecoder.status();
        final int bidCount = auctionUpdateEventDecoder.bidCount();
        final long currentPrice = auctionUpdateEventDecoder.currentPrice();
        final long winningParticipantId = auctionUpdateEventDecoder.winningParticipantId();

        if (bidCount == 0)
        {
            log("Auction update event: " + auctionId + " now in state " +
                auctionStatus.name() + ". There have been " +
                auctionUpdateEventDecoder.bidCount() + " bids.", AttributedStyle.YELLOW);
        }
        else
        {
            log("Auction update event: " + auctionId + " now in state " +
                auctionStatus.name() + ". There have been " + bidCount + " bids. Current price is " +
                currentPrice + ". The winning bidder is " + winningParticipantId,
                AttributedStyle.YELLOW);
        }
    }

    private void displayParticipants(final DirectBuffer buffer, final int offset)
    {
        participantListDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        final ParticipantListDecoder.ParticipantsDecoder participants = participantListDecoder.participants();
        final int count = participants.count();
        if (0 == count)
        {
            log("No participants exist in the cluster.",
                AttributedStyle.YELLOW);
        }
        else
        {
            log("Participant count: " + count, AttributedStyle.YELLOW);
            while (participants.hasNext())
            {
                participants.next();
                final long participantId = participants.participantId();
                final String name = participants.name();
                log("Participant: " + participantId + " name: " + name, AttributedStyle.YELLOW);
            }
        }
    }

    private void displayAuctions(final DirectBuffer buffer, final int offset)
    {
        auctionListDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        final AuctionListDecoder.AuctionsDecoder auction = auctionListDecoder.auctions();
        final int count = auction.count();
        if (0 == count)
        {
            log("No auctions exist in the cluster. Closed auctions are deleted automatically.",
                AttributedStyle.YELLOW);
        }
        else
        {
            log("Auction count: " + count, AttributedStyle.YELLOW);
            while (auction.hasNext())
            {
                auction.next();

                final long auctionId = auction.auctionId();
                final long createdBy = auction.createdByParticipantId();
                final long startTime = auction.startTime();
                final long endTime = auction.endTime();
                final long winningParticipantId = auction.winningParticipantId();
                final long currentPrice = auction.currentPrice();
                final AuctionStatus status = auction.status();
                final String name = auction.name();

                log("Auction ID: " + auctionId + " with name " + name + " created by " + createdBy +
                    " in state " + status.name(), AttributedStyle.YELLOW);

                if (winningParticipantId != -1)
                {
                    log(" Current winning participant " + winningParticipantId + " with price " +
                        currentPrice, AttributedStyle.YELLOW);
                }
            }
        }
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
        if (code != EventCode.OK)
        {
            log("Session event: " + code.name() + " " + detail + ". leadershipTermId=" + leadershipTermId,
                AttributedStyle.YELLOW);
        }
    }

    @Override
    public void onNewLeader(
        final long clusterSessionId,
        final long leadershipTermId,
        final int leaderMemberId,
        final String ingressEndpoints)
    {
        log("New Leader: " + leaderMemberId + ". leadershipTermId=" + leadershipTermId, AttributedStyle.YELLOW);
    }

    /**
     * Sets the terminal
     *
     * @param lineReader the lineReader
     */
    public void setLineReader(final LineReader lineReader)
    {
        this.lineReader = lineReader;
    }

    /**
     * Logs a message to the terminal if available or to the logger if not
     *
     * @param message message to log
     * @param color   message color to use
     */
    private void log(final String message, final int color)
    {
        LineReaderHelper.log(lineReader, message, color);
    }
}
