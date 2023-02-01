/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

import io.aeron.samples.domain.participants.Participants;
import io.aeron.samples.infra.AuctionResponder;
import io.aeron.samples.infra.SessionMessageContext;
import io.aeron.samples.infra.TimerManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuctionsTests
{
    private final SessionMessageContext sessionMessageContext = mock(SessionMessageContext.class);
    private final AuctionResponder auctionResponder = mock(AuctionResponder.class);
    private final Participants participants = mock(Participants.class);
    private final TimerManager timerManager = mock(TimerManager.class);

    @Test
    void testAuctionsCanBeAdded()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1002L, 31003L, "name", "description");

        verify(auctionResponder).onAuctionAdded(correlationId, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31003L,
            "name", "description");
        verify(timerManager).scheduleTimer(eq(1002L), any());
        verify(timerManager).scheduleTimer(eq(31003L), any());

        assertFalse(auctions.getAuctionList().isEmpty());
        assertEquals(4194304000L, auctions.getAuctionList().get(0).getAuctionId());
        assertEquals(1002L, auctions.getAuctionList().get(0).getStartTime());
        assertEquals(31003L, auctions.getAuctionList().get(0).getEndTime());
        assertEquals("name", auctions.getAuctionList().get(0).getName());
        assertEquals("description", auctions.getAuctionList().get(0).getDescription());
        assertEquals(0, auctions.getAuctionList().get(0).getBidCount());
        assertEquals(Long.MIN_VALUE, auctions.getAuctionList().get(0).getCurrentPrice());
        assertEquals(Long.MIN_VALUE, auctions.getAuctionList().get(0).getLastUpdateTime());
        assertEquals(Long.MIN_VALUE, auctions.getAuctionList().get(0).getWinningParticipantId());
        assertEquals(AuctionStatus.PRE_OPEN, auctions.getAuctionList().get(0).getAuctionStatus());
    }

    @Test
    void testAuctionBidCanBeAdded()
    {
        final String correlationId1 = UUID.randomUUID().toString();
        final String correlationId2 = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);
        when(participants.isKnownParticipant(1001L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId1, 1000L, 1002L, 31004L, "name", "description");

        verify(auctionResponder).onAuctionAdded(correlationId1, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31004L,
            "name", "description");
        verify(timerManager).scheduleTimer(eq(1002L), any());
        verify(timerManager).scheduleTimer(eq(31004L), any());

        when(sessionMessageContext.getClusterTime()).thenReturn(31003L);
        auctions.addBid(4194304000L, 1001L, 99L, correlationId2);

        verify(auctionResponder).onAuctionUpdated(correlationId2, 4194304000L, AuctionStatus.PRE_OPEN, 99L, 1, 31003L);
    }

    @Test
    void testAuctionsCanBeRehydratedAndAreSortedOnList()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);

        auctions.restoreAuction(2L, 1000L, 1003L, 1L, 31004L, 2L, 3L, "name1", "description1");
        auctions.restoreAuction(1L, 1000L, 1002L, 4L, 31003L, 5L, 6L, "name0", "description0");

        verifyNoInteractions(auctionResponder);
        verify(timerManager).restoreTimer(eq(1L), any());
        verify(timerManager).restoreTimer(eq(2L), any());
        verify(timerManager).restoreTimer(eq(3L), any());
        verify(timerManager).restoreTimer(eq(4L), any());
        verify(timerManager).restoreTimer(eq(5L), any());
        verify(timerManager).restoreTimer(eq(6L), any());

        assertFalse(auctions.getAuctionList().isEmpty());
        assertEquals(2L, auctions.getAuctionList().size());

        assertEquals(1L, auctions.getAuctionList().get(0).getAuctionId());
        assertEquals(1000L, auctions.getAuctionList().get(0).getCreatedByParticipantId());
        assertEquals(1002L, auctions.getAuctionList().get(0).getStartTime());
        assertEquals(31003L, auctions.getAuctionList().get(0).getEndTime());
        assertEquals("name0", auctions.getAuctionList().get(0).getName());
        assertEquals("description0", auctions.getAuctionList().get(0).getDescription());

        assertEquals(2L, auctions.getAuctionList().get(1).getAuctionId());
        assertEquals(1000L, auctions.getAuctionList().get(1).getCreatedByParticipantId());
        assertEquals(1003L, auctions.getAuctionList().get(1).getStartTime());
        assertEquals(31004L, auctions.getAuctionList().get(1).getEndTime());
        assertEquals("name1", auctions.getAuctionList().get(1).getName());
        assertEquals("description1", auctions.getAuctionList().get(1).getDescription());
    }

    @Test
    void testThatParticipantMustBeKnown()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(false);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1001L, 31002L, "name", "description");

        verify(auctionResponder).rejectAddAuction(correlationId, AddAuctionResult.UNKNOWN_PARTICIPANT);
    }

    @Test
    void testThatStartTimeMustBeAfterClusterTime()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 999L, 31002L, "name", "description");

        verify(auctionResponder).rejectAddAuction(correlationId, AddAuctionResult.INVALID_START_TIME);
    }

    @Test
    void testThatEndTimeMustBeAfterStartTime()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1002L, 999L, "name", "description");

        verify(auctionResponder).rejectAddAuction(correlationId, AddAuctionResult.INVALID_END_TIME);
    }

    @Test
    void testThatNameCannotBeNull()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1002L, 31003L, null, "description");

        verify(auctionResponder).rejectAddAuction(correlationId, AddAuctionResult.INVALID_NAME);
    }

    @Test
    void testThatDescriptionCannotBeNull()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1002L, 31003L, "name", null);

        verify(auctionResponder).rejectAddAuction(correlationId, AddAuctionResult.INVALID_DESCRIPTION);
    }

    @Test
    void testThatNameCannotBeBlank()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1002L, 31003L, "", "description");

        verify(auctionResponder).rejectAddAuction(correlationId, AddAuctionResult.INVALID_NAME);
    }

    @Test
    void testThatDescriptionCannotBeBlank()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1002L, 31003L, "name", "");

        verify(auctionResponder).rejectAddAuction(correlationId, AddAuctionResult.INVALID_DESCRIPTION);
    }

    @Test
    void testAuctionRejectedIfAuctionNotOpenYet()
    {
        final String correlationId1 = UUID.randomUUID().toString();
        final String correlationId2 = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);
        when(participants.isKnownParticipant(1001L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId1, 1000L, 1002L, 31004L, "name", "description");

        verify(auctionResponder).onAuctionAdded(correlationId1, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31004L,
            "name", "description");

        auctions.addBid(4194304000L, 1001L, 99L, correlationId2);

        verify(auctionResponder).rejectAddBid(correlationId2, 4194304000L, AddAuctionBidResult.AUCTION_NOT_OPEN);
    }

    @Test
    void testAuctionRejectedIfTimeAfterAuctionEnd()
    {
        final String correlationId1 = UUID.randomUUID().toString();
        final String correlationId2 = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);
        when(participants.isKnownParticipant(1001L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId1, 1000L, 1002L, 31004L, "name", "description");

        verify(auctionResponder).onAuctionAdded(correlationId1, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31004L,
            "name", "description");

        when(sessionMessageContext.getClusterTime()).thenReturn(31011L);
        auctions.addBid(4194304000L, 31011L, 99L, correlationId2);

        verify(auctionResponder).rejectAddBid(correlationId2, 4194304000L, AddAuctionBidResult.AUCTION_NOT_OPEN);
    }

    @Test
    void testAuctionRejectedIfAuctionUnknown()
    {
        final String correlationId1 = UUID.randomUUID().toString();
        final String correlationId2 = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);

        auctions.addBid(42L, 1001L, 99L, correlationId2);

        verify(auctionResponder).rejectAddBid(correlationId2, 42L, AddAuctionBidResult.UNKNOWN_AUCTION);
    }

    @Test
    void testAuctionRejectedIfBidderUnknown()
    {
        final String correlationId1 = UUID.randomUUID().toString();
        final String correlationId2 = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);
        when(participants.isKnownParticipant(1001L)).thenReturn(false);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId1, 1000L, 1002L, 31004L, "name", "description");

        verify(auctionResponder).onAuctionAdded(correlationId1, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31004L,
            "name", "description");

        when(sessionMessageContext.getClusterTime()).thenReturn(31003L);
        auctions.addBid(4194304000L, 1001L, 99L, correlationId2);

        verify(auctionResponder).rejectAddBid(correlationId2, 4194304000L, AddAuctionBidResult.UNKNOWN_PARTICIPANT);
    }

    @Test
    void testAuctionRejectedIfSelfBidding()
    {
        final String correlationId1 = UUID.randomUUID().toString();
        final String correlationId2 = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId1, 1000L, 1002L, 31004L, "name", "description");

        verify(auctionResponder).onAuctionAdded(correlationId1, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31004L,
            "name", "description");

        when(sessionMessageContext.getClusterTime()).thenReturn(31003L);
        auctions.addBid(4194304000L, 1000L, 99L, correlationId2);

        verify(auctionResponder).rejectAddBid(correlationId2, 4194304000L, AddAuctionBidResult.CANNOT_SELF_BID);
    }

    @Test
    void testAuctionRejectedIfNoPriceImprovement()
    {
        final String correlationId1 = UUID.randomUUID().toString();
        final String correlationId2 = UUID.randomUUID().toString();
        final String correlationId3 = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);
        when(participants.isKnownParticipant(1001L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId1, 1000L, 1002L, 31005L, "name", "description");

        //add first bid at 99L
        verify(auctionResponder).onAuctionAdded(correlationId1, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31005L,
            "name", "description");

        when(sessionMessageContext.getClusterTime()).thenReturn(31003L);
        auctions.addBid(4194304000L, 1001L, 99L, correlationId2);

        verify(auctionResponder).onAuctionUpdated(correlationId2, 4194304000L, AuctionStatus.PRE_OPEN, 99L, 1, 31003L);

        //add a second bid at 90L
        when(sessionMessageContext.getClusterTime()).thenReturn(31004L);
        auctions.addBid(4194304000L, 1001L, 90L, correlationId3);

        verify(auctionResponder).rejectAddBid(correlationId3, 4194304000L, AddAuctionBidResult.INVALID_PRICE);
    }

    @Test
    void testAuctionMovesThroughStatesCorrectly()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1002L, 31003L, "name", "description");

        verify(auctionResponder).onAuctionAdded(correlationId, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31003L,
            "name", "description");
        verify(timerManager).scheduleTimer(eq(1002L), any());
        verify(timerManager).scheduleTimer(eq(31003L), any());

        assertFalse(auctions.getAuctionList().isEmpty());
        assertEquals(4194304000L, auctions.getAuctionList().get(0).getAuctionId());
        assertEquals(1002L, auctions.getAuctionList().get(0).getStartTime());
        assertEquals(31003L, auctions.getAuctionList().get(0).getEndTime());
        assertEquals(AuctionStatus.PRE_OPEN, auctions.getAuctionList().get(0).getAuctionStatus());

        when(sessionMessageContext.getClusterTime()).thenReturn(31002L);
        auctions.openAuction(4194304000L);

        assertEquals(1, auctions.getAuctionList().size());
        assertEquals(AuctionStatus.OPEN, auctions.getAuctionList().get(0).getAuctionStatus());


        when(sessionMessageContext.getClusterTime()).thenReturn(31003L);
        auctions.closeAuction(4194304000L);

        assertEquals(1, auctions.getAuctionList().size());
        assertEquals(AuctionStatus.CLOSED, auctions.getAuctionList().get(0).getAuctionStatus());
    }

    @Test
    void testAuctionDoesNotMoveStateIfNotTimeToOpenYet()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1002L, 31003L, "name", "description");

        verify(auctionResponder).onAuctionAdded(correlationId, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31003L,
            "name", "description");
        verify(timerManager).scheduleTimer(eq(1002L), any());
        verify(timerManager).scheduleTimer(eq(31003L), any());

        assertFalse(auctions.getAuctionList().isEmpty());
        assertEquals(4194304000L, auctions.getAuctionList().get(0).getAuctionId());
        assertEquals(1002L, auctions.getAuctionList().get(0).getStartTime());
        assertEquals(31003L, auctions.getAuctionList().get(0).getEndTime());
        assertEquals(AuctionStatus.PRE_OPEN, auctions.getAuctionList().get(0).getAuctionStatus());

        when(sessionMessageContext.getClusterTime()).thenReturn(1001L);

        auctions.openAuction(4194304000L);

        assertEquals(1, auctions.getAuctionList().size());
        assertEquals(AuctionStatus.PRE_OPEN, auctions.getAuctionList().get(0).getAuctionStatus());


        auctions.closeAuction(4194304000L);

        assertEquals(1, auctions.getAuctionList().size());
        assertEquals(AuctionStatus.PRE_OPEN, auctions.getAuctionList().get(0).getAuctionStatus());
    }

    @Test
    void testAuctionDoesNotMoveStateIfNotLegal()
    {
        final String correlationId = UUID.randomUUID().toString();
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, auctionResponder, timerManager);
        auctions.addAuction(correlationId, 1000L, 1002L, 31003L, "name", "description");

        verify(auctionResponder).onAuctionAdded(correlationId, 4194304000L, AddAuctionResult.SUCCESS, 1002L, 31003L,
            "name", "description");
        verify(timerManager).scheduleTimer(eq(1002L), any());
        verify(timerManager).scheduleTimer(eq(31003L), any());

        assertFalse(auctions.getAuctionList().isEmpty());
        assertEquals(4194304000L, auctions.getAuctionList().get(0).getAuctionId());
        assertEquals(1002L, auctions.getAuctionList().get(0).getStartTime());
        assertEquals(31003L, auctions.getAuctionList().get(0).getEndTime());
        assertEquals(AuctionStatus.PRE_OPEN, auctions.getAuctionList().get(0).getAuctionStatus());

        when(sessionMessageContext.getClusterTime()).thenReturn(31003L);

        auctions.closeAuction(4194304000L);

        assertEquals(1, auctions.getAuctionList().size());
        assertEquals(AuctionStatus.PRE_OPEN, auctions.getAuctionList().get(0).getAuctionStatus());
    }
}
