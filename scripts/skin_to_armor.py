#!/usr/bin/env python3
"""Map a 64x64 Steve skin to 64x32 humanoid + humanoid_leggings equipment textures (1.21 layout)."""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image


def paste(out: Image.Image, skin: Image.Image, src: tuple[int, int, int, int], dst: tuple[int, int]) -> None:
    l, t, r, b = src
    out.paste(skin.crop(src), dst)


def build_humanoid(skin: Image.Image) -> Image.Image:
    """64x32 — helmet, chestplate, boots (layer_1 equivalent)."""
    out = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # Head cluster (same UV as skin top-left 32×16)
    paste(out, skin, (0, 0, 32, 16), (0, 0))
    # Body
    paste(out, skin, (20, 16, 28, 20), (20, 16))
    paste(out, skin, (28, 16, 36, 20), (28, 16))
    paste(out, skin, (16, 20, 20, 32), (16, 20))
    paste(out, skin, (20, 20, 28, 32), (20, 20))
    paste(out, skin, (28, 20, 32, 32), (28, 20))
    paste(out, skin, (32, 20, 40, 32), (32, 20))
    # Right arm
    paste(out, skin, (44, 16, 48, 20), (44, 16))
    paste(out, skin, (48, 16, 52, 20), (48, 16))
    paste(out, skin, (40, 20, 44, 32), (40, 20))
    paste(out, skin, (44, 20, 48, 32), (44, 20))
    paste(out, skin, (48, 20, 52, 32), (48, 20))
    paste(out, skin, (52, 20, 56, 32), (52, 20))
    # Left arm (skin y 48+ → armor y −32)
    paste(out, skin, (36, 48, 40, 52), (36, 16))
    paste(out, skin, (40, 48, 44, 52), (40, 16))
    paste(out, skin, (32, 52, 36, 64), (32, 20))
    paste(out, skin, (36, 52, 40, 64), (36, 20))
    paste(out, skin, (40, 52, 44, 64), (40, 20))
    paste(out, skin, (44, 52, 48, 64), (44, 20))
    # Right leg (skin y < 48)
    paste(out, skin, (4, 16, 8, 20), (4, 16))
    paste(out, skin, (8, 16, 12, 20), (8, 16))
    paste(out, skin, (0, 20, 4, 32), (0, 20))
    paste(out, skin, (4, 20, 8, 32), (4, 20))
    paste(out, skin, (8, 20, 12, 32), (8, 20))
    paste(out, skin, (12, 20, 16, 32), (12, 20))
    # Left leg (skin y 48+ → armor y −32, x +16)
    paste(out, skin, (4, 48, 8, 52), (20, 16))
    paste(out, skin, (8, 48, 12, 52), (24, 16))
    paste(out, skin, (0, 52, 4, 64), (16, 20))
    paste(out, skin, (4, 52, 8, 64), (20, 20))
    paste(out, skin, (8, 52, 12, 64), (24, 20))
    paste(out, skin, (12, 52, 16, 64), (28, 20))
    return out


def build_leggings(skin: Image.Image, mask: Image.Image) -> Image.Image:
    """64x32 — leggings; sample skin using mask from vanilla leggings leather."""
    m = mask.convert("RGBA")
    out = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    mw, mh = m.size
    for dy in range(min(32, mh)):
        for dx in range(min(64, mw)):
            if m.getpixel((dx, dy))[3] < 20:
                continue
            # Leggings UV: left half of template ≈ right leg on skin; right half ≈ left leg (skin y + 32)
            if dx < 16:
                sx, sy = dx, dy
            else:
                sx, sy = dx - 16, dy + 32
            if 0 <= sx < 64 and 0 <= sy < 64:
                out.putpixel((dx, dy), skin.getpixel((sx, sy)))
    return out


def main() -> None:
    skin_path = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    out_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else None
    if not skin_path or not skin_path.is_file():
        print("Usage: skin_to_armor.py <skin_64x64.png> <output_dir>", file=sys.stderr)
        sys.exit(1)
    out_dir = out_dir or Path(".")
    out_dir.mkdir(parents=True, exist_ok=True)

    skin = Image.open(skin_path).convert("RGBA")
    if skin.size != (64, 64):
        raise SystemExit(f"Expected 64x64 skin, got {skin.size}")

    import glob
    import zipfile

    jars = sorted(glob.glob(str(Path.home() / ".gradle/caches/neoformruntime/artifacts/minecraft_*_client.jar")))
    if not jars:
        raise SystemExit("Could not find minecraft_*_client.jar in neoformruntime cache")
    mcj = jars[-1]
    with zipfile.ZipFile(mcj, "r") as z:
        leg_mask_data = z.read("assets/minecraft/textures/entity/equipment/humanoid_leggings/leather.png")

    import io

    mask = Image.open(io.BytesIO(leg_mask_data)).convert("RGBA")

    humanoid = build_humanoid(skin)
    leggings = build_leggings(skin, mask)

    h_path = out_dir / "humanoid" / "wizard_robes_purple.png"
    l_path = out_dir / "humanoid_leggings" / "wizard_robes_purple.png"
    h_path.parent.mkdir(parents=True, exist_ok=True)
    l_path.parent.mkdir(parents=True, exist_ok=True)
    humanoid.save(h_path)
    leggings.save(l_path)
    print(f"Wrote {h_path}")
    print(f"Wrote {l_path}")


if __name__ == "__main__":
    main()
