#!/usr/bin/env python3
"""
Render atlas-template.png — the visual sheet an artist (or AI prompt loop)
fills in. Each sprite slot is drawn as a labeled, dashed rectangle on a
checkerboard background so transparency is obvious.

Usage:
    python3 generate-template.py

Outputs:
    atlas-template.png  (256x256 RGBA)

Edit SLOTS below if atlas-spec.md changes — both files must agree.
"""

from PIL import Image, ImageDraw, ImageFont
import os

ATLAS_W = 256
ATLAS_H = 256

# (name, x, y, w, h)
SLOTS = [
    # Row A - Icons (y=0..15)
    ("ICON_COIN",          0, 0, 16, 16),
    ("ICON_GOLD_INGOT",   16, 0, 16, 16),
    ("ICON_EMERALD",      32, 0, 16, 16),
    ("ICON_LAND",         48, 0, 16, 16),
    ("ICON_CROWN",        64, 0, 16, 16),
    ("ICON_GATE",         80, 0, 16, 16),
    ("ICON_PORTAL",       96, 0, 16, 16),
    ("ICON_SKULL",       112, 0, 16, 16),
    ("ICON_WAX_SEAL",    128, 0, 16, 16),
    ("ICON_DECREE_STAMP",144, 0, 16, 16),

    # Row B - Stepper (y=16..27)
    ("STEPPER_IDLE",       0, 16, 12, 12),
    ("STEPPER_HOVER",     12, 16, 12, 12),
    ("STEPPER_PRESSED",   24, 16, 12, 12),
    ("STEPPER_DISABLED",  36, 16, 12, 12),

    # Row C - Toggle (y=28..43)
    ("TOGGLE_OFF_IDLE",    0, 28, 60, 16),
    ("TOGGLE_OFF_HOVER",  60, 28, 60, 16),
    ("TOGGLE_ON_IDLE",   120, 28, 60, 16),
    ("TOGGLE_ON_HOVER",  180, 28, 60, 16),

    # Row D - 9-slice chrome (y=44..75)
    ("OUTER_FRAME",        0, 44, 32, 32),
    ("INNER_BOX",         32, 44, 32, 32),
    ("TITLE_BAR",         64, 44, 64, 32),
    ("PARCHMENT",        128, 44, 64, 32),

    # Row E - Banners (y=76..99)
    ("BANNER_RIBBON",      0, 76, 96, 24),
    ("BURDEN_BAR_BG",     96, 76, 96, 24),

    # Row F - Decorative (y=100..131)
    ("WAX_SEAL_DECREE",    0, 100, 32, 32),
    ("KING_CONSOLE_PLATE",32, 100, 128, 32),
]


def main():
    img = Image.new("RGBA", (ATLAS_W, ATLAS_H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Subtle checkerboard so artists see transparency
    for cy in range(0, ATLAS_H, 8):
        for cx in range(0, ATLAS_W, 8):
            shade = 40 if ((cx // 8) + (cy // 8)) % 2 == 0 else 56
            draw.rectangle([cx, cy, cx + 7, cy + 7], fill=(shade, shade, shade, 64))

    # Try to find a small bitmap font; fall back to PIL default
    font = None
    candidates = [
        "/System/Library/Fonts/Supplemental/Courier New Bold.ttf",
        "/System/Library/Fonts/Menlo.ttc",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            try:
                font = ImageFont.truetype(path, 8)
                break
            except Exception:
                pass
    if font is None:
        font = ImageFont.load_default()

    # Draw each slot
    for name, x, y, w, h in SLOTS:
        # Dashed border
        right, bottom = x + w - 1, y + h - 1
        for i in range(x, right + 1, 2):
            draw.point((i, y),      fill=(255, 220, 80, 220))
            draw.point((i, bottom), fill=(255, 220, 80, 220))
        for i in range(y, bottom + 1, 2):
            draw.point((x,     i), fill=(255, 220, 80, 220))
            draw.point((right, i), fill=(255, 220, 80, 220))

        # Corner pips for visibility
        for cx, cy in [(x, y), (right, y), (x, bottom), (right, bottom)]:
            draw.point((cx, cy), fill=(255, 90, 90, 255))

        # Label — drawn just inside, top-left
        # Skip label if box is too small to fit any
        if w >= 24 and h >= 10:
            label = name
            # Truncate if too wide
            tries = 0
            while font.getlength(label) > w - 4 and tries < 20:
                label = label[:-1]
                tries += 1
            draw.text((x + 1, y + 1), label, fill=(255, 220, 80, 240), font=font)
        else:
            # Tiny slots get a single index in the corner
            draw.text((x + 1, y + 1), "*", fill=(255, 200, 80, 220), font=font)

    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "atlas-template.png")
    img.save(out)
    print(f"Wrote {out} ({ATLAS_W}x{ATLAS_H}, {len(SLOTS)} slots)")


if __name__ == "__main__":
    main()
