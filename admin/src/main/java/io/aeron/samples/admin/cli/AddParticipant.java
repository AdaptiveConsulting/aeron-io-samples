/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cli;

import io.aeron.samples.cluster.protocol.AddParticipantCommandEncoder;
import io.aeron.samples.cluster.protocol.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import picocli.CommandLine;

import java.util.UUID;

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

    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder(); //cluster protocol header
    private final AddParticipantCommandEncoder addParticipantCommandEncoder = new AddParticipantCommandEncoder();
    /**
     * Determines if a participant should be added
     */
    public void run()
    {
        messageHeaderEncoder.wrap(buffer, 0);
        addParticipantCommandEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        addParticipantCommandEncoder.participantId(participantId);
        addParticipantCommandEncoder.correlationId(UUID.randomUUID().toString());
        addParticipantCommandEncoder.name(participantName);
        parent.offerClusterMessage(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            addParticipantCommandEncoder.encodedLength());
    }

}
