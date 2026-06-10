#!/usr/bin/env python3
"""Generate Ironhold-original butterfly models, textures, and item assets.

The species roster and shape assignments mirror ButterflySpecies.java. This script is
idempotent and is the source of truth for the species-specific GeckoLib wing rigs,
64x64 entity textures, luminous glow masks, loose butterfly icons, item models, lang
keys, and the bait tag. (The retired empty-jar / per-species "jarred" item assets are
cleaned up by cleanup_retired_jar_assets, not generated — they're now the placeable
Butterfly Jar block.)
"""
import json
import os
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/ironhold"
DATA = ROOT / "src/main/resources/data/ironhold"
MODEL_DIR = ASSETS / "geckolib/models/entity"
JAR_MODEL_DIR = ASSETS / "geckolib/models/item"
JAR_ANIMATION_DIR = ASSETS / "geckolib/animations/item"
ENTITY_TEXTURE_DIR = ASSETS / "textures/entity/butterfly"
ITEM_TEXTURE_DIR = ASSETS / "textures/item"
JAR_TEXTURE_DIR = ITEM_TEXTURE_DIR / "butterfly_jar"
GUI_DIR = ASSETS / "textures/gui"
PORTRAIT_DIR = GUI_DIR / "butterfly_portrait"

WING_SIZE = 30
LEFT_U = 1
RIGHT_U = 33
WING_V = 1

# id, display name, shape, base, border, accent, secondary, pattern, glowing
SPECIES = [
    ("monarch", "Monarch", "standard", "#e8791f", "#24150f", "#fff1c4", "#bf421c", "monarch", False),
    ("buckeye", "Buckeye", "standard", "#8b5a35", "#2a1b18", "#4e9bc5", "#e78632", "buckeye", False),
    ("ringlet", "Ringlet", "small", "#9b7048", "#3c2b25", "#e5c987", "#171413", "ringlet", False),
    ("little_wood", "Little Wood", "small", "#765035", "#2f241e", "#d7bd83", "#a17b51", "wood", False),
    ("orangetip", "Orangetip", "small", "#eee9d7", "#5c5a4d", "#f08322", "#b8b59b", "orangetip", False),
    ("white_hairstreak", "White Hairstreak", "swallowtail", "#e7e5da", "#67645d", "#ffffff", "#c88645", "hairstreak_white", False),
    ("spring_azure", "Spring Azure", "small", "#83c9ef", "#386890", "#d9f3ff", "#5f9fd0", "azure", False),
    ("hairstreak", "Hairstreak", "swallowtail", "#8c8174", "#312d2a", "#e4ddd0", "#d6813c", "hairstreak", False),
    ("black_swallowtail", "Black Swallowtail", "swallowtail", "#17191c", "#08090a", "#e5c944", "#4b8fc5", "black_swallowtail", False),
    ("yellow_swallowtail", "Yellow Swallowtail", "swallowtail", "#e8cc42", "#2a251c", "#f6e885", "#5487b8", "yellow_swallowtail", False),
    ("mourning_cloak", "Mourning Cloak", "standard", "#522a32", "#e9d598", "#668fc5", "#24181f", "mourning", False),
    ("cherry_rose", "Cherry Rose", "standard", "#d979a0", "#633048", "#ffd0df", "#a94d75", "cherry", False),
    ("zebra_longwing", "Zebra Longwing", "standard", "#161713", "#080907", "#e5cf5e", "#756b35", "zebra", False),
    ("rustypage", "Rustypage", "standard", "#b35e24", "#4a291c", "#e29a4a", "#6f351e", "rusty", False),
    ("charaxes", "Charaxes", "swallowtail", "#b17a3c", "#413825", "#8f9a55", "#e1bd72", "charaxes", False),
    ("birdwing", "Birdwing", "broad", "#151b19", "#080b0a", "#43b96c", "#d7b84c", "birdwing", False),
    ("blue_monarch", "Blue Monarch", "broad", "#2669c8", "#111b31", "#62c9f2", "#e8f5ff", "blue_morpho", False),
    ("emerald_swallowtail", "Emerald Swallowtail", "swallowtail", "#111817", "#070a09", "#38c878", "#a5efb9", "emerald", False),
    ("crimson_monarch", "Crimson Monarch", "standard", "#741d28", "#210d14", "#ff4b42", "#eaa052", "crimson", True),
    ("glowstone_morpho", "Glowstone Morpho", "broad", "#b96e23", "#4f2c19", "#ffd75a", "#fff0a0", "glowstone", True),
    ("chorus_morpho", "Chorus Morpho", "broad", "#6435a8", "#24183d", "#cf76ff", "#76d1e8", "chorus", True),
    ("ender_eyespot", "Ender Eyespot", "standard", "#123c40", "#070f13", "#b550dc", "#54d0bd", "ender", True),
    ("soul_monarch", "Soul Monarch", "broad", "#4aa8b6", "#173f49", "#8ff6ed", "#e2ffff", "soul", True),
]

SHAPE_MASKS = {
    "standard": [(1, 14), (1, 7), (3, 3), (7, 1), (11, 3), (13, 7),
                 (12, 12), (9, 15), (11, 19), (9, 23), (5, 25), (2, 21)],
    "swallowtail": [(1, 14), (1, 7), (4, 2), (10, 1), (14, 4), (14, 10),
                    (11, 14), (12, 18), (9, 22), (10, 28), (7, 25), (5, 20), (2, 22)],
    "broad": [(1, 15), (1, 7), (5, 2), (13, 1), (18, 5), (18, 12),
              (14, 16), (16, 20), (12, 25), (6, 25), (2, 21)],
    "small": [(1, 13), (2, 7), (5, 4), (9, 4), (11, 8), (9, 13),
              (8, 18), (5, 22), (2, 19)],
}

# x origin away from body, z origin, width, depth. One texture pixel = 0.5 model units.
def rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def shade(color, factor):
    return tuple(max(0, min(255, round(c * factor))) for c in color)


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")


def face_uv(u, v, width, depth):
    return {
        "north": {"uv": [u, v], "uv_size": [width * 2, 1]},
        "east": {"uv": [u, v], "uv_size": [depth * 2, 1]},
        "south": {"uv": [u, v], "uv_size": [width * 2, 1]},
        "west": {"uv": [u, v], "uv_size": [depth * 2, 1]},
        "up": {"uv": [u, v], "uv_size": [width * 2, depth * 2]},
        "down": {"uv": [u, v], "uv_size": [width * 2, depth * 2]},
    }


def shape_rectangles(shape):
    """Merge the portrait's 30px wing mask into pixel-aligned model rectangles."""
    mask = Image.new("1", (WING_SIZE, WING_SIZE), 0)
    ImageDraw.Draw(mask).polygon(SHAPE_MASKS[shape], fill=1)
    rows = []
    for y in range(WING_SIZE):
        runs = []
        x = 0
        while x < WING_SIZE:
            while x < WING_SIZE and not mask.getpixel((x, y)):
                x += 1
            if x >= WING_SIZE:
                break
            start = x
            while x < WING_SIZE and mask.getpixel((x, y)):
                x += 1
            runs.append((start, x))
        rows.append(runs)

    rectangles = []
    active = {}
    for y, runs in enumerate(rows + [[]]):
        current = set(runs)
        for run, (start_y, end_y) in list(active.items()):
            if run not in current:
                rectangles.append((run[0], start_y, run[1] - run[0], end_y - start_y))
                del active[run]
        for run in current:
            if run in active:
                active[run] = (active[run][0], y + 1)
            else:
                active[run] = (y, y + 1)
    return rectangles


def wing_cubes(shape, side, y_offset=0, scale=1.0):
    cubes = []
    wing_u = LEFT_U if side == "left" else RIGHT_U
    pixel_scale = 0.5 * scale
    for x, z, width, depth in shape_rectangles(shape):
        scaled_width = width * pixel_scale
        scaled_depth = depth * pixel_scale
        model_x = (x - 1) * pixel_scale
        origin_x = (
            0.45 + model_x
            if side == "left"
            else -0.45 - model_x - scaled_width
        )
        y = y_offset + 0.42
        u = wing_u + x
        v = WING_V + z
        cubes.append({
            "origin": [origin_x, y, (-6 + z * 0.5) * scale],
            "size": [scaled_width, 0.12, scaled_depth],
            # Each rectangle maps one-for-one to the same pixel region used by the book
            # portrait, giving the live model the exact silhouette of that specimen.
            "uv": face_uv(u, v, width * 0.5, depth * 0.5),
        })
    return cubes


def gen_model(spec):
    sid, _, shape, *_ = spec
    bounds = {"standard": 2.2, "swallowtail": 2.5, "broad": 2.8, "small": 1.7}[shape]
    model = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.butterfly_{sid}",
                "texture_width": 64,
                "texture_height": 64,
                "visible_bounds_width": bounds,
                "visible_bounds_height": 1.8,
                "visible_bounds_offset": [0, 0.5, 0],
            },
            "bones": [
                {
                    "name": "body",
                    "pivot": [0, 0.5, 0],
                    "cubes": [
                        {"origin": [-0.5, 0, -2], "size": [1, 1, 4], "uv": [0, 34]},
                        {"origin": [-0.7, -0.05, -1.2], "size": [1.4, 1.1, 1.8], "uv": [12, 34]},
                        {"origin": [0.12, 0.45, -3.6], "size": [0.18, 0.18, 2.2],
                         "pivot": [0.2, 0.5, -1.6], "rotation": [0, -18, 0], "uv": [24, 34]},
                        {"origin": [-0.30, 0.45, -3.6], "size": [0.18, 0.18, 2.2],
                         "pivot": [-0.2, 0.5, -1.6], "rotation": [0, 18, 0], "uv": [28, 34]},
                    ],
                },
                {
                    "name": "left_wing",
                    "parent": "body",
                    "pivot": [0.45, 0.5, 0],
                    "cubes": wing_cubes(shape, "left"),
                },
                {
                    "name": "right_wing",
                    "parent": "body",
                    "pivot": [-0.45, 0.5, 0],
                    "cubes": wing_cubes(shape, "right"),
                },
            ],
        }],
    }
    write_json(MODEL_DIR / f"butterfly_{sid}.geo.json", model)


def clipped_composite(base, overlay, mask):
    alpha = overlay.getchannel("A")
    alpha = Image.composite(alpha, Image.new("L", alpha.size, 0), mask)
    overlay.putalpha(alpha)
    base.alpha_composite(overlay)


def ellipse(draw, box, fill, outline=None, width=1):
    draw.ellipse(box, fill=fill, outline=outline, width=width)


def paint_pattern(sid, pattern, canvas, mask, colors):
    border, accent, secondary = colors
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    # Fine structural veins are shared, then each species receives its field marks.
    vein = border + (255,)
    for target in [(11, 4), (12, 11), (9, 17), (7, 23)]:
        d.line([(1, 14), target], fill=vein, width=1)
    d.line([(4, 7), (9, 15)], fill=vein, width=1)

    if pattern == "monarch":
        d.line([(5, 2), (6, 22)], fill=vein, width=1)
        for p in [(3, 5), (8, 3), (11, 7), (10, 21), (5, 23), (2, 18)]:
            ellipse(d, [p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1], accent + (255,))
    elif pattern == "buckeye":
        for x, y, r in [(9, 8, 4), (7, 20, 4)]:
            ellipse(d, [x-r, y-r, x+r, y+r], border + (255,))
            ellipse(d, [x-r+1, y-r+1, x+r-1, y+r-1], secondary + (255,))
            ellipse(d, [x-1, y-1, x+2, y+2], accent + (255,))
            d.point((x, y), fill=(240, 240, 220, 255))
    elif pattern == "ringlet":
        for x, y in [(7, 9), (6, 18)]:
            ellipse(d, [x-2, y-2, x+2, y+2], accent + (255,))
            ellipse(d, [x-1, y-1, x+1, y+1], secondary + (255,))
    elif pattern == "wood":
        d.polygon([(3, 6), (7, 5), (9, 8), (6, 11), (3, 10)], fill=accent + (255,))
        d.polygon([(3, 15), (8, 14), (7, 19), (4, 21)], fill=accent + (255,))
        d.rectangle([8, 9, 10, 12], fill=secondary + (255,))
    elif pattern == "orangetip":
        d.polygon([(6, 3), (10, 4), (11, 9), (8, 11), (6, 8)], fill=accent + (255,))
        d.line([(2, 16), (8, 19)], fill=secondary + (255,), width=2)
    elif pattern == "hairstreak_white":
        d.line([(2, 16), (9, 19), (6, 23)], fill=border + (255,), width=1)
        d.rectangle([7, 22, 9, 25], fill=secondary + (255,))
    elif pattern == "azure":
        d.polygon([(2, 8), (7, 5), (10, 8), (8, 13), (3, 14)], fill=accent + (255,))
        for p in [(4, 18), (7, 19), (3, 16)]:
            d.point(p, fill=secondary + (255,))
    elif pattern == "hairstreak":
        d.line([(2, 15), (9, 18), (6, 22), (10, 24)], fill=accent + (255,), width=1)
        ellipse(d, [6, 21, 10, 25], secondary + (255,), outline=border + (255,))
    elif pattern == "black_swallowtail":
        d.polygon([(2, 10), (12, 8), (11, 12), (3, 15)], fill=accent + (255,))
        d.polygon([(3, 17), (10, 16), (8, 22), (4, 21)], fill=secondary + (255,))
        ellipse(d, [7, 20, 10, 23], rgb("#d95d52") + (255,))
    elif pattern == "yellow_swallowtail":
        for x in [4, 7, 10]:
            d.line([(x, 3), (x - 2, 15)], fill=border + (255,), width=2)
        d.polygon([(3, 16), (10, 15), (8, 22), (4, 21)], fill=secondary + (255,))
        ellipse(d, [7, 20, 10, 23], rgb("#d95d52") + (255,))
    elif pattern == "mourning":
        for p in [(3, 7), (7, 4), (10, 7), (9, 20), (5, 23), (2, 18)]:
            ellipse(d, [p[0]-1, p[1]-1, p[0]+1, p[1]+1], accent + (255,))
    elif pattern == "cherry":
        d.polygon([(5, 5), (8, 3), (11, 6), (9, 10), (6, 9)], fill=accent + (255,))
        d.polygon([(3, 15), (7, 13), (9, 17), (7, 21), (4, 20)], fill=secondary + (255,))
        for p in [(7, 6), (6, 17)]:
            d.point(p, fill=(255, 245, 250, 255))
    elif pattern == "zebra":
        for points in [[(3, 4), (6, 3), (4, 14), (2, 15)],
                       [(8, 3), (11, 5), (7, 15), (5, 14)],
                       [(4, 17), (7, 15), (10, 20), (8, 22)]]:
            d.polygon(points, fill=accent + (255,))
    elif pattern == "rusty":
        d.polygon([(4, 4), (8, 3), (10, 7), (7, 10), (3, 8)], fill=accent + (255,))
        d.polygon([(3, 15), (8, 14), (9, 19), (5, 23), (2, 20)], fill=secondary + (255,))
        for p in [(6, 8), (9, 12), (5, 18)]:
            d.rectangle([p[0], p[1], p[0]+1, p[1]+1], fill=border + (255,))
    elif pattern == "charaxes":
        d.polygon([(2, 8), (11, 6), (12, 10), (3, 14)], fill=accent + (255,))
        d.polygon([(3, 16), (10, 15), (8, 21), (4, 22)], fill=secondary + (255,))
    elif pattern == "birdwing":
        d.polygon([(3, 8), (8, 3), (15, 5), (13, 11), (7, 14)], fill=accent + (255,))
        d.polygon([(3, 16), (11, 15), (13, 20), (8, 24), (4, 22)], fill=secondary + (255,))
    elif pattern == "blue_morpho":
        d.polygon([(2, 8), (7, 3), (15, 4), (16, 10), (10, 15), (3, 14)], fill=accent + (255,))
        d.polygon([(3, 16), (11, 15), (13, 20), (9, 24), (4, 22)], fill=accent + (255,))
        for p in [(5, 3), (11, 3), (15, 7), (11, 23), (5, 23)]:
            d.point(p, fill=secondary + (255,))
    elif pattern == "emerald":
        d.polygon([(2, 10), (12, 7), (12, 11), (3, 15)], fill=accent + (255,))
        d.polygon([(3, 17), (10, 15), (9, 20), (5, 23)], fill=secondary + (255,))
    elif pattern == "crimson":
        for target in [(6, 3), (11, 7), (9, 16), (6, 23)]:
            d.line([(1, 14), target], fill=accent + (255,), width=2)
        d.rectangle([8, 18, 10, 20], fill=secondary + (255,))
    elif pattern == "glowstone":
        d.polygon([(3, 7), (9, 3), (16, 6), (14, 12), (7, 15)], fill=accent + (255,))
        d.polygon([(3, 17), (11, 15), (14, 20), (9, 24), (4, 22)], fill=accent + (255,))
        for p in [(5, 5), (11, 7), (8, 12), (5, 19), (10, 21), (13, 18)]:
            d.rectangle([p[0], p[1], p[0]+1, p[1]+1], fill=secondary + (255,))
    elif pattern == "chorus":
        d.polygon([(2, 10), (8, 3), (15, 5), (14, 11), (8, 15)], fill=accent + (255,))
        d.polygon([(3, 17), (10, 15), (14, 20), (9, 25), (5, 22)], fill=secondary + (255,))
        d.line([(4, 6), (12, 21)], fill=rgb("#f0b0ff") + (255,), width=1)
    elif pattern == "ender":
        for x, y, r in [(9, 8, 4), (7, 20, 4)]:
            ellipse(d, [x-r, y-r, x+r, y+r], border + (255,))
            ellipse(d, [x-r+1, y-r+1, x+r-1, y+r-1], accent + (255,))
            ellipse(d, [x-1, y-1, x+1, y+1], secondary + (255,))
            d.point((x, y), fill=(225, 255, 245, 255))
    elif pattern == "soul":
        d.polygon([(2, 9), (7, 3), (15, 5), (15, 11), (9, 15), (3, 14)], fill=accent + (230,))
        d.polygon([(3, 17), (11, 15), (14, 20), (9, 25), (4, 22)], fill=secondary + (225,))
        for target in [(7, 3), (14, 8), (10, 20), (6, 24)]:
            d.line([(1, 14), target], fill=secondary + (255,), width=1)

    clipped_composite(canvas, layer, mask)


def paint_half(spec):
    sid, _, shape, base_hex, border_hex, accent_hex, secondary_hex, pattern, _ = spec
    base, border = rgb(base_hex), rgb(border_hex)
    accent, secondary = rgb(accent_hex), rgb(secondary_hex)
    mask = Image.new("L", (WING_SIZE, WING_SIZE), 0)
    ImageDraw.Draw(mask).polygon(SHAPE_MASKS[shape], fill=255)

    canvas = Image.new("RGBA", mask.size, base + (255,))
    canvas.putalpha(mask)
    eroded = mask.filter(ImageFilter.MinFilter(5))
    border_mask = Image.frombytes("L", mask.size, bytes(
        max(0, a - b) for a, b in zip(mask.tobytes(), eroded.tobytes())
    ))
    border_layer = Image.new("RGBA", mask.size, border + (255,))
    border_layer.putalpha(border_mask)
    canvas.alpha_composite(border_layer)
    paint_pattern(sid, pattern, canvas, mask, (border, accent, secondary))
    return canvas


def gen_entity_texture(spec):
    sid, _, shape, base_hex, border_hex, accent_hex, secondary_hex, _, glowing = spec
    base, border = rgb(base_hex), rgb(border_hex)
    accent, secondary = rgb(accent_hex), rgb(secondary_hex)
    half = paint_half(spec)

    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    # Left wing cell: the half as painted. Left-wing cubes sample this with plain UVs.
    image.alpha_composite(half, (LEFT_U, WING_V))
    # Right wing cell: the same art, but each plate window flipped horizontally about its
    # own centre. The right-wing geometry is already reflected in X, so reflecting its
    # texture too makes the rendered pair true mirror images — convention-independent, and
    # using only positive UVs (negative uv_size greys out in GeckoLib; the Bedrock mirror
    # flag transforms differently than a per-face flip). Flipping per-plate (not the whole
    # 30px cell) keeps the fore/hind windows aligned with their differing widths.
    for x, y, width, height in shape_rectangles(shape):
        rectangle = half.crop((x, y, x + width, y + height))
        rectangle = rectangle.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
        image.alpha_composite(rectangle, (RIGHT_U + x, WING_V + y))
    draw = ImageDraw.Draw(image)

    # Body/antenna UV field. Broad painted areas tolerate the compact cube unwrap.
    draw.rectangle([0, 33, 31, 63], fill=shade(border, 0.7) + (255,))
    draw.rectangle([2, 35, 8, 50], fill=shade(base, 0.55) + (255,))
    draw.rectangle([11, 35, 20, 48], fill=shade(border, 0.9) + (255,))
    draw.rectangle([23, 34, 31, 42], fill=shade(border, 0.65) + (255,))
    draw.point((14, 37), fill=accent + (255,))
    draw.point((17, 37), fill=accent + (255,))
    draw.line([(25, 35), (29, 40)], fill=secondary + (255,), width=1)

    ENTITY_TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    image.save(ENTITY_TEXTURE_DIR / f"{sid}.png")

    glow_path = ENTITY_TEXTURE_DIR / f"{sid}_glowmask.png"
    if glowing:
        glow = Image.new("RGBA", image.size, (0, 0, 0, 0))
        source = image.load()
        target = glow.load()
        glow_colors = (accent, secondary)
        for y in range(image.height):
            for x in range(image.width):
                pixel = source[x, y]
                if pixel[3] and any(sum(abs(pixel[i] - c[i]) for i in range(3)) < 18
                                    for c in glow_colors):
                    target[x, y] = pixel
        glow.save(glow_path)
    elif glow_path.exists():
        glow_path.unlink()


def render_specimen(spec, shadow=False):
    """Render the full spread-wing specimen used by the encyclopedia and item icons."""
    body_col = shade(rgb(spec[4]), 0.65) + (255,)
    half = paint_half(spec)
    canvas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    if shadow:
        shadow_layer = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        ImageDraw.Draw(shadow_layer).ellipse([12, 22, 54, 52], fill=(40, 28, 18, 80))
        canvas.alpha_composite(shadow_layer.filter(ImageFilter.GaussianBlur(3)))

    canvas.alpha_composite(half.transpose(Image.Transpose.FLIP_LEFT_RIGHT), (3, 17))
    canvas.alpha_composite(half, (31, 17))
    d = ImageDraw.Draw(canvas)
    d.ellipse([30, 19, 34, 31], fill=body_col)
    d.ellipse([30, 29, 33, 47], fill=body_col)
    d.line([(31, 21), (26, 12)], fill=body_col, width=1)
    d.line([(33, 21), (38, 12)], fill=body_col, width=1)
    d.ellipse([24, 10, 27, 13], fill=body_col)
    d.ellipse([37, 10, 40, 13], fill=body_col)
    return canvas


def fit_specimen_icon(spec, width, height):
    specimen = render_specimen(spec)
    bbox = specimen.getchannel("A").getbbox()
    cropped = specimen.crop(bbox)
    scale = min(width / cropped.width, height / cropped.height)
    size = (
        max(1, round(cropped.width * scale)),
        max(1, round(cropped.height * scale)),
    )
    icon = cropped.resize(size, Image.Resampling.BOX)
    icon.putalpha(icon.getchannel("A").point(lambda alpha: 255 if alpha >= 48 else 0))
    return icon


def gen_butterfly_icon(spec):
    sid = spec[0]
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon = fit_specimen_icon(spec, 14, 14)
    image.alpha_composite(icon, ((16 - icon.width) // 2, (16 - icon.height) // 2))
    image.save(ITEM_TEXTURE_DIR / f"butterfly_{sid}.png")


def gen_net_icon():
    # Adapted from the Luminous Butterflies net silhouette, with Ironhold's
    # aged-brass, ebony, and cool-steel palette.
    rows = (
        "...........555..",
        "..........51112.",
        ".........51GHG32",
        "........21GHGH32",
        "........21HGHG3E",
        "........E1GHG3A4",
        "........BE333A74",
        ".......B1CAAA794",
        "......B3C.D97974",
        ".....B1C..D7974.",
        "....F3C....D794.",
        "...F52.....D974.",
        "..252.......44..",
        ".B12............",
        "F1C.............",
        "52..............",
    )
    palette = {
        "1": (111, 78, 35, 255),
        "2": (185, 136, 47, 255),
        "3": (82, 57, 28, 255),
        "4": (132, 146, 151, 255),
        "5": (224, 185, 83, 255),
        "6": (83, 94, 98, 255),
        "7": (221, 232, 234, 255),
        "8": (55, 65, 69, 255),
        "9": (185, 202, 207, 255),
        "A": (104, 60, 27, 255),
        "B": (61, 38, 24, 255),
        "C": (31, 23, 18, 255),
        "D": (157, 174, 180, 255),
        "E": (151, 97, 30, 255),
        "F": (240, 210, 114, 255),
        "G": (195, 213, 221, 255),
        "H": (131, 157, 170, 255),
    }
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != ".":
                pixels[x, y] = palette[key]
    image.save(ITEM_TEXTURE_DIR / "butterfly_net.png")


def gen_portrait(spec):
    """A crisp 64px top-down specimen for the encyclopedia: two mirrored 30px wing-halves
    meeting at a painted body, with antennae and a soft drop shadow. Native resolution, so it
    stays sharp at display size instead of the 3x-stretched item icon."""
    sid = spec[0]
    canvas = render_specimen(spec, shadow=True)
    PORTRAIT_DIR.mkdir(parents=True, exist_ok=True)
    canvas.save(PORTRAIT_DIR / f"{sid}.png")


def gen_book_bg():
    """The open-book background: pink cover, two grain-textured parchment pages with a
    decorative rule, and a gapped centre spine (gap leaves the title + footer on parchment)."""
    random.seed(1337)  # deterministic grain so the texture is stable across regenerations
    w, h = 348, 204
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pink = (207, 47, 111, 255)
    pink_hi = (246, 104, 158, 255)
    pink_lo = (119, 20, 68, 255)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=7, fill=pink)
    d.line([(3, 2), (w - 4, 2)], fill=pink_hi)
    d.line([(2, 3), (2, h - 4)], fill=pink_hi)
    d.line([(3, h - 3), (w - 4, h - 3)], fill=pink_lo)
    d.line([(w - 3, 3), (w - 3, h - 4)], fill=pink_lo)

    # Parchment with subtle paper grain. The opaque base stays fully opaque; the soft
    # mottling/vignette goes on a separate transparent overlay and is alpha_composited on,
    # because ImageDraw REPLACES (not blends) alpha when drawn straight onto an RGBA image.
    px0, py0, px1, py1 = 5, 5, w - 5, h - 5
    pw, ph = px1 - px0, py1 - py0
    parch = Image.new("RGB", (pw, ph), (236, 223, 193))
    noise = Image.effect_noise((pw, ph), 14)
    parch = Image.blend(parch, Image.merge("RGB", (noise, noise, noise)), 0.06).convert("RGBA")
    overlay = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    for _ in range(34):
        bx, by = random.randint(0, pw), random.randint(0, ph)
        br = random.randint(5, 16)
        od.ellipse([bx - br, by - br, bx + br, by + br], fill=(150, 120, 70, 16))
    for i in range(7):
        od.rectangle([i, i, pw - 1 - i, ph - 1 - i], outline=(120, 96, 56, 30 - i * 4))
    parch.alpha_composite(overlay)
    img.alpha_composite(parch, (px0, py0))

    cx = w // 2
    rule = (196, 172, 126, 255)
    d.rounded_rectangle([px0 + 8, py0 + 8, cx - 10, py1 - 8], radius=5, outline=rule, width=1)
    d.rounded_rectangle([cx + 10, py0 + 8, px1 - 8, py1 - 8], radius=5, outline=rule, width=1)
    # Centre spine, gapped vertically so title/footer read on parchment.
    sy0, sy1 = py0 + 36, py1 - 44
    d.rectangle([cx - 5, sy0, cx + 5, sy1], fill=pink)
    d.line([(cx - 2, sy0), (cx - 2, sy1)], fill=pink_hi)
    d.line([(cx + 4, sy0), (cx + 4, sy1)], fill=pink_lo)
    for sy in (sy0, sy1):
        d.ellipse([cx - 6, sy - 3, cx + 6, sy + 3], fill=pink)

    GUI_DIR.mkdir(parents=True, exist_ok=True)
    img.save(GUI_DIR / "encyclopedia_book.png")


def cleanup_retired_jar_assets():
    """Delete the retired empty-jar + per-species jarred item assets (now replaced by
    the placeable Butterfly Jar block). Keeps the generator idempotent after the removal."""
    file_patterns = [
        (JAR_MODEL_DIR, "butterfly_jar_*.geo.json"),
        (JAR_ANIMATION_DIR, "butterfly_jar.animation.json"),
        (ITEM_TEXTURE_DIR, "jarred_*.png"),
        (ITEM_TEXTURE_DIR, "butterfly_jar.png"),
        (ASSETS / "models/item", "jarred_*.json"),
        (ASSETS / "models/item", "butterfly_jar.json"),
        (ASSETS / "items", "jarred_*.json"),
        (ASSETS / "items", "butterfly_jar.json"),
        (DATA / "recipe", "butterfly_jar.json"),
    ]
    removed = 0
    for directory, pattern in file_patterns:
        if directory.exists():
            for f in directory.glob(pattern):
                f.unlink()
                removed += 1
    if JAR_TEXTURE_DIR.exists():
        for f in JAR_TEXTURE_DIR.glob("*"):
            f.unlink()
            removed += 1
        JAR_TEXTURE_DIR.rmdir()
    print(f"  removed {removed} retired jar/jarred files")


def main():
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    for spec in SPECIES:
        gen_model(spec)
    legacy_model = MODEL_DIR / "butterfly.geo.json"
    if legacy_model.exists():
        legacy_model.unlink()
    # Remove the retired jar/jarred item assets (replaced by the placeable Butterfly Jar block).
    cleanup_retired_jar_assets()

    for spec in SPECIES:
        sid, name = spec[0], spec[1]
        for legacy in (
            ASSETS / "models/item" / f"bottled_{sid}.json",
            ASSETS / "items" / f"bottled_{sid}.json",
            ITEM_TEXTURE_DIR / f"bottled_{sid}.png",
        ):
            legacy.unlink(missing_ok=True)
        gen_entity_texture(spec)
        gen_butterfly_icon(spec)
        gen_portrait(spec)
        write_json(ASSETS / "models/item" / f"butterfly_{sid}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"ironhold:item/butterfly_{sid}"},
        })
        write_json(ASSETS / "items" / f"butterfly_{sid}.json", {
            "model": {
                "type": "minecraft:model",
                "model": f"ironhold:item/butterfly_{sid}",
            },
        })

    gen_net_icon()
    write_json(ASSETS / "models/item/butterfly_net.json", {
        "parent": "minecraft:item/handheld",
        "textures": {"layer0": "ironhold:item/butterfly_net"},
    })
    write_json(ASSETS / "items/butterfly_net.json", {
        "model": {
            "type": "minecraft:model",
            "model": "ironhold:item/butterfly_net",
        },
    })
    # The encyclopedia icon is a separately authored 32px texture; do not overwrite it.
    gen_book_bg()
    write_json(ASSETS / "models/item/butterfly_encyclopedia.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "ironhold:item/butterfly_encyclopedia"},
    })
    write_json(ASSETS / "items/butterfly_encyclopedia.json", {
        "model": {
            "type": "minecraft:model",
            "model": "ironhold:item/butterfly_encyclopedia",
        },
    })
    write_json(ASSETS / "models/item/butterfly_spawn_egg.json", {
        "parent": "minecraft:item/template_spawn_egg",
    })
    write_json(ASSETS / "items/butterfly_spawn_egg.json", {
        "model": {
            "type": "minecraft:model",
            "model": "ironhold:item/butterfly_spawn_egg",
        },
    })

    lang_path = ASSETS / "lang/en_us.json"
    with lang_path.open(encoding="utf-8") as handle:
        lang = json.load(handle)
    lang["entity.ironhold.butterfly"] = "Butterfly"
    lang["item.ironhold.butterfly_net"] = "Butterfly Net"
    lang["item.ironhold.butterfly_encyclopedia"] = "Butterfly Encyclopedia"
    lang["item.ironhold.butterfly_spawn_egg"] = "Butterfly Spawn Egg"
    # Retired: empty jar item + per-species jarred items (replaced by the Butterfly Jar block).
    lang.pop("item.ironhold.butterfly_jar", None)
    for sid, name, *_ in SPECIES:
        lang.pop(f"item.ironhold.bottled_{sid}", None)
        lang.pop(f"item.ironhold.jarred_{sid}", None)
        lang[f"item.ironhold.butterfly_{sid}"] = f"{name} Butterfly"
    with lang_path.open("w", encoding="utf-8") as handle:
        json.dump(lang, handle, indent=2, ensure_ascii=False)
        handle.write("\n")

    write_json(DATA / "tags/item/fishing_bait.json", {
        "replace": False,
        "values": [f"ironhold:butterfly_{sid}" for sid, *_ in SPECIES],
    })
    print(f"Generated butterfly assets for {len(SPECIES)} species.")


if __name__ == "__main__":
    main()
