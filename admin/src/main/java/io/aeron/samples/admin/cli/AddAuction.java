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

import io.aeron.samples.cluster.protocol.CreateAuctionCommandEncoder;
import io.aeron.samples.cluster.protocol.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.SystemEpochClock;
import picocli.CommandLine;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Adds an auction to the cluster
 */
@CommandLine.Command(name = "add-auction", mixinStandardHelpOptions = false,
    description = "Adds an auction to the cluster, starting in 0.1 seconds and ending 30 seconds later")
public class AddAuction implements Runnable
{
    @CommandLine.ParentCommand
    CliCommands parent;

    @SuppressWarnings("all")
    @CommandLine.Option(names = "name", description = "Auction name")
    private String auctionName = "New Auction";

    @SuppressWarnings("all")
    @CommandLine.Option(names = "created-by", description = "Created by participant id")
    private Integer participantId = -1;

    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder(); //cluster protocol header
    private final CreateAuctionCommandEncoder createAuctionCommandEncoder = new CreateAuctionCommandEncoder();
    /**
     * Determines if a participant should be added
     */
    public void run()
    {
        createAuctionCommandEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        createAuctionCommandEncoder.createdByParticipantId(participantId);
        createAuctionCommandEncoder.startTime(SystemEpochClock.INSTANCE.time() + TimeUnit.MILLISECONDS.toMillis(100));
        createAuctionCommandEncoder.endTime(SystemEpochClock.INSTANCE.time() + TimeUnit.SECONDS.toMillis(35));
        createAuctionCommandEncoder.correlationId(UUID.randomUUID().toString());
        createAuctionCommandEncoder.name(auctionName);
        createAuctionCommandEncoder.description("Admin auction");
        parent.offerClusterMessage(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            createAuctionCommandEncoder.encodedLength());
    }


}
