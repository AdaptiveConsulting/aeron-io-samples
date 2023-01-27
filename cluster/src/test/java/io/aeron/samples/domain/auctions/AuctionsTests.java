/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

import io.aeron.samples.domain.IdGenerators;
import io.aeron.samples.domain.participants.Participants;
import io.aeron.samples.domaininfra.AuctionResponder;
import io.aeron.samples.infra.SessionMessageContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AuctionsTests
{
    private final SessionMessageContext sessionMessageContext = mock(SessionMessageContext.class);
    private final AuctionResponder auctionResponder = mock(AuctionResponder.class);
    private final Participants participants = mock(Participants.class);

    @Test
    public void testAuctionsCanBeAdded()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, new IdGenerators(), auctionResponder);
        auctions.addAuction(1000L, 1002L, 1003L, "name", "description");

        verify(auctionResponder).onAuctionAdded(1L, AddAuctionResult.SUCCESS, 1002L, 1003L, "name", "description");

        assertFalse(auctions.getAuctionList().isEmpty());
        assertEquals(1L, auctions.getAuctionList().get(0).auctionId());
        assertEquals(1002L, auctions.getAuctionList().get(0).startTime());
        assertEquals(1003L, auctions.getAuctionList().get(0).endTime());
        assertEquals("name", auctions.getAuctionList().get(0).name());
        assertEquals("description", auctions.getAuctionList().get(0).description());
    }

    @Test
    public void testAuctionsCanBeRehydratedAndAreSortedOnList()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, new IdGenerators(), auctionResponder);

        auctions.restoreAuction(2L, 1000L, 1003L, 1004L, "name1", "description1");
        auctions.restoreAuction(1L, 1000L, 1002L, 1003L, "name0", "description0");

        verifyNoInteractions(auctionResponder);

        assertFalse(auctions.getAuctionList().isEmpty());
        assertEquals(2L, auctions.getAuctionList().size());

        assertEquals(1L, auctions.getAuctionList().get(0).auctionId());
        assertEquals(1000L, auctions.getAuctionList().get(0).createdByParticipantId());
        assertEquals(1002L, auctions.getAuctionList().get(0).startTime());
        assertEquals(1003L, auctions.getAuctionList().get(0).endTime());
        assertEquals("name0", auctions.getAuctionList().get(0).name());
        assertEquals("description0", auctions.getAuctionList().get(0).description());

        assertEquals(2L, auctions.getAuctionList().get(1).auctionId());
        assertEquals(1000L, auctions.getAuctionList().get(1).createdByParticipantId());
        assertEquals(1003L, auctions.getAuctionList().get(1).startTime());
        assertEquals(1004L, auctions.getAuctionList().get(1).endTime());
        assertEquals("name1", auctions.getAuctionList().get(1).name());
        assertEquals("description1", auctions.getAuctionList().get(1).description());
    }

    @Test
    public void testThatParticipantMustBeKnown()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(false);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, new IdGenerators(), auctionResponder);
        auctions.addAuction(1000L, 1001L, 1002L, "name", "description");

        verify(auctionResponder).rejectAddAuction(AddAuctionResult.UNKNOWN_PARTICIPANT);
    }

    @Test
    public void testThatStartTimeMustBeAfterClusterTime()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, new IdGenerators(), auctionResponder);
        auctions.addAuction(1000L, 999L, 1002L, "name", "description");

        verify(auctionResponder).rejectAddAuction(AddAuctionResult.INVALID_START_TIME);
    }

    @Test
    public void testThatEndTimeMustBeAfterStartTime()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, new IdGenerators(), auctionResponder);
        auctions.addAuction(1000L, 1002L, 999L, "name", "description");

        verify(auctionResponder).rejectAddAuction(AddAuctionResult.INVALID_END_TIME);
    }

    @Test
    public void testThatNameCannotBeNull()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, new IdGenerators(), auctionResponder);
        auctions.addAuction(1000L, 1002L, 1003L, null, "description");

        verify(auctionResponder).rejectAddAuction(AddAuctionResult.INVALID_NAME);
    }

    @Test
    public void testThatDescriptionCannotBeNull()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, new IdGenerators(), auctionResponder);
        auctions.addAuction(1000L, 1002L, 1003L, "name", null);

        verify(auctionResponder).rejectAddAuction(AddAuctionResult.INVALID_DESCRIPTION);
    }

    @Test
    public void testThatNameCannotBeBlank()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, new IdGenerators(), auctionResponder);
        auctions.addAuction(1000L, 1002L, 1003L, "", "description");

        verify(auctionResponder).rejectAddAuction(AddAuctionResult.INVALID_NAME);
    }

    @Test
    public void testThatDescriptionCannotBeBlank()
    {
        when(sessionMessageContext.getClusterTime()).thenReturn(1000L);
        when(participants.isKnownParticipant(1000L)).thenReturn(true);

        final Auctions auctions =
            new Auctions(sessionMessageContext, participants, new IdGenerators(), auctionResponder);
        auctions.addAuction(1000L, 1002L, 1003L, "name", "");

        verify(auctionResponder).rejectAddAuction(AddAuctionResult.INVALID_DESCRIPTION);
    }
}
