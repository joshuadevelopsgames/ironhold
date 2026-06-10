#!/usr/bin/env python3
"""Paint a 16x16 Filcher spawn egg based on the entity model and texture."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
TEXTURE = (
    ROOT
    / "src/main/resources/assets/ironhold/textures/item/filcher_spawn_egg.png"
)
PREVIEW = ROOT / "tmp/texture_outputs/filcher_spawn_egg_preview.png"

TRANSPARENT = (0, 0, 0, 0)
OUTLINE = (18, 13, 26, 255)
DEEP_SHADOW = (21, 16, 29, 255)
SHADOW = (28, 22, 38, 255)
MID = (46, 39, 60, 255)
LIGHT = (62, 54, 78, 255)
COOL_HIGHLIGHT = (83, 97, 116, 255)
EYE = (255, 200, 0, 255)
EYE_LIGHT = (241, 206, 79, 255)
GOLD_DARK = (160, 120, 20, 255)
GOLD = (218, 176, 44, 255)
GOLD_LIGHT = (232, 198, 91, 255)
GOLD_GLEAM = (248, 221, 114, 255)
GEM = (183, 33, 33, 255)


def paint() -> Image.Image:
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    pixels = image.load()

    def set_pixels(color, coordinates):
        for x, y in coordinates:
            pixels[x, y] = color

    # The model has a thin square band, a tall center prong, and shorter sides.
    set_pixels(GOLD_DARK, [(4, 0), (7, 0), (8, 0), (11, 0)])
    set_pixels(
        GOLD,
        [
            (4, 1), (7, 1), (8, 1), (11, 1),
            (3, 2), (4, 2), (5, 2), (6, 2), (7, 2), (8, 2),
            (9, 2), (10, 2), (11, 2), (12, 2),
        ],
    )
    set_pixels(GOLD_LIGHT, [(4, 0), (7, 0), (4, 1), (7, 1), (8, 1)])
    set_pixels(GOLD_GLEAM, [(7, 0)])
    set_pixels(GEM, [(8, 2)])

    # Oversized square head with the shallow side blocks visible in the model.
    rows = {
        3: (3, 12),
        4: (2, 13),
        5: (1, 14),
        6: (1, 14),
        7: (1, 14),
        8: (1, 14),
        9: (2, 13),
        10: (2, 13),
        11: (2, 13),
        12: (3, 12),
        13: (3, 12),
        14: (4, 11),
    }
    for y, (start, end) in rows.items():
        for x in range(start, end + 1):
            pixels[x, y] = OUTLINE

    # Front planes retain the entity texture's plum and cool gray-purple palette.
    set_pixels(
        SHADOW,
        [
            (4, 3), (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3), (11, 3),
            (3, 4), (4, 4), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4),
            (11, 4), (12, 4),
            (2, 5), (3, 5), (4, 5), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5),
            (10, 5), (11, 5), (12, 5), (13, 5),
            (2, 6), (3, 6), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6),
            (10, 6), (11, 6), (12, 6), (13, 6),
            (2, 7), (3, 7), (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7),
            (10, 7), (11, 7), (12, 7), (13, 7),
            (2, 8), (3, 8), (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8),
            (10, 8), (11, 8), (12, 8), (13, 8),
            (3, 9), (4, 9), (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9),
            (11, 9), (12, 9),
            (3, 10), (4, 10), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10),
            (10, 10), (11, 10), (12, 10),
            (3, 11), (4, 11), (5, 11), (6, 11), (7, 11), (8, 11), (9, 11),
            (10, 11), (11, 11), (12, 11),
            (4, 12), (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12),
            (11, 12),
            (4, 13), (5, 13), (6, 13), (7, 13), (8, 13), (9, 13), (10, 13),
            (11, 13),
            (5, 14), (6, 14), (7, 14), (8, 14), (9, 14), (10, 14),
        ],
    )
    set_pixels(
        MID,
        [
            (4, 4), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4),
            (3, 5), (4, 5), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5),
            (3, 6), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6),
            (3, 7), (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7),
            (3, 8), (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8),
            (4, 9), (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9),
            (4, 10), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10),
            (5, 11), (6, 11), (7, 11), (8, 11), (9, 11),
            (5, 12), (6, 12), (7, 12), (8, 12), (9, 12),
        ],
    )
    set_pixels(
        LIGHT,
        [
            (4, 4), (5, 4), (6, 4),
            (3, 5), (4, 5), (5, 5), (6, 5),
            (3, 6), (4, 6), (5, 6),
            (3, 7), (4, 7),
            (4, 8), (5, 8),
        ],
    )
    set_pixels(COOL_HIGHLIGHT, [(4, 4), (3, 5), (4, 5), (3, 6)])
    set_pixels(
        DEEP_SHADOW,
        [
            (12, 5), (13, 5), (12, 6), (13, 6), (12, 7), (13, 7),
            (12, 8), (13, 8), (11, 9), (12, 9), (11, 10), (12, 10),
            (10, 11), (11, 11), (10, 12), (11, 12),
            (9, 13), (10, 13), (11, 13), (9, 14), (10, 14),
        ],
    )

    # Two-pixel vertical eyes reproduce the glowing eye layer on the model.
    set_pixels(EYE, [(4, 6), (5, 6), (10, 6), (11, 6)])
    set_pixels(EYE_LIGHT, [(4, 7), (5, 7), (10, 7), (11, 7)])

    # A minimal nose and crooked mouth keep the face readable at actual item scale.
    set_pixels(DEEP_SHADOW, [(7, 8), (8, 8), (6, 10), (7, 10), (8, 10), (9, 10)])
    set_pixels(LIGHT, [(6, 9), (7, 9)])

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
