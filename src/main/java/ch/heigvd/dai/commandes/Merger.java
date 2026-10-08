package ch.heigvd.dai.commandes;

import ch.heigvd.dai.Main;
import ch.heigvd.dai.bmp.BmpImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
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
  @Option(names = {"-R", "--Repeat"},
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
    @Option(names = {"-r", "--rows"},
            required = true,
            description = "Nombre de lignes de la grille finale.")
    int rows;

    @Option(names = {"-c", "--cols"},
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

    List<File> imagesToProcess = new ArrayList<>();

    if (inputSources.directory != null) {
      // Lists the files in the directory
      System.out.println("Lecture depuis le dossier : " + inputSources.directory);
      File[] dirFiles = inputSources.directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".bmp"));
      if (dirFiles != null && dirFiles.length > 0) {
          Arrays.sort(dirFiles); // We have to make sure the images are in the right order (00_00, 00_01, etc)
          Collections.addAll(imagesToProcess, dirFiles);
        } else {
          System.err.println("Error: No valid BMP files found in directory.");
          return 1;
        }
    } else {
      System.out.println("Lecture des fichiers : " + inputSources.files);

      List<File> validFiles = inputSources.files.stream()
              .filter(File::isFile) // Verifies that it exists and that it is a file
              .filter(file -> file.getName().toLowerCase().endsWith(".bmp")) // Verifies that it end with bmp or BMP
              .toList(); // They get assigned to validFiles as an immutable list

      if (validFiles.isEmpty()) {
        System.err.println("Error: No valid BMP files provided in the arguments.");
        return 1;
      }

      if (repeat != null) {
        int nbFiles = validFiles.size();
        // This way, if -R is enabled, images given will be repeated like so: A B C A B etc
        for (int i = 0; i < repeat; i++) imagesToProcess.add(validFiles.get(i % nbFiles));
      } else imagesToProcess.addAll(validFiles);
    }


    //TODO faire le mode par défaut

    int r = 1; // Rows
    int c = imagesToProcess.size(); // Columns (for now it's horizontal)

    // Find out the final layout
    if (layout != null) {
      if (layout.vertical) {
        System.out.println("Mode : Vertical");
        r = imagesToProcess.size();
        c = 1;
      }
      else if (layout.horizontal) System.out.println("Mode : Horizontal");
      else if (layout.grid != null) {
        r = layout.grid.rows;
        c = layout.grid.cols;
        System.out.printf("Mode : Grille (%d lignes, %d colonnes)%n", r, c);
      }
    } else System.out.println("Mode par défaut (déduction automatique via les noms de fichiers)");


    return 0;
  }
}

