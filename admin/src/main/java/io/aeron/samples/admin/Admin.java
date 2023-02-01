/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import io.aeron.samples.admin.cli.CliCommands;
import io.aeron.samples.admin.cluster.ClusterInteractionAgent;
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
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

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
        final IdleStrategy idleStrategy = new SleepingMillisIdleStrategy();
        final UnsafeBuffer adminClusterBuffer = new UnsafeBuffer(ByteBuffer.allocate(8192 + TRAILER_LENGTH));
        final OneToOneRingBuffer adminClusterChannel = new OneToOneRingBuffer(adminClusterBuffer);

        final ClusterInteractionAgent clusterInteractionAgent = new ClusterInteractionAgent(adminClusterChannel);
        final AgentRunner clusterInteractionAgentRunner = new AgentRunner(idleStrategy, Throwable::printStackTrace,
            null, clusterInteractionAgent);
        AgentRunner.startOnThread(clusterInteractionAgentRunner);

        final Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));

        //start the terminal REPL
        try (Terminal terminal = TerminalBuilder.builder().build())
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

            final String prompt = "admin > ";

            String line;
            terminal.writer().println("-------------------------------------------------");
            terminal.writer().println("Welcome to the Aeron Cluster Sample Admin Console");
            terminal.writer().println("-------------------------------------------------");
            terminal.writer().println(" useful commands: help, exit");
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

}
