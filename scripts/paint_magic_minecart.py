#!/usr/bin/env python3
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tmp/texture_sources/vanilla_minecart/minecart.png"
OUTPUT = ROOT / "src/main/resources/assets/ironhold/textures/item/magic_minecart.png"
PREVIEW = ROOT / "tmp/texture_outputs/magic_minecart/magic_minecart_v6_preview.png"

def tint_pixel(pixel: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    r, g, b, a = pixel
    if not a:
        return pixel

    value = (r + g + b) // 3
    if value < 20:
        color = (12, 7, 25)
    elif value < 48:
        color = (31, 15, 59)
    elif value < 62:
        color = (54, 25, 91)
    elif value < 82:
        color = (79, 40, 124)
    elif value < 120:
        color = (112, 73, 153)
    elif value < 155:
        color = (151, 130, 185)
    else:
        color = (210, 207, 229)
    return (*color, a)


def make_preview(sprite: Image.Image) -> Image.Image:
    cell = 144
    preview = Image.new("RGBA", (cell * 3, cell), (35, 35, 38, 255))
    draw = ImageDraw.Draw(preview)
    for index, scale in enumerate((1, 4, 8)):
        left = index * cell
        for y in range(0, cell, 8):
            for x in range(left, left + cell, 8):
                color = (53, 53, 57, 255) if ((x // 8 + y // 8) % 2) else (70, 70, 74, 255)
                draw.rectangle((x, y, x + 7, y + 7), fill=color)
        scaled = sprite.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST)
        preview.alpha_composite(
            scaled,
            (left + (cell - scaled.width) // 2, (cell - scaled.height) // 2),
        )
    return preview


def main() -> None:
    sprite = Image.open(SOURCE).convert("RGBA")
    sprite.putdata([tint_pixel(pixel) for pixel in sprite.get_flattened_data()])

    pixels = sprite.load()
    # A broad glow fills the cart cavity instead of reading as a single gem.
    accents = {
        (8, 3): (94, 234, 255, 255),
        (9, 4): (29, 153, 221, 255),
        (6, 5): (23, 104, 174, 255),
        (7, 5): (41, 180, 231, 255),
        (8, 5): (137, 247, 255, 255),
        (9, 5): (55, 207, 241, 255),
        (10, 5): (20, 112, 183, 255),
        (7, 6): (22, 126, 193, 255),
        (8, 6): (71, 225, 249, 255),
        (9, 6): (29, 166, 218, 255),
        (8, 7): (20, 103, 172, 255),
        # Cyan runes cross both visible side panels.
        (2, 8): (50, 199, 235, 255),
        (3, 9): (113, 241, 255, 255),
        (4, 10): (42, 183, 225, 255),
        (5, 10): (19, 111, 181, 255),
        (7, 9): (31, 163, 216, 255),
        (8, 10): (117, 244, 255, 255),
        (9, 10): (35, 178, 224, 255),
        (10, 9): (22, 118, 188, 255),
        (11, 8): (43, 190, 230, 255),
        (12, 9): (120, 243, 255, 255),
        (11, 10): (31, 157, 211, 255),
        # Magenta corner rivets make the enchanted frame distinct.
        (2, 7): (187, 85, 242, 255),
        (13, 7): (163, 66, 226, 255),
        (3, 11): (125, 51, 185, 255),
        (12, 11): (109, 42, 171, 255),
    }
    for position, color in accents.items():
        if pixels[position][3]:
            pixels[position] = color

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    sprite.save(OUTPUT)
    make_preview(sprite).convert("RGB").save(PREVIEW)


if __name__ == "__main__":
    main()
