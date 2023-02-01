/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cli;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.jline.reader.LineReader;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import java.io.PrintWriter;

import static io.aeron.samples.admin.cluster.MessageTypes.CLIENT_ONLY;
import static io.aeron.samples.admin.cluster.MessageTypes.CLUSTER_PASSTHROUGH;

/**
 * Cli Command parent
 */
@CommandLine.Command(name = "",
    description = {
        "Interactive shell. " +
            "Hit @|magenta <TAB>|@ to see available commands.",
        "Hit @|magenta ALT-S|@ to toggle tailtips.",
        ""},
    subcommands = {
        AddParticipant.class, PicocliCommands.ClearScreen.class, CommandLine.HelpCommand.class,
        ConnectCluster.class, DisconnectCluster.class, AddAuction.class})
public class CliCommands implements Runnable
{
    PrintWriter out;
    private OneToOneRingBuffer adminChannel;

    /**
     * Parent for all the commands
     */
    public CliCommands()
    {
    }

    /**
     * Sets the reader
     *
     * @param reader the reader
     */
    public void setReader(final LineReader reader)
    {
        out = reader.getTerminal().writer();
    }

    /**
     * Gets the usage of the commands
     */
    public void run()
    {
        out.println(new CommandLine(this).getUsageMessage());
    }

    /**
     * Cluster interaction object
     *
     * @param adminChannel the admin client
     */
    public void setAdminChannel(final OneToOneRingBuffer adminChannel)
    {
        this.adminChannel = adminChannel;
    }

    /**
     * Offers a message to the admin channel that will be passed straight to the cluster
     *
     * @param buffer        the buffer
     * @param offset        the offset
     * @param encodedLength the encoded length
     */
    public void offerClusterMessage(final ExpandableArrayBuffer buffer, final int offset, final int encodedLength)
    {
        adminChannel.write(CLUSTER_PASSTHROUGH, buffer, offset, encodedLength);
    }

    /**
     * Offers a message to the admin channel that will be processed in the cluster client
     *
     * @param buffer        the buffer
     * @param offset        the offset
     * @param encodedLength the encoded length
     */
    public void offerClusterClientMessage(final ExpandableArrayBuffer buffer, final int offset, final int encodedLength)
    {
        adminChannel.write(CLIENT_ONLY, buffer, offset, encodedLength);
    }
}
