/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;

/**
 * The context for a single cluster session message
 */
public class SessionMessageContextImpl implements SessionMessageContext
{
    private ClientSessionEgress clientSessionEgress;
    private long timestamp;
    private ClientSession session;
    /**
     * Sets the egress to be used
     * @param clientSessionEgress the client session egress to be used
     */
    public void setClientSessionEgress(final ClientSessionEgress clientSessionEgress)
    {
        this.clientSessionEgress = clientSessionEgress;
    }

    /**
     * Sets the session context for this cluster message
     * @param session the session
     * @param timestamp the timestamp
     * @param header the header
     */
    public void setSessionContext(final ClientSession session, final long timestamp, final Header header)
    {
        this.timestamp = timestamp;
        this.session = session;
    }

    /**
     * Gets the current cluster time, as provided by the cluster
     * @return the cluster time at the time this message was received
     */
    @Override
    public long getClusterTime()
    {
        return timestamp;
    }
}
