# MC-cli

<!-- TODO: une phrase de pitch -->

## Description

<!-- TODO: ce que fait l'outil, pourquoi BMP, contexte PW1 -->

## Installation / build

```sh
./mvnw clean package
```

## Usage

```sh
java -jar target/MC-cli.jar split <image.bmp> --cols=<N> --rows=<M> --out-dir=<dossier>
java -jar target/MC-cli.jar merge <dossier> --cols=<N> --rows=<M> <sortie.bmp>
java -jar target/MC-cli.jar mosaic <cible.bmp> <banque/> <sortie.bmp>
```

<!-- TODO: exemples concrets avec captures/sorties -->

## Structure du projet

<!-- TODO -->

## Tests

```sh
./mvnw test
```

## Auteurs

<!-- TODO: noms du groupe -->
