package ch.heigvd.dai.commandes;

import ch.heigvd.dai.Main;
import ch.heigvd.dai.bmp.BmpImage;

import java.io.*;
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

//-------------------------- INPUT FILES ------------------------------------------------------------------------------
    List<File> collectedFiles = new ArrayList<>();

    if (inputSources.directory != null) {
      // Lists the files in the directory
      System.out.println("Lecture depuis le dossier : " + inputSources.directory);
      File[] dirFiles = inputSources.directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".bmp"));
      if (dirFiles != null && dirFiles.length > 0) {
          Arrays.sort(dirFiles); // We have to make sure the images are in the right order (00_00, 00_01, etc)
          Collections.addAll(collectedFiles, dirFiles);
        }
    } else {
      System.out.println("Lecture des fichiers : " + inputSources.files);

      List<File> validFiles = inputSources.files.stream()
              .filter(File::isFile) // Verifies that it exists and that it is a file
              .filter(file -> file.getName().toLowerCase().endsWith(".bmp")) // Verifies that it end with bmp or BMP
              .toList(); // They get assigned to validFiles as an immutable list
      collectedFiles.addAll(validFiles);
    }
      if (collectedFiles.isEmpty()) {
        System.err.println("Error: No valid BMP files found.");
        return 1;
      }

      List<File> imagesToProcess = new ArrayList<>();

      if (repeat != null) {
        int nbFiles = collectedFiles.size();
        // This way, if -R is enabled, images given will be repeated like so: A B C A B etc
        for (int i = 0; i < repeat; i++) imagesToProcess.add(collectedFiles.get(i % nbFiles));
      } else imagesToProcess.addAll(collectedFiles);
//---------------------------------------------------------------------------------------------------------------------

//-------------------------- LAYOUT CREATION --------------------------------------------------------------------------
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
    } else {
      System.out.println("Mode par défaut (déduction automatique via les noms de fichiers)");

      int maxRow = 0;
      int maxCol = 0;
      boolean formatDetected = false;

      // Finds the largest indexes in the file names
      for (File file : imagesToProcess) {
        String[] parts = file.getName().split("_");

        // We're looking for "row_column_"
        if (parts.length >= 3) {
          try {
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);
            maxRow = Math.max(maxRow, row);
            maxCol = Math.max(maxCol, col);
            formatDetected = true;
          }
          // In case there are non-conventional files mixed in (ex: "string_with_length_four.bmp")
          catch (NumberFormatException ignored) {}
        }
      }

      if (formatDetected) {
        r = maxRow + 1; // Begins at 0
        c = maxCol + 1;
        System.out.printf("Grille détectée : %d lignes x %d colonnes.%n", r, c);
      } else System.out.println("Format non reconnu. Fallback sur une fusion horizontale.");
    }

    if (r * c < imagesToProcess.size())
      System.err.println("Warning: The layout (" + r + "x" + c + ") is too small for "
                          + imagesToProcess.size() + " images. Some will be ignored.");
//---------------------------------------------------------------------------------------------------------------------

    try {
      processMerge(imagesToProcess, r, c, outputFile);
      System.out.println("Fusion terminée avec succès : " + outputFile.getAbsolutePath());
      return 0;
    } catch (Exception e) {
      System.err.println("Une erreur s'est produite lors de la fusion : " + e.getMessage());
      return 1;
    }
  }

  // Separate function, easier to test with JUnit
  public void processMerge(List<File> imagesToProcess, int r, int c, File outputFile) throws Exception {

    BmpImage firstImage;
    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(imagesToProcess.getFirst()))) {
      firstImage = BmpImage.readFrom(in);
    }

    // Final canvas
    int cellWidth = firstImage.getWidth();
    int cellHeight = firstImage.getHeight();
    BmpImage finalImage = BmpImage.create(cellWidth * c, cellHeight * r);




    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
      finalImage.writeTo(out);
    }
  }

}
