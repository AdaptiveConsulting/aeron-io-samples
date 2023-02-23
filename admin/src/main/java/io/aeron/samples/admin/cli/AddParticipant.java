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
        addParticipantCommandEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        addParticipantCommandEncoder.participantId(participantId);
        addParticipantCommandEncoder.correlationId(UUID.randomUUID().toString());
        addParticipantCommandEncoder.name(participantName);
        parent.offerClusterMessage(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            addParticipantCommandEncoder.encodedLength());
    }

}
