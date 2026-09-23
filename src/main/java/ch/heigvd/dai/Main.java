package ch.heigvd.dai;

import ch.heigvd.dai.commandes.Merger;
import ch.heigvd.dai.commandes.Splitter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

// TODO: options/description a affiner ; ajouter Mosaic une fois ecrite
// (ch.heigvd.dai.commandes.Mosaic) a la liste des subcommands.
@Command(
    name = "mc-cli",
    subcommands = {Splitter.class, Merger.class},
    description = "Split, merge and mosaic BMP images.")
public class Main {

  @Option(
      names = {"-n", "--name"},
      description = "Name to greet.",
      defaultValue = "world")
  protected String name;

  public String getName() {
    return name;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
