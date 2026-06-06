"""Offline orthographic silhouette preview (front + side) of the Moon Hopling.
Reads art/blockbench/moon_hopling/moon_hopling.geo.json directly and applies the
full parent->child rotation chain, so curved ears / raised tail show correctly.
Pure PIL; writes moon_hopling_3d_preview.png. Confirms assembly, not texture."""
import json
import math
from PIL import Image, ImageDraw

GEO = 'art/blockbench/moon_hopling/moon_hopling.geo.json'

with open(GEO) as f:
    bones = json.load(f)['minecraft:geometry'][0]['bones']
BY_NAME = {b['name']: b for b in bones}

# soft per-part fill colours just so the silhouette reads
COLORS = {
    'body': (180, 190, 212), 'head': (208, 216, 232), 'snout': (236, 240, 250),
    'ear_l_base': (196, 210, 240), 'ear_l_tip': (224, 240, 255),
    'ear_r_base': (196, 210, 240), 'ear_r_tip': (224, 240, 255),
    'tail': (220, 236, 255),
    'leg_fl': (150, 162, 196), 'leg_fr': (150, 162, 196),
    'leg_bl': (138, 150, 184), 'leg_br': (138, 150, 184),
}


def rot_about(p, pivot, deg):
    """Rotate point p about pivot by euler degrees (Z, then Y, then X)."""
    x, y, z = p[0] - pivot[0], p[1] - pivot[1], p[2] - pivot[2]
    rx, ry, rz = (math.radians(a) for a in deg)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    return (x + pivot[0], y + pivot[1], z + pivot[2])


def world(bone, p):
    """Apply bone rotation about its pivot, then walk up through parents."""
    rot = bone.get('rotation', [0, 0, 0])
    piv = bone['pivot']
    p = rot_about(p, piv, rot) if any(rot) else p
    parent = bone.get('parent')
    return world(BY_NAME[parent], p) if parent else p


def corners(cube):
    ox, oy, oz = cube['origin']
    w, h, d = cube['size']
    pts = []
    for dx in (0, w):
        for dy in (0, h):
            for dz in (0, d):
                pts.append((ox + dx, oy + dy, oz + dz))
    return pts


def hull(points):
    pts = sorted(set(points))
    if len(pts) <= 2:
        return pts

    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
    lower = []
    for p in pts:
        while len(lower) >= 2 and cross(lower[-2], lower[-1], p) <= 0:
            lower.pop()
        lower.append(p)
    upper = []
    for p in reversed(pts):
        while len(upper) >= 2 and cross(upper[-2], upper[-1], p) <= 0:
            upper.pop()
        upper.append(p)
    return lower[:-1] + upper[:-1]


SCALE = 11
PAD = 28
H_BLOCKS = 34


def render(view):
    W = 26 * SCALE + PAD * 2
    Ht = H_BLOCKS * SCALE + PAD * 2
    img = Image.new("RGBA", (W, Ht), (245, 247, 252, 255))
    dr = ImageDraw.Draw(img)
    cx = W // 2

    def proj(p):
        return (cx + p[0] * SCALE, Ht - PAD - p[1] * SCALE)

    drawables = []  # (depth, name, world_corners)
    for b in bones:
        for cube in b.get('cubes', []):
            wc = [world(b, p) for p in corners(cube)]
            cz = sum(c[2] for c in wc) / 8
            cxv = sum(c[0] for c in wc) / 8
            depth = cz if view == "front" else cxv
            drawables.append((depth, b['name'], wc))

    for depth, name, wc in sorted(drawables, key=lambda t: t[0], reverse=True):
        flat = [(c[0], c[1]) if view == "front" else (c[2], c[1]) for c in wc]
        poly = [proj(p) for p in hull(flat)]
        if len(poly) >= 3:
            col = COLORS.get(name, (180, 188, 208))
            dr.polygon(poly, fill=col + (255,), outline=(60, 66, 86, 255))
    dr.line([(PAD, Ht - PAD), (W - PAD, Ht - PAD)], fill=(120, 124, 140, 255), width=2)
    dr.text((PAD, 8), view.upper() + ("  (front = -Z)" if view == "side" else ""),
            fill=(40, 44, 60, 255))
    return img


front = render("front")
side = render("side")
combo = Image.new("RGBA", (front.width + side.width + 12, max(front.height, side.height)),
                  (255, 255, 255, 255))
combo.paste(front, (0, 0))
combo.paste(side, (front.width + 12, 0))
combo.save("moon_hopling_3d_preview.png")
print("wrote moon_hopling_3d_preview.png", combo.size)
