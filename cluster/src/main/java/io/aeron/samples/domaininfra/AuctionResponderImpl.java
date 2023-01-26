/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domaininfra;

import io.aeron.samples.domain.auctions.AddAuctionResult;
import io.aeron.samples.infra.SessionMessageContextImpl;

/**
 * Implementation of the {@link AuctionResponder} interface which returns SBE encoded results to the client
 */
public class AuctionResponderImpl implements AuctionResponder
{
    /**
     * Constructor
     * @param context the context to use in order to interact with clients
     */
    public AuctionResponderImpl(final SessionMessageContextImpl context)
    {

    }

    /**
     * Responds to the client that an auction has been added
     * @param auctionId the generated auction id
     * @param result the result code
     */
    @Override
    public void onAuctionAdded(final long auctionId, final AddAuctionResult result)
    {
    }

    /**
     * Responds to the client that an auction has not been added with a result code
     * @param result the result code
     */
    @Override
    public void rejectAddAuction(final AddAuctionResult result)
    {

    }
}
