"""Paint the Ender Villager 64x64 texture (vanilla VillagerModel UV layout).

A cross-breed of villager and enderman, modeled after the concept sheet:
  - Hooded silhouette with the hood pulled up (hat layer covers top/sides/back;
    front of hood is transparent so the face shows through, vanilla-style).
  - Near-black skin with a tracery of purple ender-veins and 2x1 bright purple eyes.
  - Deep purple robe with vanilla cleric-style gold trim:
    V-shaped gold collar, vertical gold seam down the gown, gold V-hem at the bottom,
    gold cuffs on the arms, gold ankle bands.
  - A purple gem medallion at the throat and a small gold "ender-thread" curl on chest.
  - Sparse purple "ender particle" stars scattered on the gown (illusioner-pants style).

Style notes verified against vanilla 1.21 villager.png / cleric.png / evoker.png /
illusioner.png / swamp.png / enderman.png:
  - 4-5 shade levels per material with hash-noise dithering (organic, not edge-only).
  - 1px gold trim with a 1px shadow tone underneath where the trim runs horizontally.
  - Eye pixels are bright (the contrast IS the read at distance); the optional
    *_glow.png lights them up via an EyesLayer if wired up.
  - Hood front face stays transparent except top-edge drape pixels (like swamp villager).

UVs match Mojang's VillagerModel.createBodyLayer() exactly:
  head     8x10x8 @ texOffs(0,0)
  nose     2x4x2  @ texOffs(24,0)
  hat      8x10x8 @ texOffs(32,0)   (outer head layer, "hood")
  body     8x12x6 @ texOffs(16,20)
  robe     8x18x6 @ texOffs(0,38)   (long gown extension under body)
  arms-LR  4x8x4  @ texOffs(44,22)  (shared by both arms, mirrored)
  arms-X   8x4x4  @ texOffs(40,38)  (folded "praying" cross-piece)
  leg      4x12x4 @ texOffs(0,22)   (shared L/R, mirrored)
"""
from PIL import Image

W, H = 64, 64

# --- Palettes (4-shade for organic vanilla-style dithering) -----------------
# Skin: near-black with a faint cool-purple cast. Uses 4 shades plus accent vein.
SKIN_DEEP   = (10, 8, 18, 255)     # #0a0812
SKIN_BASE   = (24, 20, 34, 255)    # #181422
SKIN_MID    = (38, 32, 50, 255)    # #262032
SKIN_HIGH   = (56, 46, 76, 255)    # #382e4c
VEIN        = (122, 56, 192, 255)  # #7a38c0 — purple ender vein
VEIN_BRIGHT = (170, 92, 232, 255)  # #aa5ce8 — brighter vein speck

# Hood / outer cloak: very dark plum-purple.
HOOD_DEEP   = (16, 10, 28, 255)    # #100a1c
HOOD_BASE   = (32, 22, 52, 255)    # #201634
HOOD_MID    = (50, 34, 76, 255)    # #32224c
HOOD_HIGH   = (74, 50, 116, 255)   # #4a3274

# Robe / chest cloth: a notch lighter so the silhouette reads against the cloak.
ROBE_DEEP   = (24, 16, 40, 255)    # #181028
ROBE_BASE   = (44, 30, 68, 255)    # #2c1e44
ROBE_MID    = (62, 44, 96, 255)    # #3e2c60
ROBE_HIGH   = (90, 66, 132, 255)   # #5a4284

# Gold trim — warm, vanilla-cleric gold. 3 shades.
GOLD_DEEP   = (110, 74, 20, 255)   # #6e4a14
GOLD_BASE   = (196, 156, 76, 255)  # #c49c4c
GOLD_HIGH   = (232, 200, 120, 255) # #e8c878

# Purple gem (chest medallion). Bright + saturated.
GEM_DEEP    = (96, 28, 144, 255)   # #601c90
GEM_BASE    = (188, 76, 240, 255)  # #bc4cf0
GEM_HIGH    = (232, 152, 255, 255) # #e898ff

# Eyes — bright purple (visible without glow overlay too).
EYE_BRIGHT  = (212, 120, 255, 255) # #d478ff
EYE_GLOW    = (240, 180, 255, 255) # #f0b4ff (used in _glow.png)

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()


def hash_xy(x, y, seed):
    """Stable 2D hash, 0..99."""
    return (((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) & 0xFFFF) % 100


def fill_rect(x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = color


def paint_face(x0, y0, x1, y1, deep, base, mid, high,
               brightness="mid", seed=0):
    """Vanilla-style 4-shade dithered face.

    brightness:
      'top'    — rich highlight cluster top edge, base body, sparse shadow bottom
      'mid'    — even mix, slight bottom-shadow bias (front faces, body panels)
      'side'   — base body with shadow column on back edge + 25% mid noise
      'back'   — base + heavy deep noise (slightly darker)
      'bottom' — solid deep tone
    """
    w, h = x1 - x0, y1 - y0
    if w <= 0 or h <= 0:
        return

    if brightness == "bottom":
        fill_rect(x0, y0, x1, y1, deep)
        return

    fill_rect(x0, y0, x1, y1, base)

    # Tiny faces (any side <3) stay flat with minimal speckle.
    if w < 3 or h < 3:
        if brightness == "side" and w >= 1:
            for y in range(y0, y1):
                if hash_xy(x1 - 1, y, seed) < 60:
                    px[x1 - 1, y] = mid
        return

    for y in range(y0, y1):
        for x in range(x0, x1):
            ly, lx = y - y0, x - x0
            r = hash_xy(x, y, seed)
            r2 = hash_xy(x + 31, y + 17, seed + 7)

            if brightness == "top":
                # Top face: highlights on top edge, scattered mids, sparse deep
                if ly == 0 and r < 35: px[x, y] = high
                elif ly < 2 and r < 25: px[x, y] = high
                elif r < 18: px[x, y] = mid
                elif r > 88: px[x, y] = deep
                elif r2 < 14: px[x, y] = high
            elif brightness == "back":
                # Back face: predominantly base+deep, no highlights
                if r < 30: px[x, y] = deep
                elif r2 < 12: px[x, y] = mid
            elif brightness == "side":
                # Side: base + mid noise, deep shadow column on back edge (right side)
                if lx >= w - 1 and r < 70: px[x, y] = deep
                elif lx == 0 and r < 30: px[x, y] = mid       # subtle near front edge
                elif r < 22: px[x, y] = mid
                elif r > 90: px[x, y] = deep
                elif r2 < 10: px[x, y] = high
            else:
                # mid (front faces, default)
                if ly >= h - 2 and r < 40: px[x, y] = deep   # bottom shadow band
                elif ly < 2 and r < 20: px[x, y] = high      # top highlight specks
                elif lx == 0 or lx == w - 1:
                    # silhouette edge: 1px material-shadow border (sparse)
                    if r < 65: px[x, y] = deep
                elif r < 28: px[x, y] = mid
                elif r > 92: px[x, y] = deep
                elif r2 < 14: px[x, y] = high


def unwrap(u, v, w, h, d):
    """Standard Minecraft cube UV unwrap."""
    return dict(
        top=    (u + d,         v,         u + d + w,         v + d),
        bottom= (u + d + w,     v,         u + d + w + w,     v + d),
        right=  (u,             v + d,     u + d,             v + d + h),
        front=  (u + d,         v + d,     u + d + w,         v + d + h),
        left=   (u + d + w,     v + d,     u + d + w + d,     v + d + h),
        back=   (u + d + w + d, v + d,     u + d + w + d + w, v + d + h),
    )


def paint_cube(u, v, w, h, d, deep, base, mid, high, seed=0):
    f = unwrap(u, v, w, h, d)
    paint_face(*f["top"],    deep, base, mid, high, "top",    seed + 1)
    paint_face(*f["bottom"], deep, base, mid, high, "bottom", seed + 2)
    paint_face(*f["front"],  deep, base, mid, high, "mid",    seed + 3)
    paint_face(*f["back"],   deep, base, mid, high, "back",   seed + 4)
    paint_face(*f["right"],  deep, base, mid, high, "side",   seed + 5)
    paint_face(*f["left"],   deep, base, mid, high, "side",   seed + 6)
    return f


# === HEAD (skin, partly hidden under the hood) ==============================
head_faces = paint_cube(0, 0, 8, 10, 8,
                        SKIN_DEEP, SKIN_BASE, SKIN_MID, SKIN_HIGH, seed=1001)

# Nose — vanilla villager has a prominent nose. Bridge slightly highlighted.
nose_faces = paint_cube(24, 0, 2, 4, 2,
                        SKIN_DEEP, SKIN_BASE, SKIN_MID, SKIN_HIGH, seed=1010)
nfx0, nfy0, _, _ = nose_faces["front"]
# Highlight on the bridge top
px[nfx0,     nfy0]     = SKIN_HIGH
px[nfx0 + 1, nfy0]     = SKIN_MID
# Cast shadow under the nose (bottom-front pixels)
px[nfx0,     nfy0 + 3] = SKIN_DEEP
px[nfx0 + 1, nfy0 + 3] = SKIN_DEEP

# --- Face details on the head front face -----------------------------------
fx0, fy0, fx1, fy1 = head_faces["front"]   # (8,8) .. (16,18), 8 wide x 10 tall

# Brow ridge: 1px shadow row above the eyes (asymmetric — break uniformity)
brow_y = fy0 + 2
for x in range(fx0 + 1, fx0 + 7):
    px[x, brow_y] = SKIN_DEEP
px[fx0 + 1, brow_y] = SKIN_BASE  # break left side
px[fx0 + 6, brow_y] = SKIN_BASE  # break right side (different pattern)

# Eyes — 2x1 bright purple, asymmetric (left eye one pixel lower for character)
EYE_ROW_L = fy0 + 3
EYE_ROW_R = fy0 + 3
px[fx0 + 1, EYE_ROW_L] = EYE_BRIGHT
px[fx0 + 2, EYE_ROW_L] = EYE_BRIGHT
px[fx0 + 5, EYE_ROW_R] = EYE_BRIGHT
px[fx0 + 6, EYE_ROW_R] = EYE_BRIGHT

# Glow trail under each eye (1px dim purple wisps — ender particles dripping)
px[fx0 + 1, EYE_ROW_L + 1] = VEIN
px[fx0 + 6, EYE_ROW_R + 1] = VEIN

# Mouth: subtle 2px shadow at row 6 (closed/grim line)
mouth_y = fy0 + 6
px[fx0 + 3, mouth_y] = SKIN_DEEP
px[fx0 + 4, mouth_y] = SKIN_DEEP

# Ender vein tracery on the face — branching purple cracks, asymmetric
veins_face = [
    (fx0 + 0, fy0 + 1), (fx0 + 1, fy0 + 0),                    # forehead-left branch
    (fx0 + 7, fy0 + 0), (fx0 + 7, fy0 + 2), (fx0 + 6, fy0 + 4), # right cheek branch
    (fx0 + 0, fy0 + 5), (fx0 + 0, fy0 + 6),                    # left jawline
    (fx0 + 4, fy0 + 8), (fx0 + 5, fy0 + 8),                    # chin streak
]
for vx, vy in veins_face:
    px[vx, vy] = VEIN
# One brighter vein speck for accent
px[fx0 + 7, fy0 + 0] = VEIN_BRIGHT

# Side-of-head veins (asymmetric: more on right side of model)
hr_x0, hr_y0, _, _ = head_faces["right"]
for vx, vy in [(hr_x0 + 1, hr_y0 + 2), (hr_x0 + 2, hr_y0 + 5), (hr_x0 + 6, hr_y0 + 7)]:
    px[vx, vy] = VEIN
hl_x0, hl_y0, _, _ = head_faces["left"]
for vx, vy in [(hl_x0 + 5, hl_y0 + 3)]:
    px[vx, vy] = VEIN
# Top of head: a few veins poking through where the hood doesn't fully cover
ht_x0, ht_y0, _, _ = head_faces["top"]
for vx, vy in [(ht_x0 + 2, ht_y0 + 1), (ht_x0 + 5, ht_y0 + 4), (ht_x0 + 4, ht_y0 + 6)]:
    px[vx, vy] = VEIN


# === HAT / HOOD (overlay, 8x10x8 at 32,0) ==================================
# Vanilla convention: paint top/back/sides solid, leave front face transparent
# except for hood-edge drape pixels on the top rim — exactly like swamp villager.
hat_faces = paint_cube(32, 0, 8, 10, 8,
                       HOOD_DEEP, HOOD_BASE, HOOD_MID, HOOD_HIGH, seed=1100)

# Wipe the front face to transparent (so the head's painted face shows through).
hfx0, hfy0, hfx1, hfy1 = hat_faces["front"]
fill_rect(hfx0, hfy0, hfx1, hfy1, (0, 0, 0, 0))

# Now drape hood material from the top of the front face downward, leaving an
# inverted-V opening over the face. This reads as "hood pulled up".
# Top rim: 2 rows of solid hood material across the top of the front face.
for y in range(hfy0, hfy0 + 2):
    for x in range(hfx0, hfx1):
        r = hash_xy(x, y, 1101)
        if r < 75: px[x, y] = HOOD_BASE
        elif r < 88: px[x, y] = HOOD_MID
        else: px[x, y] = HOOD_HIGH

# Side drapes — fabric coming down the sides of the face opening.
for y in range(hfy0 + 2, hfy0 + 6):
    px[hfx0, y]     = HOOD_BASE
    px[hfx1 - 1, y] = HOOD_BASE
    if y % 2 == 0:
        px[hfx0 + 1, y] = HOOD_MID
        px[hfx1 - 2, y] = HOOD_MID
# Stray fold pixels (asymmetric)
px[hfx0 + 1, hfy0 + 3] = HOOD_DEEP
px[hfx1 - 2, hfy0 + 4] = HOOD_HIGH
px[hfx0,     hfy0 + 6] = HOOD_DEEP
# Single drape pixel hanging into the face area for character
px[hfx0 + 2, hfy0 + 2] = HOOD_BASE

# A few purple ender-particle specks ON the hood top (radiating from the hood).
ht2_x0, ht2_y0, _, _ = hat_faces["top"]
for vx, vy in [(ht2_x0 + 3, ht2_y0 + 2),
               (ht2_x0 + 5, ht2_y0 + 5),
               (ht2_x0 + 1, ht2_y0 + 6),
               (ht2_x0 + 6, ht2_y0 + 1)]:
    px[vx, vy] = VEIN
# One brighter speck
px[ht2_x0 + 5, ht2_y0 + 5] = VEIN_BRIGHT
# Hood back: a faint purple sigil hint (one extra speck for the design)
hb_x0, hb_y0, _, _ = hat_faces["back"]
px[hb_x0 + 3, hb_y0 + 4] = VEIN
px[hb_x0 + 4, hb_y0 + 4] = VEIN_BRIGHT


# === BODY (8x12x6 at 16,20) ================================================
body_faces = paint_cube(16, 20, 8, 12, 6,
                        ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, seed=1200)

bfx0, bfy0, bfx1, bfy1 = body_faces["front"]

# Gold V-collar (cleric-style) — cleric has gold collar at the top neckline.
# Top row solid gold trim, asymmetric break.
for x in range(bfx0, bfx1):
    px[x, bfy0]     = GOLD_BASE
    px[x, bfy0 + 1] = GOLD_DEEP   # 1px shadow line under trim
# Asymmetric breaks (one shadow pixel each side, different positions)
px[bfx0 + 1, bfy0]     = GOLD_DEEP
px[bfx1 - 2, bfy0 + 1] = GOLD_HIGH
# V-shape opening: two angled gold pixels descending into a V at neck
px[bfx0 + 3, bfy0 + 1] = GOLD_BASE
px[bfx0 + 4, bfy0 + 1] = GOLD_BASE
px[bfx0 + 3, bfy0 + 2] = GOLD_BASE
px[bfx0 + 4, bfy0 + 2] = GOLD_BASE
px[bfx0 + 3, bfy0 + 3] = GOLD_DEEP   # bottom of V is in shadow

# Gem medallion at the throat — 2x2 with gold setting
gem_cx, gem_cy = bfx0 + 4, bfy0 + 2
px[gem_cx,     gem_cy]     = GEM_HIGH
px[gem_cx + 1, gem_cy]     = GEM_BASE
px[gem_cx,     gem_cy + 1] = GEM_BASE
px[gem_cx + 1, gem_cy + 1] = GEM_DEEP
# Gold setting around gem (top + sides)
px[gem_cx - 1, gem_cy]     = GOLD_BASE
px[gem_cx + 2, gem_cy]     = GOLD_BASE
px[gem_cx,     gem_cy - 1] = GOLD_BASE
px[gem_cx + 1, gem_cy - 1] = GOLD_BASE

# Gold "ender-thread" curl on the chest below the gem — a small spiral/curl
# Drawn as a 4-pixel curve to evoke the gold thread sigil from the concept.
curl = [
    (bfx0 + 3, bfy0 + 5),   # left tip
    (bfx0 + 3, bfy0 + 6),
    (bfx0 + 4, bfy0 + 7),
    (bfx0 + 5, bfy0 + 7),
    (bfx0 + 5, bfy0 + 6),   # curl back up
]
for cx, cy in curl:
    px[cx, cy] = GOLD_BASE
# Highlight on the curl's leading edge
px[bfx0 + 3, bfy0 + 5] = GOLD_HIGH
px[bfx0 + 5, bfy0 + 7] = GOLD_HIGH
# Center jewel of the curl (single deep purple pixel)
px[bfx0 + 4, bfy0 + 6] = GEM_BASE

# Vertical center seam (very subtle highlight stripe down body front)
seam_x = bfx0 + 4
for y in range(bfy0 + 8, bfy1 - 1):
    if y % 2 == 0:
        px[seam_x, y] = ROBE_HIGH

# Gold piping on the side seams of the body
br_x0, br_y0, br_x1, br_y1 = body_faces["right"]
for y in range(br_y0 + 2, br_y1 - 1, 4):
    px[br_x1 - 1, y] = GOLD_BASE


# === ROBE (8x18x6 at 0,38) — long gown extension ===========================
robe_faces = paint_cube(0, 38, 8, 18, 6,
                        HOOD_DEEP, HOOD_BASE, HOOD_MID, HOOD_HIGH, seed=1300)

rfx0, rfy0, rfx1, rfy1 = robe_faces["front"]

# Vertical gold center seam down the gown (continues from body seam, dashed)
robe_seam_x = rfx0 + 4
for i, y in enumerate(range(rfy0, rfy1)):
    phase = (y - rfy0) % 4
    if phase == 0:
        px[robe_seam_x, y] = GOLD_BASE
    elif phase == 1:
        px[robe_seam_x, y] = GOLD_DEEP
    elif phase == 2:
        px[robe_seam_x, y] = GOLD_HIGH

# Gold V-shaped hem at the bottom of the gown (cleric-style)
hem_y = rfy1 - 1
for x in range(rfx0, rfx1):
    px[x, hem_y]     = GOLD_BASE
    px[x, hem_y - 1] = GOLD_DEEP
# Small V-pattern in the hem (gold spike-points)
px[rfx0 + 1, hem_y - 1] = GOLD_BASE
px[rfx0 + 2, hem_y - 2] = GOLD_BASE
px[rfx0 + 5, hem_y - 2] = GOLD_BASE
px[rfx0 + 6, hem_y - 1] = GOLD_BASE
# Asymmetric breaks
px[rfx0 + 3, hem_y]     = GOLD_DEEP
px[rfx1 - 3, hem_y - 1] = GOLD_HIGH

# Ender particle stars scattered on the gown (illusioner-pants style)
# Asymmetric placement, only on front + side panels.
stars_front = [
    (rfx0 + 1, rfy0 + 3), (rfx0 + 6, rfy0 + 5),
    (rfx0 + 2, rfy0 + 8), (rfx0 + 5, rfy0 + 11),
    (rfx0 + 1, rfy0 + 14), (rfx0 + 7, rfy0 + 9),
]
for vx, vy in stars_front:
    px[vx, vy] = VEIN
# A couple brighter ones
px[rfx0 + 6, rfy0 + 5]  = VEIN_BRIGHT
px[rfx0 + 1, rfy0 + 14] = VEIN_BRIGHT

# Side panel stars (asymmetric: more on right than left)
rr_x0, rr_y0, _, _ = robe_faces["right"]
for vx, vy in [(rr_x0 + 1, rr_y0 + 4), (rr_x0 + 4, rr_y0 + 9),
               (rr_x0 + 2, rr_y0 + 14)]:
    px[vx, vy] = VEIN
rl_x0, rl_y0, _, _ = robe_faces["left"]
for vx, vy in [(rl_x0 + 3, rl_y0 + 7), (rl_x0 + 1, rl_y0 + 12)]:
    px[vx, vy] = VEIN


# === ARMS (4x8x4 at 44,22) — sleeves ========================================
arm_faces = paint_cube(44, 22, 4, 8, 4,
                       HOOD_DEEP, HOOD_BASE, HOOD_MID, HOOD_HIGH, seed=1400)

afx0, afy0, afx1, afy1 = arm_faces["front"]

# Gold cuff at wrist (bottom of sleeve front)
for x in range(afx0, afx1):
    px[x, afy1 - 1] = GOLD_BASE
    px[x, afy1 - 2] = GOLD_DEEP
# Asymmetric break
px[afx0 + 1, afy1 - 2] = GOLD_BASE

# Single gold dot on the shoulder for the pauldron pin
px[afx0 + 1, afy0 + 1] = GOLD_BASE
px[afx0 + 2, afy0 + 1] = GOLD_DEEP

# Side seam on the outer sleeve
ar_x0, ar_y0, _, ar_y1 = arm_faces["left"]
for y in range(ar_y0 + 2, ar_y1 - 1, 3):
    px[ar_x0 + 1, y] = GOLD_DEEP

# A vein pixel on the back of the arm (asymmetry)
ab_x0, ab_y0, _, _ = arm_faces["back"]
px[ab_x0 + 1, ab_y0 + 4] = VEIN


# === ARM CROSS / BELT (8x4x4 at 40,38) =====================================
cross_faces = paint_cube(40, 38, 8, 4, 4,
                         HOOD_DEEP, HOOD_BASE, HOOD_MID, HOOD_HIGH, seed=1500)

cfx0, cfy0, cfx1, cfy1 = cross_faces["front"]

# Gold belt strap across the front
for x in range(cfx0, cfx1):
    px[x, cfy0 + 1] = GOLD_BASE
    px[x, cfy0 + 2] = GOLD_DEEP

# Buckle: 2x2 gold square center, with a single gem pixel
buckle_x = cfx0 + 3
px[buckle_x,     cfy0 + 1] = GOLD_HIGH
px[buckle_x + 1, cfy0 + 1] = GOLD_BASE
px[buckle_x,     cfy0 + 2] = GOLD_BASE
px[buckle_x + 1, cfy0 + 2] = GOLD_DEEP
# Gem in the buckle
px[buckle_x, cfy0 + 1] = GEM_HIGH
# Asymmetric notch
px[cfx0 + 1, cfy0 + 1] = GOLD_DEEP
px[cfx1 - 2, cfy0 + 2] = GOLD_BASE


# === LEGS (4x12x4 at 0,22) =================================================
leg_faces = paint_cube(0, 22, 4, 12, 4,
                       ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, seed=1600)

lfx0, lfy0, lfx1, lfy1 = leg_faces["front"]

# Gold ankle band at the bottom (cleric-style)
for x in range(lfx0, lfx1):
    px[x, lfy1 - 1] = GOLD_BASE
    px[x, lfy1 - 2] = GOLD_DEEP

# Single ender-particle star on the leg front
px[lfx0 + 1, lfy0 + 5] = VEIN

# Asymmetric: vein on the left side only
ll_x0, ll_y0, _, _ = leg_faces["left"]
px[ll_x0 + 1, ll_y0 + 4] = VEIN


# === Final asymmetry pass — a few floating differing pixels =================
# Body back: a faint sigil hint (small purple symbol)
body_back = body_faces["back"]
bx, by = body_back[0], body_back[1]
# Draw a small "C"-like ender thread sigil on the back
px[bx + 4, by + 5] = VEIN
px[bx + 5, by + 4] = VEIN
px[bx + 5, by + 6] = VEIN
px[bx + 6, by + 5] = VEIN_BRIGHT


# === Save body texture =====================================================
OUT_DIR = "/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity"
img.save(f"{OUT_DIR}/ender_villager.png")


# === Glow texture: emissive overlay for eyes + chest gem + a few veins =====
glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
gpx = glow.load()

# Eye glow (full bright)
for x_eye in (fx0 + 1, fx0 + 2):
    gpx[x_eye, EYE_ROW_L] = EYE_GLOW
for x_eye in (fx0 + 5, fx0 + 6):
    gpx[x_eye, EYE_ROW_R] = EYE_GLOW

# Chest gem glow
gpx[gem_cx,     gem_cy]     = (240, 180, 255, 255)
gpx[gem_cx + 1, gem_cy]     = (220, 140, 250, 255)
gpx[gem_cx,     gem_cy + 1] = (220, 140, 250, 255)
gpx[gem_cx + 1, gem_cy + 1] = (180, 100, 220, 255)
# Belt buckle gem glow
gpx[buckle_x, cfy0 + 1] = (240, 180, 255, 255)

glow.save(f"{OUT_DIR}/ender_villager_glow.png")

print("OK — wrote ender_villager.png and ender_villager_glow.png")
