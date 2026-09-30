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

  /*TODO si on veut pouvoir input une quantité d'arguments variables on peut pas faire comme ce qui suit
  *  en fait les index sont immuables si on veut l'écrire comme ça, donc ce qu'il faut faire à la place
  *  c'est enregistrer les arguments comme une liste, et ensuite seulement traiter la liste dans le call()
  *  ou alors, on peut mettre les arguments des images à merge à la fin, comme ça elles sont optionnelles
  *  et on peut alors en mettre combien on veut*/

  @CommandLine.Parameters(index = "0",
          description = "The source directory.")
  private File inputDirectory;

  @CommandLine.Parameters(index = "1",
          description = "The destination folder for the tiles.")
  private File outputDir;

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
      System.err.println("An error occurred during merging: " + e.getMessage());
      return 1;
    }
  }
}