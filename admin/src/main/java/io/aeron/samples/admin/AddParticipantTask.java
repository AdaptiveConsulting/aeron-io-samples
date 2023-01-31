/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;


import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Command line arguments for adding a participant
 */
@CommandLine.Command(name = "participant", mixinStandardHelpOptions = true,
    description = "Adds a participant to the cluster")
public class AddParticipantTask implements Callable<Integer>
{
    @SuppressWarnings("all")
    @CommandLine.Option(names = "-participant-id", description = "Participant id")
    private Integer participantId = -1;

    @SuppressWarnings("all")
    @CommandLine.Option(names = "-participant-name", description = "Participant name")
    private String participantName = "";

    private AdminClientEgressListener adminClient;

    /**
     * Determines if a participant should be added
     * @return zero.
     */
    public Integer call()
    {
        adminClient.addParticipant(participantId, participantName);
        return 0;
    }

    /**
     * Cluster interaction object
     * @param adminClient the admin client
     */
    public void setAdminClient(final AdminClientEgressListener adminClient)
    {
        this.adminClient = adminClient;
    }
}
