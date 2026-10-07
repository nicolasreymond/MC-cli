package ch.heigvd.dai;

import ch.heigvd.dai.commandes.Merger;
import ch.heigvd.dai.commandes.Splitter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "mc-cli",
    subcommands = {Splitter.class, Merger.class},
    mixinStandardHelpOptions = true,
    version = "mc-cli 1.0-SNAPSHOT",
    description = "Split, merge and mosaic uncompressed 24-bit BMP images.")
public class Main {

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
