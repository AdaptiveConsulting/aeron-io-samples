/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import org.agrona.DirectBuffer;

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

    /**
     * Replies to the caller
     * @param buffer the buffer to read data from
     * @param offset the offset to read from
     * @param length the length to read
     */
    void reply(DirectBuffer buffer, int offset, int length);

    /**
     * Broadcast a message to all connected sessions
     * @param buffer the buffer to read data from
     * @param offset the offset to read from
     * @param length the length to read
     */
    void broadcast(DirectBuffer buffer, int offset, int length);
}

