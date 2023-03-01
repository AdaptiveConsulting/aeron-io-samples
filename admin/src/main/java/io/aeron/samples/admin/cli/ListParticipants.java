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

import io.aeron.samples.cluster.admin.protocol.ListParticipantsEncoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import picocli.CommandLine;

/**
 * Adds an auction to the cluster
 */
@CommandLine.Command(name = "list-participants", mixinStandardHelpOptions = false,
    description = "Lists all participants in the cluster")
public class ListParticipants implements Runnable
{
    @CommandLine.ParentCommand
    CliCommands parent;
    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder(); //cluster protocol header
    private final ListParticipantsEncoder listCommandEncoder = new ListParticipantsEncoder();
    /**
     * Determines if a participant should be added
     */
    public void run()
    {
        listCommandEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        parent.offerRingBufferMessage(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            listCommandEncoder.encodedLength());
    }

}
