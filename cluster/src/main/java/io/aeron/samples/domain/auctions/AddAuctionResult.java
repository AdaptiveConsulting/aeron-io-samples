/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

/**
 * Result of adding an auction
 */
public enum AddAuctionResult
{
    SUCCESS,
    INVALID_START_TIME,
    INVALID_END_TIME,
    INVALID_NAME,
    INVALID_DESCRIPTION,
    UNKNOWN_PARTICIPANT
}
