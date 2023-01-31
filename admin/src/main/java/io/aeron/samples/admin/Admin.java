/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.cluster.ClusterConfig;
import org.agrona.concurrent.SystemEpochClock;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Admin client for the cluster main class, working on a direct connection to the cluster
 */
public class Admin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Admin.class);
    private static final int PORT_BASE = 9000;

    /**
     * Main method
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) throws IOException
    {
        final AdminClientEgressListener adminClient = new AdminClientEgressListener();

        final String ingressEndpoints = ingressEndpoints(Arrays.asList("localhost")); //todo accept config
        final Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
        try (
            MediaDriver mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));
            AeronCluster aeronCluster = AeronCluster.connect(
                new AeronCluster.Context()
                .egressListener(adminClient)
                .egressChannel("aeron:udp?endpoint=localhost:0")
                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                .ingressChannel("aeron:udp")
                .ingressEndpoints(ingressEndpoints));
            Terminal terminal = TerminalBuilder.builder().build())
        {
            //create a heart beat thread to keep the cluster alive
            final Thread t = new Thread()
            {
                long nextRun = Long.MIN_VALUE;

                @Override
                public void run()
                {
                    while (true)
                    {
                        final long now = SystemEpochClock.INSTANCE.time();
                        if (nextRun < now)
                        {
                            aeronCluster.sendKeepAlive();
                            nextRun = now + 250;
                        }
                    }
                }
            };
            t.start();

            final Parser parser = new DefaultParser();
            final ConfigurationPath configPath = new ConfigurationPath(workDir.get(), workDir.get());
            final Builtins builtins = new Builtins(workDir, configPath, null);
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");
            final CliCommands commands = new CliCommands();
            commands.setAdminClient(adminClient);
            final PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory();
            final CommandLine cmd = new CommandLine(commands, factory);
            final PicocliCommands picocliCommands = new PicocliCommands(cmd);
            final SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
            systemRegistry.setCommandRegistries(builtins, picocliCommands);
            final LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(systemRegistry.completer())
                .parser(parser)
                .variable(LineReader.LIST_MAX, 50)   // max tab completion candidates
                .build();
            builtins.setLineReader(reader);
            factory.setTerminal(terminal);
            adminClient.setAeronCluster(aeronCluster);
            adminClient.setTerminal(terminal);

            final String prompt = "admin > ";

            String line;
            terminal.writer().println("Welcome to the Aeron Cluster Admin Console");
            final String s = new AttributedStringBuilder().style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                .append("Cluster connected").toAnsi(terminal);
            terminal.writer().println(s);
            while (true)
            {
                try
                {
                    systemRegistry.cleanUp();
                    line = reader.readLine(prompt, null, (MaskingCallback)null, null);
                    systemRegistry.execute(line);
                }
                catch (final UserInterruptException e)
                {
                    // Ignore
                }
                catch (final EndOfFileException e)
                {
                    return;
                }
                catch (final Exception e)
                {
                    systemRegistry.trace(e);
                }
            }
        }
    }

    /**
     * Ingress endpoint configuration
     *
     * @param hostnames list of hostnames
     * @return ingress endpoint configuration
     */
    private static String ingressEndpoints(final List<String> hostnames)
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostnames.size(); i++)
        {
            sb.append(i).append('=');
            sb.append(hostnames.get(i)).append(':').append(calculatePort(i, 10));
            sb.append(',');
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static int calculatePort(final int nodeId, final int offset)
    {
        return PORT_BASE + (nodeId * ClusterConfig.PORTS_PER_NODE) + offset;
    }

    @CommandLine.Command(name = "",
        description = {
            "Example interactive shell with completion and autosuggestions. " +
                "Hit @|magenta <TAB>|@ to see available commands.",
            "Hit @|magenta ALT-S|@ to toggle tailtips.",
            ""},
        footer = {"", "Press Ctrl-D to exit."},
        subcommands = {
            AddParticipantTask.class, PicocliCommands.ClearScreen.class, CommandLine.HelpCommand.class})
    static class CliCommands implements Runnable
    {
        PrintWriter out;
        AdminClientEgressListener adminClient;

        CliCommands()
        {
        }

        public void setReader(final LineReader reader)
        {
            out = reader.getTerminal().writer();
        }

        public void run()
        {
            out.println(new CommandLine(this).getUsageMessage());
        }

        /**
         * Cluster interaction object
         *
         * @param adminClient the admin client
         */
        public void setAdminClient(final AdminClientEgressListener adminClient)
        {
            this.adminClient = adminClient;
        }
    }

    @CommandLine.Command(name = "add-participant", mixinStandardHelpOptions = false,
        description = "Adds a participant to the cluster")
    static class AddParticipantTask implements Runnable
    {
        @CommandLine.ParentCommand
        CliCommands parent;
        @SuppressWarnings("all")
        @CommandLine.Option(names = "id", description = "Participant id")
        private Integer participantId = -1;
        @SuppressWarnings("all")
        @CommandLine.Option(names = "name", description = "Participant name")
        private String participantName = "";

        /**
         * Determines if a participant should be added
         */
        public void run()
        {
            parent.adminClient.addParticipant(participantId, participantName);
        }


    }
}
