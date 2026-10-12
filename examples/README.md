# Sample images

Uncompressed 24-bit BMP images (40-byte `BITMAPINFOHEADER`) to try the
`split`, `merge` and `mosaic` commands. They come from Wikimedia Commons and
were cropped, resized and converted to BMP with ImageMagick:

```sh
magick source.jpg -resize WxH^ -gravity center -extent WxH -strip -type TrueColor BMP3:image.bmp
```

| File | Size | Source | Author | Licence |
|---|---|---|---|---|
| `images/forest.bmp` | 960×640 | [Beauchamp Falls, Great Otway National Park](https://commons.wikimedia.org/wiki/File:Beech_Forest_(AU),_Great_Otway_National_Park,_Beauchamp_Falls_--_2019_--_1271.jpg) | Dietmar Rabich | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| `images/flowers.bmp` | 1024×819 | [African daisy bud (Osteospermum) with water droplets](https://commons.wikimedia.org/wiki/File:African_daisy_bud_(Osteospermum)_with_water_droplets,_Ponte_de_Sor,_Portugal_(approx._GPS_location)_julesvernex2.jpg) | Jules Verne Times Two | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| `images/abstract-art.bmp` | 717×1024 | ['Fathers will live', 1993](https://commons.wikimedia.org/wiki/File:%27Fathers_will_live%27_-_large_colorful,_abstract_painting_on_canvas,_painted_in_1993_in_acrylic_paint_by_Dutch_artist_Fons_Heijnsbroek._Abstract_expressionism_art_in_free_images_%26_high_resolution.jpg) | Fons Heijnsbroek | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) |

The two CC BY-SA images, and the screenshots made from `forest.bmp` in
[`docs/images/`](../docs/images/), are shared under the same licence.
