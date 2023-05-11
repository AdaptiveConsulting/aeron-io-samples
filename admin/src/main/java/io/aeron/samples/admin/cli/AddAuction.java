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

import io.aeron.samples.cluster.admin.protocol.AddAuctionEncoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.SystemEpochClock;
import picocli.CommandLine;

import java.util.concurrent.TimeUnit;

import static io.aeron.samples.admin.util.EnvironmentUtil.tryGetParticipantId;

/**
 * Adds an auction to the cluster
 */
@CommandLine.Command(name = "add-auction", mixinStandardHelpOptions = false,
    description = "Adds an auction to the cluster, starting in 0.1 seconds and ending 25 seconds later")
public class AddAuction implements Runnable
{
    @CommandLine.ParentCommand
    CliCommands parent;

    @SuppressWarnings("all")
    @CommandLine.Option(names = "name", description = "Auction name")
    private String auctionName = "New Auction";

    @SuppressWarnings("all")
    @CommandLine.Option(names = "created-by", description = "Created by participant id")
    private Integer participantId = tryGetParticipantId();

    @SuppressWarnings("all")
    @CommandLine.Option(names = "duration", description = "Auction duration in seconds (default: 25 seconds)")
    private Integer duration = 25;

    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder(); //cluster protocol header
    private final AddAuctionEncoder addAuctionEncoder = new AddAuctionEncoder();
    /**
     * Determines if a participant should be added
     */
    public void run()
    {
        addAuctionEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        addAuctionEncoder.createdByParticipantId(participantId);
        addAuctionEncoder.startTime(SystemEpochClock.INSTANCE.time() + TimeUnit.MILLISECONDS.toMillis(100));
        addAuctionEncoder.endTime(SystemEpochClock.INSTANCE.time() + TimeUnit.SECONDS.toMillis(duration));
        addAuctionEncoder.name(auctionName);
        addAuctionEncoder.description("Admin auction");
        parent.offerRingBufferMessage(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            addAuctionEncoder.encodedLength());
    }


}
