/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain;

/**
 * Generates ids for the domain objects
 */
public class IdGenerators
{
    private long auctionId = 1;

    /**
     * Returns the next auction id after incrementing
     * @return an incremented auction id
     */
    public long incrementAndGetAuctionId()
    {
        return ++auctionId;
    }

    /**
     * Gets the current auction id without incrementing
     * @return the current auction id
     */
    public long getAuctionId()
    {
        return auctionId;
    }

    /**
     * Initializes the auction id, typically from a snapshot
     * @param auctionId the auction id to initialize with
     */
    public void initializeAuctionId(final long auctionId)
    {
        this.auctionId = auctionId;
    }

}
