/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import io.aeron.samples.domain.auctions.AddAuctionBidResult;
import io.aeron.samples.domain.auctions.AddAuctionResult;
import io.aeron.samples.domain.auctions.AuctionStatus;

/**
 * Interface for responding to auction requests, encapsulating the SBE encoding and Aeron interactions
 */
public interface ClusterClientResponder
{

    /**
     * Responds to the client that an auction has been added with a result code and the auction id
     * and broadcasts the new auction to all clients
     * @param correlationId the correlation id for this request
     * @param auctionId the id of the auction
     * @param result the result code
     * @param startTime the start time of the auction
     * @param endTime the end time of the auction
     * @param name the name of the auction
     * @param description the description
     */
    void onAuctionAdded(
        String correlationId,
        long auctionId,
        AddAuctionResult result,
        long startTime,
        long endTime,
        String name,
        String description);

    /**
     * Responds to the client that an auction has not been added with a result code
     * @param correlationId the correlation id for this request
     * @param result the result code
     */
    void rejectAddAuction(String correlationId, AddAuctionResult result);

    /**
     * Responds to the client that a bid has been rejected with a result code and the auction id
     * @param correlationId the correlation id for the original request
     * @param auctionId the id of the auction provided in the original request
     * @param resultCode the result code
     */
    void rejectAddBid(String correlationId, long auctionId, AddAuctionBidResult resultCode);

    /**
     * Pushes an update to the state of an auction
     * @param correlationId the correlation id for the original request
     * @param auctionId the id of the auction
     * @param auctionStatus the status of the auction
     * @param currentPrice the current price of the auction
     * @param bidCount the number of bids
     * @param lastUpdateTime the time of the last update
     * @param winningParticipantId the id of the winning participant
     */
    void onAuctionUpdated(
        String correlationId,
        long auctionId,
        AuctionStatus auctionStatus,
        long currentPrice,
        int bidCount,
        long lastUpdateTime,
        long winningParticipantId);

    /**
     * Broadcasts an update for an auction once the state has been updated
     * @param auctionId the id of the auction
     * @param auctionStatus the status of the auction
     * @param currentPrice the current price of the auction
     * @param bidCount the number of bids
     * @param lastUpdateTime the time of the last update
     * @param winningParticipantId the id of the winning participant
     */
    void onAuctionStateUpdate(
        long auctionId,
        AuctionStatus auctionStatus,
        long currentPrice,
        int bidCount,
        long lastUpdateTime,
        long winningParticipantId);

    /**
     * Acknowledges that a participant has been added to the client using the correlation they provided
     * @param correlationId the correlation id provided by the client
     */
    void acknowledgeParticipantAdded(String correlationId);
}
