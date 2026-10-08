package ch.heigvd.dai.bmp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
}
