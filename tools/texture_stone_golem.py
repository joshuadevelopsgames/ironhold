"""Paint a "carved construct" stone-brick texture onto an existing Blockbench
(Bedrock, box-UV) model WITHOUT touching its geometry.

Style: stone bricks across the body, chiseled-stone-brick accents on the chest
and brow, cracked-stone-brick battle damage scattered around, light moss pooling
along top edges, darkened edge ambient-occlusion so each cube reads as a distinct
carved block, and glowing amber eyes on the front (-Z / "north") face of the head.

It reads whatever cubes/UVs are currently in the .bbmodel, so it adapts to a
re-proportioned model.

Usage:
    python3 tools/texture_stone_golem.py [model.bbmodel] [--link]
"""

import base64
import io
import json
import os
import sys
import zipfile

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, os.path.join(ROOT, "tools"))

from build_stone_golem_blockbench import (  # noqa: E402  (palette + helpers)
    add_moss, AMBER, AMBER_HI, DARK, MOSS, MOSS_DK,
)

DEFAULT_MODEL = os.path.join(ROOT, "art", "blockbench", "iron_golem", "iron_golem.bbmodel")
JAR = os.path.expanduser("~/Library/Application Support/minecraft/versions/26.1.1/26.1.1.jar")

FACE_SHADE = {"up": 1.14, "down": 0.72, "north": 1.0, "south": 0.85, "east": 0.94, "west": 0.9}
BROW = (22, 20, 18, 255)   # deep brow shadow
PINK = (236, 124, 180)     # body-skirt accent


def is_face_cube(name):
    return any(k in name for k in ("head", "nose", "skull", "face"))


def paint_face_eyes(draw, rect):
    """Two glowing amber eyes recessed in a dark, carved brow slot."""
    x, y, w, h = rect
    if w < 4 or h < 4:
        return
    eye_y = y + int(round(h * 0.42))
    lx = x + int(round(w * 0.30))
    rx = x + int(round(w * 0.70))
    sx0 = x + max(1, int(round(w * 0.14)))
    sx1 = x + min(w - 1, int(round(w * 0.86)))
    # recessed eye slot + carved brow shadow line above it
    draw.rectangle([sx0, eye_y - 1, sx1, eye_y + 1], fill=DARK)
    draw.line([(sx0, eye_y - 2), (sx1, eye_y - 2)], fill=BROW)
    # the glowing eyes themselves, with a bright core + faint downward bloom
    for ex in (lx, rx):
        draw.rectangle([ex - 1, eye_y, ex + 1, eye_y], fill=AMBER)
        draw.point((ex, eye_y), fill=AMBER_HI)
        draw.point((ex, eye_y + 1), fill=AMBER)


def load_tiles():
    names = {
        "brick": "stone_bricks",
        "cracked": "cracked_stone_bricks",
        "chiseled": "chiseled_stone_bricks",
        "mossy": "mossy_stone_bricks",
        "cobble": "cobblestone",
        "stone": "stone",          # smooth, for the carved face
        "andesite": "andesite",
    }
    out = {}
    with zipfile.ZipFile(JAR) as z:
        for key, fname in names.items():
            with z.open(f"assets/minecraft/textures/block/{fname}.png") as f:
                im = Image.open(io.BytesIO(f.read())).convert("RGBA")
                # animated/MER strips: keep the top 16x16
                out[key] = im.crop((0, 0, 16, 16)) if im.height > 16 else im
    return out


TILES = load_tiles()


def rng_for(i):
    # deterministic, stable per cube index
    return (i * 2654435761 + 1013904223) % 997


def face_rects(u, v, size):
    w, h, d = [int(round(s)) for s in size]
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + d + w, v + d, w, h),
    }


def tile_face(img, rect, tile, shade):
    x0, y0, w, h = rect
    tw, th = tile.width, tile.height
    for y in range(h):
        for x in range(w):
            r, g, b, a = tile.getpixel((x % tw, y % th))
            img.putpixel((x0 + x, y0 + y),
                         (min(255, int(r * shade)), min(255, int(g * shade)),
                          min(255, int(b * shade)), a))


def tint_face(img, rect, tile, color, shade):
    """Fill a face with `color`, modulated by the luminance of `tile` so the
    brick relief (mortar lines) shows through. Used for the pink skirt."""
    x0, y0, w, h = rect
    tw, th = tile.width, tile.height
    for y in range(h):
        for x in range(w):
            r, g, b, a = tile.getpixel((x % tw, y % th))
            lum = (r + g + b) / 765.0
            f = lum * shade
            img.putpixel((x0 + x, y0 + y),
                         (min(255, int(color[0] * f)), min(255, int(color[1] * f)),
                          min(255, int(color[2] * f)), a))


def edge_ao(img, rect, ring0=0.58, ring1=0.82):
    """Darken the 1-2px border of a face to fake AO so the cube reads as a block."""
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            ring = min(x, y, w - 1 - x, h - 1 - y)
            if ring == 0:
                f = ring0
            elif ring == 1 and w >= 6 and h >= 6 and ring1 < 1.0:
                f = ring1
            else:
                continue
            px = img.getpixel((x0 + x, y0 + y))
            if px[3] == 0:
                continue
            img.putpixel((x0 + x, y0 + y),
                         (int(px[0] * f), int(px[1] * f), int(px[2] * f), px[3]))


def top_edge_moss(draw, rect, seed, amount):
    """A little moss clinging to the top edge / upper crevice of a vertical face."""
    x0, y0, w, h = rect
    if w < 3 or h < 3:
        return
    r = seed
    n = max(1, (w * amount) // 5)
    for k in range(n):
        r = (r * 1103515245 + 12345) & 0x7FFFFFFF
        x = x0 + (r % w)
        run = 1 + ((r >> 8) % min(3, h))
        col = MOSS if (r >> 16) % 4 else MOSS_DK
        for dy in range(run):
            draw.point((x, y0 + dy), fill=col)
        if (r >> 20) % 3 == 0 and x + 1 < x0 + w:
            draw.point((x + 1, y0), fill=MOSS_DK)


def carve_back(img, draw, rect, seed):
    """Light moss creeping up the back crevice. Deliberately NON-directional —
    the layered torso cubes share overlapping box-UV, so any straight spine/seam
    line would land at different offsets per cube and read as doubled,
    misaligned 'overlapping patterns'. Random moss dabs can't misalign."""
    x0, y0, w, h = rect
    add_moss(draw, (x0, y0 + h - 4, w, 4), seed + 7, max(1, w // 6))


def material_for(i, name, size, is_chest, is_brow):
    if is_chest or is_brow:
        return "chiseled"
    r = rng_for(i) % 100
    if r < 16:
        return "cracked"
    if r < 30:
        return "mossy"
    if r < 38:
        return "cobble"
    return "brick"


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    flags = {a for a in sys.argv[1:] if a.startswith("--")}
    model_path = args[0] if args else DEFAULT_MODEL

    with open(model_path) as fh:
        model = json.load(fh)

    res = model.get("resolution", {"width": 128, "height": 128})
    tw, th = int(res["width"]), int(res["height"])
    elements = [e for e in model["elements"] if e.get("type", "cube") == "cube"]

    # identify the chest (largest-volume body cube) for a chiseled accent
    chest_idx = -1
    chest_vol = -1
    for i, e in enumerate(elements):
        if "body" in (e.get("name") or "").lower():
            sz = [e["to"][k] - e["from"][k] for k in range(3)]
            vol = sz[0] * sz[1] * sz[2]
            if vol > chest_vol:
                chest_vol, chest_idx = vol, i

    # grow canvas if any box-UV footprint spills past the declared resolution
    max_u, max_v = tw, th
    for e in elements:
        if not e.get("box_uv"):
            continue
        u, v = e.get("uv_offset", [0, 0])
        size = [e["to"][k] - e["from"][k] for k in range(3)]
        for (rx, ry, rw, rh) in face_rects(int(u), int(v), size).values():
            max_u, max_v = max(max_u, rx + rw), max(max_v, ry + rh)

    img = Image.new("RGBA", (max_u, max_v), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # The torso is layered from overlapping cubes that share atlas space. Painting
    # the main torso cube LAST lets it fully own its visible faces instead of
    # showing fragments of the overlay cube underneath it.
    paint_order = [k for k in range(len(elements)) if k != chest_idx]
    if chest_idx >= 0:
        paint_order.append(chest_idx)

    head_norths = []
    for i in paint_order:
        e = elements[i]
        if not e.get("box_uv"):
            print(f"  ! skipping non-box-UV cube: {e.get('name')}")
            continue
        name = (e.get("name") or "").lower()
        u, v = e.get("uv_offset", [0, 0])
        size = [e["to"][k] - e["from"][k] for k in range(3)]
        face_cube = is_face_cube(name)
        # Whole torso (incl. skirt) = ONE uniform brick, so overlapping torso cubes
        # can never show two mismatched materials meeting at a seam.
        is_body = ("body" in name)
        if face_cube:
            mat = "stone"          # smooth carved visage
        elif is_body:
            mat = "brick"          # uniform torso
        else:
            mat = material_for(i, name, size, False, False)   # arms/legs keep variety
        tile = TILES[mat]

        rects = face_rects(int(u), int(v), size)
        weathered = (not face_cube) and (not is_body) and (
            mat in ("mossy", "cobble") or "shoulder" in name or "back" in name or "top" in name)
        for face, rect in rects.items():
            x, y, w, h = rect
            if w <= 0 or h <= 0:
                continue
            tile_face(img, rect, tile, FACE_SHADE[face])
            if face_cube:
                edge_ao(img, rect, ring0=0.76, ring1=1.0)   # softer so the face isn't murky
            else:
                edge_ao(img, rect)
            if (not face_cube) and (not is_body) and face in ("north", "south", "east", "west") \
                    and (weathered or rng_for(i) % 3 == 0):
                top_edge_moss(draw, rect, rng_for(i) + ord(face[0]), 2 if weathered else 1)
            # a little non-directional moss in the lower-back crevice
            if is_body and face == "south" and w >= 10 and h >= 8:
                carve_back(img, draw, rect, rng_for(i))

        # eyes only on the actual head cube, not the protruding face plates
        if "head" in name or "skull" in name:
            head_norths.append(rects["north"])

    if not head_norths:
        print("  ! no head cube found by name; no eyes painted")
    for rect in head_norths:
        paint_face_eyes(draw, rect)

    tex = model["textures"][0]
    tex_name = os.path.basename(tex.get("path") or tex.get("name") or "texture.png")
    out_png = os.path.join(os.path.dirname(model_path), tex_name)
    img.save(out_png)
    print(f"wrote {out_png}  ({img.width}x{img.height}, {len(elements)} cubes, chest=#{chest_idx})")

    if "--link" in flags:
        # Path-only (no embedded `source`) so Blockbench MUST load it as a
        # watched external file and can't silently fall back to internal/embedded.
        tex.pop("source", None)
        tex["internal"] = False
        tex["path"] = out_png
        tex["relative_path"] = "./" + tex_name
        tex["name"] = tex_name
        tex["width"], tex["height"] = img.width, img.height
        tex["uv_width"], tex["uv_height"] = tw, th
        with open(model_path, "w") as fh:
            json.dump(model, fh, indent=2)
            fh.write("\n")
        print(f"linked texture into {model_path} (geometry unchanged)")


if __name__ == "__main__":
    main()
