"""Emit a Blockbench-ready geometry + texture template for PiglinVillagerModel.

Produces TWO files in art/blockbench/piglin_villager/:

  piglin_villager.geo.json   — Bedrock geometry that mirrors the Java
                               LayerDefinition. Open it in Blockbench:
                                 File > Open Model > pick this file.

  piglin_villager_template.png — 64×64 RGBA texture starter. The piglin head
                                 cube (UV cols 0-31, rows 0-15) and both ear
                                 cubes (UV cols 39-49 / 51-61, rows 6-15) are
                                 pre-painted with vanilla piglin pixels. The
                                 rest is transparent — that's what the user
                                 paints in Blockbench.

After painting, export the texture and overwrite:
  src/main/resources/assets/ironhold/textures/entity/piglin_villager.png

Conversion conventions (matches tools/geo_to_java.py exactly):
  PartPose.offset for root bone   = (pivot_x, 24 - bedrock_pivot_y, pivot_z)
  PartPose.offset for child bone  = (px - pp_x, pp_y - py, pz - pp_z)
  addBox y                        = bone_pivot_y_bedrock - cube_origin_y - cube_h
"""
import json
import math
import os
from PIL import Image

OUT_DIR = "/Users/joshua/Kingdom SMP/ironhold/art/blockbench/piglin_villager"
VANILLA_PIGLIN = "/tmp/mc-vanilla/assets/minecraft/textures/entity/piglin/piglin.png"

os.makedirs(OUT_DIR, exist_ok=True)


# ---------- Geometry --------------------------------------------------------

def deg(rad):
    return rad * 180.0 / math.pi


EAR_TILT = 0.5235988  # 30°
ARM_TILT = -0.75      # ≈ -42.97° (Java; sign is flipped below for Bedrock)

# IMPORTANT: when converting Java rotations to Bedrock, X- and Z-rotation signs
# must be flipped because Bedrock has Y-up while Mojang's Java models have Y-down.
# Y-rotation is the same. Failing to flip causes:
#   - Arms tilting backward instead of folding across the chest
#   - Ears tilting away from the head instead of folding against it

# Bone definitions. Pivots and cube origins are in Bedrock world coords
# (entity feet at y=0, head top at y=32). Cube `origin` is the cube's lowest
# (x, y, z) corner before any rotation.
bones = [
    {
        "name": "head",
        "pivot": [0, 24, 0],
        "cubes": [
            {"origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [0, 0]},
            # Villager-style nose: addBox(-1, -3, -6, 2, 4, 2) at head pivot, texOffs(24, 0)
            {"origin": [-1, 23, -6], "size": [2, 4, 2], "uv": [24, 0]}
        ]
    },
    {
        "name": "left_ear",
        "parent": "head",
        "pivot": [4.5, 30, 0],
        "rotation": [0, 0, deg(EAR_TILT)],   # SIGN FLIPPED: Java -EAR_TILT → Bedrock +EAR_TILT
        "cubes": [
            {"origin": [4.5, 25, -2], "size": [1, 5, 4], "uv": [51, 6]}
        ]
    },
    {
        "name": "right_ear",
        "parent": "head",
        "pivot": [-4.5, 30, 0],
        "rotation": [0, 0, deg(-EAR_TILT)],  # SIGN FLIPPED: Java +EAR_TILT → Bedrock -EAR_TILT
        "mirror": True,
        "cubes": [
            {"origin": [-5.5, 25, -2], "size": [1, 5, 4], "uv": [39, 6], "mirror": True}
        ]
    },
    {
        "name": "body",
        "pivot": [0, 24, 0],
        "cubes": [
            {"origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [16, 16]}
        ]
    },
    {
        "name": "arms",
        "pivot": [0, 21, -1],
        "rotation": [deg(-ARM_TILT), 0, 0],  # SIGN FLIPPED: Java -0.75 → Bedrock +0.75
        "cubes": [
            # Left forearm (held horizontally across chest before arm-bone rotation)
            {"origin": [-8, 15, -3], "size": [4, 8, 4], "uv": [44, 22]},
            # Right forearm — mirrored UV so left/right ear-uv stays the same texture region
            {"origin": [4,  15, -3], "size": [4, 8, 4], "uv": [44, 22], "mirror": True},
            # Chest cross-piece joining both forearms
            {"origin": [-4, 15, -3], "size": [8, 4, 4], "uv": [40, 38]}
        ]
    },
    {
        "name": "right_leg",
        "pivot": [-2, 12, 0],
        "cubes": [
            {"origin": [-4, 0, -2], "size": [4, 12, 4], "uv": [0, 16]}
        ]
    },
    {
        "name": "left_leg",
        "pivot": [2, 12, 0],
        "mirror": True,
        "cubes": [
            {"origin": [0, 0, -2], "size": [4, 12, 4], "uv": [0, 16], "mirror": True}
        ]
    },
]

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.piglin_villager",
                "texture_width": 64,
                "texture_height": 64,
                "visible_bounds_width": 2,
                "visible_bounds_height": 2.5,
                "visible_bounds_offset": [0, 0.75, 0]
            },
            "bones": bones
        }
    ]
}

geo_path = os.path.join(OUT_DIR, "piglin_villager.geo.json")
with open(geo_path, "w") as f:
    json.dump(geo, f, indent=2)
print(f"OK  {geo_path}")


# ---------- Texture template -----------------------------------------------
#
# Build a 64×64 RGBA canvas. Copy vanilla piglin head + ear UV regions in;
# leave everything else transparent so the user paints fresh in Blockbench.

if not os.path.exists(VANILLA_PIGLIN):
    raise SystemExit(
        f"Missing vanilla piglin.png at {VANILLA_PIGLIN}\n"
        f"Extract first: unzip <minecraft jar> "
        f"'assets/minecraft/textures/entity/piglin/*' -d /tmp/mc-vanilla"
    )

src = Image.open(VANILLA_PIGLIN).convert("RGBA")
canvas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def copy_rect(x0, y0, x1, y1):
    """Copy a rectangular UV region from vanilla piglin into the template."""
    region = src.crop((x0, y0, x1, y1))
    canvas.paste(region, (x0, y0), region)


# Head cube unwrap occupies cols 0-31, rows 0-15.
copy_rect(0, 0, 32, 16)

# Right ear cube unwrap (1×5×4 @ texOffs 39,6) occupies cols 39-49, rows 6-15.
copy_rect(39, 6, 49, 16)

# Left ear cube unwrap (1×5×4 @ texOffs 51,6) occupies cols 51-61, rows 6-15.
copy_rect(51, 6, 61, 16)

png_path = os.path.join(OUT_DIR, "piglin_villager_template.png")
canvas.save(png_path)
print(f"OK  {png_path}")

# Also write a 12× zoomed preview so the user can see what's pre-painted.
canvas.resize((64 * 12, 64 * 12), Image.NEAREST).save(
    os.path.join(OUT_DIR, "piglin_villager_template_x12.png"))
print(f"OK  {os.path.join(OUT_DIR, 'piglin_villager_template_x12.png')}")
