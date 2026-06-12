#!/usr/bin/env python3
"""Import Foreman Dunstan's hand-edited Blockbench model into the mod.

The canonical art source is art/blockbench/miner_dunstan/miner_dunstan.bbmodel
(bedrock-format, box UV, 128x128). This script converts it into:

  - MinerDunstanModel.java   (vanilla entity model, createBodyLayer regenerated)
  - miner_dunstan.png        (texture, with sub-pixel sampling padding — see below)
  - miner_dunstan_glow.png   (emissive map: the yellow lamp-lens pixels only)
  - miner_dunstan.geo.json   (refreshed bedrock geometry for the art pipeline)

Sub-pixel padding: Minecraft's box UV samples exact float face rects, while
Blockbench paints floor'd pixel allocations. Cubes with fractional dimensions
(boots, soles, lamp parts) therefore sample texels just outside the painted
area; any such texel that is transparent would render as a hole, so it is
filled with the nearest opaque pixel.

Run:
    python3 scripts/import_miner_dunstan.py [path/to/edited.bbmodel]

Passing a path (e.g. an export on the Desktop) copies it over the canonical
bbmodel first.
"""

import base64
import json
import math
import os
import shutil
import sys

from PIL import Image


ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART_DIR = os.path.join(ROOT, "art/blockbench/miner_dunstan")
BBMODEL_PATH = os.path.join(ART_DIR, "miner_dunstan.bbmodel")
GEO_PATH = os.path.join(ART_DIR, "miner_dunstan.geo.json")
ART_TEXTURE_PATH = os.path.join(ART_DIR, "miner_dunstan.png")
TEXTURE_PATH = os.path.join(
    ROOT, "src/main/resources/assets/ironhold/textures/entity/miner_dunstan.png"
)
GLOW_PATH = os.path.join(
    ROOT, "src/main/resources/assets/ironhold/textures/entity/miner_dunstan_glow.png"
)
JAVA_PATH = os.path.join(
    ROOT, "src/main/java/kingdom/smp/client/entity/MinerDunstanModel.java"
)

JAVA_BONE_NAMES = {
    "right_arm": "rightArm",
    "left_arm": "leftArm",
    "right_leg": "rightLeg",
    "left_leg": "leftLeg",
}


def collect_cubes(node, elems, groups, bone_name):
    """Cubes under an outliner node, flattening unrotated nested groups.

    Cube coordinates in bedrock-format bbmodels are absolute, so an unrotated
    subgroup (e.g. a helmet group under head) folds losslessly into its root
    bone. A rotated subgroup would need a child ModelPart — unsupported.
    """
    cubes = []
    for child in node.get("children", []):
        if isinstance(child, str):
            if child in elems:
                cubes.append(elems[child])
            continue
        sub = groups.get(child.get("uuid"), child)
        rotation = sub.get("rotation", [0, 0, 0])
        if any(abs(r) > 1e-6 for r in rotation):
            raise SystemExit(
                f"error: nested group '{sub.get('name')}' under bone "
                f"'{bone_name}' has rotation {rotation}; the importer only "
                "flattens unrotated subgroups — child-part emission needed"
            )
        cubes.extend(collect_cubes(child, elems, groups, bone_name))
    return cubes


def load_bones(bb):
    """Return [(group, [cube, ...])] in outliner order."""
    elems = {e["uuid"]: e for e in bb["elements"]}
    groups = {g["uuid"]: g for g in bb["groups"]}
    bones = []
    for node in bb["outliner"]:
        group = groups[node["uuid"]]
        bones.append((group, collect_cubes(node, elems, groups, group["name"])))
    return bones


def face_rects(u, v, w, h, d):
    """Vanilla box-UV face rects (float-exact, as ModelPart.Cube samples)."""
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + 2 * d + w, v + d, w, h),
    }


def cube_dims(cube):
    f, t = cube["from"], cube["to"]
    return tuple(round(t[i] - f[i], 4) for i in range(3))


def nearest_opaque(pixels, size, x, y, max_radius=3):
    width, height = size
    for radius in range(1, max_radius + 1):
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                if max(abs(dx), abs(dy)) != radius:
                    continue
                nx, ny = x + dx, y + dy
                if 0 <= nx < width and 0 <= ny < height and pixels[nx, ny][3] > 0:
                    return pixels[nx, ny]
    return None


def pad_subpixel_sampling(image, bones):
    """Fill transparent texels that float box-UV sampling would hit."""
    pixels = image.load()
    padded = 0
    for _, cubes in bones:
        for cube in cubes:
            w, h, d = cube_dims(cube)
            u, v = cube.get("uv_offset", [0, 0])
            for fx, fy, fw, fh in face_rects(u, v, w, h, d).values():
                if fw <= 0 or fh <= 0:
                    continue
                for yy in range(math.floor(fy), math.ceil(fy + fh)):
                    for xx in range(math.floor(fx), math.ceil(fx + fw)):
                        if not (0 <= xx < image.width and 0 <= yy < image.height):
                            continue
                        if pixels[xx, yy][3] > 0:
                            continue
                        source = nearest_opaque(pixels, image.size, xx, yy)
                        if source:
                            pixels[xx, yy] = source[:3] + (255,)
                            padded += 1
    return padded


def is_lamp_light(rgba):
    r, g, b, a = rgba
    return a > 0 and r >= 200 and g >= 150 and b <= 200


def emit_glow(image, bones):
    """Emissive map: yellow light pixels inside the lamp_lens UV footprint."""
    glow = Image.new("RGBA", image.size, (0, 0, 0, 0))
    src = image.load()
    dst = glow.load()
    copied = 0
    for _, cubes in bones:
        for cube in cubes:
            if cube["name"] != "lamp_lens":
                continue
            w, h, d = cube_dims(cube)
            u, v = cube.get("uv_offset", [0, 0])
            for fx, fy, fw, fh in face_rects(u, v, w, h, d).values():
                if fw <= 0 or fh <= 0:
                    continue
                for yy in range(math.floor(fy), math.ceil(fy + fh)):
                    for xx in range(math.floor(fx), math.ceil(fx + fw)):
                        if 0 <= xx < image.width and 0 <= yy < image.height:
                            if is_lamp_light(src[xx, yy]):
                                dst[xx, yy] = src[xx, yy]
                                copied += 1
    glow.save(GLOW_PATH)
    return copied


def fnum(value):
    text = f"{float(value):.2f}".rstrip("0")
    if text.endswith("."):
        text += "0"
    return text + "F"


def emit_java(bb, bones):
    width = bb["resolution"]["width"]
    height = bb["resolution"]["height"]

    bone_lines = []
    pose_rotations = {}
    for group, cubes in bones:
        name = group["name"]
        java_name = JAVA_BONE_NAMES.get(name, name)
        px, py, pz = group["origin"]
        pivot = (px, 24 - py, pz)
        rx, ry, rz = group.get("rotation", [0, 0, 0])
        # bedrock -> entity model: negate X and Y rotations
        rot = (-math.radians(rx), -math.radians(ry), math.radians(rz))
        if any(abs(r) > 1e-6 for r in rot):
            pose_rotations[name] = rot
            pose = (
                f"PartPose.offsetAndRotation({fnum(pivot[0])}, {fnum(pivot[1])}, "
                f"{fnum(pivot[2])}, {fnum(rot[0])}, {fnum(rot[1])}, {fnum(rot[2])})"
            )
        else:
            pose = f"PartPose.offset({fnum(pivot[0])}, {fnum(pivot[1])}, {fnum(pivot[2])})"

        bone_lines.append(
            f"        PartDefinition {java_name} = root.addOrReplaceChild(\"{name}\","
        )
        bone_lines.append("            CubeListBuilder.create()")
        for cube in cubes:
            w, h, d = cube_dims(cube)
            f = cube["from"]
            ox = f[0] - px
            oy = -(f[1] + h - py)
            oz = f[2] - pz
            u, v = cube.get("uv_offset", [0, 0])
            inflate = cube.get("inflate", 0)
            deformation = (
                "CubeDeformation.NONE"
                if not inflate
                else f"new CubeDeformation({inflate:.2f}F)"
            )
            # Blockbench box UV puts the first d-wide strip on +X, vanilla on
            # -X (Direction.WEST); since we keep bedrock X coordinates instead
            # of negating them, mirror() per cube restores Blockbench's face
            # mapping. A bbmodel mirror_uv flag composes as another inversion.
            mirror = "true" if not cube.get("mirror_uv") else "false"
            bone_lines.append(f"                .texOffs({int(u)}, {int(v)})")
            bone_lines.append(f"                .mirror({mirror})")
            bone_lines.append(
                "                .addBox("
                f"{fnum(ox)}, {fnum(oy)}, {fnum(oz)}, "
                f"{fnum(w)}, {fnum(h)}, {fnum(d)}, {deformation})"
            )
        bone_lines.append(f"            , {pose});")
        bone_lines.append("")

    left_arm_base = fnum(pose_rotations.get("left_arm", (0, 0, 0))[0])

    lines = [
        "package kingdom.smp.client.entity;",
        "",
        "import com.mojang.blaze3d.vertex.PoseStack;",
        "import kingdom.smp.Ironhold;",
        "import net.minecraft.client.model.ArmedModel;",
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
        "import net.minecraft.world.entity.HumanoidArm;",
        "",
        "/**",
        " * Foreman Dunstan's stocky miner model.",
        " * GENERATED by scripts/import_miner_dunstan.py from",
        " * art/blockbench/miner_dunstan/miner_dunstan.bbmodel; edit the bbmodel",
        " * in Blockbench and re-run the importer, do not edit this file.",
        " */",
        "public class MinerDunstanModel extends EntityModel<MinerDunstanRenderState>",
        "    implements ArmedModel<MinerDunstanRenderState> {",
        "",
        "    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(",
        "        Identifier.fromNamespaceAndPath(Ironhold.MODID, \"miner_dunstan\"), \"main\");",
        "",
        "    /** Rest pose of the left arm, baked in Blockbench. */",
        f"    private static final float LEFT_ARM_POSE_X = {left_arm_base};",
        "",
        "    private final ModelPart head;",
        "    private final ModelPart body;",
        "    private final ModelPart rightArm;",
        "    private final ModelPart leftArm;",
        "    private final ModelPart rightLeg;",
        "    private final ModelPart leftLeg;",
        "",
        "    public MinerDunstanModel(ModelPart root) {",
        "        super(root);",
        "        this.head = root.getChild(\"head\");",
        "        this.body = root.getChild(\"body\");",
        "        this.rightArm = root.getChild(\"right_arm\");",
        "        this.leftArm = root.getChild(\"left_arm\");",
        "        this.rightLeg = root.getChild(\"right_leg\");",
        "        this.leftLeg = root.getChild(\"left_leg\");",
        "    }",
        "",
        "    public static LayerDefinition createBodyLayer() {",
        "        MeshDefinition mesh = new MeshDefinition();",
        "        PartDefinition root = mesh.getRoot();",
        "",
    ]
    lines.extend(bone_lines)
    lines.extend(
        [
            f"        return LayerDefinition.create(mesh, {width}, {height});",
            "    }",
            "",
            "    @Override",
            "    public void translateToHand(",
            "        MinerDunstanRenderState state,",
            "        HumanoidArm arm,",
            "        PoseStack poseStack",
            "    ) {",
            "        ModelPart armPart = arm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;",
            "        armPart.translateAndRotate(poseStack);",
            "    }",
            "",
            "    @Override",
            "    public void setupAnim(MinerDunstanRenderState state) {",
            "        super.setupAnim(state);",
            "        float t = state.ageInTicks;",
            "        float walkSpeed = state.walkAnimationSpeed;",
            "        float walkPos = state.walkAnimationPos;",
            "",
            "        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;",
            "        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;",
            "",
            "        float swing = Mth.cos(walkPos * 0.6662F) * 1.2F * walkSpeed;",
            "        this.rightLeg.xRot = swing;",
            "        this.leftLeg.xRot = -swing;",
            "        this.leftArm.xRot = LEFT_ARM_POSE_X + swing * 0.65F;",
            "",
            "        // Keep the tool-bearing arm controlled and weighty.",
            "        this.rightArm.xRot = -0.30F - swing * 0.18F + Mth.sin(t * 0.04F) * 0.025F;",
            "        this.rightArm.yRot = -0.08F;",
            "        this.rightArm.zRot = 0.16F;",
            "        this.leftArm.zRot = -0.06F;",
            "        this.body.zRot = Mth.sin(walkPos * 0.6662F) * 0.025F * walkSpeed;",
            "    }",
            "}",
        ]
    )
    with open(JAVA_PATH, "w", encoding="utf-8") as output:
        output.write("\n".join(lines) + "\n")


def emit_geo(bb, bones):
    geo_bones = []
    for group, cubes in bones:
        geo_bone = {
            "name": group["name"],
            "pivot": list(group["origin"]),
            "cubes": [],
        }
        rotation = group.get("rotation", [0, 0, 0])
        if any(rotation):
            geo_bone["rotation"] = list(rotation)
        for cube in cubes:
            w, h, d = cube_dims(cube)
            geo_cube = {
                "origin": list(cube["from"]),
                "size": [w, h, d],
                "uv": list(cube.get("uv_offset", [0, 0])),
            }
            if cube.get("inflate"):
                geo_cube["inflate"] = cube["inflate"]
            geo_bone["cubes"].append(geo_cube)
        geo_bones.append(geo_bone)

    geometry = {
        "format_version": "1.16.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.ironhold:miner_dunstan",
                    "texture_width": bb["resolution"]["width"],
                    "texture_height": bb["resolution"]["height"],
                    "visible_bounds_width": 3.5,
                    "visible_bounds_height": 3.5,
                    "visible_bounds_offset": [0, 1.3, 0],
                },
                "bones": geo_bones,
            }
        ],
    }
    with open(GEO_PATH, "w", encoding="utf-8") as output:
        json.dump(geometry, output, indent=2)
        output.write("\n")


def main():
    if len(sys.argv) > 1:
        source = os.path.abspath(sys.argv[1])
        if source != BBMODEL_PATH:
            os.makedirs(ART_DIR, exist_ok=True)
            shutil.copyfile(source, BBMODEL_PATH)
            print("bbmodel <-", source)

    with open(BBMODEL_PATH, encoding="utf-8") as bb_file:
        bb = json.load(bb_file)
    bones = load_bones(bb)

    texture_b64 = bb["textures"][0]["source"].split(",", 1)[1]
    with open(ART_TEXTURE_PATH, "wb") as art_texture:
        art_texture.write(base64.b64decode(texture_b64))

    image = Image.open(ART_TEXTURE_PATH).convert("RGBA")
    padded = pad_subpixel_sampling(image, bones)
    image.save(TEXTURE_PATH)
    glow_pixels = emit_glow(image, bones)

    emit_java(bb, bones)
    emit_geo(bb, bones)

    print(f"texture -> {TEXTURE_PATH} ({padded} sub-pixel texels padded)")
    print(f"glow    -> {GLOW_PATH} ({glow_pixels} emissive texels)")
    print(f"java    -> {JAVA_PATH}")
    print(f"geo     -> {GEO_PATH}")


if __name__ == "__main__":
    main()
