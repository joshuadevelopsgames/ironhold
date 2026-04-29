"""
Translates king_enderman.geo.json directly into KingEndermanModel.java.

BB → Java conversion:
  PartPose.offset for root bone    = (pivot_x,       24 - pivot_y, pivot_z)
  PartPose.offset for child bone   = (px - pp_x,    pp_y - py,    pz - pp_z)
  addBox Y: jy = bone_pivot_y - cube_y_origin - cube_h
"""
from __future__ import annotations

import json
from pathlib import Path


GEO = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/geo/king_enderman.geo.json")
JAVA = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/java/kingdom/smp/client/entity/KingEndermanModel.java")

BONE_ORDER = ["body", "right_leg", "left_leg", "head", "right_arm", "left_arm"]
BONE_PARENT = {
    "body": None,
    "head": "body",
    "right_arm": "body",
    "left_arm": "body",
    "right_leg": None,
    "left_leg": None,
}
BONE_FIELD = {
    "body": "body",
    "head": "head",
    "right_arm": "rightArm",
    "left_arm": "leftArm",
    "right_leg": "rightLeg",
    "left_leg": "leftLeg",
}


def fmt_num(x: float) -> str:
    if x == int(x):
        return f"{int(x)}F"
    return f"{x:g}F"


def main():
    data = json.loads(GEO.read_text())
    geo = data["minecraft:geometry"][0]
    desc = geo["description"]
    tex_w = int(desc["texture_width"])
    tex_h = int(desc["texture_height"])

    bones = {b["name"]: b for b in geo["bones"]}
    pivots = {name: b["pivot"] for name, b in bones.items()}

    # Compute Java PartPose offsets.
    java_offsets = {}
    for name in bones:
        p = pivots[name]
        parent = BONE_PARENT[name]
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
        "/** AUTO-GENERATED from king_enderman.geo.json via geo_to_java.py.",
        " *  Edit the .geo.json (or open in Blockbench) and regenerate. */",
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

    declared = {}
    for name in BONE_ORDER:
        if name not in bones:
            continue
        bone = bones[name]
        p = pivots[name]
        ox, oy, oz = java_offsets[name]
        parent = BONE_PARENT[name]
        parent_var = declared[parent] if parent else "root"
        var = name

        lines.append(f"        // ── {name} (pivot BB [{p[0]:g}, {p[1]:g}, {p[2]:g}]) ──")
        lines.append(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{name}\",")
        lines.append("            CubeListBuilder.create()")

        cube_lines = []
        for c in bone.get("cubes", []):
            ox_c, oy_c, oz_c = c["origin"]
            w, h, d = c["size"]
            u, v = c.get("uv", [0, 0])
            jx = ox_c - p[0]
            jy = p[1] - oy_c - h
            jz = oz_c - p[2]
            cube_lines.append(
                f"                .texOffs({u}, {v}).addBox("
                f"{fmt_num(jx)}, {fmt_num(jy)}, {fmt_num(jz)}, "
                f"{fmt_num(w)}, {fmt_num(h)}, {fmt_num(d)}, NONE)"
            )

        # Join cubes; last one gets the ", " + PartPose.offset line.
        if cube_lines:
            lines.extend(cube_lines[:-1])
            lines.append(cube_lines[-1] + ",")
        lines.append(
            f"            PartPose.offset({fmt_num(ox)}, {fmt_num(oy)}, {fmt_num(oz)}));"
        )
        lines.append("")
        declared[name] = var

    lines.extend([
        f"        return LayerDefinition.create(mesh, {tex_w}, {tex_h});",
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
        "        this.body.y = (24F - 48F) + Mth.sin(t * 0.05F) * 0.4F;",
        "",
        "        float stride = Mth.cos(walkPos * 0.32F) * 0.85F * walkSpeed;",
        "        this.rightLeg.xRot = stride;",
        "        this.leftLeg.xRot  = -stride;",
        "        this.rightArm.xRot = -stride * 0.55F + Mth.sin(t * 0.04F) * 0.04F;",
        "        this.leftArm.xRot  =  stride * 0.55F + Mth.sin(t * 0.04F + 2F) * 0.04F;",
        "        this.rightArm.zRot =  0.10F;",
        "        this.leftArm.zRot  = -0.10F;",
        "",
        "        if (state.isEnraged) {",
        "            this.body.xRot = 0.14F;",
        "            this.rightArm.zRot += 0.18F + Mth.sin(t * 0.15F) * 0.05F;",
        "            this.leftArm.zRot  -= 0.18F + Mth.sin(t * 0.15F) * 0.05F;",
        "            this.head.xRot -= 0.08F;",
        "        } else {",
        "            this.body.xRot = 0.0F;",
        "        }",
        "    }",
        "}",
        "",
    ])
    JAVA.write_text("\n".join(lines))
    print(f"wrote {JAVA.name}")


if __name__ == "__main__":
    main()
