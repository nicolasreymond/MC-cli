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

      // TODO(bmp-io): remplacer ImageIO.read(...) par
      // ch.heigvd.dai.bmp.BmpImage.readFrom(...) -- meme raison que dans
      // Splitter : le sujet impose java.io pour ouvrir/lire/ecrire les
      // fichiers (d'autres libs restent OK pour le traitement ensuite).
      //
      // FIXME(aymeric): "firstImage" n'est jamais defini, ce qui bloquait
      // la compilation de tout le module (donc aussi split). Bloc
      // commente en attendant l'implementation reelle -- ne pas oublier
      // de la reactiver/reecrire.
      // BufferedImage image = ImageIO.read(firstImage);
      // if (image == null) {
      //   System.err.println("Error: Could not read the image.");
      //   return 1;
      // }
      System.err.println("Error: merge is not implemented yet.");
      return 1;
    }

    catch (Exception e) {
      System.err.println("An error occurred during splitting: " + e.getMessage());
      return 1;
    }
  }
}