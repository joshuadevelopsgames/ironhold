#!/usr/bin/env python3
"""Generate a 16x16 RGBA PNG for the filcher_crown item texture."""

import os
from PIL import Image

T = (0,0,0,0)
D = (150,110,8,255)
M = (205,160,20,255)
L = (240,195,55,255)
H = (255,230,100,255)
G = (180,40,40,255)

crown_pixels = [
    # row 0
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
    # row 1
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
    # row 2 — prong tips (left, center, right)
    [T,T,T,H,T,T,T,T,T,T,T,T,H,T,T,T],
    # row 3 — gem pixels on each prong
    [T,T,T,G,T,T,T,H,H,T,T,T,G,T,T,T],
    # row 4 — upper prongs
    [T,T,T,L,T,T,T,L,L,T,T,T,L,T,T,T],
    # row 5 — prong bodies widening
    [T,T,D,M,D,T,D,M,M,D,T,D,M,D,T,T],
    # row 6 — prongs meet the band
    [T,T,D,L,D,T,D,L,L,D,T,D,L,D,T,T],
    # row 7 — top of band, full width
    [T,H,M,M,M,M,M,H,H,M,M,M,M,M,H,T],
    # row 8 — band main
    [T,L,M,M,M,M,M,M,M,M,M,M,M,M,L,T],
    # row 9 — band bottom highlight
    [T,D,M,D,D,D,D,D,D,D,D,D,D,M,D,T],
    # row 10
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
    # row 11
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
    # row 12
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
    # row 13
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
    # row 14
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
    # row 15
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
]

def main():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(crown_pixels):
        for x, color in enumerate(row):
            img.putpixel((x, y), color)

    out_path = os.path.join(
        os.path.dirname(__file__),
        "..", "src", "main", "resources", "assets", "ironhold",
        "textures", "item", "filcher_crown.png"
    )
    out_path = os.path.normpath(out_path)
    img.save(out_path)
    print(f"Saved filcher_crown.png to {out_path}")

if __name__ == "__main__":
    main()
