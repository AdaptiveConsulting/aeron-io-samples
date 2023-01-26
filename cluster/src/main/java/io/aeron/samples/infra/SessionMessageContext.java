/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

/**
 * Interface to session context data
 */
public interface SessionMessageContext
{
    /**
     * Gets the cluster time
     * @return the cluster time at the time the message was written to log
     */
    long getClusterTime();
}
