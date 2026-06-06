#!/usr/bin/env python3
"""
Generate the Plague Doctor (Doctor Corvus) humanoid entity model + texture.

Single source of truth: the BONES list below, authored in Minecraft entity-model
space (y-down; ground at y=24; head extends -Y from an upper-chest pivot, the
ShulkerHerderModel / vanilla IllagerModel convention).

Emits three artifacts from the same spec so they can never drift:
  1. textures/entity/plague_doctor.png          (box-UV atlas, auto-packed)
  2. client/entity/PlagueDoctorModel.java        (NeoForge EntityModel)
  3. scratch/plague_doctor.geo.json              (Bedrock geo, for Blockbench)

Run:  python3 scripts/gen_plague_doctor.py
"""
import os, json
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_PATH  = os.path.join(ROOT, "src/main/resources/assets/ironhold/textures/entity/plague_doctor.png")
JAVA_PATH = os.path.join(ROOT, "src/main/java/kingdom/smp/client/entity/PlagueDoctorModel.java")
GEO_PATH  = os.path.join(ROOT, "scratch/plague_doctor.geo.json")

# ── Palette ────────────────────────────────────────────────────────────────
HOOD     = (38, 35, 42)     # black hood / robe
ROBE     = (32, 30, 36)     # slightly darker robe body
MANTLE   = (28, 26, 32)     # shoulder cape (darkest)
MASK     = (208, 202, 190)  # pale bird-mask face
BEAK     = (198, 192, 178)  # beak (a touch warmer)
HAT      = (24, 22, 27)     # wide-brim hat
LEATHER  = (98, 66, 40)     # belt / cuffs straps
LEATHER_D= (74, 50, 32)     # boots (darker)
SATCH    = (88, 60, 38)     # satchel
EYE      = (104, 198, 116)  # glowing green eyes
GLASS_G  = (88, 172, 108)   # green tonic
GLASS_P  = (152, 94, 184)   # purple tonic
CORK     = (122, 88, 54)
GOLD     = (198, 152, 62)   # buckle
OFFWHITE = (202, 198, 184)

# Per-face shading multipliers (top brightest, bottom darkest).
SHADE = {"up": 1.16, "down": 0.62, "north": 1.0, "south": 0.82, "east": 0.88, "west": 0.93}

def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)

# ── Model spec ──────────────────────────────────────────────────────────────
# Each bone: name, parent, pivot (absolute entity coords), and cubes.
# Each cube: id, off (bone-local min corner), size (W,H,D ints), color, inflate, deco.
BONES = [
    dict(name="head", parent=None, pivot=(0, 0, 0), cubes=[
        dict(id="head",      off=(-4, -8, -4), size=(8, 8, 8),   color=HOOD,
             faces={"north": MASK}, deco="eyes"),
        dict(id="beak",      off=(-1, -4, -8), size=(2, 3, 4),   color=BEAK),
        dict(id="hat_brim",  off=(-7, -9, -7), size=(14, 1, 14), color=HAT),
        dict(id="hat_crown", off=(-4.5, -13, -4.5), size=(9, 4, 9), color=HAT, deco="hatband"),
    ]),
    dict(name="body", parent=None, pivot=(0, 0, 0), cubes=[
        dict(id="body",      off=(-4, 0, -2),    size=(8, 12, 4), color=ROBE),
        dict(id="mantle",    off=(-5.5, 0, -3.5), size=(11, 6, 7), color=MANTLE, inflate=0.0),
        dict(id="belt",      off=(-4.5, 8.5, -2.5), size=(9, 2, 5), color=LEATHER, deco="buckle"),
        dict(id="bottle_g",  off=(2.5, 9.5, -3.0), size=(1, 3, 1), color=GLASS_G, deco="cork"),
        dict(id="bottle_p",  off=(-3.5, 9.5, -3.0), size=(1, 3, 1), color=GLASS_P, deco="cork"),
        dict(id="satchel",   off=(3.0, 9.0, -1.0), size=(2, 4, 3), color=SATCH, deco="cross"),
    ]),
    dict(name="right_arm", parent=None, pivot=(-5, 2, 0), cubes=[
        dict(id="r_sleeve", off=(-3, -2, -2), size=(4, 12, 4), color=HOOD),
        dict(id="r_cuff",   off=(-3, 8, -2),  size=(4, 2, 4),  color=LEATHER, inflate=0.35),
    ]),
    dict(name="left_arm", parent=None, pivot=(5, 2, 0), cubes=[
        dict(id="l_sleeve", off=(-1, -2, -2), size=(4, 12, 4), color=HOOD),
        dict(id="l_cuff",   off=(-1, 8, -2),  size=(4, 2, 4),  color=LEATHER, inflate=0.35),
    ]),
    dict(name="right_leg", parent=None, pivot=(-2, 12, 0), cubes=[
        dict(id="r_leg",  off=(-2, 0, -2), size=(4, 12, 4), color=ROBE),
        dict(id="r_boot", off=(-2, 9, -3), size=(4, 3, 5),  color=LEATHER_D),
    ]),
    dict(name="left_leg", parent=None, pivot=(2, 12, 0), cubes=[
        dict(id="l_leg",  off=(-2, 0, -2), size=(4, 12, 4), color=ROBE),
        dict(id="l_boot", off=(-2, 9, -3), size=(4, 3, 5),  color=LEATHER_D),
    ]),
]

# ── UV packing (shelf packer, 1px padding) ───────────────────────────────────
ATLAS_W = 128
def footprint(size):
    w, h, d = size
    return (2 * (w + d), d + h)

def pack():
    shelf_y, shelf_h, cx, maxh = 0, 0, 0, 0
    for bone in BONES:
        for cube in bone["cubes"]:
            fw, fh = footprint(cube["size"])
            if cx + fw > ATLAS_W:
                shelf_y += shelf_h + 1
                cx, shelf_h = 0, 0
            cube["uv"] = (cx, shelf_y)
            cx += fw + 1
            shelf_h = max(shelf_h, fh)
        # bookkeeping for final height handled below
    total_h = shelf_y + shelf_h
    return total_h

def pow2(n):
    p = 16
    while p < n:
        p *= 2
    return p

# ── Texture painter ───────────────────────────────────────────────────────────
def rect(px, x, y, w, h, color):
    for j in range(int(h)):
        for i in range(int(w)):
            px[x + i, y + j] = color + (255,)

def face_rects(u, v, w, h, d):
    """Return dict face -> (x, y, w, h) in standard MC box-uv unwrap."""
    return {
        "up":    (u + d,         v,     w, d),
        "down":  (u + d + w,     v,     w, d),
        "east":  (u,             v + d, d, h),
        "north": (u + d,         v + d, w, h),
        "west":  (u + d + w,     v + d, d, h),
        "south": (u + 2 * d + w, v + d, w, h),
    }

def paint_cube(px, cube):
    u, v = cube["uv"]
    w, h, d = cube["size"]
    base = cube["color"]
    overrides = cube.get("faces", {})
    rects = face_rects(u, v, w, h, d)
    for face, (rx, ry, rw, rh) in rects.items():
        col = overrides.get(face, base)
        rect(px, rx, ry, rw, rh, shade(col, SHADE[face]))
    # decorations operate on the front (north) face unless noted
    deco = cube.get("deco")
    nx, ny, nw, nh = rects["north"]
    if deco == "eyes":
        # two glowing slits in the upper face, above the beak
        ey = ny + 2
        rect(px, nx + 1, ey, 2, 1, EYE)
        rect(px, nx + nw - 3, ey, 2, 1, EYE)
    elif deco == "hatband":
        # brown band across all four side faces, bottom row of the crown
        band_y = v + d + (h - 1)
        rect(px, u, band_y, 2 * (w + d), 1, shade(LEATHER, 0.95))
    elif deco == "buckle":
        bx = nx + nw // 2 - 1
        rect(px, bx, ny, 2, rh if rh <= 2 else 2, GOLD)
    elif deco == "cork":
        for face, (rx, ry, rw, rh) in rects.items():
            rect(px, rx, ry, rw, 1, CORK)  # top row of every face = cork
    elif deco == "cross":
        cx0 = nx + nw // 2
        rect(px, cx0, ny + 1, 1, nh - 2, OFFWHITE)
        rect(px, nx + 1, ny + nh // 2, nw - 2, 1, OFFWHITE)

# ── Bedrock geo.json ──────────────────────────────────────────────────────────
def emit_geo(atlas_w, atlas_h):
    bones = []
    for bone in BONES:
        px_, py_, pz_ = bone["pivot"]
        gb = {"name": bone["name"], "pivot": [px_, 24 - py_, pz_], "cubes": []}
        if bone["parent"]:
            gb["parent"] = bone["parent"]
        for cube in bone["cubes"]:
            ox, oy, oz = cube["off"]
            w, h, dd = cube["size"]
            ax, ay, az = px_ + ox, py_ + oy, pz_ + oz   # absolute entity coords
            origin = [ax, 24 - (ay + h), az]            # bedrock min corner (y up)
            c = {"origin": origin, "size": [w, h, dd], "uv": list(cube["uv"])}
            if cube.get("inflate"):
                c["inflate"] = cube["inflate"]
            gb["cubes"].append(c)
        bones.append(gb)
    geo = {
        "format_version": "1.16.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.ironhold:plague_doctor",
                "texture_width": atlas_w, "texture_height": atlas_h,
                "visible_bounds_width": 4, "visible_bounds_height": 5,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": bones,
        }],
    }
    os.makedirs(os.path.dirname(GEO_PATH), exist_ok=True)
    with open(GEO_PATH, "w") as f:
        json.dump(geo, f, indent=2)

# ── Java emitter ───────────────────────────────────────────────────────────────
def fnum(x):
    return f"{float(x):.1f}F"

def emit_java(atlas_w, atlas_h):
    lines = []
    P = lines.append
    P("package kingdom.smp.client.entity;")
    P("")
    P("import kingdom.smp.Ironhold;")
    P("import net.minecraft.client.model.EntityModel;")
    P("import net.minecraft.client.model.geom.ModelLayerLocation;")
    P("import net.minecraft.client.model.geom.ModelPart;")
    P("import net.minecraft.client.model.geom.PartPose;")
    P("import net.minecraft.client.model.geom.builders.CubeDeformation;")
    P("import net.minecraft.client.model.geom.builders.CubeListBuilder;")
    P("import net.minecraft.client.model.geom.builders.LayerDefinition;")
    P("import net.minecraft.client.model.geom.builders.MeshDefinition;")
    P("import net.minecraft.client.model.geom.builders.PartDefinition;")
    P("import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;")
    P("import net.minecraft.resources.Identifier;")
    P("import net.minecraft.util.Mth;")
    P("")
    P("/**")
    P(" * Doctor Corvus — wide-brim-hatted, beaked plague doctor (humanoid).")
    P(" * GENERATED by scripts/gen_plague_doctor.py — edit the spec there, not here.")
    P(" * Texture %dx%d, box-uv. Pivots follow the IllagerModel convention" % (atlas_w, atlas_h))
    P(" * (head/body share pivot at upper chest; head extends -Y).")
    P(" */")
    P("public class PlagueDoctorModel extends EntityModel<LivingEntityRenderState> {")
    P("")
    P("    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(")
    P("        Identifier.fromNamespaceAndPath(Ironhold.MODID, \"plague_doctor\"), \"main\");")
    P("")
    for bone in BONES:
        P("    private final ModelPart %s;" % bone["name"])
    P("")
    P("    public PlagueDoctorModel(ModelPart root) {")
    P("        super(root);")
    for bone in BONES:
        P("        this.%s = root.getChild(\"%s\");" % (bone["name"], bone["name"]))
    P("    }")
    P("")
    P("    public static LayerDefinition createBodyLayer() {")
    P("        MeshDefinition mesh = new MeshDefinition();")
    P("        PartDefinition root = mesh.getRoot();")
    P("")
    for bone in BONES:
        px_, py_, pz_ = bone["pivot"]
        P("        root.addOrReplaceChild(\"%s\"," % bone["name"])
        P("            CubeListBuilder.create()")
        for ci, cube in enumerate(bone["cubes"]):
            u, v = cube["uv"]
            ox, oy, oz = cube["off"]
            w, h, dd = cube["size"]
            inflate = cube.get("inflate", 0.0)
            deform = "CubeDeformation.NONE" if not inflate else "new CubeDeformation(%sF)" % (f"{inflate:.2f}")
            term = "," if ci < len(bone["cubes"]) - 1 else ","
            P("                .texOffs(%d, %d)" % (u, v))
            P("                .addBox(%s, %s, %s, %d, %d, %d, %s)%s"
              % (fnum(ox), fnum(oy), fnum(oz), w, h, dd, deform,
                 "" if ci < len(bone["cubes"]) - 1 else ","))
        P("            PartPose.offset(%s, %s, %s));" % (fnum(px_), fnum(py_), fnum(pz_)))
        P("")
    P("        return LayerDefinition.create(mesh, %d, %d);" % (atlas_w, atlas_h))
    P("    }")
    P("")
    P("    @Override")
    P("    public void setupAnim(LivingEntityRenderState state) {")
    P("        super.setupAnim(state);")
    P("        float t = state.ageInTicks;")
    P("        float walkSpeed = state.walkAnimationSpeed;")
    P("        float walkPos = state.walkAnimationPos;")
    P("")
    P("        // Head tracks the player.")
    P("        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;")
    P("        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;")
    P("")
    P("        // Four-beat walk gait.")
    P("        float swing = Mth.cos(walkPos * 0.6662F) * 1.2F * walkSpeed;")
    P("        this.right_leg.xRot = swing;")
    P("        this.left_leg.xRot = -swing;")
    P("        this.right_arm.xRot = -swing * 0.6F;")
    P("        this.left_arm.xRot = swing * 0.6F;")
    P("")
    P("        // Slow, ominous idle sway.")
    P("        float sway = Mth.sin(t * 0.045F) * 0.05F;")
    P("        this.right_arm.zRot = 0.08F + sway;")
    P("        this.left_arm.zRot = -0.08F - sway;")
    P("        this.body.yRot = Mth.sin(t * 0.03F) * 0.03F;")
    P("    }")
    P("}")
    with open(JAVA_PATH, "w") as f:
        f.write("\n".join(lines) + "\n")

# ── Run ────────────────────────────────────────────────────────────────────────
def main():
    total_h = pack()
    atlas_h = pow2(total_h)
    atlas_w = ATLAS_W
    img = Image.new("RGBA", (atlas_w, atlas_h), (0, 0, 0, 0))
    px = img.load()
    for bone in BONES:
        for cube in bone["cubes"]:
            paint_cube(px, cube)
    os.makedirs(os.path.dirname(TEX_PATH), exist_ok=True)
    img.save(TEX_PATH)
    emit_java(atlas_w, atlas_h)
    emit_geo(atlas_w, atlas_h)
    print("atlas %dx%d (used height %d)" % (atlas_w, atlas_h, total_h))
    print("texture ->", TEX_PATH)
    print("java    ->", JAVA_PATH)
    print("geo     ->", GEO_PATH)

if __name__ == "__main__":
    main()
