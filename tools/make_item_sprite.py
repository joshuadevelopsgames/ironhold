#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw


KEY = (0, 255, 0)


def remove_key(image: Image.Image, tolerance: int) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    for y in range(rgba.height):
        for x in range(rgba.width):
            r, g, b, a = pixels[x, y]
            if abs(r - KEY[0]) <= tolerance and abs(g - KEY[1]) <= tolerance and abs(b - KEY[2]) <= tolerance:
                pixels[x, y] = (0, 0, 0, 0)
    return rgba


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int]:
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    if bbox is None:
        raise SystemExit("input has no non-transparent pixels after key removal")
    return bbox


def fit_to_canvas(image: Image.Image, canvas_size: int, padding: int) -> Image.Image:
    cropped = image.crop(alpha_bbox(image))
    max_size = canvas_size - padding * 2
    scale = min(max_size / cropped.width, max_size / cropped.height)
    fitted = cropped.resize(
        (max(1, round(cropped.width * scale)), max(1, round(cropped.height * scale))),
        Image.Resampling.LANCZOS,
    )
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    x = (canvas_size - fitted.width) // 2
    y = (canvas_size - fitted.height) // 2
    canvas.alpha_composite(fitted, (x, y))
    return canvas


def downscale_pixel_art(image: Image.Image, colors: int) -> Image.Image:
    stage = image.resize((32, 32), Image.Resampling.BOX)
    stage = stage.resize((16, 16), Image.Resampling.BOX)

    alpha = stage.getchannel("A")
    rgb = Image.new("RGB", stage.size, (0, 0, 0))
    rgb.paste(stage.convert("RGB"), mask=alpha)
    quantized = rgb.quantize(colors=colors, method=Image.Quantize.MEDIANCUT).convert("RGBA")

    out = Image.new("RGBA", stage.size, (0, 0, 0, 0))
    qpx = quantized.load()
    apx = alpha.load()
    opx = out.load()
    for y in range(16):
        for x in range(16):
            a = apx[x, y]
            if a >= 32:
                r, g, b, _ = qpx[x, y]
                opx[x, y] = (r, g, b, 255)
    return out


def make_preview(sprite: Image.Image, output: Path) -> None:
    cell = 128
    img = Image.new("RGBA", (cell * 3, cell), (38, 38, 38, 255))
    draw = ImageDraw.Draw(img)
    for i, scale in enumerate((1, 4, 8)):
        x0 = i * cell
        for y in range(0, cell, 8):
            for x in range(x0, x0 + cell, 8):
                c = (55, 55, 55, 255) if ((x // 8 + y // 8) % 2) else (72, 72, 72, 255)
                draw.rectangle([x, y, x + 7, y + 7], fill=c)
        scaled = sprite.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST)
        img.alpha_composite(scaled, (x0 + (cell - scaled.width) // 2, (cell - scaled.height) // 2))
    img.convert("RGB").save(output)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--preview", required=True, type=Path)
    parser.add_argument("--padding", type=int, default=1)
    parser.add_argument("--colors", type=int, default=8)
    parser.add_argument("--key-tolerance", type=int, default=22)
    args = parser.parse_args()

    source = Image.open(args.input)
    keyed = remove_key(source, args.key_tolerance)
    fitted = fit_to_canvas(keyed, 256, 12)
    sprite = downscale_pixel_art(fitted, args.colors)
    sprite = fit_to_canvas(sprite, 16, args.padding)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.preview.parent.mkdir(parents=True, exist_ok=True)
    sprite.save(args.output)
    make_preview(sprite, args.preview)


if __name__ == "__main__":
    main()
