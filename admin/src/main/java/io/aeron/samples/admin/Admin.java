/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import io.aeron.samples.admin.cli.CliCommands;
import io.aeron.samples.admin.cluster.ClusterInteractionAgent;
import io.aeron.samples.cluster.admin.protocol.ConnectClusterEncoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderEncoder;
import org.agrona.CloseHelper;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
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
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static io.aeron.samples.admin.cluster.MessageTypes.CLUSTER_CLIENT_CONTROL;
import static io.aeron.samples.admin.util.ClusterConnectUtil.getThisHostName;
import static io.aeron.samples.admin.util.ClusterConnectUtil.tryGetClusterHostsFromEnv;
import static io.aeron.samples.admin.util.ClusterConnectUtil.tryGetDumbTerminalFromEnv;
import static io.aeron.samples.admin.util.ClusterConnectUtil.tryGetResponsePortFromEnv;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;

/**
 * Admin client for the cluster main class, working on a direct connection to the cluster
 */
public class Admin
{
    /**
     * Main method
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) throws IOException
    {
        //start the agent used for cluster interaction
        final String prompt = "admin > ";
        final AtomicBoolean running = new AtomicBoolean(true);
        final IdleStrategy idleStrategy = new SleepingMillisIdleStrategy();
        final UnsafeBuffer adminClusterBuffer = new UnsafeBuffer(ByteBuffer.allocate(8192 + TRAILER_LENGTH));
        final OneToOneRingBuffer adminClusterChannel = new OneToOneRingBuffer(adminClusterBuffer);

        final ClusterInteractionAgent clusterInteractionAgent = new ClusterInteractionAgent(adminClusterChannel,
            idleStrategy, running);
        final AgentRunner clusterInteractionAgentRunner = new AgentRunner(idleStrategy, Throwable::printStackTrace,
            null, clusterInteractionAgent);
        AgentRunner.startOnThread(clusterInteractionAgentRunner);

        final Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));

        //start the terminal REPL
        try (Terminal terminal = TerminalBuilder
            .builder()
            .dumb(tryGetDumbTerminalFromEnv())
            .build())
        {
            final Parser parser = new DefaultParser();
            final ConfigurationPath configPath = new ConfigurationPath(workDir.get(), workDir.get());
            final Builtins builtins = new Builtins(workDir, configPath, null);
            final CliCommands commands = new CliCommands();
            commands.setAdminChannel(adminClusterChannel);
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
            commands.setReader(reader);
            factory.setTerminal(terminal);
            clusterInteractionAgent.setLineReader(reader);

            String line;
            terminal.writer().println("-------------------------------------------------");
            terminal.writer().println("Welcome to the Aeron Cluster Sample Admin Console");
            terminal.writer().println("-------------------------------------------------");
            terminal.writer().println("Useful commands: help, exit");
            terminal.writer().println("");

            autoConnectCluster(adminClusterChannel, terminal.writer());

            while (running.get())
            {
                try
                {
                    systemRegistry.cleanUp();
                    line = reader.readLine(prompt, null, (MaskingCallback)null, null);
                    if ("exit".equalsIgnoreCase(line))
                    {
                        running.set(false);
                        CloseHelper.quietClose(clusterInteractionAgentRunner);
                    }
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
     * Auto-connect to the cluster with environment variables if AUTO_CONNECT is true.
     *
     * @param adminClusterChannel the channel to send the connect command to
     * @param writer
     */
    private static void autoConnectCluster(final OneToOneRingBuffer adminClusterChannel, final PrintWriter writer)
    {
        if (!Boolean.parseBoolean(System.getenv("AUTO_CONNECT")))
        {
            writer.println("Not auto-connecting to cluster");
            return;
        }
        final ConnectClusterEncoder connectClusterEncoder = new ConnectClusterEncoder();
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
        final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
        connectClusterEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        connectClusterEncoder.baseport(9000);
        connectClusterEncoder.port(tryGetResponsePortFromEnv());
        connectClusterEncoder.clusterHosts(tryGetClusterHostsFromEnv());
        connectClusterEncoder.localhostName(getThisHostName());
        adminClusterChannel.write(CLUSTER_CLIENT_CONTROL, buffer, 0,
            MessageHeaderEncoder.ENCODED_LENGTH + connectClusterEncoder.encodedLength());
    }

}
