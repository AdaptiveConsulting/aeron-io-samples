/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

/**
 * Represents the status of an auction in the cluster
 */
public enum AuctionStatus
{
    PRE_OPEN,
    OPEN,
    CLOSED,
    REMOVED
}
