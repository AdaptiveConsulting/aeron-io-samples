/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

import java.util.HashMap;
import java.util.Map;

/**
 * REPL commands
 */
public class ReplCommands extends JlineCommandRegistry implements CommandRegistry
{
    private LineReader reader;

    /**
     * Constructor
     */
    public ReplCommands()
    {
        super();
        final Map<String, CommandMethods> commandExecute = new HashMap<>();
        commandExecute.put("clear", new CommandMethods(this::clear, this::defaultCompleter));
        registerCommands(commandExecute);
    }

    /**
     * Sets the line reader
     * @param reader the line reader
     */
    public void setLineReader(final LineReader reader)
    {
        this.reader = reader;
    }

    private Terminal terminal()
    {
        return reader.getTerminal();
    }

    private void clear(final CommandInput input)
    {
        final String[] usage = {
            "clear -  clear terminal",
            "Usage: clear",
            "  -? --help Displays command help"
        };
        try
        {
            parseOptions(usage, input.args());
            terminal().puts(InfoCmp.Capability.clear_screen);
            terminal().flush();
        }
        catch (final Exception e)
        {
            saveException(e);
        }
    }

}
