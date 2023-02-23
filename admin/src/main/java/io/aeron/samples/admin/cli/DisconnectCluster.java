/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cli;

import io.aeron.samples.cluster.admin.protocol.DisconnectClusterEncoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import picocli.CommandLine;

/**
 * Adds a participant to the cluster
 */
@CommandLine.Command(name = "disconnect", mixinStandardHelpOptions = false,
    description = "Disconnects from the cluster")
public class DisconnectCluster implements Runnable
{
    @CommandLine.ParentCommand
    CliCommands parent;

    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final DisconnectClusterEncoder disconnectClusterEncoder = new DisconnectClusterEncoder();

    /**
     * sends a disconnect cluster via the comms channel
     */
    public void run()
    {
        disconnectClusterEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        parent.offerClusterClientMessage(
            buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + disconnectClusterEncoder.encodedLength());
    }


}
