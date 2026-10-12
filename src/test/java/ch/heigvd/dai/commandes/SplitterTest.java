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
import java.util.stream.Stream;
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

  /** Lit une tuile du dossier de sortie. */
  private BmpImage tile(String name) throws IOException {
    try (FileInputStream in = new FileInputStream(tempDir.resolve("out").resolve(name).toFile())) {
      return BmpImage.readFrom(in);
    }
  }

  /** Nombre de fichiers dans le dossier de sortie. */
  private long tileCount() throws IOException {
    try (Stream<Path> files = Files.list(tempDir.resolve("out"))) {
      return files.count();
    }
  }

  /** Ecrit l'image donnee comme image source et renvoie son chemin. */
  private Path save(BmpImage image) throws IOException {
    Path file = tempDir.resolve("input.bmp");
    try (FileOutputStream out = new FileOutputStream(file.toFile())) {
      image.writeTo(out);
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

  // --- Noms et contenu des tuiles ---

  private static final int[] GREEN = {0, 255, 0};
  private static final int[] WHITE = {255, 255, 255};

  @Test
  void tilesAreNamedRowColSuffix() throws IOException {
    assertEquals(0, split(image(4, 6), 3, 2));

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 2; col++) {
        assertTrue(tileExists(row + "_" + col + "_tile.bmp"), row + "_" + col);
      }
    }
    assertEquals(6, tileCount());
  }

  @Test
  void indicesAreZeroPaddedToTheLargestIndex() throws IOException {
    // 11 colonnes : le plus grand indice est 10, donc 2 chiffres pour tous
    assertEquals(0, split(image(11, 2), 1, 11));

    assertTrue(tileExists("00_00_tile.bmp"));
    assertTrue(tileExists("00_10_tile.bmp"));
    assertFalse(tileExists("0_0_tile.bmp"));
  }

  @Test
  void suffixOptionReplacesTile() throws IOException {
    assertEquals(0, new CommandLine(new Main()).execute("split", image(4, 4).toString(),
        tempDir.resolve("out").toString(), "-r", "2", "-c", "2", "-s", "part"));

    assertTrue(tileExists("1_1_part.bmp"));
    assertFalse(tileExists("1_1_tile.bmp"));
  }

  @Test
  void eachTileHoldsItsPartOfTheImage() throws IOException {
    // 4x4, un quart de couleur differente par tuile
    BmpImage image = BmpImage.create(4, 4);
    for (int y = 0; y < 4; y++) {
      for (int x = 0; x < 4; x++) {
        int[] c = y < 2 ? (x < 2 ? RED : GREEN) : (x < 2 ? BLUE : WHITE);
        image.setPixel(x, y, c[0], c[1], c[2]);
      }
    }

    assertEquals(0, split(save(image), 2, 2));

    assertArrayEquals(RED, tileColor("0_0_tile.bmp"));
    assertArrayEquals(GREEN, tileColor("0_1_tile.bmp"));
    assertArrayEquals(BLUE, tileColor("1_0_tile.bmp"));
    assertArrayEquals(WHITE, tileColor("1_1_tile.bmp"));
  }

  // --- Taille des tuiles sur une grille non divisible ---

  @Test
  void tileSizeUsesIntegerDivision() throws IOException {
    assertEquals(0, split(image(7, 5), 2, 3));

    BmpImage last = tile("1_2_tile.bmp");
    assertEquals(2, last.getWidth()); // 7 / 3
    assertEquals(2, last.getHeight()); // 5 / 2
  }

  @Test
  void leftoverPixelsAreInNoTile() throws IOException {
    // 3x1 rouge, vert, bleu coupe en 2 colonnes : le bleu est perdu
    BmpImage image = BmpImage.create(3, 1);
    image.setPixel(0, 0, 255, 0, 0);
    image.setPixel(1, 0, 0, 255, 0);
    image.setPixel(2, 0, 0, 0, 255);

    assertEquals(0, split(save(image), 1, 2));

    assertArrayEquals(RED, tileColor("0_0_tile.bmp"));
    assertArrayEquals(GREEN, tileColor("0_1_tile.bmp"));
    assertEquals(2, tileCount());
  }
}
