/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domaininfra;

import io.aeron.sample.cluster.protocol.CreateAuctionCommandResultEncoder;
import io.aeron.sample.cluster.protocol.MessageHeaderEncoder;
import io.aeron.sample.cluster.protocol.NewAuctionEventEncoder;
import io.aeron.samples.domain.auctions.AddAuctionResult;
import io.aeron.samples.infra.SessionMessageContextImpl;
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
    public void onAuctionAdded(final long auctionId, final AddAuctionResult result, final long startTime,
        final long endTime, final String name, final String description)
    {
        messageHeaderEncoder.wrap(buffer, 0);

        createAuctionResultEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder)
            .auctionId(auctionId)
            .result(mapAddAuctionResult(result));

        context.reply(buffer, 0, createAuctionResultEncoder.encodedLength());

        newAuctionEventEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder)
            .auctionId(auctionId)
            .startTime(startTime)
            .endTime(endTime)
            .name(name)
            .description(description);

        context.broadcast(buffer, 0, newAuctionEventEncoder.encodedLength());
    }

    /**
     * Responds to the client that an auction has not been added with a result code
     * @param result the result code
     */
    @Override
    public void rejectAddAuction(final AddAuctionResult result)
    {
        messageHeaderEncoder.wrap(buffer, 0);
        createAuctionResultEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder)
            .auctionId(-1)
            .result(mapAddAuctionResult(result));
        context.reply(buffer, 0, createAuctionResultEncoder.encodedLength());
    }

    private io.aeron.sample.cluster.protocol.AddAuctionResult mapAddAuctionResult(final AddAuctionResult result)
    {
        switch (result)
        {
            case INVALID_START_TIME ->
            {
                return io.aeron.sample.cluster.protocol.AddAuctionResult.INVALID_START_TIME;
            }
            case SUCCESS ->
            {
                return io.aeron.sample.cluster.protocol.AddAuctionResult.SUCCESS;
            }
            case INVALID_END_TIME ->
            {
                return io.aeron.sample.cluster.protocol.AddAuctionResult.INVALID_END_TIME;
            }
            case UNKNOWN_PARTICIPANT ->
            {
                return io.aeron.sample.cluster.protocol.AddAuctionResult.UNKNOWN_PARTICIPANT;
            }
            case INVALID_NAME ->
            {
                return io.aeron.sample.cluster.protocol.AddAuctionResult.INVALID_NAME;
            }
            case INVALID_DESCRIPTION ->
            {
                return io.aeron.sample.cluster.protocol.AddAuctionResult.INVALID_DESCRIPTION;
            }
            default -> LOGGER.error("Unknown AddAuctionResult: {}", result);
        }
        return io.aeron.sample.cluster.protocol.AddAuctionResult.UNKNOWN;
    }

}
