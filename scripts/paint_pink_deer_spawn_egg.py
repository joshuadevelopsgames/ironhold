#!/usr/bin/env python3
"""Paint the 16x16 pink deer spawn egg and a nearest-neighbor preview."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
TEXTURE = (
    ROOT
    / "src/main/resources/assets/ironhold/textures/item/pink_deer_spawn_egg.png"
)
PREVIEW = ROOT / "tmp/texture_outputs/pink_deer_spawn_egg_preview.png"

TRANSPARENT = (0, 0, 0, 0)
OUTLINE = (105, 45, 67, 255)
SHADOW = (165, 65, 94, 255)
MID = (218, 105, 133, 255)
LIGHT = (244, 151, 169, 255)
HIGHLIGHT = (255, 194, 204, 255)
MUZZLE = (255, 213, 218, 255)
ANTLER_DARK = (91, 48, 38, 255)
ANTLER = (139, 76, 53, 255)
ANTLER_LIGHT = (190, 112, 76, 255)
EYE = (42, 20, 28, 255)
EYE_GLEAM = (255, 238, 239, 255)
NOSE = (119, 39, 62, 255)


def paint() -> Image.Image:
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    pixels = image.load()

    def set_pixels(color, coordinates):
        for x, y in coordinates:
            pixels[x, y] = color

    # Branched antlers, kept narrow enough to leave a readable gap above the ears.
    set_pixels(
        ANTLER_DARK,
        [
            (3, 0), (12, 0),
            (2, 1), (3, 1), (12, 1), (13, 1),
            (1, 2), (2, 2), (3, 2), (4, 2),
            (11, 2), (12, 2), (13, 2), (14, 2),
            (2, 3), (3, 3), (4, 3), (11, 3), (12, 3), (13, 3),
            (3, 4), (4, 4), (11, 4), (12, 4),
            (4, 5), (11, 5),
        ],
    )
    set_pixels(
        ANTLER,
        [
            (3, 1), (12, 1),
            (2, 2), (3, 2), (12, 2), (13, 2),
            (3, 3), (4, 3), (11, 3), (12, 3),
            (4, 4), (11, 4),
        ],
    )
    set_pixels(ANTLER_LIGHT, [(3, 0), (12, 0), (1, 2), (14, 2)])

    # Wide ears and tapered head silhouette.
    rows = {
        4: (1, 14),
        5: (0, 15),
        6: (1, 14),
        7: (3, 12),
        8: (3, 12),
        9: (3, 12),
        10: (4, 11),
        11: (4, 11),
        12: (5, 10),
        13: (5, 10),
        14: (6, 9),
    }
    for y, (start, end) in rows.items():
        for x in range(start, end + 1):
            pixels[x, y] = OUTLINE

    # Ear interiors.
    set_pixels(SHADOW, [(1, 5), (2, 5), (13, 5), (14, 5), (2, 6), (13, 6)])
    set_pixels(
        HIGHLIGHT,
        [(2, 5), (3, 5), (12, 5), (13, 5), (3, 6), (12, 6)],
    )

    # Face planes: bright upper-left, darker lower-right.
    set_pixels(
        MID,
        [
            (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5),
            (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6),
            (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7),
            (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8),
            (4, 9), (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9), (11, 9),
            (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10),
            (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11),
        ],
    )
    set_pixels(
        LIGHT,
        [
            (5, 5), (6, 5), (7, 5),
            (4, 6), (5, 6), (6, 6), (7, 6), (8, 6),
            (4, 7), (5, 7), (6, 7), (7, 7),
            (4, 8), (5, 8), (6, 8), (7, 8),
            (5, 9), (6, 9),
        ],
    )
    set_pixels(HIGHLIGHT, [(6, 5), (5, 6), (6, 6), (4, 7)])
    set_pixels(SHADOW, [(11, 7), (11, 8), (10, 9), (11, 9), (10, 10)])

    # Fawn spots, eyes, and pale muzzle.
    set_pixels(HIGHLIGHT, [(7, 6), (9, 6), (8, 7)])
    set_pixels(EYE, [(5, 8), (10, 8), (5, 9), (10, 9)])
    set_pixels(EYE_GLEAM, [(5, 8), (10, 8)])
    set_pixels(
        MUZZLE,
        [
            (6, 10), (7, 10), (8, 10), (9, 10),
            (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11),
            (6, 12), (7, 12), (8, 12), (9, 12),
            (6, 13), (7, 13), (8, 13), (9, 13),
        ],
    )
    set_pixels(NOSE, [(7, 11), (8, 11), (7, 12), (8, 12)])
    set_pixels(EYE, [(7, 12), (8, 12)])

    return image


def make_preview(image: Image.Image) -> Image.Image:
    preview = Image.new("RGBA", (240, 136), (28, 28, 32, 255))
    draw = ImageDraw.Draw(preview)

    for x in range(0, preview.width, 8):
        for y in range(0, preview.height, 8):
            color = (68, 68, 74, 255) if (x // 8 + y // 8) % 2 else (51, 51, 57, 255)
            draw.rectangle((x, y, x + 7, y + 7), fill=color)

    for scale, x, y in [(1, 8, 8), (4, 48, 8), (8, 112, 4)]:
        resized = image.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST)
        preview.alpha_composite(resized, (x, y))

    return preview


def main():
    image = paint()
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    image.save(TEXTURE)
    make_preview(image).save(PREVIEW)
    print(f"Saved {TEXTURE}")
    print(f"Saved {PREVIEW}")


if __name__ == "__main__":
    main()
