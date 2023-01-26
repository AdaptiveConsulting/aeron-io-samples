/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.participants;

import org.agrona.collections.Long2ObjectHashMap;

import java.util.List;

/**
 * Holds the participants in the cluster
 */
public class Participants
{
    private Long2ObjectHashMap<Participant> participants = new Long2ObjectHashMap<>();

    /**
     * Adds a participant to the cluster
     * @param participantId the id of the participant
     * @param name the name of the participant
     */
    public void addParticipant(final long participantId, final String name)
    {
        final var participant = new Participant(participantId, name);
        participants.put(participantId, participant);
    }

    /**
     * Lists all participants in the cluster
     * @return the list of participants
     */
    public List<Participant> getParticipants()
    {
        return participants.values().stream().toList();
    }

    /**
     * Determines if a participant is known
     * @param participantId the id of the participant to check
     * @return true if known, false if not
     */
    public boolean isKnownParticipant(final long participantId)
    {
        return participants.containsKey(participantId);
    }
}
