"""Paint a 16x16 spawn-egg icon for the Ender Villager.

Vanilla spawn-egg shape: oval with darker base, lighter speckle dots.
Use the Ender Villager palette so the egg reads as "this mob".
"""
from PIL import Image

W, H = 16, 16

# Ender villager palette
EGG_BASE   = (32, 22, 52, 255)    # #201634 — hood/robe base
EGG_SHADOW = (16, 10, 28, 255)    # #100a1c
EGG_HIGH   = (74, 50, 116, 255)   # #4a3274
SPECK      = (212, 120, 255, 255) # #d478ff — bright purple ender particle
SPECK_DIM  = (122, 56, 192, 255)  # #7a38c0

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()

# Vanilla spawn-egg silhouette (8 wide oval, top narrower than bottom)
# Row format: list of (x_start, x_end_inclusive) per row, or None for blank
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
        # Vertical gradient: top brighter, bottom darker
        if y < 4: c = EGG_HIGH
        elif y < 8: c = EGG_BASE
        else: c = EGG_SHADOW
        px[x, y] = c
        # 1px outline (silhouette edge using shadow tone)
        if x == x0 or x == x1:
            px[x, y] = EGG_SHADOW
        if y == egg_rows.index(row) and row == egg_rows[2]:
            px[x, y] = EGG_SHADOW
    # Top + bottom outline rows
for y, row in enumerate(egg_rows):
    if row is None: continue
    x0, x1 = row
    # Outline above and below where the row ends
    prev = egg_rows[y - 1] if y > 0 else None
    nxt  = egg_rows[y + 1] if y < len(egg_rows) - 1 else None
    if prev is None or x0 < prev[0]:
        px[x0, y] = EGG_SHADOW
    if prev is None or x1 > prev[1]:
        px[x1, y] = EGG_SHADOW
    if nxt is None or x0 < nxt[0]:
        px[x0, y] = EGG_SHADOW
    if nxt is None or x1 > nxt[1]:
        px[x1, y] = EGG_SHADOW

# Top highlight (catches light)
px[6, 3] = EGG_HIGH
px[7, 2] = EGG_HIGH
px[8, 2] = EGG_HIGH
px[9, 3] = EGG_HIGH

# Bright purple speckles (ender particles)
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

img.save("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/item/ender_villager_spawn_egg.png")
print("OK")
