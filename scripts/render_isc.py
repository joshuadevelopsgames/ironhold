#!/usr/bin/env python3
"""
Render a .isc structure as an isometric PNG preview.

Color-codes blocks by family (stone, wood, water, grass, light, etc.).
Only renders blocks visible from outside (at least one air neighbor) so
the interior solid fill of the dirt podium doesn't dominate the image.

Usage:
  .recipes_venv/bin/python scripts/render_isc.py structures/foo.isc
       → writes structures/foo_preview.png

  .recipes_venv/bin/python scripts/render_isc.py structures/foo.isc OUTPUT.png
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parent))
from analyze_isc import parse_isc, base_id  # noqa: E402


# Color palette per block family. Hand-picked for visual clarity.
def color_for(block_id):
    b = base_id(block_id)
    name = b.split(":")[-1]
    # Terrain
    if name in ("dirt", "coarse_dirt", "rooted_dirt"): return (110, 75, 50)
    if name == "grass_block": return (90, 145, 55)
    if name == "moss_block": return (75, 110, 45)
    if name in ("gravel",): return (130, 125, 120)
    if name in ("sand",): return (220, 205, 160)
    if name == "water": return (60, 110, 200)
    if name == "lily_pad": return (90, 160, 70)
    if name in ("tall_grass",): return (110, 160, 70)
    if "leaves" in name: return (65, 110, 50)
    # Wood
    if "dark_oak_log" in name or "dark_oak_planks" in name: return (70, 50, 35)
    if "stripped_oak" in name or "stripped_dark_oak" in name: return (180, 145, 90)
    if "oak_log" in name or "oak_planks" in name: return (140, 110, 70)
    if "spruce" in name: return (90, 65, 40)
    if "birch" in name: return (200, 180, 130)
    if "mangrove" in name: return (150, 65, 60)
    if "trapdoor" in name: return (95, 65, 45)
    if "door" in name: return (110, 75, 50)
    # Stone family (light)
    if "smooth_stone" in name: return (170, 170, 170)
    if "polished_andesite" in name: return (155, 155, 155)
    if "polished_diorite" in name: return (220, 220, 220)
    if "polished_granite" in name: return (180, 130, 110)
    if "andesite" in name: return (135, 135, 135)
    if "diorite" in name: return (200, 200, 200)
    if "granite" in name: return (170, 115, 95)
    if "polished_tuff" in name: return (115, 110, 105)
    if "tuff_brick" in name or name == "tuff_bricks": return (110, 105, 100)
    if name == "tuff": return (105, 100, 95)
    # Stone bricks
    if "mossy_stone_bricks" in name: return (95, 110, 90)
    if "cracked_stone_bricks" in name: return (130, 130, 125)
    if "chiseled_stone_bricks" in name: return (115, 115, 110)
    if "stone_brick" in name: return (125, 125, 120)
    if name == "cobblestone" or "cobblestone" in name: return (100, 100, 100)
    if name == "mossy_cobblestone" or "mossy_cobble" in name: return (100, 115, 90)
    if name == "stone": return (118, 118, 118)
    # Dark stone
    if "polished_blackstone" in name: return (45, 40, 50)
    if name == "blackstone": return (38, 35, 40)
    if "cobbled_deepslate" in name: return (60, 60, 65)
    if "deepslate_brick" in name or "deepslate_tile" in name: return (70, 70, 75)
    if "polished_deepslate" in name: return (80, 80, 85)
    if "chiseled_deepslate" in name: return (78, 78, 82)
    if "deepslate" in name: return (75, 75, 80)
    if name == "obsidian": return (25, 20, 35)
    # Cathedral / sacred
    if "quartz" in name: return (240, 240, 235)
    # Bricks
    if name == "bricks" or "brick" in name and "stone" not in name and "blackstone" not in name and "tuff" not in name and "deepslate" not in name: return (165, 90, 70)
    # Lights
    if "soul_torch" in name or "soul_lantern" in name or "soul_fire" in name: return (90, 200, 220)
    if "soul" in name: return (90, 200, 220)
    if "torch" in name or "lantern" in name or name == "fire" or name == "campfire": return (255, 195, 90)
    if "candle" in name: return (245, 220, 175)
    if "glowstone" in name or "end_rod" in name: return (245, 220, 130)
    # Decoration
    if "iron_bars" in name or "iron_door" in name: return (160, 160, 165)
    if "chain" in name: return (90, 90, 95)
    if "cobweb" in name: return (210, 210, 215)
    if name == "hay_block": return (190, 165, 70)
    if "wool" in name:
        if "red" in name: return (175, 50, 50)
        if "green" in name: return (60, 130, 60)
        if "yellow" in name: return (210, 195, 90)
        if "black" in name: return (35, 35, 35)
        return (200, 200, 200)
    if "banner" in name:
        if "red" in name: return (170, 60, 60)
        if "blue" in name: return (60, 80, 165)
        return (140, 140, 145)
    if "glass" in name: return (185, 205, 220)
    if "barrel" in name: return (130, 90, 55)
    if "anvil" in name: return (75, 75, 80)
    if "flower_pot" in name: return (160, 100, 70)
    if "lectern" in name: return (140, 105, 70)
    if "bed" in name: return (170, 60, 60)
    if name == "netherrack": return (140, 60, 60)
    if name == "magma_block": return (180, 90, 50)
    # Snow
    if "snow" in name: return (235, 240, 245)
    if "ice" in name: return (170, 200, 240)
    # Default fallback
    return (170, 100, 170)


def shade(rgb, factor):
    """Darken/lighten a color. factor < 1 darkens; > 1 lightens."""
    return tuple(max(0, min(255, int(c * factor))) for c in rgb)


def is_visible(voxels, x, y, z):
    """A block is visible if at least one of its 6 neighbors is air."""
    for dx, dy, dz in [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]:
        if (x + dx, y + dy, z + dz) not in voxels:
            return True
    return False


def render_isometric(voxels, sx, sy, sz, tile=8, scale=1):
    """45° isometric view from the +X / +Z corner, looking down.

    Returns a PIL Image.
    """
    tw = tile * scale
    th = tile * scale

    # Isometric projection per voxel (x, y, z):
    #   screen_x = (x - z) * tw/2 + offset_x
    #   screen_y = (x + z) * th/4 - y * th/2 + offset_y
    # Image dims:
    width  = (sx + sz) * tw // 2 + tw * 4
    height = (sx + sz) * th // 4 + sy * th // 2 + th * 4

    img = Image.new("RGB", (width, height), (240, 240, 245))
    draw = ImageDraw.Draw(img)

    off_x = sz * tw // 2 + tw
    off_y = th * 2

    # Pre-compute visible voxels with their colors
    visible = []
    for (x, y, z), bid in voxels.items():
        if not is_visible(voxels, x, y, z):
            continue
        c = color_for(bid)
        visible.append((x, y, z, c))

    # Sort back-to-front for correct overlap:
    #   higher (x + z) drawn LATER (front)
    #   higher y drawn LATER (above)
    # Composite sort key: prioritize x+z (depth) then y (height)
    visible.sort(key=lambda v: (v[2] - v[0], v[1]))

    for x, y, z, c in visible:
        sxp = (x - z) * tw // 2 + off_x
        syp = (x + z) * th // 4 - y * th // 2 + off_y

        # Draw a 3D cube voxel: top + left face + right face
        top_color = c
        left_color = shade(c, 0.75)
        right_color = shade(c, 0.55)

        # Top diamond
        top = [
            (sxp, syp - th // 2),                 # back
            (sxp + tw // 2, syp - th // 4),       # right
            (sxp, syp),                            # front
            (sxp - tw // 2, syp - th // 4),        # left
        ]
        draw.polygon(top, fill=top_color, outline=shade(top_color, 0.85))

        # Left face
        left = [
            (sxp - tw // 2, syp - th // 4),
            (sxp, syp),
            (sxp, syp + th // 2),
            (sxp - tw // 2, syp + th // 4),
        ]
        draw.polygon(left, fill=left_color, outline=shade(left_color, 0.85))

        # Right face
        right = [
            (sxp, syp),
            (sxp + tw // 2, syp - th // 4),
            (sxp + tw // 2, syp + th // 4),
            (sxp, syp + th // 2),
        ]
        draw.polygon(right, fill=right_color, outline=shade(right_color, 0.85))

    return img


def render_topdown(voxels, sx, sy, sz, tile=6):
    """Top-down projection, with topmost block at each (x, z) cell."""
    width = sx * tile + 20
    height = sz * tile + 20
    img = Image.new("RGB", (width, height), (240, 240, 245))
    draw = ImageDraw.Draw(img)

    # For each (x, z), find the highest non-air block
    top_blocks = {}
    for (x, y, z), bid in voxels.items():
        key = (x, z)
        if key not in top_blocks or y > top_blocks[key][0]:
            top_blocks[key] = (y, bid)

    max_y = max((p[0] for p in top_blocks.values()), default=1) or 1

    for (x, z), (y, bid) in top_blocks.items():
        c = color_for(bid)
        # Shade by height — higher blocks are lighter
        h_factor = 0.55 + 0.45 * (y / max_y)
        c = shade(c, h_factor)
        px = x * tile + 10
        pz = z * tile + 10
        draw.rectangle([px, pz, px + tile - 1, pz + tile - 1], fill=c)

    return img


def render_front(voxels, sx, sy, sz, tile=6, axis="x"):
    """Front projection (looking from +Z toward -Z if axis=x).

    For each (x, y), show the highest-z block visible.
    """
    if axis == "x":
        width = sx * tile + 20
    else:
        width = sz * tile + 20
    height = sy * tile + 20

    img = Image.new("RGB", (width, height), (240, 240, 245))
    draw = ImageDraw.Draw(img)

    # For each (x, y), find the lowest-z block (front-most)
    front_blocks = {}
    for (x, y, z), bid in voxels.items():
        key = (x, y) if axis == "x" else (z, y)
        if key not in front_blocks or z < front_blocks[key][0]:
            front_blocks[key] = (z, bid)

    max_z = max((p[0] for p in front_blocks.values()), default=1) or 1

    for (a, y), (z, bid) in front_blocks.items():
        c = color_for(bid)
        z_factor = 0.55 + 0.45 * (1 - z / max_z)
        c = shade(c, z_factor)
        px = a * tile + 10
        py = (sy - 1 - y) * tile + 10
        draw.rectangle([px, py, px + tile - 1, py + tile - 1], fill=c)

    return img


def main():
    if len(sys.argv) < 2:
        print("usage: render_isc.py <file.isc> [<output.png>]", file=sys.stderr)
        sys.exit(2)

    src = Path(sys.argv[1])
    if not src.exists():
        print(f"no such file: {src}", file=sys.stderr)
        sys.exit(2)

    sx, sy, sz, palette, voxels = parse_isc(src)
    print(f"loaded {src}: {sx}×{sy}×{sz}, {len(voxels)} placed cells")

    # Three views, stitched into one PNG
    iso = render_isometric(voxels, sx, sy, sz, tile=10)
    top = render_topdown(voxels, sx, sy, sz, tile=6)
    front = render_front(voxels, sx, sy, sz, tile=6, axis="x")

    # Layout: top view in upper-left, front view in upper-right, isometric in lower row
    pad = 20
    label_h = 22
    panel_w = max(iso.width, top.width + front.width + pad)
    panel_h = max(top.height, front.height) + iso.height + pad * 3 + label_h * 2

    composite = Image.new("RGB", (panel_w + pad * 2, panel_h + pad), (250, 250, 250))
    draw = ImageDraw.Draw(composite)
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 14)
    except Exception:
        font = ImageFont.load_default()

    # Top row: top-down view + front view
    cx = pad
    cy = pad
    draw.text((cx, cy), "Top-down (highest block per X,Z)", fill=(20, 20, 20), font=font)
    composite.paste(top, (cx, cy + label_h))

    cx2 = cx + top.width + pad
    draw.text((cx2, cy), "Front (looking from +Z)", fill=(20, 20, 20), font=font)
    composite.paste(front, (cx2, cy + label_h))

    # Bottom row: isometric view
    iso_y = max(top.height, front.height) + label_h + pad * 2
    draw.text((pad, iso_y), f"Isometric — {src.name} ({sx}×{sy}×{sz}, {len(voxels)} cells)",
              fill=(20, 20, 20), font=font)
    composite.paste(iso, (pad, iso_y + label_h))

    if len(sys.argv) >= 3:
        out = Path(sys.argv[2])
    else:
        out = src.with_name(src.stem + "_preview.png")
    composite.save(out)
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
