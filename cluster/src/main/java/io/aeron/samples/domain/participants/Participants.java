/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.participants;

import io.aeron.samples.infra.SessionMessageContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the participants in the cluster
 */
public class Participants
{
    /**
     * Constructor
     * @param context the session message context, used to send messages to participants
     */
    public Participants(final SessionMessageContext context)
    {

    }

    /**
     * Adds a participant to the cluster
     * @param participantId the id of the participant
     * @param name the name of the participant
     */
    public void addParticipant(final long participantId, final String name)
    {

    }

    /**
     * Lists all participants in the cluster
     * @return the list of participants
     */
    public List<Participant> getParticipants()
    {
        return new ArrayList<>();
    }
}
