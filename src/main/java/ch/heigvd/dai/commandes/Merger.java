package ch.heigvd.dai.commandes;

import ch.heigvd.dai.Main;
import ch.heigvd.dai.bmp.BmpImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List; // To merge a list of images
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

@Command(name = "merge",
         description = "Merge a grid of images into a single image.")
public class Merger implements Callable<Integer> {

  @ParentCommand protected Main parent;

  // Mutually exclusive inputs
  @ArgGroup(exclusive = true,
            multiplicity = "1")
  private InputSources inputSources;

  static class InputSources {
    @Parameters(paramLabel = "<files>",
                arity = "1..*",
                description = "Liste des images à fusionner (ex: img1.png img2.png).")
    List<File> files;

    @Option(names = {"-d", "--directory"},
            description = "Dossier contenant les images à fusionner.")
    File directory;
  }

  // Output file
  @Option(names = {"-o", "--output"},
          required = true,
          description = "Fichier d'image de sortie (ex: result.png).")
  private File outputFile;

  // Repeat a single image
  @Option(names = {"-R", "--Repeat"},
          description = "Répète une image d'entrée N fois (utile avec un seul fichier en entrée).")
  private Integer repeat;

  // Mutually exclusive layouts
  @ArgGroup(exclusive = true,
            multiplicity = "0..1")
  private LayoutOptions layout;

  static class LayoutOptions {
    @Option(names = {"-v", "--vertical"},
            description = "Fusion verticale (1 colonne, N lignes).")
    boolean vertical;

    @Option(names = {"-h", "--horizontal"},
            description = "Fusion horizontale (1 ligne, N colonnes).")
    boolean horizontal;

    // Non-mutually exclusive subgroup to force the usage of both rows and columns
    @ArgGroup(exclusive = false)
    GridOptions grid;
  }

  static class GridOptions {
    @Option(names = {"-r", "--rows"},
            required = true,
            description = "Nombre de lignes de la grille finale.")
    int rows;

    @Option(names = {"-c", "--cols"},
            required = true,
            description = "Nombre de colonnes de la grille finale.")
    int cols;
  }

  @Override
  public Integer call() {

    if (repeat != null && (inputSources.files == null || inputSources.files.size() != 1)) {
      System.err.println("Error : The --repeat option can only be used when the input is a single image");
      return 1;
    }

    List<File> imagesToProcess = new ArrayList<>();

    if (inputSources.directory != null) {
      // Lists the files in the directory
      System.out.println("Lecture depuis le dossier : " + inputSources.directory);
      File[] dirFiles = inputSources.directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".bmp"));
      if (dirFiles != null && dirFiles.length > 0) {
          Arrays.sort(dirFiles); // Tri très important pour que les tuiles restent dans le bon ordre (ex: 0_0, 0_1...)
          imagesToProcess.addAll(Arrays.asList(dirFiles)); //TODO checker si y a pas mieux
        } else {
          System.err.println("Error: No BMP files found in directory.");
          return 1;
        }
    } else {
      System.out.println("Lecture des fichiers : " + inputSources.files);
        if (repeat != null) {
          int nbFiles = inputSources.files.size();
          for (int i = 0; i < repeat; i++) imagesToProcess.add(inputSources.files.get(i % nbFiles));
        } else imagesToProcess.addAll(inputSources.files);
    }

    //TODO ce serait mieux de checker ça dans le if else au dessus, et de checker par la meme occasion si les files dans le else sont tous des bmp, et ensuite les mettre dans l'ordre
    if (imagesToProcess.isEmpty()) {
        System.err.println("Error: No input images to process.");
        return 1;
    }

    // Find out the final layout
    if (layout != null) {
      if (layout.vertical) System.out.println("Mode : Vertical");
      else if (layout.horizontal) System.out.println("Mode : Horizontal");
      else if (layout.grid != null)
        System.out.printf("Mode : Grille (%d lignes, %d colonnes)%n", layout.grid.rows, layout.grid.cols);
    } else System.out.println("Mode par défaut (déduction automatique via les noms de fichiers)");


    return 0;
  }
}
/**
 * @Override
 *   public Integer call() {
 *
 *
 *
 *     // 2. Déduire le layout final (rows et cols)
 *     int r = 1;
 *     int c = imagesToProcess.size(); // Par défaut: Horizontal
 *
 *     if (layout != null) {
 *       if (layout.vertical) {
 *         r = imagesToProcess.size();
 *         c = 1;
 *         System.out.println("Mode : Vertical");
 *       } else if (layout.horizontal) {
 *         r = 1;
 *         c = imagesToProcess.size();
 *         System.out.println("Mode : Horizontal");
 *       } else if (layout.grid != null) {
 *         r = layout.grid.rows;
 *         c = layout.grid.cols;
 *         System.out.printf("Mode : Grille (%d lignes, %d colonnes)%n", r, c);
 *       }
 *     } else {
 *       System.out.println("Mode par défaut (Horizontal)");
 *     }
 *
 *     if (r * c < imagesToProcess.size()) {
 *       System.err.println("Warning: The layout (" + r + "x" + c + ") is too small for " + imagesToProcess.size() + " images. Some will be ignored.");
 *     }
 *
 *     try {
 *       // 3. Lire la première image pour déterminer la taille de chaque cellule
 *       BmpImage firstImage;
 *       try (FileInputStream in = new FileInputStream(imagesToProcess.get(0))) {
 *         firstImage = BmpImage.readFrom(in);
 *       }
 *
 *       int cellWidth = firstImage.getWidth();
 *       int cellHeight = firstImage.getHeight();
 *
 *       // 4. Créer le canvas final avec BmpImage.create()
 *       int finalWidth = cellWidth * c;
 *       int finalHeight = cellHeight * r;
 *       BmpImage finalImage = BmpImage.create(finalWidth, finalHeight);
 *
 *       // 5. Parcourir la grille et copier les pixels des images source
 *       int imgIndex = 0;
 *       for (int row = 0; row < r; row++) {
 *         for (int col = 0; col < c; col++) {
 *
 *           if (imgIndex >= imagesToProcess.size()) {
 *             break; // Il n'y a plus d'images à placer, le reste sera noir
 *           }
 *
 *           File currentFile = imagesToProcess.get(imgIndex);
 *           BmpImage currentImg;
 *
 *           try (FileInputStream in = new FileInputStream(currentFile)) {
 *             currentImg = BmpImage.readFrom(in);
 *           }
 *
 *           // Vérification de la taille pour éviter les exceptions
 *           int drawW = Math.min(cellWidth, currentImg.getWidth());
 *           int drawH = Math.min(cellHeight, currentImg.getHeight());
 *
 *           int startX = col * cellWidth;
 *           int startY = row * cellHeight;
 *
 *           // Copie des pixels (java I/O pur respecté via la classe BmpImage)
 *           for (int y = 0; y < drawH; y++) {
 *             for (int x = 0; x < drawW; x++) {
 *               int[] rgb = currentImg.getPixel(x, y);
 *               finalImage.setPixel(startX + x, startY + y, rgb[0], rgb[1], rgb[2]);
 *             }
 *           }
 *           imgIndex++;
 *         }
 *       }
 *
 *       // 6. Écrire le résultat dans le fichier de sortie
 *       try (FileOutputStream out = new FileOutputStream(outputFile)) {
 *         finalImage.writeTo(out);
 *       }
 *
 *       System.out.println("Fusion terminée avec succès : " + outputFile.getAbsolutePath());
 *       return 0; // Succès
 *
 *     } catch (Exception e) {
 *       System.err.println("Une erreur s'est produite lors de la fusion : " + e.getMessage());
 *       return 1; // Erreur
 *     }
 *   }
 * }
 *
 * Commentaire Gemini:
 * Ordre garanti pour les dossiers : Si tu passes l'option -d, le programme trie les fichiers par nom
 * (Arrays.sort(dirFiles)) avant de les traiter. Le code du Splitter de ton collègue inclut des zéros dans les noms
 * (00_00_tile.bmp) pour assurer que le tri alphabétique corresponde à l'ordre de lecture de la grille. C'est donc
 * parfaitement raccord !
 */
