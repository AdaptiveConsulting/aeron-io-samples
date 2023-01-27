/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domaininfra;

import io.aeron.samples.domain.auctions.AddAuctionResult;

/**
 * Interface for responding to auction requests, encapsulating the SBE encoding and Aeron interactions
 */
public interface AuctionResponder
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
    void onAuctionAdded(String correlationId, long auctionId, AddAuctionResult result, long startTime,
        long endTime, String name, String description);

    /**
     * Responds to the client that an auction has not been added with a result code
     * @param correlationId the correlation id for this request
     * @param result the result code
     */
    void rejectAddAuction(String correlationId, AddAuctionResult result);
}
