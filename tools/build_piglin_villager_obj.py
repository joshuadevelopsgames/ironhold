"""Emit a Wavefront OBJ of PiglinVillagerModel in its REST pose.

Open the OBJ in Blender to inspect proportions and slide cubes around.
The output isn't meant to round-trip back into Minecraft as-is (OBJ is a
mesh, MC entities want cubes) — use it as a visual reference when you
iterate in Blockbench against the .geo.json.

Conventions:
  - Y axis is up (Blender / standard 3D), not Minecraft's flipped axis.
  - One cube per box, six quad faces, with box-uv unwrapped to match the
    .geo.json so the same texture lines up if you assign one in Blender.
  - Ear tilt and folded-arm tilt are baked into the OBJ so the figure
    appears in its correct rest pose, not splayed.

Output:
  art/blockbench/piglin_villager/piglin_villager.obj
  art/blockbench/piglin_villager/piglin_villager.mtl  (points at piglin_villager_template.png)
"""
import math
import os

OUT_DIR = "/Users/joshua/Kingdom SMP/ironhold/art/blockbench/piglin_villager"
os.makedirs(OUT_DIR, exist_ok=True)

TEX_W = TEX_H = 64

EAR_TILT = math.radians(30.0)
ARM_TILT_X = -0.75   # radians; pose tilt of the folded arms

# ---------- helpers ---------------------------------------------------------

def rotate_x(p, ang):
    x, y, z = p
    c, s = math.cos(ang), math.sin(ang)
    return (x, y * c - z * s, y * s + z * c)


def rotate_z(p, ang):
    x, y, z = p
    c, s = math.cos(ang), math.sin(ang)
    return (x * c - y * s, x * s + y * c, z)


def add(p, q):
    return (p[0] + q[0], p[1] + q[1], p[2] + q[2])


def sub(p, q):
    return (p[0] - q[0], p[1] - q[1], p[2] - q[2])


# ---------- cube + uv -------------------------------------------------------

def cube_corners(origin, size):
    """8 corners of an axis-aligned cube at `origin` with extents `size`."""
    x, y, z = origin
    w, h, d = size
    return [
        (x,     y,     z    ),  # 0  -X -Y -Z
        (x + w, y,     z    ),  # 1  +X -Y -Z
        (x + w, y + h, z    ),  # 2  +X +Y -Z
        (x,     y + h, z    ),  # 3  -X +Y -Z
        (x,     y,     z + d),  # 4  -X -Y +Z
        (x + w, y,     z + d),  # 5  +X -Y +Z
        (x + w, y + h, z + d),  # 6  +X +Y +Z
        (x,     y + h, z + d),  # 7  -X +Y +Z
    ]


def box_uv(uv_origin, size, mirror=False):
    """Standard MC cube unwrap → 6 face UV rectangles in pixels.
    Returns a dict: name -> [(u,v), (u,v), (u,v), (u,v)] in CCW order
    matching the corresponding face's vertex order in `cube_corners`.
    """
    u, v = uv_origin
    w, h, d = size

    def rect(ux, uy, uw, uh, flip=False):
        # OBJ uv: 0,0 is bottom-left, so flip v.
        u0 = ux / TEX_W
        u1 = (ux + uw) / TEX_W
        v0 = 1 - (uy + uh) / TEX_H
        v1 = 1 - uy / TEX_H
        if flip:
            return [(u1, v0), (u0, v0), (u0, v1), (u1, v1)]
        return [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]

    return {
        "front":  rect(u + d,             v + d,     w, h, flip=mirror),
        "back":   rect(u + d + w + d,     v + d,     w, h, flip=not mirror),
        "left":   rect(u + d + w,         v + d,     d, h, flip=mirror),
        "right":  rect(u,                 v + d,     d, h, flip=not mirror),
        "top":    rect(u + d,             v,         w, d),
        "bottom": rect(u + d + w,         v,         w, d, flip=True),
    }


# Faces: vertex indices into cube_corners (CCW from outside)
FACE_VERTS = {
    "front":  [4, 5, 6, 7],   # +Z
    "back":   [1, 0, 3, 2],   # -Z
    "left":   [5, 1, 2, 6],   # +X
    "right":  [0, 4, 7, 3],   # -X
    "top":    [3, 7, 6, 2],   # +Y
    "bottom": [4, 0, 1, 5],   # -Y
}


# ---------- bone definitions (cube origin/size + UV in pixels) --------------

bones = [
    # head
    {"cubes": [(("head"), [-4, 24, -4], [8, 8, 8], (0, 0), False)],
     "pivot": (0, 24, 0), "rot": (0, 0, 0)},
    # left ear (rotated -30° around Z about its pivot)
    {"cubes": [(("left_ear"), [4.5, 25, -2], [1, 5, 4], (51, 6), False)],
     "pivot": (4.5, 30, 0), "rot": (0, 0, -EAR_TILT)},
    # right ear (rotated +30° around Z, mirrored UV)
    {"cubes": [(("right_ear"), [-5.5, 25, -2], [1, 5, 4], (39, 6), True)],
     "pivot": (-4.5, 30, 0), "rot": (0, 0, EAR_TILT)},
    # body
    {"cubes": [(("body"), [-4, 12, -2], [8, 12, 4], (16, 16), False)],
     "pivot": (0, 24, 0), "rot": (0, 0, 0)},
    # arms (three cubes, all rotated -42.97° around X about the arms pivot)
    {"cubes": [
        (("arm_l"), [-8, 15, -3], [4, 8, 4], (44, 22), False),
        (("arm_r"), [4,  15, -3], [4, 8, 4], (44, 22), True),
        (("arm_x"), [-4, 15, -3], [8, 4, 4], (40, 38), False),
     ],
     "pivot": (0, 21, -1), "rot": (ARM_TILT_X, 0, 0)},
    # legs
    {"cubes": [(("right_leg"), [-4, 0, -2], [4, 12, 4], (0, 16), False)],
     "pivot": (-2, 12, 0), "rot": (0, 0, 0)},
    {"cubes": [(("left_leg"), [0, 0, -2], [4, 12, 4], (0, 16), True)],
     "pivot": (2, 12, 0), "rot": (0, 0, 0)},
]

# ---------- emit OBJ --------------------------------------------------------

verts = []
uvs = []
faces = []  # (name, [(v_idx, vt_idx), ...])

def add_vert(p):
    verts.append(p)
    return len(verts)

def add_uv(uv):
    uvs.append(uv)
    return len(uvs)

for bone in bones:
    pivot = bone["pivot"]
    rx, ry, rz = bone["rot"]
    for cube_def in bone["cubes"]:
        name, origin, size, uv_origin, mirror = cube_def
        corners = cube_corners(origin, size)
        # apply rotation about pivot
        if rx or ry or rz:
            posed = []
            for c in corners:
                p = sub(c, pivot)
                if rx: p = rotate_x(p, rx)
                if rz: p = rotate_z(p, rz)
                posed.append(add(p, pivot))
            corners = posed
        v_indices = [add_vert(c) for c in corners]
        face_uvs = box_uv(uv_origin, size, mirror=mirror)
        for face_name, face_vert_idxs in FACE_VERTS.items():
            uv_quad = face_uvs[face_name]
            vt_indices = [add_uv(uv) for uv in uv_quad]
            face_pairs = list(zip(
                [v_indices[i] for i in face_vert_idxs],
                vt_indices
            ))
            faces.append((f"{name}_{face_name}", face_pairs))

obj_path = os.path.join(OUT_DIR, "piglin_villager.obj")
mtl_path = os.path.join(OUT_DIR, "piglin_villager.mtl")

with open(obj_path, "w") as f:
    f.write("# PiglinVillagerModel — REST POSE — units in MC pixels (1/16 of a block)\n")
    f.write("mtllib piglin_villager.mtl\n")
    f.write("o piglin_villager\n")
    for x, y, z in verts:
        f.write(f"v {x:.4f} {y:.4f} {z:.4f}\n")
    for u, v in uvs:
        f.write(f"vt {u:.6f} {v:.6f}\n")
    f.write("usemtl piglin_villager\n")
    for name, pairs in faces:
        f.write("f " + " ".join(f"{vi}/{ti}" for vi, ti in pairs) + "\n")

with open(mtl_path, "w") as f:
    f.write("newmtl piglin_villager\n")
    f.write("Ka 1 1 1\nKd 1 1 1\nKs 0 0 0\nd 1\nillum 1\n")
    f.write("map_Kd piglin_villager_template.png\n")

print(f"OK  {obj_path}  ({len(verts)} verts, {len(faces)} faces)")
print(f"OK  {mtl_path}")
print()
print("Next: open the .obj in Blender. The model appears in rest pose;")
print("Blender 'Y up' is what we emit, so no axis correction needed at import.")
