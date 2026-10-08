# MC-cli

A command-line tool to split an image into a grid of tiles, merge several
images into a grid, and (bonus) reconstruct an image as a photo mosaic —
built for PW1 (DAI course, HEIG-VD).

## Quick start

```sh
git clone https://github.com/nicolasreymond/MC-cli.git && cd MC-cli
./mvnw clean package
java -jar target/MC-cli.jar split examples/banque/forest_1.bmp out/ -r 3 -c 4
```

The 12 tiles land in `out/`. Requires JDK 25, see
[Requirements](#requirements).

## Description

MC-cli reads and writes uncompressed 24-bit BMP files using the Java I/O
API exclusively (`java.io`, no image library for file access) — see
[`BmpImage`](src/main/java/ch/heigvd/dai/bmp/BmpImage.java) and
[`BmpHeader`](src/main/java/ch/heigvd/dai/bmp/BmpHeader.java). BMP is a
simple, uncompressed format: the pixel bytes are stored as-is, one row
after another, which makes it a good fit for parsing the file format by
hand instead of relying on `javax.imageio`.

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

### `split` — cut an image into a grid of tiles

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
division) and the leftover pixels on the right/bottom edge are dropped
silently.

**Existing tiles**: if `outputDir` already contains tiles matching the
current grid size and suffix, MC-cli asks for confirmation before
overwriting them (`[y/N]`).

Example:

```sh
java -jar target/MC-cli.jar split examples/banque/forest_1.bmp out/ -r 3 -c 4
```

### `merge` — assemble several images into a grid (work in progress)

```sh
java -jar target/MC-cli.jar merge <inputDir> <outputFile> [-r <rows>] [-c <cols>]
```

Not implemented yet.

### `mosaic` — bonus: reconstruct an image as a photo mosaic (planned)

Not implemented yet.

## Project structure

```
src/main/java/ch/heigvd/dai/
├── Main.java                 # root picocli command
├── commandes/
│   ├── Splitter.java          # split
│   └── Merger.java            # merge
└── bmp/
    ├── BmpHeader.java          # BMP file/DIB header (read/write, little-endian)
    └── BmpImage.java           # full BMP image (header + pixels), crop, pixel access
```

## Tests

```sh
./mvnw test
```

## Authors

Nicolas Reymond, Aymeric Bonny
