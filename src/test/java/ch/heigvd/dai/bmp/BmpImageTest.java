package ch.heigvd.dai.bmp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests de BmpImage et BmpHeader, entierement en memoire
 * (ByteArrayInputStream / ByteArrayOutputStream, aucun fichier sur disque).
 */
class BmpImageTest {

  /** Image de test dont chaque pixel a une couleur differente, derivee de (x, y). */
  private static BmpImage gradient(int width, int height) {
    BmpImage image = BmpImage.create(width, height);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        image.setPixel(x, y, x * 50, y * 50, (x * 7 + y * 13) % 256);
      }
    }
    return image;
  }

  private static byte[] write(BmpImage image) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    image.writeTo(out);
    return out.toByteArray();
  }

  private static BmpImage read(byte[] bytes) throws IOException {
    return BmpImage.readFrom(new ByteArrayInputStream(bytes));
  }

  private static void assertSamePixels(BmpImage expected, BmpImage actual) {
    assertEquals(expected.getWidth(), actual.getWidth());
    assertEquals(expected.getHeight(), actual.getHeight());
    for (int y = 0; y < expected.getHeight(); y++) {
      for (int x = 0; x < expected.getWidth(); x++) {
        assertArrayEquals(expected.getPixel(x, y), actual.getPixel(x, y), "pixel (" + x + ", " + y + ")");
      }
    }
  }

  /** Construit un fichier BMP octet par octet, pour controler chaque champ de l'en-tete. */
  private static byte[] bmp(int width, int height, short bitsPerPixel, int compression, int dibHeaderSize,
      byte[] pixelData) {
    ByteBuffer bb = ByteBuffer.allocate(54 + pixelData.length).order(ByteOrder.LITTLE_ENDIAN);
    bb.put((byte) 'B').put((byte) 'M');
    bb.putInt(54 + pixelData.length).putInt(0).putInt(54);
    bb.putInt(dibHeaderSize).putInt(width).putInt(height);
    bb.putShort((short) 1).putShort(bitsPerPixel);
    bb.putInt(compression).putInt(pixelData.length);
    bb.putInt(2835).putInt(2835).putInt(0).putInt(0);
    bb.put(pixelData);
    return bb.array();
  }

  /** Plus petit BMP valide : 1x1, une rangee de 4 octets (3 + 1 de padding). */
  private static byte[] validBmp() {
    return bmp(1, 1, (short) 24, 0, 40, new byte[4]);
  }

  private static void assertReadFails(byte[] bytes, String expectedMessagePart) {
    IOException e = assertThrows(IOException.class, () -> read(bytes));
    assertTrue(e.getMessage().contains(expectedMessagePart),
        "message \"" + e.getMessage() + "\" should contain \"" + expectedMessagePart + "\"");
  }

  // --- Aller-retour et padding des rangees ---

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  void writeThenReadKeepsEveryPixel(int width) throws IOException {
    BmpImage image = gradient(width, 3);
    assertSamePixels(image, read(write(image)));
  }

  @Test
  void readThenWriteGivesIdenticalBytes() throws IOException {
    byte[] original = write(gradient(5, 3));
    assertArrayEquals(original, write(read(original)));
  }

  @Test
  void onePixelImage() throws IOException {
    BmpImage image = BmpImage.create(1, 1);
    image.setPixel(0, 0, 10, 20, 30);
    byte[] bytes = write(image);
    assertEquals(54 + 4, bytes.length); // 3 octets de pixel + 1 octet de padding
    assertArrayEquals(new int[] {10, 20, 30}, read(bytes).getPixel(0, 0));
  }

  @ParameterizedTest
  @CsvSource({"1, 4", "2, 8", "3, 12", "4, 12", "5, 16"})
  void rowsArePaddedToAMultipleOfFourBytes(int width, int rowSize) throws IOException {
    int height = 2;
    byte[] bytes = write(BmpImage.create(width, height));
    assertEquals(54 + rowSize * height, bytes.length);

    ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(bytes.length, header.getInt(2)); // taille du fichier
    assertEquals(rowSize * height, header.getInt(34)); // taille des donnees pixel
  }

  // --- Ordre des pixels : B, G, R sur disque, rangee du bas en premier ---

  @Test
  void readConvertsBgrBottomUpToRgbTopDown() throws IOException {
    // 2x2, rangee du bas d'abord, chaque pixel en B, G, R, 2 octets de padding par rangee
    byte[] pixelData = {
        0, 0, (byte) 255, 0, (byte) 255, 0, 0, 0, // bas : rouge, vert
        (byte) 255, 0, 0, (byte) 255, (byte) 255, (byte) 255, 0, 0 // haut : bleu, blanc
    };
    BmpImage image = read(bmp(2, 2, (short) 24, 0, 40, pixelData));

    assertArrayEquals(new int[] {0, 0, 255}, image.getPixel(0, 0)); // haut gauche : bleu
    assertArrayEquals(new int[] {255, 255, 255}, image.getPixel(1, 0)); // haut droite : blanc
    assertArrayEquals(new int[] {255, 0, 0}, image.getPixel(0, 1)); // bas gauche : rouge
    assertArrayEquals(new int[] {0, 255, 0}, image.getPixel(1, 1)); // bas droite : vert
  }

  @Test
  void writeStoresBgrBottomUp() throws IOException {
    BmpImage image = BmpImage.create(1, 2);
    image.setPixel(0, 0, 255, 0, 0); // haut : rouge
    image.setPixel(0, 1, 0, 0, 255); // bas : bleu
    byte[] bytes = write(image);

    // rangee de 4 octets (3 + 1 de padding) : d'abord le bas (bleu), puis le haut (rouge)
    assertArrayEquals(new byte[] {(byte) 255, 0, 0}, Arrays.copyOfRange(bytes, 54, 57));
    assertArrayEquals(new byte[] {0, 0, (byte) 255}, Arrays.copyOfRange(bytes, 58, 61));
  }

  // --- En-tetes invalides ou non supportes ---

  @Test
  void rejectsMissingSignature() {
    byte[] bytes = validBmp();
    bytes[0] = 'P';
    assertReadFails(bytes, "'BM' signature");
  }

  @Test
  void rejectsTruncatedHeader() {
    assertReadFails(Arrays.copyOf(validBmp(), 20), "header is truncated");
  }

  @Test
  void rejectsTruncatedPixelData() {
    byte[] bytes = bmp(2, 2, (short) 24, 0, 40, new byte[16]);
    assertReadFails(Arrays.copyOf(bytes, bytes.length - 3), "pixel data");
  }

  @Test
  void rejectsOtherDibHeaderSizes() {
    assertReadFails(bmp(1, 1, (short) 24, 0, 124, new byte[4]), "BITMAPINFOHEADER");
  }

  @Test
  void rejectsCompressedBitmaps() {
    assertReadFails(bmp(1, 1, (short) 24, 1, 40, new byte[4]), "compressed");
  }

  @Test
  void rejectsOtherBitDepths() {
    assertReadFails(bmp(1, 1, (short) 32, 0, 40, new byte[4]), "only 24-bit");
  }

  @Test
  void rejectsTopDownBitmaps() {
    assertReadFails(bmp(1, -1, (short) 24, 0, 40, new byte[4]), "top-down");
  }

  @Test
  void rejectsEmptyImages() {
    assertThrows(IOException.class, () -> read(bmp(0, 1, (short) 24, 0, 40, new byte[0])));
    assertThrows(IOException.class, () -> read(bmp(1, 0, (short) 24, 0, 40, new byte[0])));
  }

  // --- crop ---

  @Test
  void cropCopiesTheRightPixels() {
    BmpImage image = gradient(5, 4);
    BmpImage tile = image.crop(1, 2, 3, 2);

    assertEquals(3, tile.getWidth());
    assertEquals(2, tile.getHeight());
    for (int y = 0; y < 2; y++) {
      for (int x = 0; x < 3; x++) {
        assertArrayEquals(image.getPixel(1 + x, 2 + y), tile.getPixel(x, y));
      }
    }
  }

  @Test
  void cropOfTheWholeImageIsACopy() {
    BmpImage image = gradient(5, 4);
    assertSamePixels(image, image.crop(0, 0, 5, 4));
  }

  @Test
  void cropAtTheBottomRightCorner() {
    BmpImage image = gradient(5, 4);
    assertArrayEquals(image.getPixel(4, 3), image.crop(4, 3, 1, 1).getPixel(0, 0));
  }

  @ParameterizedTest
  @CsvSource({"-1, 0, 1, 1", "0, -1, 1, 1", "0, 0, 0, 1", "0, 0, 1, 0", "4, 0, 2, 1", "0, 3, 1, 2"})
  void cropOutsideTheImageThrows(int x0, int y0, int w, int h) {
    BmpImage image = gradient(5, 4);
    assertThrows(IllegalArgumentException.class, () -> image.crop(x0, y0, w, h));
  }
}
