"""Declarative paint DSL executor.

Reads a JSON spec (see scripts/dsl/*.dsl.json) and renders a Minecraft mob
texture atlas + optional emissive glow overlay. Designed so each pixel is
intentional — face fills are vanilla-style 4-tone organic noise (not flat),
and every visible feature (eyes, gem, gold trim, vein, etc.) is a named op
with face-local coordinates that diff cleanly in git.

Schema (top-level fields):
  out          required str   path to write the body texture PNG
  glow_out     optional str   path to write the emissive overlay PNG
  size         optional [w,h] canvas size, default [64,64]
  palette      required dict  named_color -> "#rrggbb"
  cubes        optional dict  named_cube -> {"u":,"v":,"w":,"h":,"d":}
                              auto-creates regions: <name>_top/_bottom/_front/
                                                    _back/_left/_right
  regions      optional dict  named_rect -> [x0,y0,x1,y1] (raw region override)
  faces        required list  per-face spec; see below
  glow         optional dict  same shape as a face entry, ops only

Per-face spec:
  region    str             name of cube_face or raw region
  fill      optional dict   {"palette": [deep,base,mid,high], "mode": str, "seed": int}
                            mode in {"front","top","back","side","bottom","flat"}
                            default seed=1, mode=mid
  clear     optional bool   wipe to transparent before painting (for hat front)
  ops       optional list   ordered draw operations on top of fill

Op kinds (op="..."):
  px        {"color": str, "at": [x,y] | [[x,y],...]}
  rect      {"color": str, "at": [x,y], "size": [w,h]}
  line      {"color": str, "from": [x,y], "to": [x,y]}        # H/V only
  border    {"color": str, "at": [x,y]?, "size": [w,h]?}      # 1px interior border
  scatter   {"color": str, "density": 0..1, "seed": int, "near": "top|bottom|all"}
  ribbon    {"color": str, "at": [x,y], "len": int, "axis": "h|v",
             "shadow": str?, "highlight": str?, "phase": int?}
                                                              # gold trim band
  v_collar  {"gold": str, "shadow": str, "highlight": str, "depth": int}
                                                              # cleric V-collar
  v_hem     {"gold": str, "shadow": str, "highlight": str}    # cleric V-hem
  cuff      {"gold": str, "shadow": str}                      # 2-row band at bottom
  gem       {"core": str, "deep": str, "high": str, "setting": str?, "at": [x,y]}
  curl      {"color": str, "shadow": str?, "highlight": str?, "preset": str, "at": [x,y]}
  vein      {"color": str, "bright": str?, "anchor": [x,y], "branches": [[dx,dy],...]}
  earring   {"color": str, "shadow": str, "at": [x,y]}        # 2-pixel hoop
  tusk      {"color": str, "shadow": str, "at": [x,y]}        # piglin tusk
  ear       {"color": str, "shadow": str, "highlight": str, "at": [x,y], "size": [w,h]}

Coordinates in op "at" / "from" / "to" / "anchor" are FACE-LOCAL (relative to
the face's region top-left). The executor translates to atlas coords.
"""
import json
import os
import sys
from PIL import Image


# ---------- color + hash helpers --------------------------------------------

def hex_to_rgba(s):
    s = s.lstrip("#")
    if len(s) == 6:
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)
    if len(s) == 8:
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), int(s[6:8], 16))
    raise ValueError(f"bad color {s}")


def hash_xy(x, y, seed):
    return (((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) & 0xFFFF) % 100


# ---------- region resolution ------------------------------------------------

def unwrap_cube(u, v, w, h, d):
    """Standard Minecraft cube UV unwrap → 6 face rectangles."""
    return {
        "top":    (u + d,         v,         u + d + w,         v + d),
        "bottom": (u + d + w,     v,         u + d + w + w,     v + d),
        "right":  (u,             v + d,     u + d,             v + d + h),
        "front":  (u + d,         v + d,     u + d + w,         v + d + h),
        "left":   (u + d + w,     v + d,     u + d + w + d,     v + d + h),
        "back":   (u + d + w + d, v + d,     u + d + w + d + w, v + d + h),
    }


def build_regions(spec):
    regions = {}
    for name, c in (spec.get("cubes") or {}).items():
        for face_name, rect in unwrap_cube(c["u"], c["v"], c["w"], c["h"], c["d"]).items():
            regions[f"{name}_{face_name}"] = rect
    for name, rect in (spec.get("regions") or {}).items():
        regions[name] = tuple(rect)
    return regions


# ---------- low-level paint primitives ---------------------------------------

class Canvas:
    def __init__(self, size, palette):
        self.size = size
        self.img = Image.new("RGBA", size, (0, 0, 0, 0))
        self.px = self.img.load()
        self.palette = palette  # name -> rgba

    def color(self, name):
        if name in self.palette:
            return self.palette[name]
        # Allow inline #rrggbb anywhere a palette name is expected.
        if isinstance(name, str) and name.startswith("#"):
            return hex_to_rgba(name)
        raise KeyError(f"unknown palette color: {name}")

    def set(self, x, y, c):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1]:
            self.px[x, y] = c

    def fill_rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.set(x, y, c)


# ---------- the organic 4-tone face filler -----------------------------------

def fill_face(canvas, rect, palette4, mode, seed):
    """Vanilla-style 4-tone organic dither across `rect`."""
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w <= 0 or h <= 0:
        return

    deep, base, mid, high = [canvas.color(p) for p in palette4]

    if mode == "bottom":
        canvas.fill_rect(x0, y0, x1, y1, deep)
        return
    if mode == "flat":
        canvas.fill_rect(x0, y0, x1, y1, base)
        return

    canvas.fill_rect(x0, y0, x1, y1, base)

    # Tiny faces stay nearly flat (just a back-edge shadow on sides).
    if w < 3 or h < 3:
        if mode == "side" and w >= 1:
            for y in range(y0, y1):
                if hash_xy(x1 - 1, y, seed) < 60:
                    canvas.set(x1 - 1, y, mid)
        return

    for y in range(y0, y1):
        for x in range(x0, x1):
            ly, lx = y - y0, x - x0
            r  = hash_xy(x, y, seed)
            r2 = hash_xy(x + 31, y + 17, seed + 7)

            if mode == "top":
                if   ly == 0 and r < 35: canvas.set(x, y, high)
                elif ly < 2 and r < 25: canvas.set(x, y, high)
                elif r < 18: canvas.set(x, y, mid)
                elif r > 88: canvas.set(x, y, deep)
                elif r2 < 14: canvas.set(x, y, high)
            elif mode == "back":
                if   r < 30: canvas.set(x, y, deep)
                elif r2 < 12: canvas.set(x, y, mid)
            elif mode == "side":
                if   lx >= w - 1 and r < 70: canvas.set(x, y, deep)
                elif lx == 0 and r < 30:    canvas.set(x, y, mid)
                elif r < 22: canvas.set(x, y, mid)
                elif r > 90: canvas.set(x, y, deep)
                elif r2 < 10: canvas.set(x, y, high)
            else:  # mid / front
                if   ly >= h - 2 and r < 40: canvas.set(x, y, deep)
                elif ly < 2 and r < 20:     canvas.set(x, y, high)
                elif lx == 0 or lx == w - 1:
                    if r < 55: canvas.set(x, y, deep)
                elif r < 28: canvas.set(x, y, mid)
                elif r > 92: canvas.set(x, y, deep)
                elif r2 < 14: canvas.set(x, y, high)


# ---------- ops --------------------------------------------------------------

def _to_atlas(rect, x, y):
    return rect[0] + x, rect[1] + y


def _atlas_size(rect):
    return rect[2] - rect[0], rect[3] - rect[1]


def op_px(canvas, rect, op):
    color = canvas.color(op["color"])
    at = op["at"]
    if isinstance(at[0], (int, float)):
        at = [at]
    for x, y in at:
        canvas.set(*_to_atlas(rect, int(x), int(y)), color)


def op_rect(canvas, rect, op):
    color = canvas.color(op["color"])
    x, y = op["at"]
    w, h = op["size"]
    ax, ay = _to_atlas(rect, x, y)
    canvas.fill_rect(ax, ay, ax + w, ay + h, color)


def op_line(canvas, rect, op):
    color = canvas.color(op["color"])
    x0, y0 = op["from"]
    x1, y1 = op["to"]
    if x0 == x1:
        for y in range(min(y0, y1), max(y0, y1) + 1):
            canvas.set(*_to_atlas(rect, x0, y), color)
    elif y0 == y1:
        for x in range(min(x0, x1), max(x0, x1) + 1):
            canvas.set(*_to_atlas(rect, x, y0), color)
    else:
        raise ValueError("line op must be horizontal or vertical")


def op_border(canvas, rect, op):
    color = canvas.color(op["color"])
    fw, fh = _atlas_size(rect)
    x0, y0 = op.get("at", [0, 0])
    w, h = op.get("size", [fw - x0, fh - y0])
    for x in range(x0, x0 + w):
        canvas.set(*_to_atlas(rect, x, y0),         color)
        canvas.set(*_to_atlas(rect, x, y0 + h - 1), color)
    for y in range(y0, y0 + h):
        canvas.set(*_to_atlas(rect, x0,         y), color)
        canvas.set(*_to_atlas(rect, x0 + w - 1, y), color)


def op_scatter(canvas, rect, op):
    color = canvas.color(op["color"])
    density = op.get("density", 0.1)
    seed = op.get("seed", 99)
    near = op.get("near", "all")
    fw, fh = _atlas_size(rect)
    for y in range(fh):
        for x in range(fw):
            if near == "top"    and y >= fh / 2: continue
            if near == "bottom" and y <  fh / 2: continue
            if hash_xy(x, y, seed) / 100 < density:
                canvas.set(*_to_atlas(rect, x, y), color)


def op_ribbon(canvas, rect, op):
    """1-row gold trim band with optional shadow underline."""
    color    = canvas.color(op["color"])
    shadow   = canvas.color(op["shadow"])    if "shadow"    in op else None
    highlight= canvas.color(op["highlight"]) if "highlight" in op else None
    x0, y0   = op["at"]
    length   = op["len"]
    axis     = op.get("axis", "h")
    phase    = op.get("phase", 0)
    for i in range(length):
        x, y = (x0 + i, y0) if axis == "h" else (x0, y0 + i)
        canvas.set(*_to_atlas(rect, x, y), color)
        if shadow is not None:
            sx, sy = (x, y + 1) if axis == "h" else (x + 1, y)
            canvas.set(*_to_atlas(rect, sx, sy), shadow)
    # Asymmetric break: 1 highlight pixel near front, 1 shadow at end.
    if highlight is not None and length > 2:
        hx, hy = (x0 + 1 + phase % 3, y0) if axis == "h" else (x0, y0 + 1 + phase % 3)
        canvas.set(*_to_atlas(rect, hx, hy), highlight)


def op_v_collar(canvas, rect, op):
    """Cleric-style gold V at the top of a body face."""
    gold      = canvas.color(op["gold"])
    shadow    = canvas.color(op.get("shadow",    op["gold"]))
    highlight = canvas.color(op.get("highlight", op["gold"]))
    fw, _ = _atlas_size(rect)
    depth = op.get("depth", 3)
    # Solid gold across the very top row + 1 shadow row underneath
    for x in range(fw):
        canvas.set(*_to_atlas(rect, x, 0), gold)
        canvas.set(*_to_atlas(rect, x, 1), shadow)
    canvas.set(*_to_atlas(rect, fw - 2, 1), highlight)
    canvas.set(*_to_atlas(rect, 1,      0), shadow)  # asymmetric notch
    # V opening at neckline
    cx_l = (fw // 2) - 1
    cx_r = fw // 2
    for d in range(1, depth):
        canvas.set(*_to_atlas(rect, cx_l, 1 + d), gold)
        canvas.set(*_to_atlas(rect, cx_r, 1 + d), gold)
    # Bottom of V is in shadow
    canvas.set(*_to_atlas(rect, cx_l, depth), shadow)
    canvas.set(*_to_atlas(rect, cx_r, depth), shadow)


def op_v_hem(canvas, rect, op):
    """Cleric-style gold V-hem along the bottom of the gown front."""
    gold      = canvas.color(op["gold"])
    shadow    = canvas.color(op.get("shadow",    op["gold"]))
    highlight = canvas.color(op.get("highlight", op["gold"]))
    fw, fh = _atlas_size(rect)
    hem_y = fh - 1
    for x in range(fw):
        canvas.set(*_to_atlas(rect, x, hem_y), gold)
        canvas.set(*_to_atlas(rect, x, hem_y - 1), shadow)
    # Spike-points one row up at quarters
    canvas.set(*_to_atlas(rect, 1,      hem_y - 1), gold)
    canvas.set(*_to_atlas(rect, fw - 2, hem_y - 1), gold)
    canvas.set(*_to_atlas(rect, 2,      hem_y - 2), gold)
    canvas.set(*_to_atlas(rect, fw - 3, hem_y - 2), gold)
    # Asymmetric breaks
    canvas.set(*_to_atlas(rect, 3,      hem_y),     shadow)
    canvas.set(*_to_atlas(rect, fw - 3, hem_y - 1), highlight)


def op_cuff(canvas, rect, op):
    """2-row gold cuff at the bottom of a sleeve / leg."""
    gold   = canvas.color(op["gold"])
    shadow = canvas.color(op.get("shadow", op["gold"]))
    fw, fh = _atlas_size(rect)
    for x in range(fw):
        canvas.set(*_to_atlas(rect, x, fh - 1), gold)
        canvas.set(*_to_atlas(rect, x, fh - 2), shadow)


def op_gem(canvas, rect, op):
    """2x2 gem with optional gold setting."""
    core = canvas.color(op["core"])
    deep = canvas.color(op["deep"])
    high = canvas.color(op["high"])
    setting = canvas.color(op["setting"]) if op.get("setting") else None
    x, y = op["at"]
    canvas.set(*_to_atlas(rect, x,     y),     high)
    canvas.set(*_to_atlas(rect, x + 1, y),     core)
    canvas.set(*_to_atlas(rect, x,     y + 1), core)
    canvas.set(*_to_atlas(rect, x + 1, y + 1), deep)
    if setting is not None:
        for sx, sy in [(x - 1, y), (x + 2, y), (x, y - 1), (x + 1, y - 1)]:
            canvas.set(*_to_atlas(rect, sx, sy), setting)


# Curl/swirl presets (face-local pixel offsets from "at")
CURL_PRESETS = {
    "ender_thread": [
        (0, 0), (0, 1), (1, 2), (2, 2), (2, 1), (3, 0),
    ],
    "small_swirl": [
        (0, 0), (1, 0), (2, 1), (1, 2), (0, 2), (0, 1),
    ],
    "back_sigil_c": [
        (0, 1), (1, 0), (1, 2), (2, 1),
    ],
    "neck_pendant": [
        (1, 0), (0, 1), (1, 1), (2, 1), (1, 2),
    ],
}


def op_curl(canvas, rect, op):
    color = canvas.color(op["color"])
    highlight = canvas.color(op["highlight"]) if op.get("highlight") else None
    x, y = op["at"]
    preset = CURL_PRESETS[op["preset"]]
    for dx, dy in preset:
        canvas.set(*_to_atlas(rect, x + dx, y + dy), color)
    if highlight is not None and len(preset) > 0:
        hx, hy = preset[0]
        canvas.set(*_to_atlas(rect, x + hx, y + hy), highlight)


def op_vein(canvas, rect, op):
    """Branching purple ender vein from anchor with explicit offsets."""
    color = canvas.color(op["color"])
    bright = canvas.color(op["bright"]) if op.get("bright") else None
    ax, ay = op["anchor"]
    branches = op.get("branches", [(0, 0)])
    for i, (dx, dy) in enumerate(branches):
        c = bright if (bright is not None and i == op.get("bright_idx", -1)) else color
        canvas.set(*_to_atlas(rect, ax + dx, ay + dy), c)


def op_earring(canvas, rect, op):
    """2x2 hoop earring with shadow on bottom-right."""
    color  = canvas.color(op["color"])
    shadow = canvas.color(op["shadow"])
    x, y = op["at"]
    canvas.set(*_to_atlas(rect, x,     y),     color)
    canvas.set(*_to_atlas(rect, x + 1, y),     color)
    canvas.set(*_to_atlas(rect, x,     y + 1), shadow)
    canvas.set(*_to_atlas(rect, x + 1, y + 1), color)


def op_tusk(canvas, rect, op):
    """Single 1x2 piglin tusk."""
    color  = canvas.color(op["color"])
    shadow = canvas.color(op["shadow"])
    x, y = op["at"]
    canvas.set(*_to_atlas(rect, x, y),     color)
    canvas.set(*_to_atlas(rect, x, y + 1), shadow)


def op_ear(canvas, rect, op):
    """Piglin-style protruding ear: paint as a small filled rect with shading."""
    color     = canvas.color(op["color"])
    shadow    = canvas.color(op["shadow"])
    highlight = canvas.color(op["highlight"])
    x, y = op["at"]
    w, h = op["size"]
    ax, ay = _to_atlas(rect, x, y)
    canvas.fill_rect(ax, ay, ax + w, ay + h, color)
    # Top highlight + bottom shadow
    for xi in range(w):
        canvas.set(ax + xi, ay,         highlight)
        canvas.set(ax + xi, ay + h - 1, shadow)


OPS = {
    "px":       op_px,
    "rect":     op_rect,
    "line":     op_line,
    "border":   op_border,
    "scatter":  op_scatter,
    "ribbon":   op_ribbon,
    "v_collar": op_v_collar,
    "v_hem":    op_v_hem,
    "cuff":     op_cuff,
    "gem":      op_gem,
    "curl":     op_curl,
    "vein":     op_vein,
    "earring":  op_earring,
    "tusk":     op_tusk,
    "ear":      op_ear,
}


# ---------- top-level executor -----------------------------------------------

def render(spec_path):
    spec = json.load(open(spec_path))
    size = tuple(spec.get("size", [64, 64]))
    palette = {n: hex_to_rgba(c) for n, c in spec["palette"].items()}
    regions = build_regions(spec)

    body = Canvas(size, palette)
    glow = Canvas(size, palette)

    def run_face(canvas, face):
        rect = regions[face["region"]]
        if face.get("clear"):
            x0, y0, x1, y1 = rect
            for y in range(y0, y1):
                for x in range(x0, x1):
                    canvas.px[x, y] = (0, 0, 0, 0)
        if "fill" in face:
            f = face["fill"]
            fill_face(canvas, rect, f["palette"], f.get("mode", "mid"), f.get("seed", 1))
        for op in face.get("ops", []):
            kind = op["op"]
            if kind not in OPS:
                raise ValueError(f"unknown op: {kind}")
            OPS[kind](canvas, rect, op)

    for face in spec["faces"]:
        run_face(body, face)

    body.img.save(spec["out"])

    if "glow" in spec or spec.get("glow_out"):
        for face in spec.get("glow", {}).get("faces", []):
            run_face(glow, face)
        if spec.get("glow_out"):
            glow.img.save(spec["glow_out"])

    return body.img


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: paint_dsl.py <spec.json> [<spec2.json> ...]")
        sys.exit(1)
    for p in sys.argv[1:]:
        out = render(p)
        print(f"OK  {p} → {json.load(open(p))['out']}")
