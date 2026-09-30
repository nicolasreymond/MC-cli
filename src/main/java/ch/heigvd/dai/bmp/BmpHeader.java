package ch.heigvd.dai.bmp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * En-tete d'un fichier BMP non compresse (file header 14 octets + DIB
 * header BITMAPINFOHEADER 40 octets), tel que decrit au chapitre 04
 * (Java I/O) du cours : tous les champs multi-octets sont stockes en
 * little-endian.
 *
 * Reference des offsets (relatifs au debut du fichier) :
 *   0x00 (2o)  "BM"              signature
 *   0x02 (4o)  taille du fichier
 *   0x0A (4o)  offset du debut des donnees pixel
 *   0x0E (4o)  taille de l'en-tete DIB (= 40 pour BITMAPINFOHEADER)
 *   0x12 (4o)  largeur (pixels)
 *   0x16 (4o)  hauteur (pixels)
 *   0x1C (2o)  bits par pixel (24 = RVB)
 *   0x1E (4o)  compression (doit valoir 0 : non compresse)
 */
public class BmpHeader {

  public static final int FILE_HEADER_SIZE = 14;
  public static final int DIB_HEADER_SIZE = 40;
  public static final int TOTAL_HEADER_SIZE = FILE_HEADER_SIZE + DIB_HEADER_SIZE;

  public static final byte SIGNATURE_B = 'B';
  public static final byte SIGNATURE_M = 'M';

  private static final int BYTES_PER_PIXEL = 3; // RVB 24 bits, seul format supporte

  private int fileSize;
  private int pixelDataOffset;
  private int width;
  private int height;
  private short bitsPerPixel;

  public BmpHeader(int width, int height) {
    this.width = width;
    this.height = height;
    this.bitsPerPixel = 24;
    this.pixelDataOffset = TOTAL_HEADER_SIZE;
    this.fileSize = TOTAL_HEADER_SIZE + rowSizeWithPadding() * height;
  }

  private BmpHeader() {
  }

  public static BmpHeader readFrom(InputStream in) throws IOException {
    byte[] buf = new byte[TOTAL_HEADER_SIZE];
    int total = 0;
    while (total < buf.length) {
      int n = in.read(buf, total, buf.length - total);
      if (n == -1) {
        throw new IOException("Unexpected end of file: BMP header is truncated.");
      }
      total += n;
    }

    if (buf[0] != SIGNATURE_B || buf[1] != SIGNATURE_M) {
      throw new IOException("Not a BMP file: missing 'BM' signature.");
    }

    ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);

    BmpHeader header = new BmpHeader();
    header.fileSize = bb.getInt(2);
    header.pixelDataOffset = bb.getInt(10);

    int dibHeaderSize = bb.getInt(14);
    if (dibHeaderSize != DIB_HEADER_SIZE) {
      throw new IOException(
          "Unsupported BMP variant: expected a " + DIB_HEADER_SIZE
              + "-byte BITMAPINFOHEADER, got " + dibHeaderSize + ".");
    }

    header.width = bb.getInt(18);
    header.height = bb.getInt(22);
    header.bitsPerPixel = bb.getShort(28);

    int compression = bb.getInt(30);
    if (compression != 0) {
      throw new IOException("Unsupported BMP: compressed bitmaps are not supported.");
    }
    if (header.bitsPerPixel != 24) {
      throw new IOException(
          "Unsupported BMP: only 24-bit RGB is supported, got " + header.bitsPerPixel + " bits/pixel.");
    }
    if (header.width <= 0 || header.height <= 0) {
      throw new IOException("Unsupported BMP: top-down bitmaps (negative height) are not supported.");
    }

    return header;
  }

  public void writeTo(OutputStream out) throws IOException {
    ByteBuffer bb = ByteBuffer.allocate(TOTAL_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);

    bb.put(SIGNATURE_B);
    bb.put(SIGNATURE_M);
    bb.putInt(2, fileSize);
    bb.putInt(6, 0); // reserve
    bb.putInt(10, pixelDataOffset);

    bb.putInt(14, DIB_HEADER_SIZE);
    bb.putInt(18, width);
    bb.putInt(22, height);
    bb.putShort(26, (short) 1); // planes
    bb.putShort(28, bitsPerPixel);
    bb.putInt(30, 0); // compression
    bb.putInt(34, rowSizeWithPadding() * height); // taille des donnees pixel
    bb.putInt(38, 2835); // ~72 DPI
    bb.putInt(42, 2835);
    bb.putInt(46, 0); // couleurs utilisees
    bb.putInt(50, 0); // couleurs importantes

    out.write(bb.array());
  }

  /** Taille d'une rangee de pixels, alignee sur un multiple de 4 octets. */
  public int rowSizeWithPadding() {
    int rawRowSize = width * BYTES_PER_PIXEL;
    return (rawRowSize + 3) & ~3;
  }

  public int getFileSize() {
    return fileSize;
  }

  public int getPixelDataOffset() {
    return pixelDataOffset;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public short getBitsPerPixel() {
    return bitsPerPixel;
  }
}
