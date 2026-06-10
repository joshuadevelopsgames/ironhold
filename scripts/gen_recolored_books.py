#!/usr/bin/env python3

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ITEM_TEXTURES = ROOT / "src/main/resources/assets/ironhold/textures/item"

# Minecraft 26.1.2's 16x16 vanilla book texture. Cover colors are replaced
# below while the gray page pixels remain unchanged.
BOOK_ROWS = (
    "................",
    "........222.....",
    "......225182....",
    "....225111112...",
    "..225111311112..",
    "225811111311182.",
    "2581131111311132",
    "22511131111133A.",
    "226511111133767.",
    "4596581133766723",
    ".459653376672344",
    "..459676672344..",
    "...459673344....",
    "....453344......",
    ".....444........",
    "................",
)

VANILLA_PALETTE = {
    "1": (101, 75, 23, 255),
    "2": (49, 33, 4, 255),
    "3": (68, 37, 10, 255),
    "4": (22, 16, 5, 255),
    "5": (82, 46, 16, 255),
    "6": (183, 183, 183, 255),
    "7": (153, 153, 153, 255),
    "8": (84, 62, 19, 255),
    "9": (214, 214, 214, 255),
    "A": (91, 91, 91, 255),
}

PINK_COVER = {
    "1": (202, 48, 112, 255),
    "2": (78, 13, 45, 255),
    "3": (123, 22, 70, 255),
    "4": (42, 8, 27, 255),
    "5": (157, 31, 88, 255),
    "8": (177, 39, 96, 255),
}

PURPLE_COVER = {
    "1": (105, 44, 147, 255),
    "2": (35, 13, 54, 255),
    "3": (69, 25, 99, 255),
    "4": (20, 8, 31, 255),
    "5": (83, 31, 119, 255),
    "8": (93, 37, 132, 255),
}


def make_book(cover_palette, emblem):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    palette = VANILLA_PALETTE | cover_palette
    for y, row in enumerate(BOOK_ROWS):
        for x, key in enumerate(row):
            if key != ".":
                pixels[x, y] = palette[key]
    for x, y, color in emblem:
        pixels[x, y] = color
    return image


def main():
    make_book(PINK_COVER, ()).save(
        ITEM_TEXTURES / "butterfly_encyclopedia.png"
    )
    make_book(PURPLE_COVER, ()).save(
        ITEM_TEXTURES / "master_of_disguise_tome.png"
    )


if __name__ == "__main__":
    main()
