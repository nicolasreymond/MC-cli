# MC-cli

A command-line tool to split an image into a grid of tiles, merge several
images into a grid, and (bonus) reconstruct an image as a photo mosaic —
built for PW1 (DAI course, HEIG-VD).

## Quick start

```sh
git clone https://github.com/nicolasreymond/MC-cli.git && cd MC-cli
./mvnw clean package
java -jar target/MC-cli.jar split examples/images/forest.bmp out/ -r 3 -c 4
```

The 12 tiles land in `out/`. Requires JDK 25, see
[Requirements](#requirements).

## Table of contents

- [Quick start](#quick-start)
- [Description](#description)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
  - [`split`](#split)
  - [`merge`](#merge)
  - [`mosaic`](#mosaic)
- [Supported image format](#supported-image-format)
- [Exit codes](#exit-codes)
- [Troubleshooting](#troubleshooting)
- [Project structure](#project-structure)
- [Tests](#tests)
- [Contributing](#contributing)
- [Authors](#authors)

## Description

MC-cli works on uncompressed 24-bit BMP images (see
[Supported image format](#supported-image-format)). As required by the
assignment, files are opened, read and written with `java.io` only, by
parsing the BMP format by hand instead of using `javax.imageio`.

## Requirements

- **JDK 25** or newer, not just a JRE: the sources are compiled on your
  machine (`maven.compiler.release` is 25, so an older JDK fails at
  compile time). Check with `javac -version`.
- **Git**, to get the sources.
- No Maven install needed: the project ships the Maven wrapper
  (`./mvnw`, or `mvnw.cmd` on Windows).

## Installation

```sh
git clone https://github.com/nicolasreymond/MC-cli.git
cd MC-cli
./mvnw clean package
```

This produces an executable JAR at `target/MC-cli.jar`. Check that it
runs:

```sh
java -jar target/MC-cli.jar --version
```

It should print `mc-cli 1.0-SNAPSHOT`. `--help` lists the commands.

Optionally, add an alias to your shell configuration (`~/.bashrc`,
`~/.zshrc`) so you can type `mc-cli` instead of the full command:

```sh
alias mc-cli='java -jar /path/to/MC-cli/target/MC-cli.jar'
```

The examples below use the full `java -jar target/MC-cli.jar` form, run
from the project root.

## Usage

### `split`

Cuts an image into a grid of tiles.

```sh
java -jar target/MC-cli.jar split <inputFile> <outputDir> [-r <rows>] [-c <cols>] [-s <suffix>]
```

| Option | Default | Description |
|---|---|---|
| `<inputFile>` | — | Source BMP file (positional, required) |
| `<outputDir>` | — | Destination folder for the tiles (positional, required); created if it doesn't exist |
| `-r`, `--rows` | `2` | Number of grid rows |
| `-c`, `--cols` | `2` | Number of grid columns |
| `-s`, `--suffix` | `tile` | Text appended to each tile file name, after the row/column indices |
| `-h`, `--help` | — | Show the help message |

**Output filenames**: `<row>_<col>_<suffix>.bmp`, zero-padded so a plain
alphabetical sort of the output folder matches the grid's reading order
(row by row) — useful for a later `merge` of the same tiles. Example
with a 3×17 grid: `00_00_tile.bmp`, `00_01_tile.bmp`, ...

**Non-divisible dimensions**: if the image size isn't an exact multiple
of the grid, each tile is `width/cols` × `height/rows` (integer
division) and the leftover pixels on the right/bottom edge are dropped.
All tiles keep the same size, which keeps `merge` simple. A warning on
stderr tells how many pixels were lost; the exit code stays `0`.

**Existing tiles**: if some of the files this split is about to write
already exist in `outputDir`, MC-cli asks before overwriting them
(`[y/N]`). Answering anything but `y` (or just pressing Enter) keeps
them: the new tiles get the first free number after the suffix, the
same for the whole grid (`0_0_tile_1.bmp`, then `_2`, ...). Each set
still sorts in grid order, but to `merge` one set, keep it alone in its
folder or pass its files explicitly.

#### Example: split a photo into a 3×4 grid

The repository ships a few test images in `examples/images/`. Starting
from the project root, after building:

```sh
java -jar target/MC-cli.jar split examples/images/forest.bmp out/ -r 3 -c 4
```

Expected output:

```
Warning: 1 px dropped on the bottom edge (640 is not a multiple of 3 rows).
Split forest.bmp into a 3x4 grid (rows x cols) in out
```

`out/` now contains 12 tiles, named in reading order:

```
0_0_tile.bmp  0_1_tile.bmp  0_2_tile.bmp  0_3_tile.bmp
1_0_tile.bmp  1_1_tile.bmp  1_2_tile.bmp  1_3_tile.bmp
2_0_tile.bmp  2_1_tile.bmp  2_2_tile.bmp  2_3_tile.bmp
```

The source image is 960×640, so each tile is 240×213: 640 is not a
multiple of 3, which drops the last row of pixels (hence the warning).

| Before (`forest.bmp`) | After (12 tiles) |
|---|---|
| ![Original forest image](docs/images/forest.png) | ![The same image cut into a 3×4 grid of tiles](docs/images/forest-split.png) |

Running the same command again asks before overwriting. Answering `n`
keeps the first set and numbers the new one:

```
Tiles already exist in the output folder. Overwrite them? [y/N] n
Keeping the existing tiles; the new ones are named like 0_0_tile_1.bmp.
```

Use `-s` to give the tiles another name, e.g. `-s part` produces
`0_0_part.bmp`, `0_1_part.bmp`, ...

### `merge`

Assembles several BMP images into a single image, laid out on a grid.

```sh
java -jar target/MC-cli.jar merge (<files>... | -d <dir>) -o <outputFile> [-r <rows> -c <cols> | -v | -H] [-R <n>]
```

| Option | Default | Description |
|---|---|---|
| `<files>...` | — | BMP images to merge, in reading order (positional) |
| `-d`, `--directory` | — | Folder whose `.bmp` files are merged, sorted by name. Use either `<files>` or `-d` |
| `-o`, `--output` | — | Output BMP file (required) |
| `-r`, `--rows` / `-c`, `--cols` | — | Grid size; give both together |
| `-v`, `--vertical` | — | Stack the images vertically (1 column) |
| `-H`, `--horizontal` | — | Put the images side by side (1 row) |
| `-R`, `--repeat` | — | Use the input images N times in total, cycling through them (A B C A B ...) |
| `-h`, `--help` | — | Show the help message |

**Layout**: `-r`/`-c`, `-v` and `-H` are mutually exclusive. Without any
of them:

- if every file name starts with `<row>_<col>_`, as the tiles written by
  `split`, each image goes to its row and column. `merge -d` on a `split`
  folder therefore rebuilds the original image;
- otherwise the images are put side by side, in one row.

**Size**: every cell takes the size of the first image, so the output is
`cols × width` by `rows × height` of that image. Images fill the grid in
reading order (left to right, then top to bottom). If there are more
images than cells, the extra ones are ignored and a warning is printed.

#### Example: rebuild a split image

```sh
java -jar target/MC-cli.jar split examples/images/forest.bmp tiles/ -r 2 -c 2
java -jar target/MC-cli.jar merge -d tiles/ -o rebuilt.bmp
```

`rebuilt.bmp` is identical to `forest.bmp`, pixel for pixel. With a
grid that does not divide the image (e.g. `-r 3 -c 4`), it lacks the
pixels that `split` dropped: 960×639 instead of 960×640.

Other layouts:

```sh
# two tiles side by side: 960x320
java -jar target/MC-cli.jar merge tiles/0_0_tile.bmp tiles/1_1_tile.bmp -H -o pair.bmp

# four images in a 2x2 grid, in the order given
java -jar target/MC-cli.jar merge a.bmp b.bmp c.bmp d.bmp -r 2 -c 2 -o grid.bmp

# 3 images in a row, cycling through the two given (A B A): 1440x320
java -jar target/MC-cli.jar merge tiles/0_0_tile.bmp tiles/0_1_tile.bmp -R 3 -o repeat.bmp
```

**Known limitations**, being fixed: a file that does not exist is
skipped, a missing tile leaves a black cell, images of different sizes
are cropped or padded to the first one, and a folder holding several
sets of tiles (`tile`, `tile_1`, ...) is not handled (#29). Keep one set
of tiles per folder.

### `mosaic`

Bonus: rebuilds an image as a mosaic of solid-color tiles (pixel art
effect).

```sh
java -jar target/MC-cli.jar mosaic <inputFile> -o <outputFile> [-r <rows>] [-c <cols>]
```

| Option | Default | Description |
|---|---|---|
| `<inputFile>` | — | Source BMP file (positional, required) |
| `-o`, `--output` | — | Output BMP file (required) |
| `-r`, `--rows` | `20` | Number of rows of tiles |
| `-c`, `--cols` | `20` | Number of columns of tiles |
| `-h`, `--help` | — | Show the help message |

**How it works**: the three steps of the project in one command, in
memory (no intermediate files):

1. **split**: the image is cut into `rows × cols` tiles, with the same
   rules as `split` (all tiles have the same size, leftover pixels on the
   right/bottom edge are dropped with a warning);
2. **recolor**: each tile is filled with its own average color: the mean
   of the red, green and blue values of all its pixels, rounded;
3. **merge**: the tiles are put back at their place.

The output has the size of the tiles put together, i.e. the input minus
the dropped pixels. More tiles keep more details; fewer tiles give
bigger blocks. With one tile per pixel (`-r <height> -c <width>`), the
image is unchanged.

#### Example: the forest as a mosaic

```sh
java -jar target/MC-cli.jar mosaic examples/images/forest.bmp -o forest-mosaic.bmp -r 16 -c 24
```

Expected output:

```
Mosaic of forest.bmp (16x24 tiles, rows x cols) written to forest-mosaic.bmp
```

| Original | 16×24 tiles | 32×48 tiles |
|---|---|---|
| ![Original forest image](docs/images/forest.png) | ![Forest as a mosaic of 16x24 solid-color tiles](docs/images/forest-mosaic-24.png) | ![Forest as a mosaic of 32x48 solid-color tiles](docs/images/forest-mosaic-48.png) |

To keep square tiles, choose `rows` and `cols` in the same ratio as the
image: `forest.bmp` is 960×640 (3:2), so `-r 16 -c 24` gives
40×40 px tiles.

## Supported image format

MC-cli only reads and writes **BMP** files with these properties:

| Property | Required value |
|---|---|
| Signature | `BM` |
| DIB header | `BITMAPINFOHEADER` (40 bytes) |
| Bits per pixel | 24 (one byte each for blue, green, red) |
| Compression | none (`BI_RGB`) |
| Row order | bottom-up (positive height) |

Inside the file, all multi-byte fields are little-endian, pixels are
stored as B, G, R, and each row is padded with zeros to a multiple of
4 bytes. Tiles written by MC-cli always follow this format.

### Converting an image

Many tools write BMP files with a larger header (V4/V5, 108 or 124
bytes) or 32 bits per pixel, which MC-cli rejects. To convert any image
(PNG, JPEG, or another BMP) with [ImageMagick](https://imagemagick.org):

```sh
magick input.png -type TrueColor BMP3:output.bmp
```

`BMP3:` forces the 40-byte header and `-type TrueColor` forces 24 bits
per pixel. In GIMP, use *File → Export As…* with a `.bmp` name, tick
*Do not write color space information*, and pick *24 bits R8 G8 B8*
under *Advanced Options*.

## Exit codes

| Code | Meaning |
|---|---|
| `0` | Success |
| `1` | Error while running the command (missing file, unreadable or unsupported image, invalid grid, write failure) |
| `2` | Invalid usage (unknown option, missing argument); the usage message is printed |

## Troubleshooting

Errors about the image file itself are printed as
`Error: Could not read the image: <message>`.

| Message | Cause and fix |
|---|---|
| `Error: Input file does not exist.` | Check the path to the source image (relative paths start from the current directory). |
| `Error: Input is not a file: ...` | The input path is a folder. Give the path to a `.bmp` file. |
| `Not a BMP file: missing 'BM' signature.` | The file is not a BMP (e.g. a renamed PNG). [Convert it](#converting-an-image). |
| `Unsupported BMP variant: expected a 40-byte BITMAPINFOHEADER, got 124.` | V4/V5 BMP, as written by default by many editors. [Convert it](#converting-an-image) with `BMP3:`. |
| `Unsupported BMP: only 24-bit RGB is supported, got 32 bits/pixel.` | Image with an alpha channel or a palette. [Convert it](#converting-an-image) with `-type TrueColor`. |
| `Unsupported BMP: compressed bitmaps are not supported.` | RLE-compressed BMP. [Convert it](#converting-an-image). |
| `Unsupported BMP: top-down bitmaps (negative height) are not supported.` | Rows stored top to bottom. [Convert it](#converting-an-image). |
| `Invalid BMP: width and height must be positive.` | The header announces an empty image: the file is corrupted. |
| `Unexpected end of file ...` | The file is truncated or corrupted. |
| `Error: a 3x4 grid (rows x cols) is too large for a 2x2 image.` | More rows or columns than pixels. Use a smaller grid. |
| `Error: rows and cols must be positive.` | `-r` and `-c` must be at least 1. |
| `Error: No valid BMP files found` (`merge`) | None of the given files exists or ends with `.bmp`, or the `-d` folder holds no `.bmp` file. |
| `Missing required option: '--output=<outputFile>'` (`merge`) | `merge` needs `-o <file>` to know where to write the result. |
| `Error: Input file does not exist or is not a file: ...` (`mosaic`) | Check the path to the source image; it must be a `.bmp` file, not a folder. |

## Project structure

```
MC-cli/
├── pom.xml                         # Maven build: Java 25, picocli, JUnit 5, executable JAR
├── mvnw, mvnw.cmd, .mvn/           # Maven wrapper (no Maven install needed)
├── src/
│   ├── main/java/ch/heigvd/dai/
│   │   ├── Main.java               # root `mc-cli` command, registers the subcommands
│   │   ├── commandes/
│   │   │   ├── Splitter.java       # `split` command
│   │   │   ├── Merger.java         # `merge` command
│   │   │   └── Mosaic.java         # `mosaic` command (bonus)
│   │   └── bmp/
│   │       ├── BmpHeader.java      # 54-byte BMP header: read, validate, write (little-endian)
│   │       └── BmpImage.java       # whole image: pixel I/O, crop, get/set pixel
│   └── test/java/ch/heigvd/dai/
│       ├── bmp/BmpImageTest.java   # BMP reading/writing, pixel order, errors, crop
│       └── commandes/
│           ├── SplitterTest.java   # `split` run on generated images
│           └── MosaicTest.java     # average colors and the `mosaic` command
├── examples/images/                # sample 24-bit BMP images to try the commands
└── docs/images/                    # PNG screenshots used in this README
```

The code is split in two layers:

- **`bmp`** is the only package that opens, reads or writes image files,
  using `java.io` only, as required by the assignment. In memory, an
  image is a plain `byte[]` of R, G, B values, top row first, without
  padding, so the commands never deal with the on-disk layout.
- **`commandes`** holds one [picocli](https://picocli.info) class per
  subcommand. Each one parses its options, validates them, and works on
  `BmpImage` objects.

To add a command, create a class in `commandes` that implements
`Callable<Integer>`, annotate it with `@Command`, and add it to the
`subcommands` list in `Main`.

## Tests

```sh
./mvnw test
```

## Contributing

The two authors work through GitHub issues and pull requests:

1. Pick an issue and assign yourself (`gh issue edit <n> --add-assignee @me`).
2. Create a branch from `dev`, named after the kind of change:
   `feat/...`, `fix/...`, `docs/...`, `test/...`, `chore/...`.
3. Commit in small steps using
   [Conventional Commits](https://www.conventionalcommits.org) messages
   (`feat(merge): ...`, `fix(split): ...`, `docs: ...`), and tick the
   matching task boxes in the issue as you go.
4. Open a pull request to `dev` with `Closes #<n>` in its description and
   ask the other author for a review.
5. Once `dev` is stable, it is merged into `main` through a pull request.

Make sure `./mvnw clean package` succeeds before opening a pull request.

## Authors

- Nicolas Reymond ([@nicolasreymond](https://github.com/nicolasreymond))
- Aymeric Bonny ([@albonny](https://github.com/albonny))
