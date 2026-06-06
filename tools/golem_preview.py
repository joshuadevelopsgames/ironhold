#!/usr/bin/env python3
"""Offline previewer for the stone golem GeckoLib rig + animations.

Replicates GeckoLib's bone math (hierarchy, rest+anim Euler rotation in Z*Y*X order about each
bone's pivot, per-cube rotation, position/scale channels) so attack poses can be inspected and tuned
without launching Minecraft. Renders shaded boxes from a fixed 3/4 camera to a contact sheet.

Usage:
  python3 tools/golem_preview.py <animation> t0 t1 t2 ...   # e.g. slam 0 0.35 0.5 0.65 0.85
  python3 tools/golem_preview.py rest                        # bind/rest pose (validation)
"""
import json, math, sys
import numpy as np
from PIL import Image, ImageDraw

GEO = "src/main/resources/assets/ironhold/geckolib/models/entity/stone_golem.geo.json"
ANI = "src/main/resources/assets/ironhold/geckolib/animations/entity/stone_golem.animation.json"

def Rx(d):
    r = math.radians(d); c, s = math.cos(r), math.sin(r)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
def Ry(d):
    r = math.radians(d); c, s = math.cos(r), math.sin(r)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
def Rz(d):
    r = math.radians(d); c, s = math.cos(r), math.sin(r)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])
def euler_zyx(rx, ry, rz):
    return Rz(rz) @ Ry(ry) @ Rx(rx)

def affine(R=np.eye(3), t=(0, 0, 0)):
    M = np.eye(4); M[:3, :3] = R; M[:3, 3] = t
    return M
def T(t):
    return affine(t=t)

geo = json.load(open(GEO))["minecraft:geometry"][0]
bones = {b["name"]: b for b in geo["bones"]}

# Hammer geo (rendered at weapon_mount) + the baked grip transform (block units -> *16 to pixels).
HAMMER_GEO = "src/main/resources/assets/ironhold/geckolib/models/item/battle_hammer.geo.json"
GRIP_POS = np.array([0.95, 0.30, -1.05]) * 16.0   # blocks -> pixels
GRIP_ROT = (-50.0, -85.0, -30.0)
GRIP_SCALE = 1.60
WEAPON_BONES = {"right_arm", "right_forearm", "weapon_mount"}

def sample(channel, t):
    """Linear interp of a {time: [x,y,z]} keyframe map at time t."""
    if channel is None:
        return None
    keys = sorted((float(k), v) for k, v in channel.items())
    if t <= keys[0][0]:
        return np.array(keys[0][1], float)
    if t >= keys[-1][0]:
        return np.array(keys[-1][1], float)
    for i in range(len(keys) - 1):
        t0, v0 = keys[i]; t1, v1 = keys[i + 1]
        if t0 <= t <= t1:
            f = (t - t0) / (t1 - t0) if t1 > t0 else 0
            return np.array(v0, float) * (1 - f) + np.array(v1, float) * f
    return np.array(keys[-1][1], float)

def bone_local(b, anim_bones, t):
    """Local affine for a bone: T(animPos) @ T(P) @ R @ S @ T(-P)."""
    P = np.array(b.get("pivot", [0, 0, 0]), float)
    rest = np.array(b.get("rotation", [0, 0, 0]), float)
    ab = anim_bones.get(b["name"], {}) if anim_bones else {}
    arot = sample(ab.get("rotation"), t)
    apos = sample(ab.get("position"), t)
    ascl = sample(ab.get("scale"), t)
    rot = rest + (arot if arot is not None else 0)
    R = euler_zyx(*rot)
    S = np.diag(ascl) if ascl is not None else np.eye(3)
    RS = R @ S
    M = T(apos if apos is not None else [0, 0, 0]) @ T(P) @ affine(RS) @ T(-P)
    return M

def world_matrices(anim_bones, t):
    world = {}
    def visit(name, parentM):
        b = bones[name]
        M = parentM @ bone_local(b, anim_bones, t)
        world[name] = M
        for child in bones:
            if bones[child].get("parent") == name:
                visit(child, M)
    # roots first
    for name, b in bones.items():
        if b.get("parent") is None:
            visit(name, np.eye(4))
    return world

def cube_faces(cube, M):
    o = np.array(cube["origin"], float); s = np.array(cube["size"], float)
    corners = []
    for dx in (0, s[0]):
        for dy in (0, s[1]):
            for dz in (0, s[2]):
                corners.append(o + [dx, dy, dz])
    corners = np.array(corners)
    if "rotation" in cube:  # per-cube rotation about its own pivot
        cp = np.array(cube.get("pivot", o), float)
        Rc = euler_zyx(*cube["rotation"])
        corners = (Rc @ (corners - cp).T).T + cp
    # to homogeneous, apply bone world matrix
    hom = np.c_[corners, np.ones(8)]
    wc = (M @ hom.T).T[:, :3]
    # index corners by (dx,dy,dz) order above: 0=000 1=001 2=010 3=011 4=100 5=101 6=110 7=111
    idx = {(0,0,0):0,(0,0,1):1,(0,1,0):2,(0,1,1):3,(1,0,0):4,(1,0,1):5,(1,1,0):6,(1,1,1):7}
    faces_def = [
        [(0,0,0),(0,1,0),(0,1,1),(0,0,1)],  # -X
        [(1,0,0),(1,1,0),(1,1,1),(1,0,1)],  # +X
        [(0,0,0),(1,0,0),(1,0,1),(0,0,1)],  # -Y
        [(0,1,0),(1,1,0),(1,1,1),(0,1,1)],  # +Y
        [(0,0,0),(1,0,0),(1,1,0),(0,1,0)],  # -Z
        [(0,0,1),(1,0,1),(1,1,1),(0,1,1)],  # +Z
    ]
    return [np.array([wc[idx[c]] for c in f]) for f in faces_def]

# ---- hammer (static) cubes in hammer-model pixel space ----
def load_hammer_faces():
    hg = json.load(open(HAMMER_GEO))["minecraft:geometry"][0]
    hb = {b["name"]: b for b in hg["bones"]}
    hw = {}
    def visit(name, parentM):
        b = hb[name]
        hw[name] = parentM @ bone_local(b, None, 0)
        for c in hb:
            if hb[c].get("parent") == name:
                visit(c, hw[name])
    for name, b in hb.items():
        if b.get("parent") is None:
            visit(name, np.eye(4))
    faces = []
    for name, b in hb.items():
        for cube in b.get("cubes", []):
            faces += cube_faces(cube, hw[name])
    return faces  # list of (4,3) arrays in hammer-model space
HAMMER_FACES = load_hammer_faces()

def grip_matrix():
    R = euler_zyx(*GRIP_ROT)
    return T(GRIP_POS) @ affine(R * GRIP_SCALE)

LIGHT = np.array([-0.5, 0.85, 0.45]); LIGHT = LIGHT / np.linalg.norm(LIGHT)
CENTER = np.array([0, 34, -4])   # model centre (geo space) for framing

def quads(anim_bones, t):
    """All visible quads as (4x3 world verts, base_rgb)."""
    world = world_matrices(anim_bones, t)
    out = []
    for name, b in bones.items():
        rgb = np.array([0.82, 0.40, 0.36]) if name in WEAPON_BONES else np.array([0.74, 0.74, 0.77])
        for cube in b.get("cubes", []):
            for face in cube_faces(cube, world[name]):
                out.append((face, rgb))
    Hm = world["weapon_mount"] @ grip_matrix()
    for face in HAMMER_FACES:
        hom = np.c_[face, np.ones(4)]
        out.append(((Hm @ hom.T).T[:, :3], np.array([0.26, 0.26, 0.30])))
    return out

def view_R(azim, elev):
    a, e = math.radians(azim), math.radians(elev)
    Ry = np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]])
    Rx = np.array([[1, 0, 0], [0, math.cos(e), -math.sin(e)], [0, math.sin(e), math.cos(e)]])
    return Rx @ Ry

def raster_view(qs, azim, elev, W, H, scale):
    """Orthographic z-buffer rasteriser. Returns an (H,W,3) uint8 image."""
    R = view_R(azim, elev)
    img = np.full((H, W, 3), 24, np.uint8)
    zb = np.full((H, W), -1e18)
    cx, cy = W / 2, H * 0.56
    for face, rgb in qs:
        wn = np.cross(face[1] - face[0], face[2] - face[0]); ln = np.linalg.norm(wn)
        sh = 0.45 + 0.55 * max(0.0, float(np.dot(wn / ln, LIGHT))) if ln > 0 else 0.5
        col = np.clip(rgb * sh, 0, 1) * 255
        v = (R @ (face - CENTER).T).T            # view space
        sx = cx + v[:, 0] * scale
        sy = cy - v[:, 1] * scale
        dep = v[:, 2]
        for tri in ((0, 1, 2), (0, 2, 3)):
            p = np.array([[sx[i], sy[i]] for i in tri])
            d = np.array([dep[i] for i in tri])
            _fill_tri(img, zb, p, d, col)
    return img

def _fill_tri(img, zb, p, d, col):
    minx = max(int(np.floor(p[:, 0].min())), 0); maxx = min(int(np.ceil(p[:, 0].max())), img.shape[1] - 1)
    miny = max(int(np.floor(p[:, 1].min())), 0); maxy = min(int(np.ceil(p[:, 1].max())), img.shape[0] - 1)
    if maxx < minx or maxy < miny:
        return
    x0, y0 = p[0]; x1, y1 = p[1]; x2, y2 = p[2]
    denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
    if abs(denom) < 1e-9:
        return
    ys, xs = np.mgrid[miny:maxy + 1, minx:maxx + 1]
    xc = xs + 0.5; yc = ys + 0.5
    a = ((y1 - y2) * (xc - x2) + (x2 - x1) * (yc - y2)) / denom
    b = ((y2 - y0) * (xc - x2) + (x0 - x2) * (yc - y2)) / denom
    c = 1 - a - b
    inside = (a >= 0) & (b >= 0) & (c >= 0)
    depth = a * d[0] + b * d[1] + c * d[2]
    sub = zb[miny:maxy + 1, minx:maxx + 1]
    win = inside & (depth > sub)
    sub[win] = depth[win]
    img[miny:maxy + 1, minx:maxx + 1][win] = col.astype(np.uint8)

def render(anim_bones, times, title, out):
    W, H, scale = 240, 320, 3.0
    views = [("3/4", -34, -12), ("side", -90, -8)]
    pad = 6
    sheet = Image.new("RGB", (len(times) * (W + pad) + pad, len(views) * (H + pad) + 28), (16, 16, 16))
    dr = ImageDraw.Draw(sheet)
    dr.text((6, 4), title, fill=(230, 230, 120))
    for r, (vname, az, el) in enumerate(views):
        for ci, t in enumerate(times):
            qs = quads(anim_bones, t)
            arr = raster_view(qs, az, el, W, H, scale)
            x = pad + ci * (W + pad); y = 24 + r * (H + pad)
            sheet.paste(Image.fromarray(arr), (x, y))
            dr.text((x + 2, y + 2), f"{vname}  t={t}", fill=(200, 200, 200))
    sheet.save(out)
    print("wrote", out)

if __name__ == "__main__":
    name = sys.argv[1] if len(sys.argv) > 1 else "rest"
    if name == "rest":
        render(None, [0.0], "REST POSE (validation)", "tmp/golem_rest.png")
    elif name == "pose":
        # ad-hoc static pose: bone=rx,ry,rz ...  (added to rest); renders one frame
        ab = {}
        label = []
        for arg in sys.argv[2:]:
            bn, vals = arg.split("=")
            xyz = [float(v) for v in vals.split(",")]
            ab[bn] = {"rotation": {"0.0": xyz}}
            label.append(f"{bn}={vals}")
        render(ab, [0.0], "POSE  " + "  ".join(label), "tmp/golem_pose.png")
    else:
        anims = json.load(open(ANI))["animations"]
        ab = anims[name]["bones"]
        times = [float(x) for x in sys.argv[2:]] or [0.0]
        render(ab, times, f"animation: {name}", f"tmp/golem_{name}.png")
