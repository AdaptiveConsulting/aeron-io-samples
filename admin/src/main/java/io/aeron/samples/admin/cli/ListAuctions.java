/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cli;

import io.aeron.samples.cluster.protocol.ListAuctionsCommandEncoder;
import io.aeron.samples.cluster.protocol.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import picocli.CommandLine;

/**
 * Adds an auction to the cluster
 */
@CommandLine.Command(name = "list-auctions", mixinStandardHelpOptions = false,
    description = "Lists all auctions in the cluster")
public class ListAuctions implements Runnable
{
    @CommandLine.ParentCommand
    CliCommands parent;
    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder(); //cluster protocol header
    private final ListAuctionsCommandEncoder listAuctionsCommandEncoder = new ListAuctionsCommandEncoder();
    /**
     * Determines if a participant should be added
     */
    public void run()
    {
        listAuctionsCommandEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        parent.offerClusterMessage(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH +
            listAuctionsCommandEncoder.encodedLength());
    }

}
