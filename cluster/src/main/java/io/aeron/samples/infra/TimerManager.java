/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import io.aeron.cluster.service.Cluster;
import org.agrona.collections.Long2ObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Manages timers within the cluster
 */
public class TimerManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TimerManager.class);
    private static final int SCHEDULE_RETRY_LIMIT = 100;
    private final SessionMessageContextImpl context;
    private Cluster cluster;

    private final Long2ObjectHashMap<Runnable> correlationIdToRunnable = new Long2ObjectHashMap<>();

    private long correlationId = 0;

    /**
     * Constructor, accepting the context to update the cluster timestamp
     * @param context the context to update the cluster timestamp
     */
    public TimerManager(final SessionMessageContextImpl context)
    {
        this.context = context;
    }

    /**
     * Schedules a timer
     *
     * @param deadline the deadline of the timer
     * @param timerRunnable the timerRunnable to perform when the timer fires
     */
    public void scheduleTimer(final long deadline, final Runnable timerRunnable)
    {
        correlationId++;
        int count = 0;
        Objects.requireNonNull(cluster, "Cluster must be set before scheduling timers");
        correlationIdToRunnable.put(correlationId, timerRunnable);

        cluster.idleStrategy().reset();
        while (!cluster.scheduleTimer(correlationId, deadline))
        {
            cluster.idleStrategy().idle();
            count++;

            if (count > SCHEDULE_RETRY_LIMIT)
            {
                LOGGER.warn("Failed to schedule timer for deadline {}", deadline);
                break;
            }
        }
    }

    /**
     * Called when a timer cluster event occurs
     * @param correlationId the cluster timer id
     * @param timestamp     the timestamp the timer was fired at
     */
    public void onTimerEvent(final long correlationId, final long timestamp)
    {
        context.setClusterTime(timestamp);
        if (correlationIdToRunnable.containsKey(correlationId))
        {
            correlationIdToRunnable.get(correlationId).run();
            correlationIdToRunnable.remove(correlationId);
        }
        else
        {
            LOGGER.warn("Timer fired for unknown correlation id {}", correlationId);
        }
    }

    /***
     * Sets the cluster object used for scheduling timers
     * @param cluster the cluster object
     */
    public void setCluster(final Cluster cluster)
    {
        this.cluster = cluster;
    }
}
