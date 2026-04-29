"""Paint a 16x16 spawn-egg icon for the Piglin Villager.

Vanilla spawn-egg silhouette (rounded rectangle) recolored with the piglin-
villager palette: pink-tan base + dark brown specks (matching the robe color).
"""
from PIL import Image

W, H = 16, 16

EGG_BASE   = (180, 116, 90, 255)   # #b4745a — piglin pink base
EGG_SHADOW = (132, 84, 64, 255)    # #845440
EGG_HIGH   = (212, 148, 116, 255)  # #d49474
SPECK      = (58, 36, 26, 255)     # #3a241a — robe brown
SPECK_DIM  = (90, 54, 36, 255)     # #5a3624

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()

egg_rows = [
    None,
    None,
    (6, 9),
    (5, 10),
    (4, 11),
    (4, 11),
    (4, 11),
    (4, 11),
    (3, 12),
    (3, 12),
    (3, 12),
    (3, 12),
    (4, 11),
    (5, 10),
    None,
    None,
]

for y, row in enumerate(egg_rows):
    if row is None: continue
    x0, x1 = row
    for x in range(x0, x1 + 1):
        if y < 4: c = EGG_HIGH
        elif y < 8: c = EGG_BASE
        else: c = EGG_SHADOW
        px[x, y] = c
    px[x0, y] = EGG_SHADOW
    px[x1, y] = EGG_SHADOW

# Top highlight
px[6, 3] = EGG_HIGH
px[7, 2] = EGG_HIGH
px[8, 2] = EGG_HIGH
px[9, 3] = EGG_HIGH

# Brown specks (mirror the robe color)
specks = [
    (5, 5,  SPECK_DIM),
    (8, 6,  SPECK),
    (10, 7, SPECK_DIM),
    (4, 9,  SPECK),
    (7, 10, SPECK_DIM),
    (10, 11, SPECK),
    (6, 12, SPECK_DIM),
    (9, 9, SPECK),
]
for x, y, c in specks:
    px[x, y] = c

img.save("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/item/piglin_villager_spawn_egg.png")
print("OK")
