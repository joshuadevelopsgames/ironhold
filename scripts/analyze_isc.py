#!/usr/bin/env python3
"""
Analyze an Ironhold Structure Code (.isc), a Litematica .litematic, or a
Sponge .schem and report:
  - Footprint silhouette (XZ projection, ASCII)
  - Y-profile (non-air cells per Y level)
  - Block frequency (top 20)
  - Material era breakdown (founding / curtain / refinement / modern)
  - Light source count + breakdown
  - Decay percentage (cracked variants)
  - Defensive-feature checks (crenellations, portcullis, machicolations)
  - Build-quality scorecard against the master-builders principles

Usage:
    .recipes_venv/bin/python scripts/analyze_isc.py <file> [<file2> ...]
"""
import sys
import re
import gzip
import math
from collections import Counter, defaultdict
from pathlib import Path


# --- Block classification ---------------------------------------------

ERA_FOUNDING = {
    # Rough / crude / oldest masonry. Raw rock and uncut stone.
    "minecraft:cobbled_deepslate",
    "minecraft:mossy_cobblestone",
    "minecraft:cobblestone",
    "minecraft:cobblestone_wall",
    "minecraft:mossy_cobblestone_wall",
    "minecraft:cobblestone_slab",
    "minecraft:cobblestone_stairs",
    # Raw natural rock — the founding course of any building
    "minecraft:stone",
    "minecraft:andesite",
    "minecraft:diorite",
    "minecraft:granite",
    "minecraft:tuff",
    "minecraft:deepslate",
}
ERA_CURTAIN = {
    # Dressed brick — the working mature castle stone
    "minecraft:deepslate_bricks",
    "minecraft:stone_bricks",
    "minecraft:mossy_stone_bricks",
    "minecraft:deepslate_brick_slab",
    "minecraft:deepslate_brick_stairs",
    "minecraft:deepslate_brick_wall",
    "minecraft:stone_brick_stairs",
    "minecraft:stone_brick_slab",
    "minecraft:stone_brick_wall",
    "minecraft:chiseled_stone_bricks",
}
ERA_REFINEMENT = {
    # Polished / worked stone — peace-time wealth
    "minecraft:polished_deepslate",
    "minecraft:polished_deepslate_slab",
    "minecraft:polished_deepslate_stairs",
    "minecraft:polished_deepslate_wall",
    "minecraft:deepslate_tiles",
    "minecraft:deepslate_tile_slab",
    "minecraft:deepslate_tile_stairs",
    "minecraft:chiseled_deepslate",
    # Polished natural-stone variants
    "minecraft:polished_andesite",
    "minecraft:polished_andesite_slab",
    "minecraft:polished_andesite_stairs",
    "minecraft:polished_diorite",
    "minecraft:polished_diorite_slab",
    "minecraft:polished_diorite_stairs",
    "minecraft:polished_granite",
    "minecraft:polished_granite_slab",
    "minecraft:polished_granite_stairs",
    "minecraft:smooth_stone",
    "minecraft:smooth_stone_slab",
    # Cathedral-grade
    "minecraft:quartz_block",
    "minecraft:chiseled_quartz_block",
    "minecraft:quartz_slab",
    "minecraft:smooth_quartz",
    "minecraft:smooth_quartz_slab",
}
ERA_MODERN = {
    # Blackstone family — the dark-castle modern era
    "minecraft:polished_blackstone_bricks",
    "minecraft:polished_blackstone",
    "minecraft:chiseled_polished_blackstone",
    "minecraft:blackstone",
    "minecraft:polished_blackstone_brick_slab",
    "minecraft:polished_blackstone_brick_stairs",
    "minecraft:polished_blackstone_brick_wall",
    "minecraft:polished_blackstone_wall",
    "minecraft:gilded_blackstone",
    # Tuff family — the natural-stone modern era (MC 1.21)
    "minecraft:tuff_bricks",
    "minecraft:polished_tuff",
    "minecraft:chiseled_tuff",
    "minecraft:chiseled_tuff_bricks",
    "minecraft:tuff_brick_slab",
    "minecraft:tuff_brick_stairs",
    "minecraft:tuff_brick_wall",
    "minecraft:polished_tuff_slab",
    "minecraft:polished_tuff_stairs",
    "minecraft:polished_tuff_wall",
    "minecraft:tuff_slab",
    "minecraft:tuff_stairs",
    "minecraft:tuff_wall",
}
DECAY_BLOCKS = {
    "minecraft:cracked_deepslate_bricks",
    "minecraft:cracked_polished_blackstone_bricks",
    "minecraft:cracked_stone_bricks",
    "minecraft:cobweb",
}
LIGHT_NORMAL = {
    "minecraft:torch",
    "minecraft:wall_torch",
    "minecraft:lantern",
    "minecraft:fire",
    "minecraft:campfire",
    "minecraft:glowstone",
    "minecraft:end_rod",
}
LIGHT_SOUL = {
    "minecraft:soul_torch",
    "minecraft:soul_wall_torch",
    "minecraft:soul_lantern",
    "minecraft:soul_fire",
    "minecraft:soul_campfire",
}
LIGHT_CANDLE = {
    f"minecraft:{c}_candle" for c in
    ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
     "gray", "light_gray", "cyan", "purple", "blue", "brown", "green",
     "red", "black")
} | {"minecraft:candle"}
WOOD_PRIMARY = {
    "minecraft:oak_log", "minecraft:dark_oak_log", "minecraft:spruce_log",
    "minecraft:birch_log", "minecraft:oak_planks", "minecraft:dark_oak_planks",
    "minecraft:spruce_planks", "minecraft:birch_planks",
}
GLASS = {
    "minecraft:glass", "minecraft:glass_pane",
}
DEFENSIVE = {
    "iron_bars": "portcullis / window grate",
    "fence": "barrier",
    "wall": "low parapet / merlon",
}


def base_id(full_id):
    """Strip block-state suffix, return canonical block id."""
    return full_id.split("[")[0]


def classify_era(bid):
    if bid in ERA_FOUNDING: return "founding"
    if bid in ERA_CURTAIN:  return "curtain"
    if bid in ERA_REFINEMENT: return "refinement"
    if bid in ERA_MODERN: return "modern"
    return None


def classify_light(bid):
    if bid in LIGHT_NORMAL: return "normal"
    if bid in LIGHT_SOUL: return "soul"
    if bid in LIGHT_CANDLE: return "candle"
    return None


# --- ISC parser -------------------------------------------------------

def parse_litematic(path):
    """Load a Litematica .litematic file.
    Returns (sx, sy, sz, palette_dict, voxel_dict, metadata_dict).
    """
    import nbtlib
    with gzip.open(str(path)) as f:
        nbt = nbtlib.File.parse(f)
    root = nbt.root if hasattr(nbt, "root") else nbt

    meta_compound = root.get("Metadata", {})
    meta = {
        "name": str(meta_compound.get("Name", "")),
        "author": str(meta_compound.get("Author", "")),
        "description": str(meta_compound.get("Description", "")),
        "total_blocks": int(meta_compound.get("TotalBlocks", 0)),
        "total_volume": int(meta_compound.get("TotalVolume", 0)),
        "minecraft_data_version": int(root.get("MinecraftDataVersion", 0)),
    }

    regions = root.get("Regions", {})
    voxels = {}
    palette_full = {}

    # Track total enclosing size
    bbox_min = [float("inf"), float("inf"), float("inf")]
    bbox_max = [float("-inf"), float("-inf"), float("-inf")]

    for region_name, reg in regions.items():
        # Read region geometry
        size = reg["Size"]
        pos = reg["Position"]
        sx_r, sy_r, sz_r = int(size["x"]), int(size["y"]), int(size["z"])
        px, py, pz = int(pos["x"]), int(pos["y"]), int(pos["z"])

        # Sign-handling: negative size means the region grows in the negative direction
        # from the position. Normalize to positive size and offset origin accordingly.
        ox, oy, oz = px, py, pz
        if sx_r < 0:
            ox = px + sx_r + 1
            sx_r = -sx_r
        if sy_r < 0:
            oy = py + sy_r + 1
            sy_r = -sy_r
        if sz_r < 0:
            oz = pz + sz_r + 1
            sz_r = -sz_r

        bbox_min[0] = min(bbox_min[0], ox); bbox_max[0] = max(bbox_max[0], ox + sx_r - 1)
        bbox_min[1] = min(bbox_min[1], oy); bbox_max[1] = max(bbox_max[1], oy + sy_r - 1)
        bbox_min[2] = min(bbox_min[2], oz); bbox_max[2] = max(bbox_max[2], oz + sz_r - 1)

        # Decode block-state palette
        pal_nbt = reg.get("BlockStatePalette", [])
        palette_idx_to_id = []
        for entry in pal_nbt:
            name = str(entry.get("Name", "minecraft:air"))
            props = entry.get("Properties", {})
            if props:
                prop_pairs = sorted((str(k), str(v)) for k, v in props.items())
                state = "[" + ",".join(f"{k}={v}" for k, v in prop_pairs) + "]"
                full = name + state
            else:
                full = name
            palette_idx_to_id.append(full)
        # Stash this region's palette for later use
        for i, fid in enumerate(palette_idx_to_id):
            palette_full[f"{region_name}:{i}"] = fid

        # Decode BlockStates LongArray
        block_states = reg["BlockStates"]
        n_cells = sx_r * sy_r * sz_r
        bits_per_block = max(2, math.ceil(math.log2(len(palette_idx_to_id))))
        per_long = 64 // bits_per_block
        # Litematica uses Java edition modern packing (no cross-long packing)
        # since the .litematic spec. Bit layout: low bits of long are index 0,
        # next bits index 1, etc. within the long; long order is sequential.

        # Convert NBT longs (signed int64) to unsigned
        longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in block_states]
        mask = (1 << bits_per_block) - 1

        # NOTE: Litematica's packing is sequential-bit (cross-long packing) —
        # not the modern Minecraft world packing. Each block uses `bits_per_block`
        # contiguous bits starting at bit offset `idx * bits_per_block`.
        n_longs = len(longs)
        air_id = "minecraft:air"

        # Iterate in litematic's index order: y * (sx_r * sz_r) + z * sx_r + x
        for idx in range(n_cells):
            start_bit = idx * bits_per_block
            start_long = start_bit // 64
            start_off = start_bit % 64
            if start_long >= n_longs:
                pid = 0
            else:
                if start_off + bits_per_block <= 64:
                    pid = (longs[start_long] >> start_off) & mask
                else:
                    # Spans two longs
                    low_bits = 64 - start_off
                    high_bits = bits_per_block - low_bits
                    low = longs[start_long] >> start_off
                    high = longs[start_long + 1] & ((1 << high_bits) - 1) if start_long + 1 < n_longs else 0
                    pid = (low | (high << low_bits)) & mask

            if pid >= len(palette_idx_to_id):
                continue
            full_id = palette_idx_to_id[pid]
            if base_id(full_id) == "minecraft:air":
                continue

            # Convert index to (x, y, z) in region-local space
            xy_area = sx_r * sz_r
            y_local = idx // xy_area
            rem = idx % xy_area
            z_local = rem // sx_r
            x_local = rem % sx_r

            # Convert to absolute coords (use ox, oy, oz as origin)
            voxels[(ox + x_local, oy + y_local, oz + z_local)] = full_id

    # Normalize to (0,0,0) origin
    if voxels:
        mn = [min(v[i] for v in voxels) for i in range(3)]
        voxels = {(x - mn[0], y - mn[1], z - mn[2]): bid for (x, y, z), bid in voxels.items()}
        mx = [max(v[i] for v in voxels) for i in range(3)]
        sx = mx[0] + 1
        sy = mx[1] + 1
        sz = mx[2] + 1
    else:
        sx = sy = sz = 0

    return sx, sy, sz, palette_full, voxels, meta


def parse_isc(path):
    """Returns (sx, sy, sz, palette_dict, voxel_dict)
    voxel_dict maps (x,y,z) -> full block_id (with state).
    """
    text = Path(path).read_text()
    lines = text.splitlines()

    sx = sy = sz = None
    palette = {}
    voxels = {}

    state = "header"
    cur_y = None
    cur_z = 0

    for line in lines:
        if state == "header":
            if line.startswith("#"):
                continue
            if line.startswith("size "):
                _, sx_s, sy_s, sz_s = line.split()
                sx, sy, sz = int(sx_s), int(sy_s), int(sz_s)
                continue
            if line.strip() == "palette":
                state = "palette"
                continue
            if line.strip() == "body":
                state = "body"
                continue
            continue

        if state == "palette":
            if line.strip() == "body":
                state = "body"
                continue
            s = line.strip()
            if not s:
                continue
            # `XX minecraft:block[state]`
            parts = s.split(None, 1)
            if len(parts) == 2:
                code, block_id = parts
                palette[code] = block_id
            continue

        if state == "body":
            stripped = line.strip()
            if not stripped:
                continue
            # Layer markers like `y=0`
            m = re.match(r"y\s*=\s*(\d+)", stripped)
            if m:
                cur_y = int(m.group(1))
                cur_z = 0
                continue
            # Data row: cells separated by whitespace
            cells = stripped.split()
            if cur_y is None:
                continue
            for x, code in enumerate(cells):
                bid = palette.get(code, "minecraft:air")
                if base_id(bid) != "minecraft:air":
                    voxels[(x, cur_y, cur_z)] = bid
            cur_z += 1

    return sx, sy, sz, palette, voxels


# --- Analysis primitives ---------------------------------------------

def y_profile(voxels, sy):
    counts = [0] * sy
    for (x, y, z) in voxels:
        if 0 <= y < sy:
            counts[y] += 1
    return counts


def block_freq(voxels):
    c = Counter()
    for bid in voxels.values():
        c[base_id(bid)] += 1
    return c


def era_breakdown(voxels):
    counts = Counter()
    total_stone = 0
    for bid in voxels.values():
        b = base_id(bid)
        era = classify_era(b)
        if era is not None:
            counts[era] += 1
            total_stone += 1
        if b in DECAY_BLOCKS:
            counts["decay"] += 1
    return counts, total_stone


def light_breakdown(voxels):
    counts = Counter()
    for bid in voxels.values():
        b = base_id(bid)
        kind = classify_light(b)
        if kind:
            counts[kind] += 1
    return counts


def footprint_ascii(voxels, sx, sz, max_w=80, max_h=40):
    """ASCII silhouette of the XZ footprint (top-down)."""
    occupied = set()
    for (x, y, z) in voxels:
        occupied.add((x, z))

    # Downsample if huge
    sx_disp = min(sx, max_w)
    sz_disp = min(sz, max_h)
    step_x = max(1, sx // sx_disp)
    step_z = max(1, sz // sz_disp)

    lines = []
    for z in range(0, sz, step_z):
        row = []
        for x in range(0, sx, step_x):
            # Cell is occupied if any cell in the block is occupied
            hit = any((x + dx, z + dz) in occupied
                      for dx in range(step_x) for dz in range(step_z))
            row.append("█" if hit else " ")
        lines.append("".join(row))
    return lines


def silhouette_ascii(voxels, sx, sy, max_h=40, max_w=80, axis="x"):
    """ASCII silhouette projected onto a vertical plane.

    axis="x" projects onto the XY plane (looking from +Z toward -Z).
    """
    occupied = set()
    for (x, y, z) in voxels:
        if axis == "x":
            occupied.add((x, y))
        else:
            occupied.add((z, y))

    width = sx if axis == "x" else sx  # caller chooses
    height = sy

    # Downsample
    w_disp = min(width, max_w)
    step_w = max(1, width // w_disp)
    step_h = max(1, height // max_h)

    lines = []
    # Render from top to bottom
    for y in range(height - 1, -1, -step_h):
        row = []
        for x in range(0, width, step_w):
            hit = any((x + dx, y - dy) in occupied
                      for dx in range(step_w) for dy in range(step_h))
            row.append("█" if hit else " ")
        lines.append("".join(row))
    return lines


# --- Report ----------------------------------------------------------

def load_any(path):
    """Dispatch on extension. Returns (sx, sy, sz, palette, voxels, meta_or_None)."""
    p = Path(path)
    if p.suffix == ".litematic":
        sx, sy, sz, palette, voxels, meta = parse_litematic(p)
        return sx, sy, sz, palette, voxels, meta
    elif p.suffix == ".isc":
        sx, sy, sz, palette, voxels = parse_isc(p)
        return sx, sy, sz, palette, voxels, None
    else:
        raise ValueError(f"unknown extension: {p.suffix} (need .isc or .litematic)")


def report(path):
    sx, sy, sz, palette, voxels, meta = load_any(path)
    out = []
    out.append(f"# Analysis: {path}")
    if meta:
        out.append(f"  Title: {meta.get('name') or '(unnamed)'}")
        out.append(f"  Author: {meta.get('author') or '(unknown)'}")
        if meta.get('description'):
            out.append(f"  Description: {meta['description']}")
        out.append(f"  Reported total blocks: {meta.get('total_blocks')}")
        out.append(f"  Minecraft data version: {meta.get('minecraft_data_version')}")
    out.append(f"  Dimensions: {sx} × {sy} × {sz}  (volume {sx*sy*sz}, "
               f"placed {len(voxels)} = {100*len(voxels)/(sx*sy*sz):.1f}%)")
    out.append(f"  Palette size: {len(palette)} entries")

    # Block frequency
    freq = block_freq(voxels)
    out.append("\n## Top 15 blocks (non-air)")
    for bid, n in freq.most_common(15):
        out.append(f"  {n:>6}  {100*n/len(voxels):>5.1f}%  {bid}")

    # Y profile
    yp = y_profile(voxels, sy)
    max_y = max(yp) if yp else 1
    out.append("\n## Y-profile (cells per Y layer; * scaled to peak)")
    bar_w = 50
    for y, c in enumerate(yp):
        bar = "*" * (int(bar_w * c / max_y) if max_y else 0)
        out.append(f"  y={y:>3} {c:>5}  {bar}")

    # Era breakdown
    eras, total_stone = era_breakdown(voxels)
    out.append("\n## Material era breakdown")
    if total_stone:
        for era_name in ("founding", "curtain", "refinement", "modern"):
            n = eras.get(era_name, 0)
            out.append(f"  {era_name:>11}: {n:>5}  ({100*n/total_stone:.1f}% of stone)")
        out.append(f"  {'decay':>11}: {eras.get('decay', 0):>5}  "
                   f"({100*eras.get('decay', 0)/total_stone:.1f}% of stone)")
    else:
        out.append("  no era-classified stone found")

    # Lighting
    lights = light_breakdown(voxels)
    out.append("\n## Lighting breakdown")
    for kind in ("normal", "soul", "candle"):
        out.append(f"  {kind:>7}: {lights.get(kind, 0)}")
    total_lights = sum(lights.values())
    floor_area = sx * sz
    if floor_area > 0:
        out.append(f"  light density: {total_lights} lights / {floor_area} m² = "
                   f"{total_lights/floor_area*10:.2f} per 10 m²")

    # Defensive features
    out.append("\n## Defensive feature signals")
    portcullis = sum(n for bid, n in freq.items() if "iron_bars" in bid)
    out.append(f"  iron_bars cells: {portcullis} (portcullises, grate windows)")
    walls = sum(n for bid, n in freq.items() if "_wall" in bid)
    out.append(f"  wall blocks: {walls} (low parapets, merlon tops)")
    fence = sum(n for bid, n in freq.items() if "_fence" in bid)
    out.append(f"  fence blocks: {fence} (railings, sentry posts)")

    # Footprint silhouette
    out.append("\n## Footprint (XZ projection, top-down)")
    for line in footprint_ascii(voxels, sx, sz, max_w=60, max_h=30):
        out.append("  " + line)

    # Front silhouette (XY)
    out.append("\n## Front silhouette (XY projection, looking from +Z)")
    for line in silhouette_ascii(voxels, sx, sy, max_h=24, max_w=60, axis="x"):
        out.append("  " + line)

    # Scorecard against principles
    out.append("\n## Master-builders scorecard")
    score = []

    # Era diversity (material storytelling)
    eras_present = sum(1 for k in ("founding", "curtain", "refinement", "modern")
                        if eras.get(k, 0) > 50)
    score.append(("Era diversity (≥3 of 4 categories)", eras_present, ">=3"))

    # Decay percentage (~5-12% is healthy)
    if total_stone:
        decay_pct = 100 * eras.get("decay", 0) / total_stone
        score.append(("Decay percentage", f"{decay_pct:.1f}%", "5-12%"))
    else:
        score.append(("Decay percentage", "n/a", "5-12%"))

    # Light commitment (soul-only or normal-only)
    soul = lights.get("soul", 0)
    normal = lights.get("normal", 0)
    light_purity = "PURE_SOUL" if normal == 0 and soul > 0 else \
                   "PURE_NORMAL" if soul == 0 and normal > 0 else \
                   "MIXED" if soul > 0 and normal > 0 else "NONE"
    score.append(("Light commitment", light_purity, "pure (one kind dominant)"))

    # Y-profile: variety in heights
    nonzero_ys = [c for c in yp if c > 5]
    score.append(("Y-levels with >5 cells (silhouette layers)", len(nonzero_ys), ">=8"))

    # Footprint ratio
    out.append("")
    for name, value, target in score:
        out.append(f"  {name:<50} {value!s:<14} target: {target}")

    return "\n".join(out)


def main():
    if len(sys.argv) < 2:
        print("usage: analyze_isc.py <file.isc|.litematic> [<file2> ...]", file=sys.stderr)
        sys.exit(2)
    for path in sys.argv[1:]:
        print(report(path))
        print()
        print("=" * 72)
        print()


if __name__ == "__main__":
    main()
