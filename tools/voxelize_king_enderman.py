"""
Voxelizes the Meshy AI OBJ of King Enderman into a Minecraft-style cuboid
model, then greedy-meshes the filled voxels into rectangular boxes so the
resulting geometry is small enough for Minecraft entity rendering.

Outputs:
  - list of (origin_xyz, size_xyz) boxes in Minecraft/Blockbench coords
    saved as JSON for the Blockbench .geo.json generator to consume.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

import numpy as np
import trimesh
from scipy import ndimage


OBJ_PATH = Path("/Users/joshua/Downloads/Meshy_AI_King_Enderman_0421191653_generate.obj")
OUT_PATH = Path("/Users/joshua/Kingdom SMP/ironhold/tools/king_enderman_voxels.json")

# Resolution is voxels along the model's tallest axis.
# Lower = fewer cubes in final model. ~36 gives ~50-150 boxes after meshing.
TARGET_VOX_HEIGHT = 36
MIN_BOX_VOLUME = 4  # Cull stragglers below this voxel volume.


def load_and_fix_orientation(path: Path) -> trimesh.Trimesh:
    print(f"Loading {path.name} ({path.stat().st_size / 1e6:.1f} MB)...")
    mesh = trimesh.load(str(path), force="mesh", process=False)
    print(f"  verts={len(mesh.vertices)}  faces={len(mesh.faces)}")

    # Inspect extents; assume longest dimension should be height (Y).
    extents = mesh.extents
    print(f"  extents xyz = {extents}")

    # If Z is tallest (common for Meshy/Blender Z-up), rotate to Y-up.
    longest_axis = int(np.argmax(extents))
    if longest_axis == 2:
        print("  rotating Z-up → Y-up")
        R = trimesh.transformations.rotation_matrix(-np.pi / 2, [1, 0, 0])
        mesh.apply_transform(R)

    # Recenter so feet sit at Y=0 and model is centered at X=0, Z=0.
    mins = mesh.bounds[0]
    maxs = mesh.bounds[1]
    cx = (mins[0] + maxs[0]) / 2
    cz = (mins[2] + maxs[2]) / 2
    mesh.apply_translation([-cx, -mins[1], -cz])
    return mesh


def voxelize(mesh: trimesh.Trimesh, target_height: int) -> np.ndarray:
    """Returns a 3D boolean numpy array of filled voxels [x, y, z]."""
    y_extent = mesh.extents[1]
    pitch = y_extent / target_height
    print(f"  voxelizing at pitch={pitch:.4f} (target height {target_height})")

    vox = mesh.voxelized(pitch=pitch).fill()
    mat = vox.matrix  # shape [X, Y, Z], dtype=bool
    print(f"  raw voxel matrix shape = {mat.shape}, filled = {int(mat.sum())}")
    return mat, pitch


def smooth_voxels(mat: np.ndarray) -> np.ndarray:
    """Morphological closing: dilate then erode to fill pinholes + smooth noise."""
    closed = ndimage.binary_closing(mat, iterations=1)
    print(f"  after closing: filled = {int(closed.sum())}")
    return closed


def symmetrize_x(mat: np.ndarray) -> np.ndarray:
    """
    Enforce X-axis mirror symmetry. For each (y, z) column, pair x and (sx-1-x);
    output is True only if BOTH halves were filled. This kills asymmetric
    features like the staff held on one side.
    """
    sx = mat.shape[0]
    mirrored = mat[::-1, :, :]
    symmetric = mat & mirrored
    center = sx // 2
    # If the model has an odd X width, the center column stays as-is.
    if sx % 2 == 1:
        symmetric[center, :, :] = mat[center, :, :]
    print(f"  after X-symmetrize: filled = {int(symmetric.sum())}")
    return symmetric


def largest_component(mat: np.ndarray) -> np.ndarray:
    """Keep only the single largest connected component (drops floating cubes)."""
    labels, n = ndimage.label(mat, structure=np.ones((3, 3, 3), dtype=bool))
    if n <= 1:
        return mat
    sizes = ndimage.sum(mat, labels, index=range(1, n + 1))
    keep_label = int(np.argmax(sizes)) + 1
    kept = labels == keep_label
    print(f"  largest_component: {n} components → kept {int(kept.sum())} voxels")
    return kept


def greedy_mesh(mat: np.ndarray) -> list[tuple[tuple[int, int, int], tuple[int, int, int]]]:
    """
    Greedy 3D box extraction: for each unassigned filled voxel, expand into the
    largest axis-aligned box where every voxel is filled. Mark them used.
    Returns a list of (origin, size) tuples in voxel units.
    """
    sx, sy, sz = mat.shape
    used = np.zeros_like(mat, dtype=bool)
    boxes: list[tuple[tuple[int, int, int], tuple[int, int, int]]] = []

    for x in range(sx):
        for y in range(sy):
            for z in range(sz):
                if used[x, y, z] or not mat[x, y, z]:
                    continue

                # Expand X
                wx = 1
                while (
                    x + wx < sx
                    and mat[x + wx, y, z]
                    and not used[x + wx, y, z]
                ):
                    wx += 1

                # Expand Y (keep X-row filled at each new Y)
                wy = 1
                while y + wy < sy:
                    row = mat[x : x + wx, y + wy, z]
                    used_row = used[x : x + wx, y + wy, z]
                    if not row.all() or used_row.any():
                        break
                    wy += 1

                # Expand Z (keep XY-slab filled at each new Z)
                wz = 1
                while z + wz < sz:
                    slab = mat[x : x + wx, y : y + wy, z + wz]
                    used_slab = used[x : x + wx, y : y + wy, z + wz]
                    if not slab.all() or used_slab.any():
                        break
                    wz += 1

                used[x : x + wx, y : y + wy, z : z + wz] = True
                boxes.append(((x, y, z), (wx, wy, wz)))

    return boxes


def merge_small_boxes(
    boxes: list[tuple[tuple[int, int, int], tuple[int, int, int]]],
    min_volume: int = 2,
) -> list[tuple[tuple[int, int, int], tuple[int, int, int]]]:
    """Discard boxes with volume < min_volume to cull tiny stragglers."""
    kept = [b for b in boxes if b[1][0] * b[1][1] * b[1][2] >= min_volume]
    print(f"  merge_small: kept {len(kept)}/{len(boxes)} (min vol={min_volume})")
    return kept


def main():
    mesh = load_and_fix_orientation(OBJ_PATH)
    mat, pitch = voxelize(mesh, TARGET_VOX_HEIGHT)

    print("Cleaning voxel grid...")
    mat = smooth_voxels(mat)
    mat = symmetrize_x(mat)
    mat = smooth_voxels(mat)       # second pass closes post-symmetrize gaps
    mat = largest_component(mat)

    print("Greedy meshing...")
    boxes = greedy_mesh(mat)
    print(f"  raw boxes = {len(boxes)}")

    boxes = merge_small_boxes(boxes, min_volume=MIN_BOX_VOLUME)

    # Save as JSON: each box is (origin voxel indices, size voxel counts).
    # The MC-coord builder will map these to Blockbench units later.
    data = {
        "source": str(OBJ_PATH),
        "voxel_pitch_m": float(pitch),
        "matrix_shape": list(mat.shape),
        "boxes": [
            {"origin": list(o), "size": list(s)} for (o, s) in boxes
        ],
    }
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text(json.dumps(data))
    print(f"Wrote {OUT_PATH} with {len(boxes)} boxes.")


if __name__ == "__main__":
    main()
