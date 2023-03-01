/*
 * Copyright 2023 Adaptive Financial Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.aeron.samples.domain.participants;

import io.aeron.samples.infra.ClusterClientResponder;
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
    private final Long2ObjectHashMap<Participant> participantMap = new Long2ObjectHashMap<>();
    private final ClusterClientResponder clusterClientResponder;

    /**
     * Constructor
     * @param clusterClientResponder the cluster client responder
     */
    public Participants(final ClusterClientResponder clusterClientResponder)
    {
        this.clusterClientResponder = clusterClientResponder;
        addDefaultParticipants();
    }

    /**
     * Adds a participant to the cluster
     * @param participantId the id of the participant
     * @param name the name of the participant
     * @param correlationId the correlation id of the request
     */
    public void addParticipant(final long participantId, final String correlationId, final String name)
    {
        LOGGER.info("Adding participant {} with name {}", participantId, name);
        final var participant = new Participant(participantId, name);
        participantMap.put(participantId, participant);
        clusterClientResponder.acknowledgeParticipantAdded(participantId, correlationId);
    }

    /**
     * Restores a participant to the cluster
     * @param participantId the id of the participant
     * @param name the name of the participant
     */
    public void restoreParticipant(final long participantId, final String name)
    {
        LOGGER.info("Restoring participant {} with name {}", participantId, name);
        final var participant = new Participant(participantId, name);
        participantMap.put(participantId, participant);
    }

    /**
     * Lists all participants in the cluster, after sorting by participant id
     * @return the list of participants
     */
    public List<Participant> getParticipantList()
    {
        return participantMap
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
        return participantMap.containsKey(participantId);
    }

    /**
     * Adds the default participants to the cluster to ease simple demos
     */
    private void addDefaultParticipants()
    {
        final var initiator = new Participant(500, "initiator");
        participantMap.put(500, initiator);

        final var responder = new Participant(501, "responder");
        participantMap.put(501, responder);
    }
}
