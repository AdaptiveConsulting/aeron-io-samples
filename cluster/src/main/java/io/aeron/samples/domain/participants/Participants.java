/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.participants;

import org.agrona.collections.Long2ObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * Holds the participants in the cluster
 */
public class Participants
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Participants.class);
    private Long2ObjectHashMap<Participant> participants = new Long2ObjectHashMap<>();

    /**
     * Adds a participant to the cluster
     * @param participantId the id of the participant
     * @param name the name of the participant
     */
    public void addParticipant(final long participantId, final String name)
    {
        LOGGER.info("Adding participant {} with name {}", participantId, name);
        final var participant = new Participant(participantId, name);
        participants.put(participantId, participant);
    }

    /**
     * Lists all participants in the cluster, after sorting by participant id
     * @return the list of participants
     */
    public List<Participant> getParticipants()
    {
        return participants
            .values()
            .stream()
            .sorted(Comparator.comparingLong(Participant::participantId))
            .toList();
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
