#!/usr/bin/env python3
"""Textures for back-mounted vanity cosmetics drawn by BackCosmeticLayer.

Back pieces are large, so their entity textures use a 64x64 UV space (the layer
calls LayerDefinition.create(mesh, 64, 64)). Uniform-colour zones keep box-UV
precision irrelevant. Also emits 16x16 inventory icons.
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..",
                    "src", "main", "resources", "assets", "ironhold", "textures")
ENT = os.path.join(ROOT, "entity", "cosmetic")
ITEM = os.path.join(ROOT, "item")
os.makedirs(ENT, exist_ok=True)
os.makedirs(ITEM, exist_ok=True)
OUTLINE = (34, 34, 40, 255)


def C(r, g, b):
    return (r, g, b, 255)


# ---- entity sheets (64x64) ---------------------------------------------------
def sheet64(fill):
    return Image.new("RGBA", (64, 64), fill)


def sheet64_split(top, bottom, split=32):
    img = Image.new("RGBA", (64, 64), top)
    ImageDraw.Draw(img).rectangle([0, split, 63, 63], fill=bottom)
    return img


sheets = {
    "cape_red": sheet64(C(168, 32, 42)),
    "cat_tail": sheet64(C(110, 110, 118)),
    "fox_tail": sheet64_split(C(215, 120, 50), C(240, 240, 242)),  # orange body, white tip zone
    "dragon_tail": sheet64(C(70, 140, 70)),
    "devil_tail": sheet64(C(150, 30, 35)),
}
def shade(img):
    """Soft top-lit gradient + edge AO + faint dither, so fills aren't dead-flat."""
    px = img.load()
    w, h = img.size
    for y in range(h):
        g = 1.15 - 0.36 * (y / (h - 1))
        for x in range(w):
            r, gr, b, a = px[x, y]
            if a == 0:
                continue
            f = g
            if x in (0, w - 1) or y in (0, h - 1):
                f *= 0.80
            f *= 1.0 + (((x * 7 + y * 13) % 7) - 3) / 90.0
            px[x, y] = (min(255, int(r * f)), min(255, int(gr * f)), min(255, int(b * f)), a)
    return img


for name, img in sheets.items():
    shade(img).save(os.path.join(ENT, name + ".png"))


# ---- inventory icons (16x16) -------------------------------------------------
def icon_cape():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    red, collar = C(168, 32, 42), C(120, 22, 30)
    d.polygon([(4, 4), (12, 4), (14, 14), (2, 14)], fill=red, outline=OUTLINE)
    d.line([8, 4, 8, 14], fill=collar, width=1)        # centre fold
    d.rectangle([5, 2, 11, 4], fill=collar, outline=OUTLINE)  # collar
    return img


def icon_tail(body, tip=None, spikes=False, spade=False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pts = [(6, 1), (9, 1), (11, 8), (9, 14), (6, 14), (8, 8)]  # tapering S-ish tail
    d.polygon(pts, fill=body, outline=OUTLINE)
    if tip:
        d.ellipse([6, 11, 10, 15], fill=tip, outline=OUTLINE)
    if spikes:
        for y in (3, 6, 9):
            d.polygon([(7, y), (8, y - 2), (9, y)], fill=body, outline=OUTLINE)
    if spade:
        d.polygon([(5, 12), (11, 12), (8, 16)], fill=body, outline=OUTLINE)
    return img


icons = {
    "cape": icon_cape(),
    "cat_tail": icon_tail(C(110, 110, 118)),
    "fox_tail": icon_tail(C(215, 120, 50), tip=C(240, 240, 242)),
    "dragon_tail": icon_tail(C(70, 140, 70), spikes=True),
    "devil_tail": icon_tail(C(150, 30, 35), spade=True),
}
for name, img in icons.items():
    img.save(os.path.join(ITEM, name + ".png"))

print("wrote", ", ".join(sheets))
