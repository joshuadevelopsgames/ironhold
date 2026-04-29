"""
Reads king_enderman_voxels.json (output of voxelize_king_enderman.py) and
emits:
  - king_enderman.geo.json (Blockbench-compatible)
  - KingEndermanModel.java (vanilla NeoForge model class)
  - A fresh diffuse + emissive texture

Coordinate conversion:
  Voxel grid shape: (SX, SY, SZ)
  Each voxel → MC_UNITS_PER_VOXEL Blockbench units.
  Final model is centered on X/Z and sits feet-on-ground at Y=0.
"""
from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image


VOX_JSON = Path("/Users/joshua/Kingdom SMP/ironhold/tools/king_enderman_voxels.json")
GEO_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/geo/king_enderman.geo.json")
JAVA_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/java/kingdom/smp/client/entity/KingEndermanModel.java")
TEX_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman.png")
GLOW_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman_glow.png")

MC_UNITS_PER_VOXEL = 2.5  # 36 voxels tall × 2.5 = 90 MC units (~5.6 blocks)
TEX_W, TEX_H = 64, 64


def load_boxes():
    data = json.loads(VOX_JSON.read_text())
    boxes = data["boxes"]
    sx, sy, sz = data["matrix_shape"]
    return boxes, (sx, sy, sz)


def to_blockbench(boxes, shape):
    """
    Convert voxel-grid boxes to Blockbench coords.
    - X is centered on 0 (model symmetry axis)
    - Z is centered on 0
    - Y=0 is the feet (ground)
    Returns list of dicts: {origin: [x, y, z], size: [w, h, d]}
    """
    sx, sy, sz = shape
    # Compute X/Z center offset so model is centered horizontally.
    cx = sx / 2.0
    cz = sz / 2.0
    out = []
    for b in boxes:
        ox, oy, oz = b["origin"]
        wx, wy, wz = b["size"]
        # Voxel→MC units. X/Z centered, Y from ground up.
        bb_x = (ox - cx) * MC_UNITS_PER_VOXEL
        bb_y = oy * MC_UNITS_PER_VOXEL
        bb_z = (oz - cz) * MC_UNITS_PER_VOXEL
        bb_w = wx * MC_UNITS_PER_VOXEL
        bb_h = wy * MC_UNITS_PER_VOXEL
        bb_d = wz * MC_UNITS_PER_VOXEL
        out.append({
            "origin": [bb_x, bb_y, bb_z],
            "size": [bb_w, bb_h, bb_d],
        })
    return out


# ── Bone assignment: split the model into head/body/arms/legs by position ──
def classify_bone(origin, size, bounds):
    """
    bounds = (min_y, max_y, min_x, max_x, min_z, max_z)
    Returns one of: "head", "right_arm", "left_arm", "right_leg", "left_leg", "body".
    """
    _, max_y, _, _, _, _ = bounds
    cx = origin[0] + size[0] / 2
    cy = origin[1] + size[1] / 2

    # Head: upper ~22% of total height
    if cy > max_y * 0.78:
        return "head"

    # Legs: lower ~45% AND off-center in X (ignore central hip pieces)
    if cy < max_y * 0.45:
        if cx < -3:
            return "right_leg"
        if cx > 3:
            return "left_leg"
        return "body"  # central hip

    # Mid-height: arms are far off-center
    if cy < max_y * 0.78 and cy > max_y * 0.35:
        if cx < -14:
            return "right_arm"
        if cx > 14:
            return "left_arm"

    return "body"


def pivot_for(bone, bounds):
    """Return an anchor pivot (in BB coords) for each bone's rotation."""
    max_y = bounds[1]
    return {
        "body":      [0, max_y * 0.48, 0],
        "head":      [0, max_y * 0.82, 0],
        "right_arm": [-17, max_y * 0.78, 0],
        "left_arm":  [ 17, max_y * 0.78, 0],
        "right_leg": [-7,  max_y * 0.48, 0],
        "left_leg":  [ 7,  max_y * 0.48, 0],
    }[bone]


def main():
    raw_boxes, shape = load_boxes()
    bb_boxes = to_blockbench(raw_boxes, shape)
    print(f"Loaded {len(bb_boxes)} boxes.")

    # Shift every cube so the lowest filled Y sits at exactly 0 (feet on ground).
    min_y = min(b["origin"][1] for b in bb_boxes)
    for b in bb_boxes:
        b["origin"][1] -= min_y

    # Compute world-space bounds for bone classification.
    xs = [b["origin"][0] for b in bb_boxes] + [b["origin"][0] + b["size"][0] for b in bb_boxes]
    ys = [b["origin"][1] for b in bb_boxes] + [b["origin"][1] + b["size"][1] for b in bb_boxes]
    zs = [b["origin"][2] for b in bb_boxes] + [b["origin"][2] + b["size"][2] for b in bb_boxes]
    bounds = (min(ys), max(ys), min(xs), max(xs), min(zs), max(zs))
    print(f"Bounds X=[{min(xs):.1f},{max(xs):.1f}] Y=[{min(ys):.1f},{max(ys):.1f}] Z=[{min(zs):.1f},{max(zs):.1f}]")

    # Assign each box to a bone.
    by_bone = {k: [] for k in ["body", "head", "right_arm", "left_arm", "right_leg", "left_leg"]}
    for b in bb_boxes:
        bone = classify_bone(b["origin"], b["size"], bounds)
        by_bone[bone].append(b)
    for k, v in by_bone.items():
        print(f"  {k}: {len(v)} cubes")

    # ── Generate geo.json ──────────────────────────────────────────────────
    bones_json = []
    # Root hierarchy: body is child of root; head/arms are children of body; legs are children of root.
    bone_parent = {
        "body": None,
        "head": "body",
        "right_arm": "body",
        "left_arm": "body",
        "right_leg": None,
        "left_leg": None,
    }
    for name in ["body", "head", "right_arm", "left_arm", "right_leg", "left_leg"]:
        if not by_bone[name]:
            continue
        bone_entry = {
            "name": name,
            "pivot": pivot_for(name, bounds),
            "cubes": [
                {
                    "origin": [round(c["origin"][i], 3) for i in range(3)],
                    "size":   [round(c["size"][i],   3) for i in range(3)],
                    "uv": [0, 0],
                } for c in by_bone[name]
            ],
        }
        if bone_parent[name]:
            bone_entry["parent"] = bone_parent[name]
        bones_json.append(bone_entry)

    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.ironhold:king_enderman",
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 8,
                "visible_bounds_height": 7,
                "visible_bounds_offset": [0, 3, 0],
            },
            "bones": bones_json,
        }],
    }
    GEO_OUT.write_text(json.dumps(geo, indent=2))
    print(f"Wrote {GEO_OUT.name} ({len(bb_boxes)} cubes).")

    # ── Generate Java model ────────────────────────────────────────────────
    emit_java(by_bone, bounds)

    # ── Generate textures ──────────────────────────────────────────────────
    emit_textures()


def emit_java(by_bone, bounds):
    """
    Generate KingEndermanModel.java — maps each BB cube to vanilla NeoForge addBox.

    Conversion for each bone:
      Java PartPose.offset = (pivot_x, 24 - pivot_y, pivot_z)
      For each cube (BB origin=[x,y,z], size=[w,h,d]) with bone pivot (px,py,pz):
        Java addBox(x - px, py - y - h, z - pz, w, h, d)
    """
    bone_parent = {
        "body": None,
        "head": "body",
        "right_arm": "body",
        "left_arm": "body",
        "right_leg": None,
        "left_leg": None,
    }

    # Resolve pivots in the same way as geo.json.
    pivots = {name: pivot_for(name, bounds) for name in by_bone}

    # Java offset for each bone:
    #   Root bones (parent=None): offset = (px, 24 - py, pz)
    #   Child bones: offset relative to parent pivot = (px - ppx, ppy - py, pz - ppz)
    java_offsets = {}
    for name in by_bone:
        p = pivots[name]
        parent = bone_parent[name]
        if parent is None:
            java_offsets[name] = (p[0], 24 - p[1], p[2])
        else:
            pp = pivots[parent]
            java_offsets[name] = (p[0] - pp[0], pp[1] - p[1], p[2] - pp[2])

    lines = [
        "package kingdom.smp.client.entity;",
        "",
        "import kingdom.smp.Ironhold;",
        "import net.minecraft.client.model.EntityModel;",
        "import net.minecraft.client.model.geom.ModelLayerLocation;",
        "import net.minecraft.client.model.geom.ModelPart;",
        "import net.minecraft.client.model.geom.PartPose;",
        "import net.minecraft.client.model.geom.builders.CubeDeformation;",
        "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
        "import net.minecraft.client.model.geom.builders.LayerDefinition;",
        "import net.minecraft.client.model.geom.builders.MeshDefinition;",
        "import net.minecraft.client.model.geom.builders.PartDefinition;",
        "import net.minecraft.resources.Identifier;",
        "import net.minecraft.util.Mth;",
        "",
        "/** AUTO-GENERATED from Meshy OBJ via voxelize_king_enderman.py.",
        " *  Do not hand-edit; refine the source and regenerate.",
        " *  Geometry identifier: geometry.ironhold:king_enderman. */",
        "public class KingEndermanModel extends EntityModel<KingEndermanRenderState> {",
        "",
        "    public static final ModelLayerLocation LAYER_LOCATION =",
        "        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, \"king_enderman\"), \"main\");",
        "",
        "    private static final CubeDeformation NONE = CubeDeformation.NONE;",
        "",
        "    private final ModelPart body;",
        "    private final ModelPart head;",
        "    private final ModelPart rightArm;",
        "    private final ModelPart leftArm;",
        "    private final ModelPart rightLeg;",
        "    private final ModelPart leftLeg;",
        "",
        "    public KingEndermanModel(ModelPart root) {",
        "        super(root);",
        "        this.body     = root.getChild(\"body\");",
        "        this.head     = this.body.getChild(\"head\");",
        "        this.rightArm = this.body.getChild(\"right_arm\");",
        "        this.leftArm  = this.body.getChild(\"left_arm\");",
        "        this.rightLeg = root.getChild(\"right_leg\");",
        "        this.leftLeg  = root.getChild(\"left_leg\");",
        "    }",
        "",
        "    public static LayerDefinition createBodyLayer() {",
        "        MeshDefinition mesh = new MeshDefinition();",
        "        PartDefinition root = mesh.getRoot();",
        "",
    ]

    # Bone declarations in order; parents before children.
    emit_order = ["body", "right_leg", "left_leg", "head", "right_arm", "left_arm"]
    declared = {}

    for name in emit_order:
        if not by_bone.get(name):
            continue
        p = pivots[name]
        ox, oy, oz = java_offsets[name]
        parent = bone_parent[name]
        parent_var = declared[parent] if parent else "root"
        var_name = name if parent is None else name  # flat names fine

        lines.append(f"        // ── {name}  (pivot BB [{p[0]:.2f}, {p[1]:.2f}, {p[2]:.2f}]) ──")
        lines.append(f"        PartDefinition {var_name} = {parent_var}.addOrReplaceChild(\"{name}\",")
        lines.append("            CubeListBuilder.create()")

        for c in by_bone[name]:
            ox_c, oy_c, oz_c = c["origin"]
            w, h, d = c["size"]
            # Convert BB cube to Java addBox: x relative to pivot, Y flipped.
            jx = ox_c - p[0]
            jy = p[1] - oy_c - h
            jz = oz_c - p[2]
            lines.append(
                f"                .texOffs(0, 0).addBox({jx:.2f}F, {jy:.2f}F, {jz:.2f}F, "
                f"{w:.2f}F, {h:.2f}F, {d:.2f}F, NONE)"
            )
        # Finish the CubeListBuilder chain with a semicolon at the LAST line.
        # Easiest: append a no-op pose and close.
        lines[-1] = lines[-1] + ","
        lines.append(
            f"            PartPose.offset({ox:.2f}F, {oy:.2f}F, {oz:.2f}F));"
        )
        lines.append("")
        declared[name] = var_name

    lines.extend([
        f"        return LayerDefinition.create(mesh, {TEX_W}, {TEX_H});",
        "    }",
        "",
        "    @Override",
        "    public void setupAnim(KingEndermanRenderState state) {",
        "        super.setupAnim(state);",
        "        float t = state.ageInTicks;",
        "        float walkSpeed = state.walkAnimationSpeed;",
        "        float walkPos   = state.walkAnimationPos;",
        "",
        "        this.head.yRot = state.yRot * ((float) Math.PI / 180F);",
        "        this.head.xRot = state.xRot * ((float) Math.PI / 180F);",
        "        this.head.xRot += Mth.sin(t * 0.03F) * 0.04F;",
        "",
        "        float stride = Mth.cos(walkPos * 0.3F) * 0.8F * walkSpeed;",
        "        this.rightLeg.xRot = stride;",
        "        this.leftLeg.xRot  = -stride;",
        "        this.rightArm.xRot = -stride * 0.6F;",
        "        this.leftArm.xRot  =  stride * 0.6F;",
        "",
        "        if (state.isEnraged) {",
        "            this.body.xRot = 0.12F;",
        "            this.rightArm.zRot =  0.22F;",
        "            this.leftArm.zRot  = -0.22F;",
        "        } else {",
        "            this.body.xRot = 0.0F;",
        "            this.rightArm.zRot = 0.06F;",
        "            this.leftArm.zRot  = -0.06F;",
        "        }",
        "    }",
        "}",
        "",
    ])
    JAVA_OUT.write_text("\n".join(lines))
    print(f"Wrote {JAVA_OUT.name}")


def emit_textures():
    """Paint a small noisy obsidian texture — every cube reads (0,0) patch."""
    random.seed(0xBEEF)
    PAL = [
        (0x0D, 0x03, 0x19),
        (0x14, 0x04, 0x28),
        (0x1A, 0x07, 0x32),
        (0x23, 0x0E, 0x3E),
        (0x2C, 0x17, 0x48),
        (0x36, 0x22, 0x54),
        (0x42, 0x2E, 0x60),
    ]

    def pick():
        r = random.random()
        if r < 0.65:
            return PAL[random.randint(0, 2)]
        if r < 0.92:
            return PAL[random.randint(2, 4)]
        return PAL[random.randint(4, 6)]

    def jitter(c, amt=4):
        return tuple(max(0, min(255, ch + random.randint(-amt, amt))) for ch in c)

    base = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    bp = base.load()
    for y in range(TEX_H):
        for x in range(TEX_W):
            c = jitter(pick(), 4)
            if random.random() < 0.005:
                c = jitter((0x66, 0x22, 0x8A), 6)  # magenta speck
            bp[x, y] = (*c, 255)
    base.save(TEX_OUT)
    print(f"Wrote {TEX_OUT.name}")

    # Glow: transparent (eyes are now part of the voxel model; handled via
    # dedicated "eye" cubes in a later pass). For now, blank.
    glow = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    glow.save(GLOW_OUT)
    print(f"Wrote {GLOW_OUT.name}")


if __name__ == "__main__":
    main()
