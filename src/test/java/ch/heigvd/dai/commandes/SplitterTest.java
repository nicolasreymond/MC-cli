package ch.heigvd.dai.commandes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.heigvd.dai.Main;
import ch.heigvd.dai.bmp.BmpImage;
import java.io.ByteArrayOutputStream;
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
 * Tests de la commande split, lancee comme depuis la ligne de commande,
 * sur des images creees dans un dossier temporaire.
 */
class SplitterTest {

  @TempDir Path tempDir;

  private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
  private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
  private PrintStream originalOut;
  private PrintStream originalErr;

  @BeforeEach
  void captureOutput() {
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(new PrintStream(stdout, true));
    System.setErr(new PrintStream(stderr, true));
  }

  @AfterEach
  void restoreOutput() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  /** Ecrit une image noire de la taille donnee et renvoie son chemin. */
  private Path image(int width, int height) throws IOException {
    Path file = tempDir.resolve("input.bmp");
    try (FileOutputStream out = new FileOutputStream(file.toFile())) {
      BmpImage.create(width, height).writeTo(out);
    }
    return file;
  }

  private int split(Path input, int rows, int cols) {
    return new CommandLine(new Main()).execute("split", input.toString(),
        tempDir.resolve("out").toString(), "-r", String.valueOf(rows), "-c", String.valueOf(cols));
  }

  // --- Pixels perdus sur une grille non divisible ---

  @Test
  void divisibleGridPrintsNoWarning() throws IOException {
    assertEquals(0, split(image(4, 6), 3, 2));
    assertEquals("", stderr.toString());
  }

  @Test
  void warnsAboutTheRightEdge() throws IOException {
    assertEquals(0, split(image(5, 4), 2, 2));
    assertEquals("Warning: 1 px dropped on the right edge (5 is not a multiple of 2 columns).",
        stderr.toString().strip());
  }

  @Test
  void warnsAboutTheBottomEdge() throws IOException {
    assertEquals(0, split(image(4, 5), 3, 2));
    assertEquals("Warning: 2 px dropped on the bottom edge (5 is not a multiple of 3 rows).",
        stderr.toString().strip());
  }

  @Test
  void warnsAboutBothEdges() throws IOException {
    assertEquals(0, split(image(8, 5), 2, 3));
    String warnings = stderr.toString();
    assertTrue(warnings.contains("2 px dropped on the right edge (8 is not a multiple of 3 columns)"), warnings);
    assertTrue(warnings.contains("1 px dropped on the bottom edge (5 is not a multiple of 2 rows)"), warnings);
  }

  @Test
  void warningGoesToStderrOnly() throws IOException {
    assertEquals(0, split(image(5, 5), 2, 2));
    assertFalse(stdout.toString().contains("Warning"), stdout.toString());
    assertTrue(stdout.toString().contains("Split input.bmp into a 2x2 grid"), stdout.toString());
  }
}
