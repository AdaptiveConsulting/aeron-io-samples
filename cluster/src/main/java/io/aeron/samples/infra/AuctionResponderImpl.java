/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import io.aeron.samples.cluster.protocol.AddAuctionBidCommandResultEncoder;
import io.aeron.samples.cluster.protocol.AuctionUpdateEventEncoder;
import io.aeron.samples.cluster.protocol.CreateAuctionCommandResultEncoder;
import io.aeron.samples.cluster.protocol.MessageHeaderEncoder;
import io.aeron.samples.cluster.protocol.NewAuctionEventEncoder;
import io.aeron.samples.domain.auctions.AddAuctionBidResult;
import io.aeron.samples.domain.auctions.AddAuctionResult;
import io.aeron.samples.domain.auctions.AuctionStatus;
import org.agrona.ExpandableDirectByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link AuctionResponder} interface which returns SBE encoded results to the client
 */
public class AuctionResponderImpl implements AuctionResponder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionResponderImpl.class);
    private final SessionMessageContextImpl context;
    private final CreateAuctionCommandResultEncoder createAuctionResultEncoder =
        new CreateAuctionCommandResultEncoder();
    private final NewAuctionEventEncoder newAuctionEventEncoder = new NewAuctionEventEncoder();
    private final AddAuctionBidCommandResultEncoder addAuctionBidResultEncoder =
        new AddAuctionBidCommandResultEncoder();

    private final AuctionUpdateEventEncoder auctionUpdateEncoder = new AuctionUpdateEventEncoder();
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(1024);

    /**
     * Constructor
     *
     * @param context the context to use in order to interact with clients
     */
    public AuctionResponderImpl(final SessionMessageContextImpl context)
    {
        this.context = context;
    }


    /**
     * Responds to the client that an auction has been added with a result code and the auction id
     * and broadcasts the new auction to all clients
     * @param auctionId the id of the auction
     * @param result the result code
     * @param startTime the start time of the auction
     * @param endTime the end time of the auction
     * @param name the name of the auction
     * @param description the description
     */
    @Override
    public void onAuctionAdded(final String correlationId, final long auctionId, final AddAuctionResult result,
        final long startTime, final long endTime, final String name, final String description)
    {
        messageHeaderEncoder.wrap(buffer, 0);

        createAuctionResultEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder)
            .auctionId(auctionId)
            .result(mapAddAuctionResult(result))
            .correlationId(correlationId);

        context.reply(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + createAuctionResultEncoder.encodedLength());

        newAuctionEventEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder)
            .auctionId(auctionId)
            .startTime(startTime)
            .endTime(endTime)
            .name(name)
            .description(description);

        context.broadcast(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + newAuctionEventEncoder.encodedLength());
    }

    /**
     * Responds to the client that an auction has not been added with a result code
     * @param result the result code
     */
    @Override
    public void rejectAddAuction(final String correlationId, final AddAuctionResult result)
    {
        messageHeaderEncoder.wrap(buffer, 0);
        createAuctionResultEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder)
            .auctionId(-1)
            .result(mapAddAuctionResult(result))
            .correlationId(correlationId);
        context.reply(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + createAuctionResultEncoder.encodedLength());
    }

    /***
     * Maps the domain bid rejection to the SBE AddAuctionBidCommandResult
     * @param correlationId the correlation id for the original request
     * @param auctionId the id of the auction provided in the original request
     * @param resultCode the result code
     */
    @Override
    public void rejectAddBid(final String correlationId, final long auctionId, final AddAuctionBidResult resultCode)
    {
        messageHeaderEncoder.wrap(buffer, 0);
        addAuctionBidResultEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        addAuctionBidResultEncoder.auctionId(auctionId);
        addAuctionBidResultEncoder.result(mapAddAuctionBidResult(resultCode));
        addAuctionBidResultEncoder.correlationId(correlationId);
        context.reply(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + addAuctionBidResultEncoder.encodedLength());
    }

    @Override
    public void onAuctionUpdated(
        final String correlationId,
        final long auctionId,
        final AuctionStatus auctionStatus,
        final long currentPrice,
        final int bidCount,
        final long lastUpdateTime,
        final long winningParticipantId)
    {
        messageHeaderEncoder.wrap(buffer, 0);
        addAuctionBidResultEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        addAuctionBidResultEncoder.auctionId(auctionId);
        addAuctionBidResultEncoder.result(mapAddAuctionBidResult(AddAuctionBidResult.SUCCESS));
        addAuctionBidResultEncoder.correlationId(correlationId);
        context.reply(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + addAuctionBidResultEncoder.encodedLength());

        onAuctionStateUpdate(auctionId, auctionStatus, currentPrice, bidCount, lastUpdateTime, winningParticipantId);
    }

    @Override
    public void onAuctionStateUpdate(
        final long auctionId,
        final AuctionStatus auctionStatus,
        final long currentPrice,
        final int bidCount,
        final long lastUpdateTime,
        final long winningParticipantId)
    {
        messageHeaderEncoder.wrap(buffer, 0);
        auctionUpdateEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        auctionUpdateEncoder.auctionId(auctionId);
        auctionUpdateEncoder.status(mapAuctionStatus(auctionStatus));
        auctionUpdateEncoder.currentPrice(currentPrice);
        auctionUpdateEncoder.bidCount(bidCount);
        auctionUpdateEncoder.lastUpdate(lastUpdateTime);
        auctionUpdateEncoder.winningParticipantId(winningParticipantId);
        context.broadcast(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + auctionUpdateEncoder.encodedLength());
    }

    private io.aeron.samples.cluster.protocol.AuctionStatus mapAuctionStatus(final AuctionStatus status)
    {
        switch (status)
        {
            case OPEN ->
            {
                return io.aeron.samples.cluster.protocol.AuctionStatus.OPEN;
            }
            case CLOSED ->
            {
                return io.aeron.samples.cluster.protocol.AuctionStatus.CLOSED;
            }
            case PRE_OPEN ->
            {
                return io.aeron.samples.cluster.protocol.AuctionStatus.PRE_OPEN;
            }
            default -> LOGGER.error("Unknown status {}", status);
        }

        return io.aeron.samples.cluster.protocol.AuctionStatus.UNKNOWN;
    }

    private io.aeron.samples.cluster.protocol.AddAuctionBidResult mapAddAuctionBidResult(
        final AddAuctionBidResult result)
    {
        switch (result)
        {
            case SUCCESS ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionBidResult.SUCCESS;
            }
            case PRICE_BELOW_CURRENT_WINNING_BID ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionBidResult.PRICE_BELOW_CURRENT_WINNING_BID;
            }
            case INVALID_PRICE ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionBidResult.INVALID_PRICE;
            }
            case UNKNOWN_AUCTION ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionBidResult.UNKNOWN_AUCTION;
            }
            case UNKNOWN_PARTICIPANT ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionBidResult.UNKNOWN_PARTICIPANT;
            }
            case AUCTION_NOT_OPEN ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionBidResult.AUCTION_NOT_OPEN;
            }
            case CANNOT_SELF_BID ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionBidResult.CANNOT_SELF_BID;
            }
            default -> LOGGER.error("Unknown AddAuctionBidResult: {}", result);
        }
        return io.aeron.samples.cluster.protocol.AddAuctionBidResult.UNKNOWN;
    }

    private io.aeron.samples.cluster.protocol.AddAuctionResult mapAddAuctionResult(final AddAuctionResult result)
    {
        switch (result)
        {
            case INVALID_START_TIME ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionResult.INVALID_START_TIME;
            }
            case SUCCESS ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionResult.SUCCESS;
            }
            case INVALID_END_TIME ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionResult.INVALID_END_TIME;
            }
            case UNKNOWN_PARTICIPANT ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionResult.UNKNOWN_PARTICIPANT;
            }
            case INVALID_NAME ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionResult.INVALID_NAME;
            }
            case INVALID_DESCRIPTION ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionResult.INVALID_DESCRIPTION;
            }
            case INVALID_DURATION ->
            {
                return io.aeron.samples.cluster.protocol.AddAuctionResult.INVALID_DURATION;
            }
            default -> LOGGER.error("Unknown AddAuctionResult: {}", result);
        }
        return io.aeron.samples.cluster.protocol.AddAuctionResult.UNKNOWN;
    }

}
