/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import picocli.CommandLine;

/**
 * Adds a participant to the cluster
 */
@CommandLine.Command(name = "add-participant", mixinStandardHelpOptions = false,
    description = "Adds a participant to the cluster")
public class AddParticipant implements Runnable
{
    @CommandLine.ParentCommand
    CliCommands parent;
    @SuppressWarnings("all")
    @CommandLine.Option(names = "id", description = "Participant id")
    private Integer participantId = -1;
    @SuppressWarnings("all")
    @CommandLine.Option(names = "name", description = "Participant name")
    private String participantName = "";

    /**
     * Determines if a participant should be added
     */
    public void run()
    {
        //    parent.adminClient.addParticipant(participantId, participantName);
    }


}
