package dev.gregross.gig.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class SongCommandTest {

    @Test
    void songCommandRegisteredInCli() {
        CommandLine cli = new CommandLine(new GigCli());
        assertNotNull(cli.getSubcommands().get("song"),
            "GigCli should have 'song' subcommand");
    }

    @Test
    void songHasDumpSubcommand() {
        CommandLine cli = new CommandLine(new GigCli());
        CommandLine songCmd = cli.getSubcommands().get("song");
        assertNotNull(songCmd.getSubcommands().get("dump"),
            "song should have 'dump' subcommand");
    }

    @Test
    void songHasRebuildSubcommand() {
        CommandLine cli = new CommandLine(new GigCli());
        CommandLine songCmd = cli.getSubcommands().get("song");
        assertNotNull(songCmd.getSubcommands().get("rebuild"),
            "song should have 'rebuild' subcommand");
    }

    @Test
    void dumpHelpShowsOutputOption() {
        CommandLine cli = new CommandLine(new GigCli());
        StringWriter sw = new StringWriter();
        cli.setOut(new PrintWriter(sw));

        int exitCode = cli.execute("song", "dump", "--help");

        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("--output"), "dump help should mention --output option");
        assertTrue(output.contains("-o"), "dump help should mention -o shorthand");
    }

    @Test
    void rebuildHelpShowsFileParameter() {
        CommandLine cli = new CommandLine(new GigCli());
        StringWriter sw = new StringWriter();
        cli.setOut(new PrintWriter(sw));

        int exitCode = cli.execute("song", "rebuild", "--help");

        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("<filePath>"), "rebuild help should show filePath parameter");
    }

    @Test
    void rebuildRequiresFileArgument() {
        CommandLine cli = new CommandLine(new GigCli());
        StringWriter sw = new StringWriter();
        StringWriter errSw = new StringWriter();
        cli.setOut(new PrintWriter(sw));
        cli.setErr(new PrintWriter(errSw));

        int exitCode = cli.execute("song", "rebuild");

        assertNotEquals(0, exitCode, "rebuild without file should fail");
    }

    @Test
    void cliHasSevenSubcommands() {
        CommandLine cli = new CommandLine(new GigCli());
        // transport, track, device, note, snapshot, rpc, song
        assertEquals(7, cli.getSubcommands().size(),
            "GigCli should have 7 subcommands");
    }
}
