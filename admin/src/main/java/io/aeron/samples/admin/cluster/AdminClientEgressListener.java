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
import io.aeron.samples.cluster.protocol.AuctionUpdateEventDecoder;
import io.aeron.samples.cluster.protocol.CreateAuctionCommandResultDecoder;
import io.aeron.samples.cluster.protocol.MessageHeaderDecoder;
import io.aeron.samples.cluster.protocol.NewAuctionEventDecoder;
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
    private final CreateAuctionCommandResultDecoder createAuctionResultDecoder =
        new CreateAuctionCommandResultDecoder();
    private final NewAuctionEventDecoder newAuctionEventDecoder = new NewAuctionEventDecoder();
    private final AddAuctionBidCommandResultDecoder addBidResultDecoder = new AddAuctionBidCommandResultDecoder();
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
            case AuctionUpdateEventDecoder.TEMPLATE_ID ->
            {
                auctionUpdateEventDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                log("Auction update event: " + auctionUpdateEventDecoder.auctionId() + " now in state " +
                    auctionUpdateEventDecoder.status().name() + ". There have been " +
                    auctionUpdateEventDecoder.bidCount() + " bids. The current price is " +
                    auctionUpdateEventDecoder.currentPrice(), AttributedStyle.YELLOW);
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
            default ->
            {
                log("unknown message type: " + messageHeaderDecoder.templateId(), AttributedStyle.RED);
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
        LOGGER.info("Received session event {} for session {} with detail {}", code, clusterSessionId, detail);
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
     * @param color message color to use
     */
    private void log(final String message, final int color)
    {
        LineReaderHelper.log(lineReader, message, color);
    }
}
