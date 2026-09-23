package ch.heigvd.commands;

import ch.heigvd.Main;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "merge", description = "Merge a grid of images into a single image.")
public class Merger implements Callable<Integer> {

  @CommandLine.ParentCommand protected Main parent;

  @CommandLine.Option(
      names = {"-g", "--greetings"},
      description = "The greetings to address the user.",
      defaultValue = "Hello")
  protected String greetings;

  @Override
  public Integer call() {
    System.out.println(greetings + " " + parent.getName() + "!");
    return 0;
  }
}