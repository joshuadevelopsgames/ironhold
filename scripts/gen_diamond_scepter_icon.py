#!/usr/bin/env python3
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
TEXTURE = ROOT / "src/main/resources/assets/ironhold/textures/item/diamond_scepter.png"
PREVIEW = ROOT / "tmp/texture_outputs/diamond_scepter/diamond_scepter_model_faithful_preview.png"

TRANSPARENT = (0, 0, 0, 0)
OUTLINE = (18, 20, 29, 255)
WOOD_DARK = (60, 33, 20, 255)
WOOD = (104, 58, 28, 255)
WOOD_LIGHT = (155, 91, 36, 255)
STEEL_DARK = (60, 79, 91, 255)
STEEL = (135, 165, 174, 255)
STEEL_LIGHT = (211, 236, 235, 255)
BLUE_DARK = (15, 102, 160, 255)
BLUE = (24, 174, 224, 255)
CYAN = (68, 225, 244, 255)
ICE = (205, 255, 255, 255)


def paint() -> Image.Image:
    image = Image.new("RGBA", (32, 32), TRANSPARENT)
    draw = ImageDraw.Draw(image)

    # Bottom cap and long narrow handle, matching the 3D model proportions.
    draw.polygon([(2, 28), (4, 25), (7, 28), (4, 31)], fill=OUTLINE)
    draw.polygon([(3, 28), (4, 27), (6, 28), (4, 30)], fill=WOOD_LIGHT)
    draw.polygon([(4, 25), (6, 27), (19, 14), (17, 12)], fill=OUTLINE)
    draw.polygon([(6, 25), (7, 26), (18, 15), (17, 14)], fill=WOOD)
    draw.line([(7, 24), (9, 24)], fill=WOOD_LIGHT)
    draw.line([(10, 21), (12, 21)], fill=WOOD_LIGHT)
    draw.line([(13, 18), (15, 18)], fill=WOOD_LIGHT)
    draw.line([(6, 27), (18, 15)], fill=WOOD_DARK)

    # Steel collar and compact four-prong holder.
    draw.polygon([(15, 14), (18, 11), (21, 14), (18, 17)], fill=OUTLINE)
    draw.polygon([(17, 14), (18, 13), (20, 14), (18, 16)], fill=STEEL)
    image.putpixel((18, 13), STEEL_LIGHT)
    draw.line([(18, 12), (19, 10)], fill=STEEL_DARK, width=2)
    draw.line([(20, 13), (22, 11)], fill=STEEL_DARK, width=2)
    draw.line([(16, 12), (18, 10)], fill=STEEL_DARK, width=2)
    draw.line([(20, 9), (22, 10)], fill=STEEL)

    # Layered crystal from the model: broad middle, stepped top, tapered base.
    crystal = [(18, 7), (20, 3), (24, 2), (28, 6), (27, 9), (23, 12), (20, 11)]
    draw.polygon(crystal, fill=OUTLINE)
    draw.polygon([(20, 7), (21, 4), (24, 3), (27, 6), (26, 8), (23, 10), (21, 10)], fill=BLUE)
    draw.polygon([(21, 5), (24, 3), (24, 7), (22, 9), (20, 7)], fill=CYAN)
    draw.polygon([(24, 3), (27, 6), (24, 7)], fill=ICE)
    draw.polygon([(24, 7), (27, 6), (26, 8), (23, 10), (22, 9)], fill=BLUE_DARK)
    image.putpixel((22, 4), ICE)
    image.putpixel((19, 8), CYAN)

    return image


def preview(sprite: Image.Image) -> Image.Image:
    cell = 160
    result = Image.new("RGBA", (cell * 3, cell), (38, 38, 38, 255))
    draw = ImageDraw.Draw(result)
    for index, scale in enumerate((1, 2, 4)):
        left = index * cell
        for y in range(0, cell, 8):
            for x in range(left, left + cell, 8):
                color = (55, 55, 55, 255) if ((x // 8 + y // 8) % 2) else (72, 72, 72, 255)
                draw.rectangle((x, y, x + 7, y + 7), fill=color)
        enlarged = sprite.resize((32 * scale, 32 * scale), Image.Resampling.NEAREST)
        result.alpha_composite(
            enlarged,
            (left + (cell - enlarged.width) // 2, (cell - enlarged.height) // 2),
        )
    return result.convert("RGB")


def main() -> None:
    sprite = paint()
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    sprite.save(TEXTURE)
    preview(sprite).save(PREVIEW)


if __name__ == "__main__":
    main()
