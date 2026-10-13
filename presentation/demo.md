# Démo MC-cli

Toutes les commandes se lancent depuis la racine du projet, dans l'ordre.
Les résultats vont dans `demo/`, à ouvrir avec une visionneuse d'images.

## Avant la présentation

Construire le jar :

```sh
./mvnw clean package -DskipTests
```

Repartir d'un dossier `demo/` vide :

```sh
rm -rf demo
```

## 1. L'aide

Picocli génère l'aide de chaque commande.

```sh
java -jar target/MC-cli.jar --help
```

```sh
java -jar target/MC-cli.jar split --help
```

## 2. split : découper

La forêt (960×640) est découpée en 2 lignes et 3 colonnes, donc 6 tuiles de
320×320. Le dossier de sortie est créé s'il n'existe pas.

```sh
java -jar target/MC-cli.jar split examples/images/forest.bmp demo/tiles/ -r 2 -c 3
```

Les tuiles sont nommées `<ligne>_<colonne>_tile.bmp` :

```sh
ls demo/tiles/
```

## 3. merge : recoller

Sans option de disposition, merge retrouve la grille à partir des noms.

```sh
java -jar target/MC-cli.jar merge -d demo/tiles/ -o demo/rebuilt.bmp
```

Les pixels sont identiques à l'original. On saute les 54 octets d'en-tête,
car la résolution d'impression enregistrée dedans peut être différente :

```sh
cmp -i 54 demo/rebuilt.bmp examples/images/forest.bmp && echo "same pixels"
```

## 4. merge -R : répéter

Deux tuiles reprises en boucle (A B A B), ce qui donne une bande de 1280×320.

```sh
java -jar target/MC-cli.jar merge demo/tiles/0_0_tile.bmp demo/tiles/1_2_tile.bmp -R 4 -o demo/strip.bmp
```

## 5. mosaic : pixeliser

Chaque case prend la couleur moyenne de ses pixels. D'abord une grille
grossière de 8×12 cases, puis une fine de 32×48.

```sh
java -jar target/MC-cli.jar mosaic examples/images/forest.bmp -r 8 -c 12 -o demo/mosaic-coarse.bmp
```

```sh
java -jar target/MC-cli.jar mosaic examples/images/forest.bmp -r 32 -c 48 -o demo/mosaic-fine.bmp
```

Si la grille ne tombe pas juste, les pixels en trop sont ignorés avec un
avertissement (1024 n'est pas un multiple de 20, ni 819 de 16) :

```sh
java -jar target/MC-cli.jar mosaic examples/images/flowers.bmp -r 16 -c 20 -o demo/flowers-mosaic.bmp
```

## 6. Une erreur (optionnel)

Une option obligatoire manquante affiche l'aide et renvoie le code 2 :

```sh
java -jar target/MC-cli.jar mosaic examples/images/forest.bmp; echo "exit code: $?"
```

## Si ça plante

- `Could not write demo/...` : le dossier `demo/` n'existe pas, lancer
  d'abord le split (étape 2) ou `mkdir -p demo`.
- `Unable to access jarfile` : le jar n'est pas construit, relancer
  `./mvnw clean package -DskipTests`.
- En dernier recours, montrer les images avant / après de la slide 1.
