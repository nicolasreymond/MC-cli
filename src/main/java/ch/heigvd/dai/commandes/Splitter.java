package ch.heigvd.dai.commandes;

import ch.heigvd.dai.Main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import javax.imageio.ImageIO;

@CommandLine.Command(name = "split", description = "Split an image into a grid of tiles.")
public class Splitter implements Callable<Integer> {

  @CommandLine.ParentCommand protected Main parent;

  @CommandLine.Parameters(index = "0",
                          description = "The source image file.")
  private File inputFile;

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
      if (!inputFile.exists()) {
        System.err.println("Error: Input file does not exist.");
        return 1;
      }

      if (!outputDir.exists() && !outputDir.mkdirs()) { // fonctionne comme en c++? (ie. évalue l'un après l'autre)
        System.err.println("Error: Output folder not created correctly");
        return 1;
      }

      BufferedImage image = ImageIO.read(inputFile);
      if (image == null) {
        System.err.println("Error: Could not read the image.");
        return 1;
      }

      ImageIO.write(RenderedImage im, String formatName, File outputDir);
    }

    catch (Exception e) {
      System.err.println("An error occurred during splitting: " + e.getMessage());
      return 1;
    }

    return 0;
  }
}