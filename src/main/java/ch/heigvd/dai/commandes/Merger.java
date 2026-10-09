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
      boolean isStrictGrid = false;
      File[][] gridFiles = null;

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
      isStrictGrid = true;

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
          }
          // In case there are non-conventional files mixed in (ex: "string_with_length_four.bmp")
          catch (NumberFormatException e) {
            isStrictGrid = false; // The parsing has failed
            break; // No use in checking the rest of it
          }
        } else {
          isStrictGrid = false; // Not enough parts in the name
          break; // No use in checking the rest of it
        }
      }



      if (isStrictGrid) {
        System.out.println("Mode : Grille spatiale absolue détectée.");
        r = maxRow + 1;
        c = maxCol + 1;
        gridFiles = new File[r][c];

        // Placement des fichiers dans la grille
        for (File file : imagesToProcess) {
          String[] parts = file.getName().split("_");
          int row = Integer.parseInt(parts[0]); // Plus besoin de try-catch, on sait que ça passe
          int col = Integer.parseInt(parts[1]);
          gridFiles[row][col] = file;
        }
      } else System.out.println("Mode : Fichiers arbitraires. Fusion séquentielle classique.");
    }

    if (r * c < imagesToProcess.size())
      System.err.println("Warning: The layout (" + r + "x" + c + ") is too small for "
                          + imagesToProcess.size() + " images. Some will be ignored.");
//---------------------------------------------------------------------------------------------------------------------

    try {
      processMerge(imagesToProcess, gridFiles, isStrictGrid, r, c, outputFile);
      System.out.println("Fusion terminée avec succès : " + outputFile.getAbsolutePath());
      return 0;
    } catch (Exception e) {
      System.err.println("Une erreur s'est produite lors de la fusion : " + e.getMessage());
      return 1;
    }
  }

  //---------------------------------------------------- MERGE OPERATION ----------------------------------------------
  // Separate function, easier to test with JUnit
  public void processMerge(List<File> sequentialFiles, File[][] gridFiles, boolean isStrictGrid, int r, int c, File outputFile) throws Exception {

    BmpImage firstImage;
    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(sequentialFiles.getFirst()))) {
      firstImage = BmpImage.readFrom(in);
    }

    // Final canvas
    int cellWidth = firstImage.getWidth();
    int cellHeight = firstImage.getHeight();
    BmpImage finalImage = BmpImage.create(cellWidth * c, cellHeight * r);

    int imgIndex = 0;
    for (int row = 0; row < r; row++) {
      for (int col = 0; col < c; col++) {
        File currentFile;

        if (isStrictGrid) { // We place the tiles using their index
          currentFile = gridFiles[row][col];
          if (currentFile == null) continue; // Missing tile, we keep the tile empty
        } else { // We merge every image next to one another
          if (imgIndex >= sequentialFiles.size()) break;
          currentFile = sequentialFiles.get(imgIndex);
          imgIndex++;
        }

        BmpImage currentImg;

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(currentFile))) {
          currentImg = BmpImage.readFrom(in);
        }

        int drawW = Math.min(cellWidth, currentImg.getWidth());
        int drawH = Math.min(cellHeight, currentImg.getHeight());
        int startX = col * cellWidth;
        int startY = row * cellHeight;

        for (int y = 0; y < drawH; y++) {
          for (int x = 0; x < drawW; x++) {
            int[] rgb = currentImg.getPixel(x, y);
            finalImage.setPixel(startX + x, startY + y, rgb[0], rgb[1], rgb[2]);
          }
        }
      }
    }

    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
      finalImage.writeTo(out);
    }
  }
//---------------------------------------------------------------------------------------------------------------------
}
