package ch.heigvd.dai.commandes;

import ch.heigvd.dai.Main;
import ch.heigvd.dai.bmp.BmpImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List; // To merge a list of images
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

@Command(name = "merge",
         description = "Merge a grid of images into a single image.")
public class Merger implements Callable<Integer> {

  @ParentCommand protected Main parent;

  // Mutually exclusive inputs
  @ArgGroup(exclusive = true,
            multiplicity = "1")
  private InputSources inputSources;

  static class InputSources {
    @Parameters(paramLabel = "<files>",
                arity = "1..*",
                description = "Liste des images à fusionner (ex: img1.png img2.png).")
    List<File> files;

    @Option(names = {"-d", "--directory"},
            description = "Dossier contenant les images à fusionner.")
    File directory;
  }

  // Output file
  @Option(names = {"-o", "--output"},
          required = true,
          description = "Fichier d'image de sortie (ex: result.png).")
  private File outputFile;

  // Repeat a single image
  @Option(names = {"-r", "--repeat"},
          description = "Répète une image d'entrée N fois (utile avec un seul fichier en entrée).")
  private Integer repeat;

  // Mutually exclusive layouts
  @ArgGroup(exclusive = true,
            multiplicity = "0..1")
  private LayoutOptions layout;

  static class LayoutOptions {
    @Option(names = {"-v", "--vertical"},
            description = "Fusion verticale (1 colonne, N lignes).")
    boolean vertical;

    @Option(names = {"-h", "--horizontal"},
            description = "Fusion horizontale (1 ligne, N colonnes).")
    boolean horizontal;

    // Non-mutually exclusive subgroup to force the usage of both rows and columns
    @ArgGroup(exclusive = false)
    GridOptions grid;
  }

  static class GridOptions {
    @Option(names = {"--rows"},
            required = true,
            description = "Nombre de lignes de la grille finale.")
    int rows;

    @Option(names = {"--cols"},
            required = true,
            description = "Nombre de colonnes de la grille finale.")
    int cols;
  }

  @Override
  public Integer call() {

    if (repeat != null && (inputSources.files == null || inputSources.files.size() != 1)) {
      System.err.println("Error : The --repeat option can only be used when the input is a single image");
      return 1;
    }

    List<File> imagesToProcess;
    if (inputSources.directory != null) {
      // Lists the files in the directory
      System.out.println("Lecture depuis le dossier : " + inputSources.directory);
    } else {
      imagesToProcess = inputSources.files;
      System.out.println("Lecture des fichiers : " + imagesToProcess);
    }

    // Find out the final layout
    if (layout != null) {
      if (layout.vertical) System.out.println("Mode : Vertical");
      else if (layout.horizontal) System.out.println("Mode : Horizontal");
      else if (layout.grid != null)
        System.out.printf("Mode : Grille (%d lignes, %d colonnes)%n", layout.grid.rows, layout.grid.cols);
    } else System.out.println("Mode par défaut (déduction automatique via les noms de fichiers)");


    return 0;
  }
}
