package ch.heigvd.dai.commandes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.heigvd.dai.Main;
import ch.heigvd.dai.bmp.BmpImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Tests de mosaic : la couleur moyenne, puis la commande complete sur de
 * petites images dont le resultat attendu est connu pixel par pixel.
 */
class MosaicTest {

  private static final int[] RED = {255, 0, 0};
  private static final int[] GREEN = {0, 255, 0};
  private static final int[] BLUE = {0, 0, 255};

  @TempDir Path tempDir;

  private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
  private PrintStream originalOut;
  private PrintStream originalErr;

  @BeforeEach
  void captureOutput() {
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(new PrintStream(new ByteArrayOutputStream(), true));
    System.setErr(new PrintStream(stderr, true));
  }

  @AfterEach
  void restoreOutput() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  private static void fill(BmpImage image, int x0, int y0, int w, int h, int[] rgb) {
    for (int y = y0; y < y0 + h; y++) {
      for (int x = x0; x < x0 + w; x++) {
        image.setPixel(x, y, rgb[0], rgb[1], rgb[2]);
      }
    }
  }

  private Path save(BmpImage image) throws IOException {
    Path file = tempDir.resolve("input.bmp");
    try (FileOutputStream out = new FileOutputStream(file.toFile())) {
      image.writeTo(out);
    }
    return file;
  }

  private BmpImage output() throws IOException {
    try (FileInputStream in = new FileInputStream(tempDir.resolve("mosaic.bmp").toFile())) {
      return BmpImage.readFrom(in);
    }
  }

  private int mosaic(Path input, int rows, int cols) {
    return new CommandLine(new Main()).execute("mosaic", input.toString(),
        "-o", tempDir.resolve("mosaic.bmp").toString(), "-r", String.valueOf(rows), "-c", String.valueOf(cols));
  }

  // --- Couleur moyenne ---

  @Test
  void averageColorOfASolidImageIsThatColor() {
    BmpImage image = BmpImage.create(3, 2);
    fill(image, 0, 0, 3, 2, new int[] {10, 20, 30});
    assertArrayEquals(new int[] {10, 20, 30}, Mosaic.averageColor(image));
  }

  @Test
  void averageColorIsRounded() {
    BmpImage image = BmpImage.create(2, 1);
    image.setPixel(1, 0, 255, 255, 255); // un pixel noir, un pixel blanc : 127.5
    assertArrayEquals(new int[] {128, 128, 128}, Mosaic.averageColor(image));
  }

  // --- Commande complete ---

  @Test
  void eachTileIsFilledWithItsOwnAverageColor() throws IOException {
    // 4x2, deux tuiles de 2x2 : a gauche moitie rouge / moitie bleu, a droite vert
    BmpImage image = BmpImage.create(4, 2);
    fill(image, 0, 0, 1, 2, RED);
    fill(image, 1, 0, 1, 2, BLUE);
    fill(image, 2, 0, 2, 2, GREEN);

    assertEquals(0, mosaic(save(image), 1, 2));

    BmpImage result = output();
    assertEquals(4, result.getWidth());
    assertEquals(2, result.getHeight());
    int[] purple = {128, 0, 128};
    for (int y = 0; y < 2; y++) {
      for (int x = 0; x < 4; x++) {
        assertArrayEquals(x < 2 ? purple : GREEN, result.getPixel(x, y), "pixel (" + x + ", " + y + ")");
      }
    }
  }

  @Test
  void oneTilePerPixelGivesTheSameImage() throws IOException {
    BmpImage image = BmpImage.create(3, 2);
    fill(image, 0, 0, 1, 1, RED);
    fill(image, 2, 1, 1, 1, BLUE);

    assertEquals(0, mosaic(save(image), 2, 3));

    BmpImage result = output();
    for (int y = 0; y < 2; y++) {
      for (int x = 0; x < 3; x++) {
        assertArrayEquals(image.getPixel(x, y), result.getPixel(x, y));
      }
    }
  }

  @Test
  void droppedPixelsAreReported() throws IOException {
    assertEquals(0, mosaic(save(BmpImage.create(5, 4)), 2, 2));
    assertEquals("Warning: 1 px dropped on the right edge (5 is not a multiple of 2 columns).",
        stderr.toString().strip());
    assertEquals(4, output().getWidth());
  }

  @Test
  void gridLargerThanTheImageIsAnError() throws IOException {
    assertEquals(1, mosaic(save(BmpImage.create(2, 2)), 3, 3));
  }

  @Test
  void missingInputIsAnError() {
    assertEquals(1, mosaic(tempDir.resolve("nope.bmp"), 2, 2));
  }

  @Test
  void missingOutputOptionIsAUsageError() throws IOException {
    Path input = save(BmpImage.create(2, 2));
    assertEquals(2, new CommandLine(new Main()).execute("mosaic", input.toString()));
  }
}
