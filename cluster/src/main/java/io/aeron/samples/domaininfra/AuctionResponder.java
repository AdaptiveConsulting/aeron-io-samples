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
     * Responds to the client that an auction has been added
     * @param auctionId the generated auction id
     * @param result the result code
     */
    void onAuctionAdded(long auctionId, AddAuctionResult result);

    /**
     * Responds to the client that an auction has not been added with a result code
     * @param result the result code
     */
    void rejectAddAuction(AddAuctionResult result);
}
