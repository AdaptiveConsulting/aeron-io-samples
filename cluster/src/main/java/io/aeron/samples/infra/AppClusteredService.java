/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.infra;

import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import io.aeron.samples.domain.auctions.Auctions;
import io.aeron.samples.domain.participants.Participants;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The clustered service for the auction application.
 */
public class AppClusteredService implements ClusteredService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AppClusteredService.class);
    private final SessionMessageContext context = new SessionMessageContext();
    private final Participants participants = new Participants(context);
    private final Auctions auctions = new Auctions(context);
    private final SnapshotManager snapshotManager = new SnapshotManager(auctions, participants);
    private final TimerManager timerManager = new TimerManager(auctions);
    private final SbeDemuxer sbeDemuxer = new SbeDemuxer(context, participants, auctions);
    private ClientSessionEgress clientSessionEgress;

    @Override
    public void onStart(final Cluster cluster, final Image snapshotImage)
    {
        clientSessionEgress = new ClientSessionEgress(cluster);
        context.setClientSessionEgress(clientSessionEgress);
        snapshotManager.setIdleStrategy(cluster.idleStrategy());
        if (snapshotImage != null)
        {
            snapshotManager.loadSnapshot(snapshotImage);
        }
    }

    @Override
    public void onSessionOpen(final ClientSession session, final long timestamp)
    {
        clientSessionEgress.addSession(session);
    }

    @Override
    public void onSessionClose(final ClientSession session, final long timestamp, final CloseReason closeReason)
    {
        clientSessionEgress.removeSession(session);
    }

    @Override
    public void onSessionMessage(final ClientSession session, final long timestamp, final DirectBuffer buffer,
        final int offset, final int length, final Header header)
    {
        context.setSessionContext(session, timestamp, header);
        sbeDemuxer.dispatch(buffer, offset, length);
    }

    @Override
    public void onTimerEvent(final long correlationId, final long timestamp)
    {
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
