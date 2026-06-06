#!/usr/bin/env python3
"""Generate emissive glow-mask frames for the stone golem's eyes (the "borrowed life within").

Isolates the warm amber eye pixels from the base texture and emits them as a bright additive
mask on transparent stone, with a soft bloom. Four brightness frames drive a slow "heartbeat"
pulse (swapped per-frame by StoneGolemGlowLayer): frame 0 = dim resting glow, frame 3 = bright.
Everything that is not an eye is fully transparent so only the eyes light up in the dark.
"""
import numpy as np
from PIL import Image, ImageFilter

BASE = "src/main/resources/assets/ironhold/textures/entity/stone_golem.png"
OUT = "src/main/resources/assets/ironhold/textures/entity/"

base = np.array(Image.open(BASE).convert("RGBA")).astype(int)
R, G, B, A = base[:, :, 0], base[:, :, 1], base[:, :, 2], base[:, :, 3]

# Amber eyes are (255,174,42)/(255,232,114): bright, warm, R well above B, low-ish blue.
eye = (A > 8) & (R > 150) & (G > 100) & (B < 160) & (R - B > 55) & (R >= G - 10)
print("eye pixels:", int(eye.sum()))

EMISSIVE = (255, 184, 66)          # the colour the eyes throw
h, w = eye.shape
core = np.zeros((h, w, 4), np.uint8)
core[eye] = (*EMISSIVE, 255)
core_img = Image.fromarray(core, "RGBA")

# Dilate the tiny eye pixels into a solid 2-3px core so the glow reads clearly on the model.
core_img = core_img.filter(ImageFilter.MaxFilter(3))
# Soft bloom halo so the glow doesn't read as flat pixels.
bloom = core_img.filter(ImageFilter.GaussianBlur(1.4))
glow = Image.alpha_composite(bloom, core_img)
glow = Image.alpha_composite(glow, core_img)   # double the core back over the bloom for a hot centre
glow_arr = np.array(glow).astype(float)

# Four additive-brightness frames; dim resting glow never fully dies (that is death, handled in code).
for i, fac in enumerate((0.50, 0.68, 0.84, 1.0)):
    arr = glow_arr.copy()
    arr[:, :, :3] *= fac
    out = OUT + f"stone_golem_glow_{i}.png"
    Image.fromarray(arr.clip(0, 255).astype(np.uint8), "RGBA").save(out)
    print("wrote", out)
