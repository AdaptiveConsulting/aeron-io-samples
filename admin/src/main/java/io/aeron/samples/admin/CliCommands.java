/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.jline.reader.LineReader;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import java.io.PrintWriter;

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
        ConnectCluster.class})
public class CliCommands implements Runnable
{
    PrintWriter out;
    private OneToOneRingBuffer adminChannel;

    CliCommands()
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
     * Offers a message to the admin channel
     *
     * @param buffer        the buffer
     * @param offset        the offset
     * @param encodedLength the encoded length
     */
    public void offer(final ExpandableArrayBuffer buffer, final int offset, final int encodedLength)
    {
        adminChannel.write(1, buffer, offset, encodedLength);
    }
}
