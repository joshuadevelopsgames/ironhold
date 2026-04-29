"""
Paints the King Enderman textures:
  - king_enderman.png       : 256x256 base diffuse texture
  - king_enderman_glow.png  : 256x256 emissive overlay (transparent except eyes)

Palette is derived from vanilla obsidian pixel samples with pixel-level
variation (noise + face-direction shading) so armor plates don't look flat.
Eye glow uses a hot magenta-purple that reads as "ender" without looking neon.
"""
from PIL import Image
import random
import math

# ── Configuration ────────────────────────────────────────────────────────────
W, H = 256, 256
SEED = 0xC0DECAFE
random.seed(SEED)

# Vanilla obsidian palette samples (eyeballed from obsidian.png, low/mid/high):
PAL = [
    (0x0D, 0x03, 0x19),  # deepest shadow
    (0x14, 0x04, 0x28),  # dark
    (0x1A, 0x07, 0x32),  # mid-dark
    (0x23, 0x0E, 0x3E),  # mid
    (0x2C, 0x17, 0x48),  # mid-high
    (0x36, 0x22, 0x54),  # highlight
    (0x42, 0x2E, 0x60),  # rim
]

# Subtle magenta accent for "enderish" armor edges:
ACCENT = (0x66, 0x22, 0x8A)

# Eye emissive hot magenta (matches purple_allay_eyes feel but hotter):
EYE_GLOW_CORE = (0xF0, 0x6A, 0xFF, 255)
EYE_GLOW_EDGE = (0xB8, 0x3A, 0xE6, 255)


def pick(weight_low=0.55):
    """Weighted pick from palette. weight_low biases toward darker shades."""
    r = random.random()
    if r < weight_low:
        return PAL[random.randint(0, 2)]
    if r < weight_low + 0.30:
        return PAL[random.randint(2, 4)]
    return PAL[random.randint(4, 6)]


def jitter(c, amt=6):
    return tuple(max(0, min(255, ch + random.randint(-amt, amt))) for ch in c)


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


# ── Build base texture ──────────────────────────────────────────────────────
base = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = base.load()

# Fill with noisy obsidian pattern everywhere (safe default for any UV region).
for y in range(H):
    for x in range(W):
        # Vertical gradient: top slightly lighter (simulates top-down lighting)
        row_bias = y / H  # 0 at top, 1 at bottom
        shade = pick(weight_low=0.45 + 0.25 * row_bias)
        shade = jitter(shade, 5)

        # Sparse magenta speckle for armor wear
        if random.random() < 0.008:
            shade = jitter(ACCENT, 10)

        # Occasional bright pixel highlight (reflective armor fleck)
        if random.random() < 0.004:
            shade = jitter(PAL[6], 8)

        px[x, y] = (*shade, 255)


# ── Armor seam lines: pseudo-random horizontal + vertical dark grooves ──────
def draw_seam(x0, y0, x1, y1, color=PAL[0]):
    # Bresenham-lite
    dx = x1 - x0
    dy = y1 - y0
    steps = max(abs(dx), abs(dy))
    if steps == 0:
        return
    for i in range(steps + 1):
        t = i / steps
        x = int(x0 + dx * t)
        y = int(y0 + dy * t)
        if 0 <= x < W and 0 <= y < H:
            px[x, y] = (*jitter(color, 3), 255)


# Seam patterns across the atlas to simulate plate edges on multiple parts.
random.seed(SEED + 1)
for _ in range(140):
    x = random.randint(0, W - 1)
    y = random.randint(0, H - 1)
    length = random.randint(4, 14)
    if random.random() < 0.5:
        draw_seam(x, y, min(W - 1, x + length), y)
    else:
        draw_seam(x, y, x, min(H - 1, y + length))


# ── Rim highlights: scatter lighter pixels adjacent to dark seams ───────────
random.seed(SEED + 2)
for _ in range(400):
    x = random.randint(1, W - 2)
    y = random.randint(1, H - 2)
    hl = jitter(PAL[5], 6)
    px[x, y] = (*hl, 255)


# ── Darken the eye socket regions (so emissive glow reads as "lit") ─────────
# Eye cubes in geo.json:
#   left_eye  UV [74, 0], size 4x3x1  → front face at (75, 1)..(79, 4)
#   right_eye UV [90, 0], size 4x3x1  → front face at (91, 1)..(95, 4)
def fill_rect(x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < W and 0 <= y < H:
                px[x, y] = (*color, 255)


fill_rect(75, 1, 79, 4, PAL[0])
fill_rect(91, 1, 95, 4, PAL[0])


# ── Subtle magenta tint on select armor sections (chest plate + pauldrons) ──
# Chest plate: body UV [0, 48], size 23x14x2 → front face at (2, 50)..(25, 64)
def tint_rect(x0, y0, x1, y1, color, amount=0.22):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < W and 0 <= y < H:
                r, g, b, a = px[x, y]
                nr, ng, nb = mix((r, g, b), color, amount)
                # keep noise feel: small jitter after mix
                nr = max(0, min(255, nr + random.randint(-3, 3)))
                ng = max(0, min(255, ng + random.randint(-3, 3)))
                nb = max(0, min(255, nb + random.randint(-3, 3)))
                px[x, y] = (nr, ng, nb, a)


random.seed(SEED + 3)
# Chest plate front face
tint_rect(2, 50, 25, 64, ACCENT, 0.18)
# Back plate
tint_rect(58, 50, 81, 64, ACCENT, 0.14)
# Pauldrons (front faces of 10x8x14 cubes)
# right_pauldron UV [176, 112] → front at (176+14, 112+14)..(+10, +8) = (190, 126)..(200, 134)
tint_rect(190, 126, 200, 134, ACCENT, 0.20)
# left_pauldron UV [176, 144] → front at (190, 158)..(200, 166)
tint_rect(190, 158, 200, 166, ACCENT, 0.20)

base.save("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman.png")


# ── Build emissive glow texture (transparent except eyes) ───────────────────
glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
gpx = glow.load()


def fill_glow(x0, y0, x1, y1):
    # Core bright center, softer edges
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < W and 0 <= y < H:
                # Edge cells get edge color, center gets core
                edge = (x == x0 or x == x1 - 1 or y == y0 or y == y1 - 1)
                gpx[x, y] = EYE_GLOW_EDGE if edge else EYE_GLOW_CORE


fill_glow(75, 1, 79, 4)
fill_glow(91, 1, 95, 4)

glow.save("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman_glow.png")

print("Wrote king_enderman.png and king_enderman_glow.png")
