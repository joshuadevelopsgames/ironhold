#!/usr/bin/env python3
"""Turn a Minecraft player skin into a carved-stone statue texture.

The statue must read as ONE stone material — vanilla stone colour + grain, fully
neutral gray, no painted-on skin colours — yet still be recognisable as a
particular person. Recognition comes from CARVED RELIEF, not colour: the skin's
light/dark detail is engraved into the stone (grooves go darker, ridges go
lighter), exactly like a real chiselled statue.

How each opaque pixel is built:
  * base   = real vanilla stone.png, tiled across the sheet (colour + grain).
  * form   = the skin's big-shape shading (normalised luminance), applied gently
             so the face/torso have volume without looking like a photo.
  * relief = the skin's fine detail (luminance minus its 3x3 blur), applied
             strongly so eyes, brows, hair strands, collars, seams, stripes read
             as carved lines.
  output = stone + form + relief, added equally to R/G/B so it stays neutral gray.

Base body layer AND outer/overlay regions are processed; transparent pixels keep
their alpha so empty overlay areas stay empty.

Usage:
    python3 scripts/stonify_skin.py INPUT.png OUTPUT.png
        [--stone-tex art/vanilla_stone16.png] [--relief 1.5] [--form 30]
        [--relief-clamp 70]
"""
import argparse


def parse_rgb(s):
    parts = [int(x) for x in s.split(",")]
    if len(parts) != 3:
        raise argparse.ArgumentTypeError("expected R,G,B")
    return tuple(parts)


def lum(r, g, b):
    return 0.299 * r + 0.587 * g + 0.114 * b


def percentile(sorted_vals, q):
    if not sorted_vals:
        return 0.0
    idx = q * (len(sorted_vals) - 1)
    lo = int(idx)
    hi = min(lo + 1, len(sorted_vals) - 1)
    frac = idx - lo
    return sorted_vals[lo] * (1 - frac) + sorted_vals[hi] * frac


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input")
    ap.add_argument("output")
    ap.add_argument("--stone-tex", default="art/vanilla_stone16.png",
                    help="tile this texture as the stone base (vanilla stone.png)")
    ap.add_argument("--relief", type=float, default=1.5,
                    help="gain on fine carved detail (edges/features)")
    ap.add_argument("--form", type=float, default=30.0,
                    help="span of gentle big-shape shading (+/- form/2)")
    ap.add_argument("--relief-clamp", type=float, default=70.0,
                    help="max magnitude of a single relief stroke")
    ap.add_argument("--alpha-thresh", type=int, default=16,
                    help="alpha below this is treated as fully empty")
    ap.add_argument("--no-flatten", action="store_true",
                    help="skip folding outer layers into the base + solidifying")
    args = ap.parse_args()

    from PIL import Image
    im = Image.open(args.input).convert("RGBA")
    w, h = im.size
    px = im.load()

    stone = Image.open(args.stone_tex).convert("RGB")
    sw, sh = stone.size
    spx = stone.load()

    # Luminance map (None where transparent).
    L = [[None] * w for _ in range(h)]
    lums = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a >= args.alpha_thresh:
                v = lum(r, g, b)
                L[y][x] = v
                lums.append(v)

    if not lums:
        im.save(args.output)
        print("no opaque pixels; copied through")
        return

    lums.sort()
    p_lo = percentile(lums, 0.03)
    p_hi = percentile(lums, 0.97)
    span = max(1e-3, p_hi - p_lo)

    def blurred(x, y):
        tot = 0.0
        n = 0
        for dy in (-1, 0, 1):
            yy = y + dy
            if yy < 0 or yy >= h:
                continue
            row = L[yy]
            for dx in (-1, 0, 1):
                xx = x + dx
                if 0 <= xx < w and row[xx] is not None:
                    tot += row[xx]
                    n += 1
        return tot / n if n else L[y][x]

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out.load()
    clampc = lambda v: 0 if v < 0 else 255 if v > 255 else int(round(v))
    rc = args.relief_clamp

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < args.alpha_thresh:
                continue
            li = L[y][x]

            # Fine carved detail (signed high-pass), gained + clamped per stroke.
            relief = (li - blurred(x, y)) * args.relief
            if relief > rc:
                relief = rc
            elif relief < -rc:
                relief = -rc

            # Gentle big-shape volume.
            lnorm = (li - p_lo) / span
            lnorm = 0.0 if lnorm < 0 else 1.0 if lnorm > 1 else lnorm
            form = (lnorm - 0.5) * args.form

            delta = relief + form
            st = spx[x % sw, y % sh]   # neutral vanilla-stone base (R==G==B)
            opx[x, y] = (
                clampc(st[0] + delta),
                clampc(st[1] + delta),
                clampc(st[2] + delta),
                a,
            )

    # ── Flatten 2nd-layer (jacket/sleeves/pants) onto the base, and solidify ──
    # The statue is rendered with a plain HumanoidModel, which only draws the
    # base body + the head/hat overlay. So fold the body/arm/leg overlays down
    # onto their base regions here, then force every base region fully opaque —
    # filling any holes (transparent base pixels, e.g. arcatheone's arm) with
    # plain stone so the statue is never see-through.
    if not args.no_flatten:
        opx2 = out.load()
        # (dst_x, dst_y, src_x, src_y, w, h)  overlay -> base
        overlay_pairs = [
            (16, 16, 16, 32, 24, 16),   # jacket -> body
            (40, 16, 40, 32, 16, 16),   # right sleeve -> right arm
            (32, 48, 48, 48, 16, 16),   # left sleeve -> left arm
            (0, 16, 0, 32, 16, 16),     # right pants -> right leg
            (16, 48, 0, 48, 16, 16),    # left pants -> left leg
        ]
        for dx, dy, sx, sy, bw, bh in overlay_pairs:
            region = out.crop((sx, sy, sx + bw, sy + bh))
            out.alpha_composite(region, (dx, dy))
        opx2 = out.load()
        # base (layer-1) UV boxes the model samples (head included; hat kept as-is)
        base_boxes = [
            (0, 0, 32, 16),    # head
            (16, 16, 24, 16),  # body
            (40, 16, 16, 16),  # right arm
            (0, 16, 16, 16),   # right leg
            (16, 48, 16, 16),  # left leg
            (32, 48, 16, 16),  # left arm
        ]
        for bx, by, bw, bh in base_boxes:
            for yy in range(by, by + bh):
                for xx in range(bx, bx + bw):
                    r, g, b, a = opx2[xx, yy]
                    if a < 250:                       # hole -> fill with stone
                        st = spx[xx % sw, yy % sh]
                        opx2[xx, yy] = (st[0], st[1], st[2], 255)
                    else:
                        opx2[xx, yy] = (r, g, b, 255)

    out.save(args.output)
    print(f"stonified(relief-gray,flattened) {w}x{h} -> {args.output}")


if __name__ == "__main__":
    main()
