"""Render king_enderman.geo.json via trimesh → PNG, robust framing."""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import trimesh


GEO = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/geo/king_enderman.geo.json")

BONE_COLORS = {
    "body":      (0x2A, 0x15, 0x38, 255),
    "head":      (0x36, 0x20, 0x3A, 255),
    "right_arm": (0x30, 0x1A, 0x40, 255),
    "left_arm":  (0x30, 0x1A, 0x40, 255),
    "right_leg": (0x22, 0x14, 0x38, 255),
    "left_leg":  (0x22, 0x14, 0x38, 255),
}
EYE_GLOW = (0xF0, 0x6A, 0xFF, 255)


def build_scene():
    data = json.loads(GEO.read_text())
    bones = data["minecraft:geometry"][0]["bones"]

    meshes = []
    for bone in bones:
        name = bone["name"]
        base_color = BONE_COLORS.get(name, (100, 60, 150, 255))
        for cube in bone.get("cubes", []):
            ox, oy, oz = cube["origin"]
            sx, sy, sz = cube["size"]
            uv = cube.get("uv", [0, 0])
            is_eye = uv[1] >= 116 and sx <= 4 and sz <= 2
            color = EYE_GLOW if is_eye else base_color
            box = trimesh.creation.box(extents=(sx, sy, sz))
            box.apply_translation((ox + sx / 2, oy + sy / 2, oz + sz / 2))
            box.visual.face_colors = color
            meshes.append(box)
    return trimesh.util.concatenate(meshes)


def render(mesh, out_path, angle_deg=30, elev_deg=0, distance_mult=1.8):
    """Render the mesh from a chosen azimuth/elevation."""
    scene = trimesh.Scene(mesh)

    # Compute camera position around model center.
    bounds = mesh.bounds
    center = mesh.centroid
    diag = float(np.linalg.norm(bounds[1] - bounds[0]))

    a = np.deg2rad(angle_deg)
    e = np.deg2rad(elev_deg)
    cam_offset = np.array([
        np.sin(a) * np.cos(e),
        np.sin(e),
        np.cos(a) * np.cos(e),
    ]) * diag * distance_mult

    # Camera transform pointing at the model center.
    cam_pos = center + cam_offset
    T = trimesh.scene.cameras.look_at([center], fov=(35, 35),
                                      rotation=None, distance=None,
                                      center=None)
    # Simpler: use scene.set_camera which takes angles.
    scene.set_camera(
        angles=(np.deg2rad(elev_deg), np.deg2rad(angle_deg), 0),
        distance=diag * distance_mult,
        center=center,
    )

    try:
        png = scene.save_image(resolution=(600, 900), background=(10, 6, 18, 255))
        Path(out_path).write_bytes(png)
        print(f"wrote {out_path}")
    except Exception as e:
        print(f"render failed ({out_path}): {e}")


if __name__ == "__main__":
    mesh = build_scene()
    print(f"bounds = {mesh.bounds}")
    print(f"centroid = {mesh.centroid}")

    out_dir = Path("/tmp")
    render(mesh, out_dir / "king_front.png", angle_deg=0,  elev_deg=0)
    render(mesh, out_dir / "king_3q.png",    angle_deg=30, elev_deg=-5)
    render(mesh, out_dir / "king_side.png",  angle_deg=90, elev_deg=0)
