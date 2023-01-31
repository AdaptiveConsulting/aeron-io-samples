/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;


import picocli.CommandLine;

/**
 * Command line arguments for adding a participant
 */
@CommandLine.Command(name = "participant", mixinStandardHelpOptions = true,
    description = "Adds a participant to the cluster")
class AddParticipantTask implements Runnable
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
     */
    public void run()
    {
        adminClient.addParticipant(participantId, participantName);
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
