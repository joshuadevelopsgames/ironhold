#!/usr/bin/env python3
from PIL import Image
import os

TEX_DIR = "src/main/resources/assets/ironhold/textures/block"
os.makedirs(TEX_DIR, exist_ok=True)

# Generate moon_stone (simple gray noise)
img_stone = Image.new("RGBA", (16, 16), (150, 150, 150, 255))
import random
random.seed(42)
for x in range(16):
    for y in range(16):
        c = 150 + random.randint(-15, 15)
        img_stone.putpixel((x, y), (c, c, c, 255))
img_stone.save(f"{TEX_DIR}/moon_stone.png")

# Generate moon_dust (lighter gray noise)
img_dust = Image.new("RGBA", (16, 16), (180, 180, 180, 255))
for x in range(16):
    for y in range(16):
        c = 180 + random.randint(-10, 10)
        img_dust.putpixel((x, y), (c, c, c, 255))
img_dust.save(f"{TEX_DIR}/moon_dust.png")

# Generate moon_portal (semi-transparent blue)
img_portal = Image.new("RGBA", (16, 16), (50, 100, 255, 180))
for x in range(16):
    for y in range(16):
        c_r = 50 + random.randint(-20, 20)
        c_g = 100 + random.randint(-20, 20)
        c_b = 255 - random.randint(0, 30)
        img_portal.putpixel((x, y), (c_r, c_g, c_b, 180))
img_portal.save(f"{TEX_DIR}/moon_portal.png")

print("Generated moon_stone.png, moon_dust.png, and moon_portal.png")
