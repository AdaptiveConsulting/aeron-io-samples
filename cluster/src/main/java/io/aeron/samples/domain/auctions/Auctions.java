/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

import io.aeron.samples.infra.SessionMessageContext;

/**
 * Domain model for the auctions in the cluster
 */
public class Auctions
{
    private long nextAuctionId = 1;
    /**
     * Constructor
     * @param context the session message context
     */
    public Auctions(final SessionMessageContext context)
    {

    }

    /**
     * Creartes an auction
     * @param createdByParticipantId
     * @param startTime
     * @param endTime
     * @param name
     * @param description
     */
    public void createAuction(final long createdByParticipantId, final long startTime,
        final long endTime, final String name, final String description)
    {

    }

}
