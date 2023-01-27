/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

/**
 * Result of adding an auction
 */
public enum AddAuctionBidResult
{
    SUCCESS,
    PRICE_BELOW_CURRENT_WINNING_BID,
    INVALID_PRICE,
    UNKNOWN_AUCTION,
    UNKNOWN_PARTICIPANT,
    AUCTION_NOT_OPEN, CANNOT_SELF_BID
}
