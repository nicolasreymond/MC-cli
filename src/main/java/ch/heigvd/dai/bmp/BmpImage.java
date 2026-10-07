package ch.heigvd.dai.bmp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Image BMP complete (en-tete + pixels bruts). A utiliser a la place de
 * BufferedImage/ImageIO.read(...) dans les commandes split/merge/mosaic.
 *
 * Regle du sujet PW1 (section "What your CLI must do") : "Use the Java
 * I/O API from chapter 04 to read and write the files. You may use
 * other libraries for the processing itself, but not to open, read or
 * write the files." Autrement dit : ouvrir/lire/ecrire le fichier passe
 * obligatoirement par cette classe (java.io), mais le TRAITEMENT
 * (redimensionnement, calcul de couleur moyenne...) peut, si utile,
 * s'appuyer sur d'autres bibliotheques une fois les pixels en memoire.
 *
 * Pixels stockes en memoire du haut vers le bas (ligne 0 = haut de
 * l'image), 3 octets R,V,B par pixel, sans padding -- le BMP sur disque
 * est bas-vers-haut avec padding, la conversion se fait dans
 * readFrom/writeTo.
 */
public class BmpImage {

  private final int width;
  private final int height;
  private final byte[] pixels; // taille width*height*3, ordre R,V,B, ligne 0 = haut

  private BmpImage(int width, int height, byte[] pixels) {
    this.width = width;
    this.height = height;
    this.pixels = pixels;
  }

  /** Cree une image vide (noire) de la taille donnee. */
  public static BmpImage create(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("width and height must be positive");
    }
    return new BmpImage(width, height, new byte[width * height * 3]);
  }

  public static BmpImage readFrom(InputStream in) throws IOException {
    BmpHeader header = BmpHeader.readFrom(in);

    // si l'en-tete indique un offset plus grand que la taille standard
    // (54 octets), il y a des donnees supplementaires (ex. palette) a
    // sauter avant les pixels.
    int extra = header.getPixelDataOffset() - BmpHeader.TOTAL_HEADER_SIZE;
    if (extra < 0) {
      throw new IOException("Invalid BMP: pixel data offset is inside the header.");
    }
    skipFully(in, extra);

    int width = header.getWidth();
    int height = header.getHeight();
    int rowSize = header.rowSizeWithPadding();
    byte[] pixels = new byte[width * height * 3];
    byte[] rowBuffer = new byte[rowSize];

    // le fichier stocke les rangees de bas en haut ; on les replace
    // dans l'ordre haut-vers-bas en memoire.
    for (int fileRow = 0; fileRow < height; fileRow++) {
      readFully(in, rowBuffer);
      int memoryRow = height - 1 - fileRow;
      int destOffset = memoryRow * width * 3;
      for (int x = 0; x < width; x++) {
        byte b = rowBuffer[x * 3];
        byte g = rowBuffer[x * 3 + 1];
        byte r = rowBuffer[x * 3 + 2];
        pixels[destOffset + x * 3] = r;
        pixels[destOffset + x * 3 + 1] = g;
        pixels[destOffset + x * 3 + 2] = b;
      }
    }

    return new BmpImage(width, height, pixels);
  }

  public void writeTo(OutputStream out) throws IOException {
    BmpHeader header = new BmpHeader(width, height);
    header.writeTo(out);

    int rowSize = header.rowSizeWithPadding();
    byte[] rowBuffer = new byte[rowSize]; // reste a 0 = padding deja correct

    for (int fileRow = 0; fileRow < height; fileRow++) {
      int memoryRow = height - 1 - fileRow;
      int srcOffset = memoryRow * width * 3;
      for (int x = 0; x < width; x++) {
        byte r = pixels[srcOffset + x * 3];
        byte g = pixels[srcOffset + x * 3 + 1];
        byte b = pixels[srcOffset + x * 3 + 2];
        rowBuffer[x * 3] = b;
        rowBuffer[x * 3 + 1] = g;
        rowBuffer[x * 3 + 2] = r;
      }
      out.write(rowBuffer);
    }
  }

  /** Extrait la sous-image [x0, y0) a [x0+w, y0+h) (coordonnees en pixels, y=0 en haut). */
  public BmpImage crop(int x0, int y0, int w, int h) {
    if (x0 < 0 || y0 < 0 || w <= 0 || h <= 0 || x0 + w > width || y0 + h > height) {
      throw new IllegalArgumentException("crop region is out of bounds");
    }
    byte[] cropped = new byte[w * h * 3];
    for (int y = 0; y < h; y++) {
      int srcOffset = (y0 + y) * width * 3 + x0 * 3;
      int destOffset = y * w * 3;
      System.arraycopy(pixels, srcOffset, cropped, destOffset, w * 3);
    }
    return new BmpImage(w, h, cropped);
  }

  public int[] getPixel(int x, int y) {
    checkBounds(x, y);
    int offset = (y * width + x) * 3;
    return new int[] {
        pixels[offset] & 0xFF,
        pixels[offset + 1] & 0xFF,
        pixels[offset + 2] & 0xFF
    };
  }

  public void setPixel(int x, int y, int r, int g, int b) {
    checkBounds(x, y);
    int offset = (y * width + x) * 3;
    pixels[offset] = (byte) r;
    pixels[offset + 1] = (byte) g;
    pixels[offset + 2] = (byte) b;
  }

  private void checkBounds(int x, int y) {
    if (x < 0 || x >= width || y < 0 || y >= height) {
      throw new IndexOutOfBoundsException("pixel (" + x + ", " + y + ") is outside " + width + "x" + height);
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  private static void readFully(InputStream in, byte[] buf) throws IOException {
    int total = 0;
    while (total < buf.length) {
      int n = in.read(buf, total, buf.length - total);
      if (n == -1) {
        throw new IOException("Unexpected end of file while reading pixel data.");
      }
      total += n;
    }
  }

  private static void skipFully(InputStream in, int n) throws IOException {
    long remaining = n;
    while (remaining > 0) {
      long skipped = in.skip(remaining);
      if (skipped <= 0) {
        if (in.read() == -1) {
          throw new IOException("Unexpected end of file while skipping extra header bytes.");
        }
        skipped = 1;
      }
      remaining -= skipped;
    }
  }
}
