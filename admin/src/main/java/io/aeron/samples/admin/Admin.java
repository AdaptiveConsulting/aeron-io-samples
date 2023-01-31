/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.cluster.ClusterConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import java.io.IOException;
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
        final AddParticipantTask addParticipant = new AddParticipantTask();
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
            final Parser parser = new DefaultParser();
            final ConfigurationPath configPath = new ConfigurationPath(workDir.get(), workDir.get());
            final Builtins builtins = new Builtins(workDir, configPath, null);
            builtins.rename(Builtins.Command.TTOP, "top");
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");
            final AddParticipantTask commands = new AddParticipantTask();

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
            addParticipant.setAdminClient(adminClient);

            final String prompt = "admin> ";

            String line;
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
}
