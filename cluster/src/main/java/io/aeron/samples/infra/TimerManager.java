/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import io.aeron.samples.domain.auctions.Auctions;

/**
 * Manages timers within the cluster
 */
public class TimerManager
{
    /**
     * Constructor
     * @param auctions the auction domain model to interact with on timer events
     */
    public TimerManager(final Auctions auctions)
    {

    }

    /**
     * Called when a timer cluster event occurs
     * @param correlationId the cluster timer id
     * @param timestamp the timestamp the timer was fired at
     */
    public void onTimerEvent(final long correlationId, final long timestamp)
    {

    }
}
