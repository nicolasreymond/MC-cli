package ch.heigvd.dai.commandes;

import ch.heigvd.dai.Main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;

import javax.imageio.ImageIO;

@CommandLine.Command(name = "merge", description = "Merge a grid of images into a single image.")
public class Merger implements Callable<Integer> {

  @CommandLine.ParentCommand protected Main parent;

  @CommandLine.Parameters(index = "0",
          description = "The source image file.")
  private File inputDirectory;

  @CommandLine.Parameters(index = "1",
          description = "The destination folder for the tiles.")
  private File outputDir;

  @CommandLine.Option(names = {"-r", "--rows"},
          description = "Number of rows",
          defaultValue = "2")
  private int rows;

  @CommandLine.Option(names = {"-c", "--cols"},
          description = "Number of columns",
          defaultValue = "2")
  private int cols;

  @Override
  public Integer call() {
    try {
      if (!inputDirectory.exists() || !inputDirectory.isDirectory()) {
        System.err.println("Error: Input files do not exist.");
        return 1;
      }

      BufferedImage image = ImageIO.read(firstImage);
      if (image == null) {
        System.err.println("Error: Could not read the image.");
        return 1;
      }
    }

    catch (Exception e) {
      System.err.println("An error occurred during splitting: " + e.getMessage());
      return 1;
    }

    return 0;
  }
}