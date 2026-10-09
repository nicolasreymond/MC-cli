package ch.heigvd.dai.commandes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.heigvd.dai.Main;
import ch.heigvd.dai.bmp.BmpImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
  private InputStream originalIn;

  @BeforeEach
  void captureOutput() {
    originalOut = System.out;
    originalErr = System.err;
    originalIn = System.in;
    System.setOut(new PrintStream(stdout, true));
    System.setErr(new PrintStream(stderr, true));
    answer(""); // jamais de lecture bloquante sur le vrai clavier
  }

  @AfterEach
  void restoreOutput() {
    System.setOut(originalOut);
    System.setErr(originalErr);
    System.setIn(originalIn);
  }

  /** Simule ce que l'utilisateur tape a la question [y/N]. */
  private static void answer(String typed) {
    System.setIn(new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8)));
  }

  /** Ecrit une image noire de la taille donnee et renvoie son chemin. */
  private Path image(int width, int height) throws IOException {
    Path file = tempDir.resolve("input.bmp");
    try (FileOutputStream out = new FileOutputStream(file.toFile())) {
      BmpImage.create(width, height).writeTo(out);
    }
    return file;
  }

  /** Ecrit une image unie de la couleur donnee et renvoie son chemin. */
  private Path image(int width, int height, int r, int g, int b) throws IOException {
    BmpImage image = BmpImage.create(width, height);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        image.setPixel(x, y, r, g, b);
      }
    }
    Path file = tempDir.resolve("input.bmp");
    try (FileOutputStream out = new FileOutputStream(file.toFile())) {
      image.writeTo(out);
    }
    return file;
  }

  /** Couleur du premier pixel d'une tuile du dossier de sortie. */
  private int[] tileColor(String name) throws IOException {
    try (FileInputStream in = new FileInputStream(tempDir.resolve("out").resolve(name).toFile())) {
      return BmpImage.readFrom(in).getPixel(0, 0);
    }
  }

  private boolean tileExists(String name) {
    return Files.exists(tempDir.resolve("out").resolve(name));
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

  // --- Tuiles deja presentes dans le dossier de sortie ---

  private static final int[] RED = {255, 0, 0};
  private static final int[] BLUE = {0, 0, 255};

  @Test
  void answeringNoKeepsTheOldTilesAndNumbersTheNewOnes() throws IOException {
    assertEquals(0, split(image(4, 4, 255, 0, 0), 2, 2));
    answer("n\n");
    assertEquals(0, split(image(4, 4, 0, 0, 255), 2, 2));

    assertArrayEquals(RED, tileColor("0_0_tile.bmp"));
    assertArrayEquals(BLUE, tileColor("0_0_tile_1.bmp"));
    assertArrayEquals(BLUE, tileColor("1_1_tile_1.bmp"));
    assertTrue(stdout.toString().contains("named like 0_0_tile_1.bmp"), stdout.toString());
  }

  @Test
  void anEmptyAnswerTakesTheNextFreeNumber() throws IOException {
    Path input = image(4, 4, 255, 0, 0);
    split(input, 2, 2);
    split(input, 2, 2); // pas de reponse : _1
    assertEquals(0, split(input, 2, 2)); // _1 est pris : _2

    for (String name : new String[] {"0_0", "0_1", "1_0", "1_1"}) {
      assertTrue(tileExists(name + "_tile_2.bmp"), name);
    }
    assertFalse(tileExists("0_0_tile_3.bmp"));
  }

  @Test
  void answeringYesOverwrites() throws IOException {
    split(image(4, 4, 255, 0, 0), 2, 2);
    answer("y\n");
    assertEquals(0, split(image(4, 4, 0, 0, 255), 2, 2));

    assertArrayEquals(BLUE, tileColor("0_0_tile.bmp"));
    assertFalse(tileExists("0_0_tile_1.bmp"));
  }

  @Test
  void onlyTheTilesAboutToBeWrittenTriggerTheQuestion() throws IOException {
    Files.createDirectories(tempDir.resolve("out"));
    Files.createFile(tempDir.resolve("out").resolve("7_7_tile.bmp")); // pas dans une grille 2x2

    assertEquals(0, split(image(4, 4, 255, 0, 0), 2, 2));
    assertFalse(stdout.toString().contains("Overwrite"), stdout.toString());
    assertTrue(tileExists("0_0_tile.bmp"));
  }
}
