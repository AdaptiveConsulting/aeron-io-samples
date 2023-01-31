/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

/**
 * Admin REPL
 */
public class AdminRepl
{
    /**
     * Main method
     * @param args command line arguments
     * @throws IOException if an I/O error occurs
     */
    public static void main(final String[] args) throws IOException
    {
        final Terminal terminal = TerminalBuilder.builder().build();
        if (terminal.getWidth() == 0 || terminal.getHeight() == 0)
        {
            if (terminal.getWidth() == 0 || terminal.getHeight() == 0)
            {
                terminal.setSize(new Size(120, 40));   // hard coded terminal size when redirecting
            }
            final Thread executeThread = Thread.currentThread();
            terminal.handle(Terminal.Signal.INT, signal -> executeThread.interrupt());
            final ReplCommands commands = new ReplCommands();
            final DefaultParser parser = new DefaultParser();
            parser.setEofOnUnclosedBracket(DefaultParser.Bracket.CURLY, DefaultParser.Bracket.ROUND,
                DefaultParser.Bracket.SQUARE);
            parser.setEofOnUnclosedQuote(true);
            parser.setRegexCommand("[:]{0,1}[a-zA-Z!]{1,}\\S*");    // change default regex to support shell commands
            parser.blockCommentDelims(new DefaultParser.BlockCommentDelims("/*", "*/"))
                .lineCommentDelims(new String[]{"//"});
            final LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(NullCompleter.INSTANCE)
                .highlighter(new DefaultHighlighter())
                .parser(parser)
                .build();

            commands.setLineReader(reader);

        }
    }
}
