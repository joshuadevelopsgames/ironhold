"""
Renders king_enderman.geo.json to a PNG so we can see the model silhouette
and compare with the reference image. Uses matplotlib 3D axis-aligned boxes.

Each bone's cubes are colored by bone so we can also diagnose cube placement.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
import numpy as np


GEO = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/geo/king_enderman.geo.json")

# Obsidian-ish base + distinctive color per bone so we can see groupings.
BONE_COLORS = {
    "body":      "#2a1538",
    "head":      "#36203a",
    "right_arm": "#301a40",
    "left_arm":  "#301a40",
    "right_leg": "#221438",
    "left_leg":  "#221438",
}
EYE_GLOW_COLOR = "#f06aff"


def box_faces(origin, size):
    x, y, z = origin
    dx, dy, dz = size
    v = [
        (x,      y,      z),
        (x + dx, y,      z),
        (x + dx, y + dy, z),
        (x,      y + dy, z),
        (x,      y,      z + dz),
        (x + dx, y,      z + dz),
        (x + dx, y + dy, z + dz),
        (x,      y + dy, z + dz),
    ]
    # 6 faces as quads (vertex indices into v)
    faces = [
        [v[0], v[1], v[2], v[3]],  # -Z front
        [v[4], v[5], v[6], v[7]],  # +Z back
        [v[0], v[1], v[5], v[4]],  # -Y bottom
        [v[3], v[2], v[6], v[7]],  # +Y top
        [v[0], v[3], v[7], v[4]],  # -X right (from viewer)
        [v[1], v[2], v[6], v[5]],  # +X left
    ]
    return faces


def render(geo_path, out_path, angle=(20, -60)):
    data = json.loads(geo_path.read_text())
    bones = data["minecraft:geometry"][0]["bones"]

    fig = plt.figure(figsize=(6, 10), facecolor="#0a0612")
    ax = fig.add_subplot(111, projection="3d")
    ax.set_facecolor("#0a0612")

    all_pts = []
    for bone in bones:
        name = bone["name"]
        base_color = BONE_COLORS.get(name, "#444444")
        for cube in bone.get("cubes", []):
            origin = cube["origin"]
            size = cube["size"]
            # Eye-glow cubes: detected by UV y>100 + small size (heuristic)
            uv = cube.get("uv", [0, 0])
            is_eye = uv[1] >= 116 and size[0] <= 4 and size[2] <= 2
            color = EYE_GLOW_COLOR if is_eye else base_color
            faces = box_faces(origin, size)
            poly = Poly3DCollection(
                faces,
                facecolors=color,
                edgecolors="#060208",
                linewidths=0.25,
                alpha=1.0,
            )
            ax.add_collection3d(poly)
            for face in faces:
                for pt in face:
                    all_pts.append(pt)

    # Axes / framing — give all three axes equal world-space scale so the
    # model is not squashed. Plot X = world X, plot Y = world Z (depth),
    # plot Z = world Y (vertical).
    pts = np.array(all_pts)
    mn, mx = pts.min(axis=0), pts.max(axis=0)
    ctr = (mn + mx) / 2
    span = float(max(mx - mn)) * 0.58  # a bit of padding
    ax.set_xlim(ctr[0] - span, ctr[0] + span)
    ax.set_ylim(ctr[2] - span, ctr[2] + span)
    ax.set_zlim(ctr[1] - span, ctr[1] + span)
    ax.set_box_aspect((1, 1, 1))

    ax.view_init(elev=angle[0], azim=angle[1])
    ax.set_axis_off()

    plt.tight_layout()
    plt.savefig(out_path, dpi=110, facecolor=fig.get_facecolor(),
                bbox_inches="tight", pad_inches=0)
    plt.close(fig)
    print(f"wrote {out_path}")


if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/tmp/king_render.png")
    # Render three angles for side-by-side comparison.
    render(GEO, out.with_name("king_front.png"), angle=(5, -90))    # straight front
    render(GEO, out.with_name("king_3q.png"),    angle=(10, -60))   # 3/4 view (like reference)
    render(GEO, out.with_name("king_side.png"),  angle=(5, 0))      # profile
