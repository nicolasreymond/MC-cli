package ch.heigvd.dai.commandes;

import ch.heigvd.dai.bmp.BmpImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "mosaic",
                     description = "Rebuild an image as a mosaic: cut it into tiles, fill each tile "
                         + "with its average color, and put the tiles back together.")
public class Mosaic implements Callable<Integer> {

  @CommandLine.Parameters(index = "0",
                          description = "The source image file.")
  private File inputFile;

  @CommandLine.Option(names = {"-o", "--output"},
                      required = true,
                      description = "Output BMP file.")
  private File outputFile;

  @CommandLine.Option(names = {"-r", "--rows"},
                      description = "Number of rows of tiles.",
                      defaultValue = "20")
  private int rows;

  @CommandLine.Option(names = {"-c", "--cols"},
                      description = "Number of columns of tiles.",
                      defaultValue = "20")
  private int cols;

  @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message.")
  private boolean helpRequested = false;

  @Override
  public Integer call() {
    if (!inputFile.isFile()) {
      System.err.println("Error: Input file does not exist or is not a file: " + inputFile);
      return 1;
    }

    if (rows <= 0 || cols <= 0) {
      System.err.println("Error: rows and cols must be positive.");
      return 1;
    }

    BmpImage image;
    try (FileInputStream in = new FileInputStream(inputFile)) {
      image = BmpImage.readFrom(in);
    } catch (Exception e) {
      System.err.println("Error: Could not read the image: " + e.getMessage());
      return 1;
    }

    // Meme decoupage que split : tuiles de meme taille, l'exces de pixels
    // (bord droit/bas) est ignore et signale.
    int tileWidth = image.getWidth() / cols;
    int tileHeight = image.getHeight() / rows;
    if (tileWidth == 0 || tileHeight == 0) {
      System.err.println(
          "Error: a " + rows + "x" + cols + " grid (rows x cols) is too large for a "
              + image.getWidth() + "x" + image.getHeight() + " image.");
      return 1;
    }

    int droppedRight = image.getWidth() % cols;
    int droppedBottom = image.getHeight() % rows;
    if (droppedRight > 0) {
      System.err.println("Warning: " + droppedRight + " px dropped on the right edge ("
          + image.getWidth() + " is not a multiple of " + cols + " columns).");
    }
    if (droppedBottom > 0) {
      System.err.println("Warning: " + droppedBottom + " px dropped on the bottom edge ("
          + image.getHeight() + " is not a multiple of " + rows + " rows).");
    }

    // split -> recoloration -> merge, en memoire : chaque tuile est decoupee,
    // remplacee par sa couleur moyenne, puis remise a sa place.
    BmpImage mosaic = BmpImage.create(cols * tileWidth, rows * tileHeight);
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        int x0 = col * tileWidth;
        int y0 = row * tileHeight;
        BmpImage tile = image.crop(x0, y0, tileWidth, tileHeight);
        int[] average = averageColor(tile);
        for (int y = y0; y < y0 + tileHeight; y++) {
          for (int x = x0; x < x0 + tileWidth; x++) {
            mosaic.setPixel(x, y, average[0], average[1], average[2]);
          }
        }
      }
    }

    try (FileOutputStream out = new FileOutputStream(outputFile)) {
      mosaic.writeTo(out);
    } catch (Exception e) {
      System.err.println("Error: Could not write " + outputFile + ": " + e.getMessage());
      return 1;
    }

    System.out.println("Mosaic of " + inputFile.getName() + " (" + rows + "x" + cols
        + " tiles, rows x cols) written to " + outputFile);
    return 0;
  }

  /** Couleur moyenne (R, V, B arrondis) de toute l'image. */
  static int[] averageColor(BmpImage image) {
    long r = 0;
    long g = 0;
    long b = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int[] rgb = image.getPixel(x, y);
        r += rgb[0];
        g += rgb[1];
        b += rgb[2];
      }
    }
    long count = (long) image.getWidth() * image.getHeight();
    return new int[] {
        (int) Math.round((double) r / count),
        (int) Math.round((double) g / count),
        (int) Math.round((double) b / count)
    };
  }
}
