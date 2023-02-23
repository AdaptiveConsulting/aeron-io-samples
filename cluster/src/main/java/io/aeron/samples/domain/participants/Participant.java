/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.participants;

/**
 * Represents a participant in the cluster
 * @param participantId the id of the participant
 * @param name the name of the participant
 */
public record Participant(long participantId, String name)
{
}
