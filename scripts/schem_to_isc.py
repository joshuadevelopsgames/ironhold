#!/usr/bin/env python3
"""
Convert a Sponge-format .schem (or legacy .schematic) into Ironhold's .isc.

Why: the .isc format is plain text and AI-readable. Once a public Minecraft
build is in .isc, every analysis tool we already have (analyze_isc.py,
visual inspection, pattern extraction) just works on it.

Usage:
    .recipes_venv/bin/python scripts/schem_to_isc.py input.schem > output.isc

Handles:
  - Sponge schematic v2 / v3 (most modern public builds use this)
  - Legacy MCEdit .schematic format (older PMC and worldedit exports)

Limitations:
  - Skips block entities (chests with loot, sign text, banner patterns).
    The block ID + state survives; the inner data does not.
  - Skips entities.
  - Caps at IscStructure limits (each dim ≤ 64, volume ≤ 64^3). Larger
    builds are scaled / cropped with a warning.
"""
import sys
import gzip
import re
from pathlib import Path

import nbtlib
import mcschematic


MAX_DIM = 64
MAX_VOLUME = 64 * 64 * 64
MAX_PALETTE = 36 * 36


def base36(i: int) -> str:
    def d(n):
        return chr(ord('0') + n) if n < 10 else chr(ord('a') + n - 10)
    return d(i // 36) + d(i % 36)


def crop_window(sx: int, sy: int, sz: int):
    """Return (x0,y0,z0,x1,y1,z1) — clamp each dim to MAX_DIM, centered.
       Falls back to the largest fitting volume if total exceeds MAX_VOLUME."""
    cx = min(sx, MAX_DIM)
    cy = min(sy, MAX_DIM)
    cz = min(sz, MAX_DIM)
    while cx * cy * cz > MAX_VOLUME:
        # Reduce the largest axis first
        if cx >= cy and cx >= cz:
            cx -= 1
        elif cy >= cz:
            cy -= 1
        else:
            cz -= 1
    # Center the window
    x0 = max(0, (sx - cx) // 2)
    y0 = max(0, (sy - cy) // 2)
    z0 = max(0, (sz - cz) // 2)
    return x0, y0, z0, x0 + cx - 1, y0 + cy - 1, z0 + cz - 1


def load_with_mcschematic(path: Path):
    """Try Sponge format first via mcschematic. Returns (sx, sy, sz, get_block)
       or None on failure."""
    try:
        schem = mcschematic.MCSchematic(schematicToLoadPath_or_mcStructure=str(path))
        # mcschematic exposes the bounding box via getStructureDimensions or similar
        # Different versions of the lib have different APIs; try a couple
        if hasattr(schem, "getStructureDimensions"):
            dims = schem.getStructureDimensions()
            sx, sy, sz = dims
        elif hasattr(schem, "structureSize"):
            sx, sy, sz = schem.structureSize
        else:
            # Fall back to scanning all blocks
            blocks = schem.getBlocks() if hasattr(schem, "getBlocks") else {}
            if not blocks:
                return None
            xs = [k[0] for k in blocks]
            ys = [k[1] for k in blocks]
            zs = [k[2] for k in blocks]
            sx = max(xs) - min(xs) + 1
            sy = max(ys) - min(ys) + 1
            sz = max(zs) - min(zs) + 1
        # API: getBlockDataAt((x,y,z)) → "minecraft:stone[...]"
        def get_block(x, y, z):
            try:
                return schem.getBlockDataAt((x, y, z))
            except Exception:
                return "minecraft:air"
        return sx, sy, sz, get_block
    except Exception as e:
        print(f"  mcschematic loader failed: {e}", file=sys.stderr)
        return None


def load_legacy_nbt(path: Path):
    """Try legacy MCEdit .schematic (gzipped NBT) via nbtlib."""
    try:
        nbt = nbtlib.load(str(path))
        root = nbt.root if hasattr(nbt, "root") else nbt
        if "Width" not in root or "Blocks" not in root:
            return None
        sx = int(root["Width"])
        sy = int(root["Height"])
        sz = int(root["Length"])
        blocks = root["Blocks"]  # ByteArray — block IDs (legacy 1.12 IDs)
        # Legacy block IDs are numeric — we need to map them to modern names.
        # Without a full vanilla flattening table baked in, we make a minimal
        # mapping for the most common medieval-build blocks.
        from_legacy = LEGACY_ID_MAP

        def get_block(x, y, z):
            idx = (y * sz + z) * sx + x
            if idx >= len(blocks):
                return "minecraft:air"
            bid = int(blocks[idx]) & 0xFF
            return from_legacy.get(bid, "minecraft:stone")
        return sx, sy, sz, get_block
    except Exception as e:
        print(f"  nbtlib loader failed: {e}", file=sys.stderr)
        return None


# Minimal legacy ID → modern block-id table (1.12.2 -> 1.20+ flattening).
# Only covers the blocks that show up in 99% of medieval builds; everything
# else falls back to stone.  Expand as needed.
LEGACY_ID_MAP = {
    0:   "minecraft:air",
    1:   "minecraft:stone",
    2:   "minecraft:grass_block[snowy=false]",
    3:   "minecraft:dirt",
    4:   "minecraft:cobblestone",
    5:   "minecraft:oak_planks",
    7:   "minecraft:bedrock",
    8:   "minecraft:water[level=0]",
    9:   "minecraft:water[level=0]",
    12:  "minecraft:sand",
    13:  "minecraft:gravel",
    17:  "minecraft:oak_log[axis=y]",
    18:  "minecraft:oak_leaves[distance=7,persistent=false,waterlogged=false]",
    20:  "minecraft:glass",
    24:  "minecraft:sandstone",
    35:  "minecraft:white_wool",
    44:  "minecraft:stone_brick_slab[type=bottom,waterlogged=false]",
    45:  "minecraft:bricks",
    48:  "minecraft:mossy_cobblestone",
    49:  "minecraft:obsidian",
    50:  "minecraft:torch",
    53:  "minecraft:oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
    54:  "minecraft:chest[facing=north,type=single,waterlogged=false]",
    65:  "minecraft:ladder[facing=south,waterlogged=false]",
    67:  "minecraft:cobblestone_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
    85:  "minecraft:oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]",
    98:  "minecraft:stone_bricks",
    101: "minecraft:iron_bars[east=false,north=false,south=false,waterlogged=false,west=false]",
    102: "minecraft:glass_pane[east=false,north=false,south=false,waterlogged=false,west=false]",
    109: "minecraft:stone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
    112: "minecraft:nether_bricks",
    126: "minecraft:oak_slab[type=bottom,waterlogged=false]",
    128: "minecraft:sandstone_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
    134: "minecraft:spruce_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
    135: "minecraft:birch_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
    136: "minecraft:jungle_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
    139: "minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]",
    155: "minecraft:quartz_block",
    156: "minecraft:quartz_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
    162: "minecraft:acacia_log[axis=y]",
    170: "minecraft:hay_block[axis=y]",
    179: "minecraft:red_sandstone",
}


def main():
    if len(sys.argv) < 2:
        print("usage: schem_to_isc.py <input.schem|.schematic>", file=sys.stderr)
        sys.exit(2)

    path = Path(sys.argv[1])
    if not path.exists():
        print(f"no such file: {path}", file=sys.stderr)
        sys.exit(2)

    loaded = load_with_mcschematic(path)
    if loaded is None:
        loaded = load_legacy_nbt(path)
    if loaded is None:
        print(f"could not parse {path} as Sponge or legacy schematic", file=sys.stderr)
        sys.exit(1)

    sx, sy, sz, get_block = loaded
    print(f"# source dims: {sx}×{sy}×{sz}", file=sys.stderr)

    # Crop to ISC limits
    x0, y0, z0, x1, y1, z1 = crop_window(sx, sy, sz)
    cx, cy, cz = x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1
    if (cx, cy, cz) != (sx, sy, sz):
        print(f"# cropped to {cx}×{cy}×{cz} (centered window)", file=sys.stderr)

    # Build palette + data
    palette_idx = {}
    palette = []
    data = [None] * (cx * cy * cz)

    def index_for_state(state: str) -> int:
        # Normalize: ensure mc namespace, strip nbt-data clauses
        if state is None or state == "":
            state = "minecraft:air"
        # mcschematic returns lowercase. State props in brackets are kept.
        if "{" in state:  # NBT data clause
            state = state.split("{", 1)[0]
        if not state.startswith("minecraft:"):
            state = "minecraft:" + state
        if state not in palette_idx:
            if len(palette) >= MAX_PALETTE:
                # Out of palette space — collapse to air (rare; only on
                # extremely state-rich builds)
                state = "minecraft:air"
            else:
                palette_idx[state] = len(palette)
                palette.append(state)
        return palette_idx[state]

    for y in range(cy):
        for z in range(cz):
            for x in range(cx):
                state = get_block(x0 + x, y0 + y, z0 + z)
                data[(y * cz + z) * cx + x] = index_for_state(state)

    # ── Emit ISC ──
    out = sys.stdout
    out.write("# Ironhold Structure Code v1\n")
    out.write(f"# Converted from {path.name}\n")
    if (cx, cy, cz) != (sx, sy, sz):
        out.write(f"# Note: cropped from {sx}x{sy}x{sz} → {cx}x{cy}x{cz}\n")
    out.write(f"size {cx} {cy} {cz}\n")
    out.write("palette\n")
    for i, st in enumerate(palette):
        out.write(f"  {base36(i)} {st}\n")
    out.write("body\n")
    for y in range(cy):
        out.write(f"y={y}\n")
        for z in range(cz):
            row = " ".join(base36(data[(y * cz + z) * cx + x]) for x in range(cx))
            out.write(f"  {row}\n")

    solid = sum(1 for v in data if palette[v] != "minecraft:air")
    print(f"# converted: {cx}x{cy}x{cz}  solid={solid}  palette={len(palette)}", file=sys.stderr)


if __name__ == "__main__":
    main()
