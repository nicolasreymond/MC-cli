package ch.heigvd.dai.commandes;

import ch.heigvd.dai.Main;
import ch.heigvd.dai.bmp.BmpImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import picocli.CommandLine;

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
                      description = "Number of rows.",
                      defaultValue = "2")
  private int rows;

  @CommandLine.Option(names = {"-c", "--cols"},
                      description = "Number of columns.",
                      defaultValue = "2")
  private int cols;

  @CommandLine.Option(names = {"-s", "--suffix"},
                      description = "Text appended to each tile file name, after the row/column indices.",
                      defaultValue = "tile")
  private String suffix;

  @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message.")
  private boolean helpRequested = false;

  @Override
  public Integer call() {
    if (!inputFile.exists()) {
      System.err.println("Error: Input file does not exist.");
      return 1;
    }

    if (!inputFile.isFile()) {
      System.err.println("Error: Input is not a file: " + inputFile);
      return 1;
    }

    if (rows <= 0 || cols <= 0) {
      System.err.println("Error: rows and cols must be positive.");
      return 1;
    }

    if (!outputDir.exists() && !outputDir.mkdirs()) {
      System.err.println("Error: Output folder not created correctly");
      return 1;
    }

    BmpImage image;
    try (FileInputStream in = new FileInputStream(inputFile)) {
      image = BmpImage.readFrom(in);
    } catch (Exception e) {
      System.err.println("Error: Could not read the image: " + e.getMessage());
      return 1;
    }

    // Grille possiblement non divisible : l'exces de pixels (bord droit/bas)
    // est simplement ignore, chaque tuile fait width/cols x height/rows.
    int tileWidth = image.getWidth() / cols;
    int tileHeight = image.getHeight() / rows;
    if (tileWidth == 0 || tileHeight == 0) {
      System.err.println(
          "Error: a " + rows + "x" + cols + " grid (rows x cols) is too large for a "
              + image.getWidth() + "x" + image.getHeight() + " image.");
      return 1;
    }

    // Les indices de position sont mis en premier dans le nom (avant le
    // suffixe), avec zero-padding, pour que le tri alphabetique du dossier
    // reste dans l'ordre de la grille (utile pour un merge ulterieur qui
    // trie par nom).
    int indexDigits = Integer.toString(Math.max(rows, cols) - 1).length();
    String indexFormat = "%0" + indexDigits + "d";

    if (existingTiles(outputDir, indexDigits) && !confirmOverwrite()) {
      System.out.println("Aborted: tiles already exist in " + outputDir);
      return 1;
    }

    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        BmpImage tile = image.crop(col * tileWidth, row * tileHeight, tileWidth, tileHeight);
        String fileName = String.format(indexFormat, row) + "_" + String.format(indexFormat, col)
            + "_" + suffix + ".bmp";
        File tileFile = new File(outputDir, fileName);
        try (FileOutputStream out = new FileOutputStream(tileFile)) {
          tile.writeTo(out);
        } catch (Exception e) {
          System.err.println("Error: Could not write tile " + tileFile.getName() + ": " + e.getMessage());
          return 1;
        }
      }
    }

    System.out.println("Split " + inputFile.getName() + " into a " + rows + "x" + cols
        + " grid (rows x cols) in " + outputDir);
    return 0;
  }

  /** Vrai si outputDir contient deja des tuiles issues d'un split precedent avec ce suffixe. */
  private boolean existingTiles(File outputDir, int indexDigits) {
    File[] files = outputDir.listFiles();
    if (files == null) {
      return false;
    }
    Pattern tilePattern = Pattern.compile(
        "\\d{" + indexDigits + "}_\\d{" + indexDigits + "}_" + Pattern.quote(suffix) + "\\.bmp");
    for (File f : files) {
      if (tilePattern.matcher(f.getName()).matches()) {
        return true;
      }
    }
    return false;
  }

  private boolean confirmOverwrite() {
    System.out.print("Tiles already exist in the output folder. Overwrite? [y/N] ");
    Scanner scanner = new Scanner(System.in);
    String answer = scanner.hasNextLine() ? scanner.nextLine().trim().toLowerCase() : "";
    return answer.equals("y") || answer.equals("yes");
  }
}