package ch.heigvd.dai.commandes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.heigvd.dai.Main;
import ch.heigvd.dai.bmp.BmpImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Tests de la commande merge, lancée comme depuis la ligne de commande,
 * sur des images créées dans un dossier temporaire.
 */
class MergerTest {

    private static final int[] RED = {255, 0, 0};
    private static final int[] GREEN = {0, 255, 0};
    private static final int[] BLUE = {0, 0, 255};
    private static final int[] WHITE = {255, 255, 255};
    private static final int[] BLACK = {0, 0, 0};

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

    /** Écrit une image unie de la couleur donnée et renvoie son chemin. */
    private Path image(String name, int width, int height, int[] rgb) throws IOException {
        BmpImage img = BmpImage.create(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setPixel(x, y, rgb[0], rgb[1], rgb[2]);
            }
        }
        Path file = tempDir.resolve(name);
        // Crée les dossiers intermédiaires si nécessaire
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (FileOutputStream out = new FileOutputStream(file.toFile())) {
            img.writeTo(out);
        }
        return file;
    }

    /** Lit une image BMP depuis le disque pour vérifier les pixels. */
    private BmpImage read(Path file) throws IOException {
        try (FileInputStream in = new FileInputStream(file.toFile())) {
            return BmpImage.readFrom(in);
        }
    }

    private int merge(String... args) {
        return new CommandLine(new Main()).execute(args);
    }

    // --- Layout et Positionnement ---

    @Test
    void tilesArePlacedAtTheRightPositionWithExplicitGrid() throws IOException {
        Path p1 = image("1.bmp", 2, 2, RED);
        Path p2 = image("2.bmp", 2, 2, GREEN);
        Path p3 = image("3.bmp", 2, 2, BLUE);
        Path p4 = image("4.bmp", 2, 2, WHITE);
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", p1.toString(), p2.toString(), p3.toString(), p4.toString(),
                "-o", out.toString(), "-r", "2", "-c", "2"));

        BmpImage result = read(out);
        assertArrayEquals(RED, result.getPixel(0, 0));
        assertArrayEquals(GREEN, result.getPixel(2, 0));
        assertArrayEquals(BLUE, result.getPixel(0, 2));
        assertArrayEquals(WHITE, result.getPixel(2, 2));
    }

    @Test
    void outputSizeMatchesGridAndTileDimensions() throws IOException {
        Path p1 = image("1.bmp", 5, 4, BLACK);
        Path p2 = image("2.bmp", 5, 4, BLACK);
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", p1.toString(), p2.toString(), "-o", out.toString(), "-H"));

        BmpImage result = read(out);
        assertEquals(10, result.getWidth()); // 2 cols * 5px
        assertEquals(4, result.getHeight()); // 1 row * 4px
    }

    @Test
    void filesInDirectoryAreReadInNameOrder() throws IOException {
        // b.bmp, a.bmp, c.bmp ne sont pas créés dans l'ordre alphabétique
        image("tiles/b.bmp", 1, 1, GREEN);
        image("tiles/a.bmp", 1, 1, RED);
        image("tiles/c.bmp", 1, 1, BLUE);
        Path dir = tempDir.resolve("tiles");
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", "-d", dir.toString(), "-o", out.toString(), "-H"));

        // L'ordre alphabétique forcé doit donner a=RED, b=GREEN, c=BLUE
        BmpImage result = read(out);
        assertArrayEquals(RED, result.getPixel(0, 0));
        assertArrayEquals(GREEN, result.getPixel(1, 0));
        assertArrayEquals(BLUE, result.getPixel(2, 0));
    }

    @Test
    void roundTripSplitThenMergeReproducesOriginalImage() throws IOException {
        // Création d'une image avec des pixels distincts
        BmpImage original = BmpImage.create(4, 4);
        original.setPixel(0, 0, 255, 0, 0);
        original.setPixel(3, 3, 0, 255, 0);
        Path input = tempDir.resolve("input.bmp");
        try (FileOutputStream out = new FileOutputStream(input.toFile())) {
            original.writeTo(out);
        }

        Path tilesDir = tempDir.resolve("tiles");
        Files.createDirectories(tilesDir);

        // Split
        new CommandLine(new Main()).execute("split", input.toString(), tilesDir.toString(), "-r", "2", "-c", "2");

        // Merge
        Path out = tempDir.resolve("out.bmp");
        assertEquals(0, merge("merge", "-d", tilesDir.toString(), "-o", out.toString()));

        BmpImage rebuilt = read(out);
        assertEquals(4, rebuilt.getWidth());
        assertEquals(4, rebuilt.getHeight());
        assertArrayEquals(new int[]{255, 0, 0}, rebuilt.getPixel(0, 0));
        assertArrayEquals(new int[]{0, 255, 0}, rebuilt.getPixel(3, 3));
    }

    @Test
    void gridIsInferredFromTileNames() throws IOException {
        image("tiles/0_0_tile.bmp", 2, 2, RED);
        image("tiles/0_1_tile.bmp", 2, 2, GREEN);
        image("tiles/1_0_tile.bmp", 2, 2, BLUE);
        image("tiles/1_1_tile.bmp", 2, 2, WHITE);
        Path dir = tempDir.resolve("tiles");
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", "-d", dir.toString(), "-o", out.toString()));

        assertTrue(stdout.toString().contains("Absolute spatial grid detected"));
        BmpImage result = read(out);
        assertArrayEquals(RED, result.getPixel(0, 0));
        assertArrayEquals(WHITE, result.getPixel(2, 2));
    }

    // --- Gestion des Erreurs et Avertissements ---

    @Test
    void missingInputFileCausesAnError() {
        Path out = tempDir.resolve("out.bmp");
        int exitCode = merge("merge", tempDir.resolve("nope.bmp").toString(), "-o", out.toString(), "-H");

        assertEquals(1, exitCode);
        assertTrue(stderr.toString().contains("At least one file is not valid") ||
                stderr.toString().contains("No valid BMP files found"));
    }

    @Test
    void emptyInputDirectoryCausesAnError() throws IOException {
        Path dir = tempDir.resolve("empty");
        Files.createDirectories(dir);
        Path out = tempDir.resolve("out.bmp");

        int exitCode = merge("merge", "-d", dir.toString(), "-o", out.toString());
        assertEquals(1, exitCode);
        assertTrue(stderr.toString().contains("No valid BMP files found"));
    }

    @Test
    void missingTileLeavesBlankAndWarns() throws IOException {
        image("tiles/0_0_tile.bmp", 2, 2, RED);
        // 0_1 is deliberately missing
        image("tiles/1_0_tile.bmp", 2, 2, BLUE);
        image("tiles/1_1_tile.bmp", 2, 2, WHITE);
        Path dir = tempDir.resolve("tiles");
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", "-d", dir.toString(), "-o", out.toString()));

        assertTrue(stderr.toString().contains("could not be found"));
        BmpImage result = read(out);
        // The missing tile area should be initialized with 0s (black)
        assertArrayEquals(BLACK, result.getPixel(2, 0));
    }

    @Test
    void differentTileSizesCauseAnError() throws IOException {
        Path p1 = image("1.bmp", 2, 2, BLACK);
        Path p2 = image("2.bmp", 3, 3, BLACK);
        Path out = tempDir.resolve("out.bmp");

        int exitCode = merge("merge", p1.toString(), p2.toString(), "-o", out.toString(), "-H");

        assertEquals(1, exitCode);
        assertTrue(stderr.toString().contains("tile sizes are different"));
    }

    @Test
    void multipleSetsInOneFolderShowAWarning() throws IOException {
        image("tiles/0_0_tile.bmp", 2, 2, RED);
        image("tiles/0_0_tile_1.bmp", 2, 2, GREEN);
        Path dir = tempDir.resolve("tiles");
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", "-d", dir.toString(), "-o", out.toString()));
        assertTrue(stderr.toString().contains("Multiple sets detected"));
    }

    @Test
    void multipleSetsOfDifferentSizesKeepTheLastSet() throws IOException {
        // split 1x1, puis split 2x2 dans le meme dossier sans ecraser : la 2e serie est en _1
        image("tiles/0_0_tile.bmp", 4, 4, RED);
        image("tiles/0_0_tile_1.bmp", 2, 2, GREEN);
        image("tiles/0_1_tile_1.bmp", 2, 2, BLUE);
        image("tiles/1_0_tile_1.bmp", 2, 2, WHITE);
        image("tiles/1_1_tile_1.bmp", 2, 2, GREEN);
        Path dir = tempDir.resolve("tiles");
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", "-d", dir.toString(), "-o", out.toString()));

        assertTrue(stderr.toString().contains("Multiple sets detected"));
        BmpImage result = read(out);
        assertEquals(4, result.getWidth());
        assertEquals(4, result.getHeight());
        assertArrayEquals(GREEN, result.getPixel(0, 0));
        assertArrayEquals(BLUE, result.getPixel(2, 0));
        assertArrayEquals(WHITE, result.getPixel(0, 2));
    }

    @Test
    void nonBmpFilesInFolderAreIgnored() throws IOException {
        image("tiles/a.bmp", 2, 2, RED);
        // Create a dummy txt file
        Path txtFile = tempDir.resolve("tiles/ignore.txt");
        Files.createDirectories(txtFile.getParent());
        Files.writeString(txtFile, "Not a BMP");
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", "-d", tempDir.resolve("tiles").toString(), "-o", out.toString(), "-H"));

        BmpImage result = read(out);
        assertEquals(2, result.getWidth()); // Seule l'image BMP est lue
    }

    @Test
    void gridNotInferableDefaultsToSequentialMerge() throws IOException {
        image("tiles/random_name.bmp", 2, 2, RED);
        image("tiles/another_name.bmp", 2, 2, GREEN);
        Path dir = tempDir.resolve("tiles");
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", "-d", dir.toString(), "-o", out.toString()));

        // Par défaut, sans noms de grille ni options, c'est traité de façon séquentielle horizontale
        assertTrue(stdout.toString().contains("standard sequential merge"));
        BmpImage result = read(out);
        assertEquals(4, result.getWidth());
        assertEquals(2, result.getHeight());
    }

    // --- Nouveaux tests d'Inférence, Layouts et Erreurs ---

    @Test
    void gridIsInferredWithZeroPaddedNamesInRandomOrder() throws IOException {
        // Noms avec zéros initiaux
        Path p00 = image("tiles/00_00_tile.bmp", 2, 2, RED);
        Path p01 = image("tiles/00_01_tile.bmp", 2, 2, GREEN);
        Path p10 = image("tiles/01_00_tile.bmp", 2, 2, BLUE);
        Path p11 = image("tiles/01_01_tile.bmp", 2, 2, WHITE);
        Path out = tempDir.resolve("out.bmp");

        // Fichiers passés dans un ordre aléatoire sur la ligne de commande
        assertEquals(0, merge("merge", p10.toString(), p01.toString(), p11.toString(), p00.toString(), "-o", out.toString()));

        assertTrue(stdout.toString().contains("Absolute spatial grid detected"));
        BmpImage result = read(out);
        assertEquals(4, result.getWidth());
        assertEquals(4, result.getHeight());
        assertArrayEquals(RED, result.getPixel(0, 0));   // 00_00 au bon endroit
        assertArrayEquals(GREEN, result.getPixel(2, 0)); // 00_01 au bon endroit
        assertArrayEquals(BLUE, result.getPixel(0, 2));  // 01_00 au bon endroit
        assertArrayEquals(WHITE, result.getPixel(2, 2)); // 01_01 au bon endroit
    }

    @Test
    void verticalLayoutStacksImages() throws IOException {
        Path p1 = image("1.bmp", 2, 2, RED);
        Path p2 = image("2.bmp", 2, 2, BLUE);
        Path out = tempDir.resolve("out.bmp");

        assertEquals(0, merge("merge", p1.toString(), p2.toString(), "-v", "-o", out.toString()));

        BmpImage result = read(out);
        assertEquals(2, result.getWidth());
        assertEquals(4, result.getHeight()); // 2 images l'une sous l'autre (2 rows x 2px)
        assertArrayEquals(RED, result.getPixel(0, 0));
        assertArrayEquals(BLUE, result.getPixel(0, 2));
    }

    @Test
    void repeatLayoutCyclesThroughImages() throws IOException {
        Path p1 = image("1.bmp", 2, 2, RED);
        Path p2 = image("2.bmp", 2, 2, BLUE);
        Path out = tempDir.resolve("out.bmp");

        // 3 images horizontales en cyclant sur les 2 fournies
        assertEquals(0, merge("merge", p1.toString(), p2.toString(), "-R", "3", "-o", out.toString()));

        BmpImage result = read(out);
        assertEquals(6, result.getWidth()); // 3 images l'une à côté de l'autre (3 cols x 2px)
        assertEquals(2, result.getHeight());
        assertArrayEquals(RED, result.getPixel(0, 0));  // 1ère image
        assertArrayEquals(BLUE, result.getPixel(2, 0)); // 2ème image
        assertArrayEquals(RED, result.getPixel(4, 0));  // 3ème image (recyclage de la 1ère)
    }

    @Test
    void extraImagesBeyondGridCapacityAreIgnoredWithWarning() throws IOException {
        Path p1 = image("1.bmp", 2, 2, RED);
        Path p2 = image("2.bmp", 2, 2, GREEN);
        Path p3 = image("3.bmp", 2, 2, BLUE); // Image en trop
        Path out = tempDir.resolve("out.bmp");

        // Grille de 1 ligne et 2 colonnes (capacité 2 images)
        assertEquals(0, merge("merge", p1.toString(), p2.toString(), p3.toString(), "-r", "1", "-c", "2", "-o", out.toString()));

        assertTrue(stderr.toString().contains("some will be ignored"));
        BmpImage result = read(out);
        assertEquals(4, result.getWidth());
        assertEquals(2, result.getHeight());
        assertArrayEquals(RED, result.getPixel(0, 0));
        assertArrayEquals(GREEN, result.getPixel(2, 0));
    }

    @Test
    void usageErrorsReturnExitCode2() throws IOException {
        Path p1 = image("1.bmp", 2, 2, RED);
        Path p2 = image("2.bmp", 2, 2, BLUE);
        Path out = tempDir.resolve("out.bmp");
        Path dir = tempDir.resolve("tiles");
        Files.createDirectories(dir);

        // 1. Manque de l'option obligatoire -o
        assertEquals(2, merge("merge", p1.toString(), p2.toString()));

        // 2. Options de grille incomplètes (-r fourni mais sans -c)
        assertEquals(2, merge("merge", p1.toString(), p2.toString(), "-o", out.toString(), "-r", "2"));

        // 3. Layouts mutuellement exclusifs (-v et -H ensemble)
        assertEquals(2, merge("merge", p1.toString(), p2.toString(), "-o", out.toString(), "-v", "-H"));

        // 4. Inputs mutuellement exclusifs (fichiers individuels ET dossier -d ensemble)
        assertEquals(2, merge("merge", p1.toString(), "-d", dir.toString(), "-o", out.toString()));
    }
}