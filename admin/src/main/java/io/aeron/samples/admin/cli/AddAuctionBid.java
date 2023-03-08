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

import io.aeron.samples.cluster.admin.protocol.AddAuctionBidEncoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import picocli.CommandLine;

import static io.aeron.samples.admin.util.EnvironmentUtil.tryGetParticipantId;

/**
 * Adds an auction to the cluster
 */
@CommandLine.Command(name = "add-bid", mixinStandardHelpOptions = false,
    description = "Adds a bid to an auction in the cluster")
public class AddAuctionBid implements Runnable
{
    @CommandLine.ParentCommand
    CliCommands parent;

    @SuppressWarnings("all")
    @CommandLine.Option(names = "auction-id", description = "Auction ID")
    private Long auctionId = Long.MIN_VALUE;

    @SuppressWarnings("all")
    @CommandLine.Option(names = "created-by", description = "Created by participant id")
    private Integer participantId = tryGetParticipantId();

    @SuppressWarnings("all")
    @CommandLine.Option(names = "price", description = "Bid price")
    private Long price = 0L;

    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder(); //cluster protocol header
    private final AddAuctionBidEncoder addAuctionBidEncoder = new AddAuctionBidEncoder();
    /**
     * Determines if a participant should be added
     */
    public void run()
    {
        addAuctionBidEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        addAuctionBidEncoder.auctionId(auctionId);
        addAuctionBidEncoder.addedByParticipantId(participantId);
        addAuctionBidEncoder.price(price);
        parent.offerRingBufferMessage(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            addAuctionBidEncoder.encodedLength());
    }
}
