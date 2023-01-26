/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

/**
 * Represents an auction in the cluster
 * @param auctionId the id of the auction
 * @param createdByParticipantId the id of the participant that created the auction
 * @param startTime the start time of the auction
 * @param endTime the end time of the auction
 * @param name the name of the auction
 * @param description the description of the auction
 */
public record Auction(long auctionId, long createdByParticipantId, long startTime, long endTime, String name,
    String description)
{
}
