/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

/**
 * Represents an auction in the cluster
 */
public class Auction
{
    private final long auctionId;
    private final long createdByParticipantId;
    private final long startTime;
    private final long endTime;
    private final String name;
    private final String description;
    private long currentPrice = 0;
    private long winningParticipantId = Long.MIN_VALUE;
    private long lastUpdateTime = Long.MIN_VALUE;
    private int bidCount = 0;
    private AuctionStatus auctionStatus = AuctionStatus.PRE_OPEN;

    private long startTimerCorrelationId = Long.MIN_VALUE;
    private long endTimerCorrelationId = Long.MIN_VALUE;
    private long removalTimerCorrelationId = Long.MIN_VALUE;

    /**
     * Constructor
     * @param auctionId the id of the auction
     * @param createdByParticipantId the id of the participant that created the auction
     * @param startTime the start time of the auction
     * @param endTime the end time of the auction
     * @param name the name of the auction
     * @param description the description of the auction
     * @param winningParticipantId the id of the winning participant
     */
    public Auction(final long auctionId, final long createdByParticipantId, final long startTime, final long endTime,
        final String name, final String description, final long winningParticipantId)
    {
        this.auctionId = auctionId;
        this.createdByParticipantId = createdByParticipantId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.name = name;
        this.description = description;
        this.winningParticipantId = winningParticipantId;
    }

    /**
     * Returns the current price for this auction
     * @return the current price
     */
    public long currentPrice()
    {
        return currentPrice;
    }

    /**
     * Returns the auction id
     * @return the auction id
     */
    public long getAuctionId()
    {
        return auctionId;
    }

    /**
     * Returns the participant id of the participant that created this auction
     * @return the participant id
     */
    public long getCreatedByParticipantId()
    {
        return createdByParticipantId;
    }

    /**
     * Returns the start time of the auction
     * @return the start time
     */
    public long getStartTime()
    {
        return startTime;
    }

    /**
     * Returns the end time of the auction
     * @return the end time
     */
    public long getEndTime()
    {
        return endTime;
    }

    /**
     * Returns the name of the auction
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the description of the auction
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Returns the current price
     * @return the current price
     */
    public long getCurrentPrice()
    {
        return currentPrice;
    }

    /**
     * Returns the participant id of the winning participant
     * @return the participant id
     */
    public long getWinningParticipantId()
    {
        return winningParticipantId;
    }

    /**
     * Sets the current price and winning participant for this auction
     * It is assumed that the bid is rejected as invalid withing Auctions before the auction is updated
     * @param participantId the participant id
     * @param price the price
     * @param time the time the bid was added
     */
    public void setWinningBid(final long participantId, final long price, final long time)
    {
        winningParticipantId = participantId;
        currentPrice = price;
        lastUpdateTime = time;
        bidCount++;
    }

    /**
     * The cluster time at which the winning bid was added
     * @return the time
     */
    public long getLastUpdateTime()
    {
        return lastUpdateTime;
    }

    /**
     * Gets the number of bids for this auction
     * @return the number of bids
     */
    public int getBidCount()
    {
        return bidCount;
    }

    /**
     * Gets the current auction status
     * @return the auction status
     */
    public AuctionStatus getAuctionStatus()
    {
        return auctionStatus;
    }

    /**
     * Sets the auction status to the new value
     * @param newStatus the new auction status
     */
    public void setAuctionStatus(final AuctionStatus newStatus)
    {
        auctionStatus = newStatus;
    }

    /**
     * Gets the timerCorrelationId for the start timer
     * @return the timerCorrelationId
     */
    public long getStartTimerCorrelationId()
    {
        return startTimerCorrelationId;
    }

    /**
     * Sets the timerCorrelationId for the start timer
     * @param startTimerCorrelationId the timerCorrelationId
     */
    public void setStartTimerCorrelationId(final long startTimerCorrelationId)
    {
        this.startTimerCorrelationId = startTimerCorrelationId;
    }

    /**
     * Gets the timerCorrelationId for the end timer
     * @return the timerCorrelationId
     */
    public long getEndTimerCorrelationId()
    {
        return endTimerCorrelationId;
    }

    /**
     * Sets the timerCorrelationId for the end timer
     * @param endTimerCorrelationId the timerCorrelationId
     */
    public void setEndTimerCorrelationId(final long endTimerCorrelationId)
    {
        this.endTimerCorrelationId = endTimerCorrelationId;
    }

    /**
     * Gets the timerCorrelationId for the removal timer
     * @return the timerCorrelationId
     */
    public long getRemovalTimerCorrelationId()
    {
        return removalTimerCorrelationId;
    }

    /**
     * Sets the timerCorrelationId for the removal timer
     * @param removalTimerCorrelationId the timerCorrelationId
     */
    public void setRemovalTimerCorrelationId(final long removalTimerCorrelationId)
    {
        this.removalTimerCorrelationId = removalTimerCorrelationId;
    }
}
