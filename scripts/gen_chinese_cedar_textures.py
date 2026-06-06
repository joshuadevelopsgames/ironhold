#!/usr/bin/env python3
"""Generate Chinese Cedar wood textures for the Ironhold mod.

The block-face PATTERNS are taken from the "cherry-to-wisteria" resource pack
(textures by Aoisuzuki, vendored under scripts/wisteria_src/) and recoloured to
the Ironhold palette while preserving the original shading:

    bark  = #b59b79  (warm tan outer bark)
    wood  = #db86a2  (pink heartwood / milled wood)

Each source pixel is classified as wood (warm, R>B), bark (cool, B>=R) or
"ink" (near-black door window / outlines, kept as-is). Wood and bark pixels are
luminance-multiplied onto the target hue so the *average* wood pixel lands on
#db86a2 and the *average* bark pixel on #b59b79 — i.e. a faithful re-tint, not a
flat fill. The guillotine (a custom block with no wisteria counterpart) is still
recoloured from the birch guillotine skin, masking wood pixels so the metal
blade / rope stay metallic.
"""
import os
import numpy as np
from PIL import Image

HERE = os.path.dirname(__file__)
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "ironhold", "textures")
BLOCK = os.path.join(ROOT, "block")
ITEM = os.path.join(ROOT, "item")
WSRC = os.path.join(HERE, "wisteria_src")          # vendored wisteria pattern source

WOOD = np.array((0xd2, 0x8a, 0xa2), np.float64)    # pink heartwood (slightly desaturated from #db86a2)
BARK = np.array((0x9e, 0x80, 0x5f), np.float64)    # darker brown bark
INK_MAX = 64                                        # <= this (max channel) = window/outline, keep


def lum(rgb):
    return 0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]


def load(name):
    return np.asarray(Image.open(os.path.join(WSRC, name)).convert("RGBA")).astype(np.float64)


def save(arr, path):
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(path)
    print("wrote", os.path.relpath(path, os.path.join(HERE, "..")))


def masks(src):
    rgb, a = src[..., :3], src[..., 3]
    opaque = a > 8
    ink = opaque & (rgb.max(axis=2) <= INK_MAX)
    bark = opaque & ~ink & (rgb[..., 2] >= rgb[..., 0])     # cool  -> bark
    wood = opaque & ~ink & ~bark                            # warm  -> wood
    return opaque, ink, bark, wood


# Reference luminances: average of the pure-material source faces, so the mean
# wood/bark pixel maps exactly onto the target hue.
def reference_luminances():
    wl, bl = [], []
    for n in ("cherry_planks.png", "stripped_cherry_log.png"):
        s = load(n); _, _, _, w = masks(s)
        wl.append(lum(s[..., :3])[w])
    s = load("cherry_log.png"); _, _, b, _ = masks(s)
    bl.append(lum(s[..., :3])[b])
    return float(np.concatenate(wl).mean()), float(np.concatenate(bl).mean())


LW_REF, LB_REF = reference_luminances()
print("  wood Lref=%.1f  bark Lref=%.1f" % (LW_REF, LB_REF))


def recolor(src_name, dst_path):
    """Re-tint a wisteria face: wood->pink, bark->tan, window/outline kept."""
    s = load(src_name)
    out = s.copy()
    L = lum(s[..., :3])
    _, ink, bark, wood = masks(s)
    for ch in range(3):
        out[..., ch] = np.where(wood, WOOD[ch] * (L / LW_REF), out[..., ch])
        out[..., ch] = np.where(bark, BARK[ch] * (L / LB_REF), out[..., ch])
    # ink + transparent pixels are left untouched
    save(out, dst_path)


def gen_faces():
    recolor("cherry_planks.png",           os.path.join(BLOCK, "chinese_cedar_planks.png"))
    recolor("cherry_log.png",              os.path.join(BLOCK, "chinese_cedar_log.png"))
    recolor("cherry_log_top.png",          os.path.join(BLOCK, "chinese_cedar_log_top.png"))
    recolor("stripped_cherry_log.png",     os.path.join(BLOCK, "stripped_chinese_cedar_log.png"))
    recolor("stripped_cherry_log_top.png", os.path.join(BLOCK, "stripped_chinese_cedar_log_top.png"))
    recolor("cherry_door_top.png",         os.path.join(BLOCK, "chinese_cedar_door_top.png"))
    recolor("cherry_door_bottom.png",      os.path.join(BLOCK, "chinese_cedar_door_bottom.png"))
    recolor("cherry_trapdoor.png",         os.path.join(BLOCK, "chinese_cedar_trapdoor.png"))
    recolor("cherry_door_item.png",        os.path.join(ITEM,  "chinese_cedar_door.png"))


# ─── guillotine recolour (cedar's wood pattern is cherry-derived) ────────────
# The mod's guillotine skins each use their own wood: a frame of that wood's
# plank/log texture + a wood-tinted rope coil + a grey metal blade. Cedar shares
# cherry's pattern (via the wisteria pack), so we recolour the CHERRY guillotine:
# colourful pixels (wood / rope / outline) are luminance-multiplied onto the
# cedar pink so the mean wood pixel = WOOD; near-grey pixels (the metal blade)
# are kept untouched. Result == the cherry guillotine in cedar colours, matching
# both the cedar blocks and the other variants' construction.
def _mean_color(name):
    a = np.asarray(Image.open(os.path.join(BLOCK, name)).convert("RGBA")).astype(np.float64)
    return a[..., :3][a[..., 3] > 8].mean(axis=0)


def _retint(out, mask, L, target, lref):
    """In-place: mask pixels -> target colour scaled by their luminance / lref."""
    for ch in range(3):
        out[..., ch] = np.where(mask, target[ch] * (L / lref), out[..., ch])


# The guillotine frame is planks+stripped wood; two horizontal bands use the
# LOG (bark) texture (varies per wood — see scripts/guillotine_log_mask.png,
# derived from the shared UV layout). We re-tint plank pixels onto the cedar
# planks colour and log-band pixels onto the cedar bark colour, each preserving
# the cherry skin's grain via luminance; the grey metal blade is kept.
def recolor_guillotine_block(cherry_path, mask_path, dst_path, sat_keep=0.12, frame_lmin=100):
    s = np.asarray(Image.open(cherry_path).convert("RGBA")).astype(np.float64)
    rgb, alpha = s[..., :3], s[..., 3]
    opaque = alpha > 8
    mx = rgb.max(axis=2); mn = rgb.min(axis=2)
    sat = np.where(mx > 0, (mx - mn) / np.where(mx == 0, 1, mx), 0)
    L = lum(rgb)
    metal = opaque & (sat < sat_keep)
    logm = (np.asarray(Image.open(mask_path).convert("RGBA"))[..., 3] > 8) & opaque & ~metal
    plank = opaque & ~metal & ~logm
    plank_target, log_target = _mean_color("chinese_cedar_planks.png"), _mean_color("chinese_cedar_log.png")
    out = s.copy()
    _retint(out, plank, L, plank_target, float(L[plank & (L > frame_lmin)].mean()))
    _retint(out, logm, L, log_target, float(L[logm].mean()))
    save(out, dst_path)


# The 16x16 item icon (different layout, no mask): the dark frame posts ARE the
# log/bark colour, the lighter cross-beams are planks. Split by luminance.
def recolor_guillotine_item(cherry_path, dst_path, sat_keep=0.12, dark_lmax=95):
    s = np.asarray(Image.open(cherry_path).convert("RGBA")).astype(np.float64)
    rgb, alpha = s[..., :3], s[..., 3]
    opaque = alpha > 8
    mx = rgb.max(axis=2); mn = rgb.min(axis=2)
    sat = np.where(mx > 0, (mx - mn) / np.where(mx == 0, 1, mx), 0)
    L = lum(rgb)
    metal = opaque & (sat < sat_keep) & (L > 70)
    dark = opaque & ~metal & (L < dark_lmax)           # posts -> bark
    light = opaque & ~metal & ~dark                    # beams -> planks
    plank_target, log_target = _mean_color("chinese_cedar_planks.png"), _mean_color("chinese_cedar_log.png")
    out = s.copy()
    _retint(out, light, L, plank_target, float(L[light].mean()))
    _retint(out, dark, L, log_target, float(L[dark].mean()))
    save(out, dst_path)


def gen_guillotine():
    recolor_guillotine_block(os.path.join(BLOCK, "guillotine_cherry.png"),
                             os.path.join(HERE, "guillotine_log_mask.png"),
                             os.path.join(BLOCK, "guillotine_chinese_cedar.png"))
    recolor_guillotine_item(os.path.join(ITEM, "guillotine_cherry.png"),
                            os.path.join(ITEM, "guillotine_chinese_cedar.png"))


if __name__ == "__main__":
    gen_faces()
    gen_guillotine()
    print("done")
