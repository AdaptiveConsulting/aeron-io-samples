/*
 * Copyright (c) 2023 Adaptive Financial Consulting
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
    description = "Adds an auction to the cluster, starting in 10 seconds and ending 45 seconds later")
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
        messageHeaderEncoder.wrap(buffer, 0);
        createAuctionCommandEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        createAuctionCommandEncoder.createdByParticipantId(participantId);
        createAuctionCommandEncoder.startTime(SystemEpochClock.INSTANCE.time() + TimeUnit.SECONDS.toMillis(10));
        createAuctionCommandEncoder.endTime(SystemEpochClock.INSTANCE.time() + TimeUnit.SECONDS.toMillis(55));
        createAuctionCommandEncoder.correlationId(UUID.randomUUID().toString());
        createAuctionCommandEncoder.name(auctionName);
        createAuctionCommandEncoder.description("Admin auction");
        parent.offerClusterMessage(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            createAuctionCommandEncoder.encodedLength());
    }


}
