/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domaininfra;

import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import io.aeron.samples.domain.IdGenerators;
import io.aeron.samples.domain.auctions.Auctions;
import io.aeron.samples.domain.participants.Participants;
import io.aeron.samples.infra.ClientSessions;
import io.aeron.samples.infra.SessionMessageContextImpl;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The clustered service for the auction application.
 */
public class AppClusteredService implements ClusteredService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AppClusteredService.class);
    private final ClientSessions clientSessions = new ClientSessions();
    private final SessionMessageContextImpl context = new SessionMessageContextImpl(clientSessions);
    private final Participants participants = new Participants();
    private final IdGenerators idGenerators = new IdGenerators();
    private final AuctionResponder auctionResponder = new AuctionResponderImpl(context);
    private final Auctions auctions = new Auctions(context, participants, idGenerators, auctionResponder);
    private final SnapshotManager snapshotManager = new SnapshotManager(auctions, participants, idGenerators, context);
    private final TimerManager timerManager = new TimerManager(auctions);
    private final SbeDemuxer sbeDemuxer = new SbeDemuxer(participants, auctions);

    @Override
    public void onStart(final Cluster cluster, final Image snapshotImage)
    {
        snapshotManager.setIdleStrategy(cluster.idleStrategy());
        context.setIdleStrategy(cluster.idleStrategy());
        if (snapshotImage != null)
        {
            snapshotManager.loadSnapshot(snapshotImage);
        }
    }

    @Override
    public void onSessionOpen(final ClientSession session, final long timestamp)
    {
        context.setClusterTime(timestamp);
        clientSessions.addSession(session);
    }

    @Override
    public void onSessionClose(final ClientSession session, final long timestamp, final CloseReason closeReason)
    {
        context.setClusterTime(timestamp);
        clientSessions.removeSession(session);
    }

    @Override
    public void onSessionMessage(final ClientSession session, final long timestamp, final DirectBuffer buffer,
        final int offset, final int length, final Header header)
    {
        context.setSessionContext(session, timestamp);
        sbeDemuxer.dispatch(buffer, offset, length);
    }

    @Override
    public void onTimerEvent(final long correlationId, final long timestamp)
    {
        context.setClusterTime(timestamp);
        timerManager.onTimerEvent(correlationId, timestamp);
    }

    @Override
    public void onTakeSnapshot(final ExclusivePublication snapshotPublication)
    {
        snapshotManager.takeSnapshot(snapshotPublication);
    }

    @Override
    public void onRoleChange(final Cluster.Role newRole)
    {
        LOGGER.info("Role change: {}", newRole);
    }

    @Override
    public void onTerminate(final Cluster cluster)
    {
        LOGGER.info("Terminating");
    }
}
