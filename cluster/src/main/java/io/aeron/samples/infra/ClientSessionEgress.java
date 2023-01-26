/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import org.agrona.collections.ObjectHashSet;

/**
 * Manages client sessions within the cluster
 */
public class ClientSessionEgress
{
    private final Cluster cluster;
    private final ObjectHashSet<ClientSession> allSessions = new ObjectHashSet<>();

    /**
     * Constructor
     * @param cluster the cluster to which the client sessions are connected
     */
    public ClientSessionEgress(final Cluster cluster)
    {
        this.cluster = cluster;
    }

    /**
     * Adds a client session
     * @param session the session to add
     */
    public void addSession(final ClientSession session)
    {
        //
    }

    /**
     * Removes a client session
     * @param session the session to remove
     */
    public void removeSession(final ClientSession session)
    {
        //
    }
}
