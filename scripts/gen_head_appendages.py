#!/usr/bin/env python3
"""Generate textures for the head-appendage vanity cosmetics drawn by
HeadAppendageLayer (bunny ears, fox ears, devil horns, unicorn horn).

Each appendage has its own 16x16 entity texture (uniform-colour zones so box-UV
precision is irrelevant) plus a 16x16 inventory icon. Colour zones are matched
to the box texOffs used in HeadAppendageLayer.
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


# ---- entity swatch sheets ----------------------------------------------------
def sheet(fill, bands=()):
    img = Image.new("RGBA", (16, 16), fill)
    d = ImageDraw.Draw(img)
    for (y0, y1, col) in bands:
        d.rectangle([0, y0, 15, y1], fill=col)
    return img


sheets = {
    # bunny: white body, pink inner zone at the bottom (inner-box texOffs v=10)
    "bunny_ears":  sheet(C(238, 238, 240), [(10, 15, C(240, 176, 186))]),
    # fox: orange body, white inner zone at the bottom (inner-box texOffs v=9)
    "fox_ears":    sheet(C(222, 128, 52),  [(8, 15, C(240, 240, 242))]),
    # devil: uniform red
    "devil_horns": sheet(C(200, 40, 40)),
    # unicorn: uniform gold
    "unicorn_horn": sheet(C(235, 200, 90)),
    # ram: uniform tan
    "ram_horns": sheet(C(170, 140, 95)),
    # antennae: dark stalk zone (top) + bright bobble zone (bottom, v=8)
    "antennae": sheet(C(40, 40, 46), [(8, 15, C(240, 220, 90))]),
    # elf ears: uniform skin tone
    "elf_ears": sheet(C(235, 195, 165)),
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


# ---- inventory icons ---------------------------------------------------------
def icon_bunny():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    white, pink = C(238, 238, 240), C(240, 176, 186)
    for x in (4, 9):
        d.rounded_rectangle([x, 2, x + 3, 13], radius=2, fill=white, outline=OUTLINE)
        d.line([x + 1, 5, x + 1, 11], fill=pink, width=1)
        d.line([x + 2, 5, x + 2, 11], fill=pink, width=1)
    return img


def icon_fox():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    orange, white = C(222, 128, 52), C(240, 240, 242)
    d.polygon([(1, 13), (7, 13), (2, 2)], fill=orange, outline=OUTLINE)
    d.polygon([(3, 11), (5, 11), (3, 5)], fill=white)
    d.polygon([(9, 13), (15, 13), (14, 2)], fill=orange, outline=OUTLINE)
    d.polygon([(11, 11), (13, 11), (13, 5)], fill=white)
    return img


def icon_devil():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    red = C(200, 40, 40)
    d.polygon([(2, 13), (5, 13), (1, 3), (3, 6)], fill=red, outline=OUTLINE)
    d.polygon([(14, 13), (11, 13), (15, 3), (13, 6)], fill=red, outline=OUTLINE)
    return img


def icon_unicorn():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    gold, dark = C(235, 200, 90), C(190, 150, 50)
    d.polygon([(6, 14), (10, 14), (8, 1)], fill=gold, outline=OUTLINE)
    for y in (4, 7, 10):
        d.line([7, y + 1, 9, y - 1], fill=dark, width=1)  # spiral hint
    return img


def icon_ram():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    tan = C(170, 140, 95)
    d.arc([1, 3, 8, 13], start=200, end=20, fill=tan, width=2)   # left curl
    d.arc([8, 3, 15, 13], start=160, end=340, fill=tan, width=2)  # right curl
    return img


def icon_antennae():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    dark, bob = C(40, 40, 46), C(240, 220, 90)
    d.line([6, 14, 5, 5], fill=dark, width=1)
    d.line([10, 14, 11, 5], fill=dark, width=1)
    d.ellipse([3, 2, 6, 5], fill=bob, outline=OUTLINE)
    d.ellipse([10, 2, 13, 5], fill=bob, outline=OUTLINE)
    return img


def icon_elf():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    skin = C(235, 195, 165)
    d.polygon([(2, 11), (4, 13), (1, 4)], fill=skin, outline=OUTLINE)
    d.polygon([(14, 11), (12, 13), (15, 4)], fill=skin, outline=OUTLINE)
    return img


icons = {
    "bunny_ears": icon_bunny(),
    "fox_ears": icon_fox(),
    "devil_horns": icon_devil(),
    "unicorn_horn": icon_unicorn(),
    "ram_horns": icon_ram(),
    "antennae": icon_antennae(),
    "elf_ears": icon_elf(),
}
for name, img in icons.items():
    img.save(os.path.join(ITEM, name + ".png"))

print("wrote", ", ".join(sheets))
