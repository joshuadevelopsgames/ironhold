#!/usr/bin/env python3
from PIL import Image
import os
import random

TEX_DIR = "src/main/resources/assets/ironhold/textures/environment"
os.makedirs(TEX_DIR, exist_ok=True)

# Generate earth_phases.png (128x64 atlas of 8 earth phases)
img_atlas = Image.new("RGBA", (128, 64), (0, 0, 0, 0))

def draw_earth(img, ox, oy, phase_idx):
    # Draw soft blue glow
    for x in range(32):
        for y in range(32):
            dx = abs(x - 15.5)
            dy = abs(y - 15.5)
            dist = max(dx, dy)
            if 8 < dist <= 14:
                alpha = int(120 * (14 - dist) / 6.0)
                img.putpixel((ox+x, oy+y), (0, 80, 255, alpha))
                
    # Draw 16x16 square pixel art in the center (8x8 scaled by 2)
    random.seed(phase_idx + 42)
    
    for r in range(8):
        for c in range(8):
            val = random.random()
            # Green land, Blue ocean
            color = (40, 200, 40, 255) if val > 0.65 else (20, 100, 255, 255)
            
            # Draw 2x2 pixels for chunky look
            for i in range(2):
                for j in range(2):
                    px = ox + 8 + c * 2 + i
                    py = oy + 8 + r * 2 + j
                    img.putpixel((px, py), color)

# 8 phases
for i in range(8):
    col = i % 4
    row = i // 4
    draw_earth(img_atlas, col * 32, row * 32, i)

img_atlas.save(f"{TEX_DIR}/earth_phases.png")
print("Generated earth_phases.png")
