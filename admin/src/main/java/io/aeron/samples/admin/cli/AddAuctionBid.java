/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cli;

import io.aeron.samples.cluster.protocol.AddAuctionBidCommandEncoder;
import io.aeron.samples.cluster.protocol.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import picocli.CommandLine;

import java.util.UUID;

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
    private Integer participantId = -1;

    @SuppressWarnings("all")
    @CommandLine.Option(names = "price", description = "Bid price")
    private Long price = 0L;

    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder(); //cluster protocol header
    private final AddAuctionBidCommandEncoder bidCommandEncoder = new AddAuctionBidCommandEncoder();
    /**
     * Determines if a participant should be added
     */
    public void run()
    {
        bidCommandEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        bidCommandEncoder.auctionId(auctionId);
        bidCommandEncoder.addedByParticipantId(participantId);
        bidCommandEncoder.price(price);
        bidCommandEncoder.correlationId(UUID.randomUUID().toString());
        parent.offerClusterMessage(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            bidCommandEncoder.encodedLength());
    }


}
