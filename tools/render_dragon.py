#!/usr/bin/env python3
"""Offline shaded render of a GeckoLib/Bedrock geo.json (no Blockbench needed).

Does bone-hierarchy forward kinematics (geo pivots are absolute; each bone rotates its cubes +
children about its pivot, order Rz*Ry*Rx, composed through parents — matching MC ModelPart), then
a painter's-algorithm shaded render from several camera angles into one PNG. Lets model edits be
eyeballed quickly. Categorical colors: body=tan, neck=olive, head=red, wings=blue, tail=green.

Usage: python3 tools/render_dragon.py [in.geo.json] [out.png]
"""
import sys, json, math
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.collections import PolyCollection

IN = sys.argv[1] if len(sys.argv) > 1 else "art/blockbench/dragon/dragon.geo.json"
OUT = sys.argv[2] if len(sys.argv) > 2 else "/tmp/dragon_render.png"

doc = json.load(open(IN))
bones = doc["minecraft:geometry"][0]["bones"]
by = {b["name"]: b for b in bones}


def euler(rx, ry, rz):  # degrees -> 3x3, matrix = Rz*Ry*Rx (MC order)
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def aff(R, t):
    M = np.eye(4); M[:3, :3] = R; M[:3, 3] = t; return M


_W = {}
def world(name):
    if name in _W:
        return _W[name]
    b = by[name]
    piv = np.array(b.get("pivot", [0, 0, 0]), float)
    R = euler(*b.get("rotation", [0, 0, 0]))
    L = aff(np.eye(3), piv) @ aff(R, np.zeros(3)) @ aff(np.eye(3), -piv)  # T(piv) R T(-piv)
    p = b.get("parent")
    _W[name] = (world(p) @ L) if (p and p in by) else L
    return _W[name]


def cat_color(name):
    n = name.lower()
    if name == "body":
        return (0.85, 0.74, 0.50)
    if "neck" in n:
        return (0.70, 0.72, 0.40)
    if any(k in n for k in ("head", "jaw", "horn", "snout")):
        return (0.85, 0.45, 0.40)
    if any(k in n for k in ("wing", "hand", "arm", "shoulder", "claw", "frame")):
        return (0.55, 0.68, 0.90)
    if any(k in n for k in ("tail", "fin", "spike")):
        return (0.55, 0.82, 0.55)
    return (0.7, 0.7, 0.7)


FACES = []
CORNER = [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0), (0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]
QUADS = [(0, 1, 2, 3), (5, 4, 7, 6), (4, 0, 3, 7), (1, 5, 6, 2), (3, 2, 6, 7), (4, 5, 1, 0)]
for b in bones:
    M = world(b["name"])
    col = cat_color(b["name"])
    for c in b.get("cubes", []):
        o = np.array(c["origin"], float); s = np.array(c["size"], float)
        pts = np.array([o + np.array(cn) * s for cn in CORNER])
        ptsw = (M @ np.c_[pts, np.ones(8)].T).T[:, :3]
        for q in QUADS:
            quad = ptsw[list(q)]
            nrm = np.cross(quad[1] - quad[0], quad[3] - quad[0])
            ln = np.linalg.norm(nrm)
            nrm = nrm / ln if ln > 1e-9 else np.array([0, 1.0, 0])
            FACES.append((quad, col, nrm))

light = np.array([0.35, 0.8, 0.45]); light = light / np.linalg.norm(light)
allpts = np.array([p for f in FACES for p in f[0]])
ctr = allpts.mean(0)
span = (allpts.max(0) - allpts.min(0)).max()


def render(ax, d, upref, title):
    d = np.array(d, float); d = d / np.linalg.norm(d)
    right = np.cross(upref, d); right = right / np.linalg.norm(right)
    up = np.cross(d, right)
    polys, colors, depths = [], [], []
    for quad, col, nrm in FACES:
        rel = quad - ctr
        polys.append(np.c_[rel @ right, rel @ up])
        depths.append((rel @ d).mean())
        sh = 0.32 + 0.68 * max(0.0, float(nrm @ light))
        colors.append((col[0] * sh, col[1] * sh, col[2] * sh))
    order = np.argsort(depths)[::-1]
    ax.add_collection(PolyCollection([polys[i] for i in order], facecolors=[colors[i] for i in order],
                                     edgecolors=(0, 0, 0, 0.22), linewidths=0.3))
    lim = span * 0.6
    ax.set_xlim(-lim, lim); ax.set_ylim(-lim, lim); ax.set_aspect("equal")
    ax.set_title(title, fontsize=10, color="white")
    ax.set_facecolor("0.12"); ax.set_xticks([]); ax.set_yticks([])


fig, axes = plt.subplots(1, 3, figsize=(15, 5.4)); fig.patch.set_facecolor("0.12")
render(axes[0], d=(1, 0, 0), upref=(0, 1, 0), title="LEFT SIDE (-Z faces left)")
render(axes[1], d=(0.7, -0.35, 1.0), upref=(0, 1, 0), title="3/4 FRONT")
render(axes[2], d=(0, 1, 0), upref=(0, 0, -1), title="TOP")
plt.tight_layout()
plt.savefig(OUT, dpi=92, facecolor="0.12")
print("wrote", OUT, "| faces:", len(FACES), "| bones:", len(bones))
