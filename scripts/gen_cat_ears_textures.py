#!/usr/bin/env python3
"""Generate cat-ears vanity cosmetic textures for every colour variant.

Each ear is a stepped triangle (wide base box + narrow tip box, charcoal) with
a pink inner box. Boxes sample uniform-colour zones of a 16x16 sheet, so exact
box-UV precision is irrelevant. The sheet has three horizontal zones, matched
to the box texOffs used in CatEarsLayer:
    y[0,4)  -> BASE colour  (base box, texOffs 0,0)
    y[4,8)  -> TIP colour   (tip box,  texOffs 0,4)
    y[8,16) -> INNER colour (pink box, texOffs 0,9)
Variants with a tip colour different from the base give two-tone ears
(calico = white tips, siamese = dark points).

Also emits a 16x16 inventory icon per variant: two short splayed triangles
(base colour, tip-colour apex, pink inner) on a small head band.
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..",
                    "src", "main", "resources", "assets", "ironhold", "textures")
OUTLINE = (38, 38, 44, 255)

# name -> (base, tip, inner)
VARIANTS = {
    "cat_ears":          ((74, 74, 82),    (74, 74, 82),    (232, 158, 170)),  # gray (default)
    "cat_ears_black":    ((30, 30, 34),    (30, 30, 34),    (236, 150, 162)),
    "cat_ears_white":    ((236, 236, 238), (236, 236, 238), (242, 176, 186)),
    "cat_ears_calico":   ((222, 135, 60),  (238, 238, 240), (242, 176, 186)),  # ginger, white tips
    "cat_ears_siamese":  ((228, 212, 182), (92, 66, 50),    (212, 150, 150)),  # cream, brown points
}


def rgba(c):
    return (c[0], c[1], c[2], 255)


def make_sheet(base, tip, inner):
    img = Image.new("RGBA", (16, 16), rgba(base))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 4, 15, 7], fill=rgba(tip))     # tip zone
    d.rectangle([0, 8, 15, 15], fill=rgba(inner))  # inner zone
    return img


def make_icon(base, tip, inner):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 12, 12, 13], fill=rgba(base))               # head band
    # left ear: short wide triangle splayed outward
    d.polygon([(1, 12), (7, 12), (2, 6)], fill=rgba(base), outline=OUTLINE)
    d.polygon([(2, 7), (4, 7), (2, 6)], fill=rgba(tip))         # tip-colour apex
    d.polygon([(3, 11), (5, 11), (3, 8)], fill=rgba(inner))     # pink inner
    # right ear (mirror)
    d.polygon([(8, 12), (14, 12), (13, 6)], fill=rgba(base), outline=OUTLINE)
    d.polygon([(11, 7), (13, 7), (13, 6)], fill=rgba(tip))
    d.polygon([(10, 11), (12, 11), (12, 8)], fill=rgba(inner))
    return img


ent_dir = os.path.join(ROOT, "entity", "cosmetic")
item_dir = os.path.join(ROOT, "item")
os.makedirs(ent_dir, exist_ok=True)
os.makedirs(item_dir, exist_ok=True)

def shade(img):
    """Lift flat fills off 'untextured' by adding a soft top-lit vertical gradient,
    edge ambient-occlusion, and a faint dither — applied to non-transparent pixels."""
    px = img.load()
    w, h = img.size
    for y in range(h):
        g = 1.15 - 0.36 * (y / (h - 1))           # bright at top -> dim at bottom
        for x in range(w):
            r, gr, b, a = px[x, y]
            if a == 0:
                continue
            f = g
            if x in (0, w - 1) or y in (0, h - 1):
                f *= 0.80                           # darken outer edge (AO)
            f *= 1.0 + (((x * 7 + y * 13) % 7) - 3) / 90.0   # faint dither
            px[x, y] = (min(255, int(r * f)), min(255, int(gr * f)), min(255, int(b * f)), a)
    return img


for name, (base, tip, inner) in VARIANTS.items():
    shade(make_sheet(base, tip, inner)).save(os.path.join(ent_dir, name + ".png"))
    make_icon(base, tip, inner).save(os.path.join(item_dir, name + ".png"))
    print("wrote", name)
