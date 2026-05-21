"""
Fix accidental tinted pixels in black_shulker.png and white_shulker.png.

The original recoloring missed the base UV region (y=48..63), leaving
purple-tinted pixels on the black variant and blue-tinted pixels on the
white variant. This rebuilds a per-vanilla-color LUT from the pixels
that *were* recolored correctly, then re-applies it everywhere the
shell is still tinted. The yellow-head region is intentionally tinted
(purple on black, light blue on white) and is preserved.
"""
from collections import Counter, defaultdict
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
VANILLA = Path(
    "/Users/joshua/Minecraft Server/minecraft-fabric-server/"
    "minecraft-decompiled/assets/minecraft/textures/entity/shulker/shulker.png"
)
BLACK = ROOT / "src/main/resources/assets/ironhold/textures/entity/black_shulker.png"
WHITE = ROOT / "src/main/resources/assets/ironhold/textures/entity/white_shulker.png"


def is_purple(r, g, b):
    return r > g + 15 and b > g + 15


def is_blue_tint(r, g, b):
    return b > r + 12 and b > g + 5


def is_yellow(r, g, b):
    return r > b + 30 and g > b + 30


def build_lut(vanilla, variant, stray_check):
    lut = defaultdict(Counter)
    w, h = vanilla.size
    for y in range(h):
        for x in range(w):
            vp = vanilla.getpixel((x, y))
            if vp[3] == 0 or is_yellow(*vp[:3]):
                continue
            cur = variant.getpixel((x, y))
            if cur[3] == 0:
                continue
            if not stray_check(*cur[:3]):
                lut[vp[:3]][cur[:3]] += 1
    return {vc: ctr.most_common(1)[0][0] for vc, ctr in lut.items()}


def lookup(lut, vc):
    if vc in lut:
        return lut[vc]
    # Fall back to the nearest vanilla color we *do* have a mapping for.
    return min(lut.items(), key=lambda kv: sum((a - b) ** 2 for a, b in zip(kv[0], vc)))[1]


def fix_variant(vanilla_path, variant_path, stray_check, label):
    vanilla = Image.open(vanilla_path).convert("RGBA")
    variant = Image.open(variant_path).convert("RGBA")
    lut = build_lut(vanilla, variant, stray_check)

    fixed = variant.copy()
    fixed_count = 0
    w, h = variant.size
    for y in range(h):
        for x in range(w):
            vp = vanilla.getpixel((x, y))
            if vp[3] == 0 or is_yellow(*vp[:3]):
                continue
            cp = variant.getpixel((x, y))
            if cp[3] == 0 or not stray_check(*cp[:3]):
                continue
            replacement = lookup(lut, vp[:3])
            fixed.putpixel((x, y), (*replacement, cp[3]))
            fixed_count += 1

    fixed.save(variant_path)
    print(f"{label}: replaced {fixed_count} stray pixels")


if __name__ == "__main__":
    fix_variant(VANILLA, BLACK, is_purple, "black_shulker")
    fix_variant(VANILLA, WHITE, is_blue_tint, "white_shulker")
