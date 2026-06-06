#!/usr/bin/env python3
"""Generate magma_boots worn-armor texture (64x32) + GUI icon (16x16) by
SAMPLING the vanilla obsidian + magma block textures (extracted to
scratch/vanilla_tex/). Obsidian -> shaft cube; magma (frame 0) -> lava sole cube.

Model (per leg, leg-local; y=0 hip .. y=12 foot, -z front):
  shaft (obsidian): box(-2.5,3,-2.5, 5,8,5) texOffs(0,0)
  sole  (magma)   : box(-2.5,9,-4,   5,3,6) texOffs(0,14)

Box-UV face rects (u,v,w,h) for box (W,H,D) at texOffs(ou,ov):
  top=(ou+D,ov,W,D) bottom=(ou+D+W,ov,W,D)
  east=(ou,ov+D,D,H) north=(ou+D,ov+D,W,H) west=(ou+D+W,ov+D,D,H) south=(ou+2D+W,ov+D,W,H)
"""
from PIL import Image

OBS = Image.open("scratch/vanilla_tex/obsidian.png").convert("RGBA").load()
MAG = Image.open("scratch/vanilla_tex/magma.png").convert("RGBA").crop((0, 0, 16, 16)).load()

TEX_W, TEX_H = 64, 32
img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
px = img.load()


def box_faces(ou, ov, W, H, D):
    return {
        "top":    (ou + D,         ov,     W, D),
        "bottom": (ou + D + W,     ov,     W, D),
        "east":   (ou,             ov + D, D, H),
        "north":  (ou + D,         ov + D, W, H),
        "west":   (ou + D + W,     ov + D, D, H),
        "south":  (ou + 2 * D + W, ov + D, W, H),
    }


def paste_face(rect, src, ox, oy):
    """Tile-sample a 16x16 source into the face rect at offset (ox,oy)."""
    x0, y0, w, h = rect
    for j in range(h):
        for i in range(w):
            px[x0 + i, y0 + j] = src[(ox + i) % 16, (oy + j) % 16]


# Per-face crop offsets so the six faces don't look identically tiled.
SHAFT_OFF = {"top": (0, 0), "bottom": (5, 8), "east": (0, 4),
             "north": (3, 0), "west": (8, 2), "south": (11, 6)}
SOLE_OFF  = {"top": (0, 0), "bottom": (6, 7), "east": (1, 9),
             "north": (4, 2), "west": (9, 5), "south": (11, 1)}

for name, rect in box_faces(0, 0, 5, 8, 5).items():      # obsidian shaft
    paste_face(rect, OBS, *SHAFT_OFF[name])
for name, rect in box_faces(0, 14, 5, 3, 6).items():     # magma sole
    paste_face(rect, MAG, *SOLE_OFF[name])

img.save("src/main/resources/assets/ironhold/textures/entity/equipment/humanoid/magma_boots.png")
print("wrote worn texture 64x32 (sampled obsidian + magma)")

# ── GUI icon: two boots, pixels sampled from obsidian (shaft) + magma (sole) ──
icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
ip = icon.load()
OUTLINE = (5, 4, 9, 255)


def draw_boot(x0):
    w = 5
    for y in range(3, 14):
        for dx in range(w):
            x = x0 + dx
            if y <= 9:                       # obsidian shaft
                ip[x, y] = OBS[dx % 16, y % 16]
            else:                            # magma sole
                ip[x, y] = MAG[dx % 16, y % 16]
    # dark silhouette outline for item-icon pop
    for y in range(3, 14):
        for x in (x0 - 1, x0 + w):
            if 0 <= x < 16 and ip[x, y][3] == 0:
                ip[x, y] = OUTLINE
    for x in range(x0 - 1, x0 + w + 1):
        if 0 <= x < 16:
            if ip[x, 2][3] == 0:
                ip[x, 2] = OUTLINE
            if ip[x, 14][3] == 0:
                ip[x, 14] = OUTLINE


draw_boot(2)
draw_boot(9)
icon.save("src/main/resources/assets/ironhold/textures/item/magma_boots.png")
print("wrote GUI icon 16x16 (sampled obsidian + magma)")
