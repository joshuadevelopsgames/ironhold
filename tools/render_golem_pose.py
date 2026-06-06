#!/usr/bin/env python3
"""Offline render of the stone golem geo *posed by one of its animations* (no Blockbench).

Forward-kinematics (geo pivots absolute; per-bone rotation about pivot, order Rz*Ry*Rx; animation
rotation is ADDED to the bone's rest rotation and animation position translates the bone — GeckoLib's
additive convention). Renders 3 shaded views + marks the weapon_mount (hammer grip) and draws an
approximate hammer haft so poses like "kneel, head on the hammer" can be checked.

Usage: python3 tools/render_golem_pose.py <anim_name> [time] [out.png]
  e.g. python3 tools/render_golem_pose.py dormant 0 /tmp/golem_pose.png
"""
import sys, json, math
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.collections import PolyCollection

GEO = "src/main/resources/assets/ironhold/geckolib/models/entity/stone_golem.geo.json"
ANIMS = "src/main/resources/assets/ironhold/geckolib/animations/entity/stone_golem.animation.json"
ANIM = sys.argv[1] if len(sys.argv) > 1 else None
TIME = float(sys.argv[2]) if len(sys.argv) > 2 else 0.0
OUT = sys.argv[3] if len(sys.argv) > 3 else "/tmp/golem_pose.png"
# hammer grip tuning (StoneGolemHammerTuning) — pos px, rot deg, scale
H_POS, H_ROT, H_SCALE = (0.95, 0.30, -1.05), (-50.0, -85.0, -30.0), 1.60

bones = json.load(open(GEO))["minecraft:geometry"][0]["bones"]
by = {b["name"]: b for b in bones}


def euler(rx, ry, rz):
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    cx, sx = math.cos(rx), math.sin(rx); cy, sy = math.cos(ry), math.sin(ry); cz, sz = math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def aff(R=None, t=(0, 0, 0)):
    M = np.eye(4)
    if R is not None:
        M[:3, :3] = R
    M[:3, 3] = t
    return M


def num(v):
    if isinstance(v, (int, float)):
        return float(v)
    return 0.0  # molang string → 0 for preview (dormant/static poses are numeric)


def chan(track, t):  # value of a [x,y,z] channel at time t (nearest/first key — poses are static here)
    if not track:
        return [0.0, 0.0, 0.0]
    keys = sorted(track.items(), key=lambda kv: float(kv[0]))
    val = keys[0][1]
    for k, v in keys:
        if float(k) <= t + 1e-6:
            val = v
    vec = val.get("vector", val) if isinstance(val, dict) else val
    return [num(vec[0]), num(vec[1]), num(vec[2])]


POSE = {}
if ANIM:
    a = json.load(open(ANIMS))["animations"][ANIM]["bones"]
    for name, ch in a.items():
        POSE[name] = (chan(ch.get("rotation"), TIME), chan(ch.get("position"), TIME))


_W = {}
def world(name):
    if name in _W:
        return _W[name]
    b = by[name]
    piv = np.array(b.get("pivot", [0, 0, 0]), float)
    base = b.get("rotation", [0, 0, 0])
    arot, apos = POSE.get(name, ([0, 0, 0], [0, 0, 0]))
    R = euler(base[0] + arot[0], base[1] + arot[1], base[2] + arot[2])  # additive (GeckoLib)
    L = aff(t=apos) @ aff(np.eye(3), piv) @ aff(R) @ aff(np.eye(3), -piv)
    p = b.get("parent")
    _W[name] = (world(p) @ L) if (p and p in by) else L
    return _W[name]


def cat(name):
    n = name.lower()
    if "head" in n: return (0.85, 0.45, 0.40)
    if "arm" in n or "weapon" in n: return (0.55, 0.68, 0.90)
    if "leg" in n or "shin" in n: return (0.60, 0.82, 0.55)
    if "chest" in n or "body" in n: return (0.85, 0.74, 0.50)
    return (0.7, 0.7, 0.7)


FACES = []
CORNER = [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0), (0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]
QUADS = [(0, 1, 2, 3), (5, 4, 7, 6), (4, 0, 3, 7), (1, 5, 6, 2), (3, 2, 6, 7), (4, 5, 1, 0)]
for b in bones:
    M = world(b["name"]); col = cat(b["name"])
    for c in b.get("cubes", []):
        o = np.array(c["origin"], float); s = np.array(c["size"], float)
        pts = (M @ np.c_[np.array([o + np.array(cn) * s for cn in CORNER]), np.ones(8)].T).T[:, :3]
        for q in QUADS:
            quad = pts[list(q)]
            nrm = np.cross(quad[1] - quad[0], quad[3] - quad[0])
            ln = np.linalg.norm(nrm); nrm = nrm / ln if ln > 1e-9 else np.array([0, 1.0, 0])
            FACES.append((quad, col, nrm))

# Approximate hammer: a haft along the tuned grip direction through weapon_mount.
Wm = world("weapon_mount")
grip = (Wm @ np.array([H_POS[0], H_POS[1], H_POS[2], 1]))[:3]
haft_dir = Wm[:3, :3] @ euler(*H_ROT) @ np.array([0, 1, 0])  # hammer haft runs +Y in its own space
haft_dir = haft_dir / (np.linalg.norm(haft_dir) + 1e-9)
hammer_bottom = grip - haft_dir * 16 * H_SCALE
hammer_head = grip + haft_dir * 22 * H_SCALE   # head end
HAMMER = (hammer_bottom, hammer_head)

light = np.array([0.35, 0.8, 0.45]); light /= np.linalg.norm(light)
allpts = np.array([p for f in FACES for p in f[0]] + [HAMMER[0], HAMMER[1], grip])
ctr = allpts.mean(0); span = (allpts.max(0) - allpts.min(0)).max()


def render(ax, d, upref, title):
    d = np.array(d, float); d /= np.linalg.norm(d)
    right = np.cross(upref, d); right /= np.linalg.norm(right); up = np.cross(d, right)
    def proj(p): r = np.array(p) - ctr; return [r @ right, r @ up]
    polys, colors, depths = [], [], []
    for quad, col, nrm in FACES:
        polys.append([proj(p) for p in quad]); depths.append(((np.array([proj(p) for p in quad])).mean()))
        sh = 0.32 + 0.68 * max(0.0, float(nrm @ light)); colors.append(tuple(c * sh for c in col))
        depths[-1] = ((quad - ctr) @ d).mean()
    order = np.argsort(depths)[::-1]
    ax.add_collection(PolyCollection([polys[i] for i in order], facecolors=[colors[i] for i in order],
                                     edgecolors=(0, 0, 0, 0.2), linewidths=0.3))
    hb, hh = proj(HAMMER[0]), proj(HAMMER[1]); g = proj(grip)
    ax.plot([hb[0], hh[0]], [hb[1], hh[1]], color="#d9a441", lw=4, solid_capstyle="round", zorder=5)
    ax.plot(hh[0], hh[1], "s", color="#b06b2a", ms=10, zorder=6)   # hammer head end
    ax.plot(g[0], g[1], "o", color="white", ms=4, zorder=7)        # grip (weapon_mount)
    lim = span * 0.62
    ax.set_xlim(-lim, lim); ax.set_ylim(-lim, lim); ax.set_aspect("equal")
    ax.set_title(title, fontsize=10, color="white"); ax.set_facecolor("0.12"); ax.set_xticks([]); ax.set_yticks([])


fig, axes = plt.subplots(1, 3, figsize=(15, 6)); fig.patch.set_facecolor("0.12")
render(axes[0], (1, 0, 0), (0, 1, 0), f"LEFT SIDE  [{ANIM or 'rest'} t={TIME}]")
render(axes[1], (0.7, -0.25, 1.0), (0, 1, 0), "3/4 FRONT")
render(axes[2], (-1, 0, 0), (0, 1, 0), "RIGHT SIDE")
plt.tight_layout(); plt.savefig(OUT, dpi=92, facecolor="0.12")
print("wrote", OUT)
