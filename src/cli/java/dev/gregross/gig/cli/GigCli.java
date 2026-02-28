package dev.gregross.gig.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "gig",
    description = "CLI for Gig Maestro — control Bitwig Studio via JSON-RPC.",
    mixinStandardHelpOptions = true,
    version = "gig-cli 0.3.0",
    subcommands = {
        TransportCommand.class,
        TrackCommand.class,
        DeviceCommand.class,
        NoteCommand.class,
        SnapshotCommand.class,
        RpcCommand.class
    }
)
public class GigCli {

    @Option(names = {"--host"}, description = "Server host (default: localhost)", defaultValue = "localhost")
    String host;

    @Option(names = {"--port"}, description = "Server port (default: 8787)", defaultValue = "8787")
    int port;

    @Option(names = {"--pretty"}, description = "Pretty-print JSON output")
    boolean pretty;

    RpcClient createClient() {
        return new RpcClient(host, port);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new GigCli()).execute(args);
        System.exit(exitCode);
    }
}
