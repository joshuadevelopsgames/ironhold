#!/usr/bin/env python3
"""
Paint crown UV regions onto the filcher entity texture (filcher.png).

Minecraft UV layout for a cube at texOffs(u, v) with size W x H x D:
  Top strip   (D rows):  u+0 -> depth region, u+D -> top face (W wide)
  Side strip  (H rows):  u+0 -> right(D), u+D -> front(W), u+2D -> left(D), u+2D+W -> back(W)

Crown parts from FilcherModel:
  Band:        texOffs( 0, 55), addBox 8x1x8  (W=8, H=1, D=8)
  Front prong: texOffs(32, 55), addBox 2x3x2  (W=2, H=3, D=2)
  Left prong:  texOffs(40, 55), addBox 2x2x2  (W=2, H=2, D=2)
  Right prong: texOffs(48, 55), addBox 2x2x2  (W=2, H=2, D=2)

The texture sheet is 64x64 (but we only need rows 55-63).
"""

import os
from PIL import Image

# Gold palette
GOLD_DARK  = (150, 110,   8, 255)
GOLD_MID   = (205, 160,  20, 255)
GOLD_LITE  = (240, 195,  55, 255)
GOLD_HILIT = (255, 230, 100, 255)
GEM        = (180,  40,  40, 255)
TRANS      = (  0,   0,   0,   0)

D = GOLD_DARK
M = GOLD_MID
L = GOLD_LITE
H = GOLD_HILIT
G = GEM


def paint_band(img):
    """
    Band: texOffs(0, 55), addBox(-4,−7,−4, 8,1,8) → W=8, H=1, D=8
    Full UV region: 2*(W+D) = 32 wide, D+H = 9 tall  → x:0-31, y:55-63
    Layout:
      Top strip (D=8 rows, y:55-62):
        x0-7:   right face (D x D = 8x8 — but only D=8 wide, we show depth)
                Actually MC layout top strip: cols [right_depth | top_W | left_depth | bottom_W]
                = [8 | 8 | 8 | 8] = 32 total, 8 tall
      Side strip (H=1 row, y:63):
        [right D=8 | front W=8 | left D=8 | back W=8] = 32 wide, 1 tall
    """
    # Top face region: x:8-15, y:55-62 (8x8) — top of the band
    for row in range(8):  # y offset within top strip
        for col in range(8):  # W=8 wide
            shade = H if row == 0 else (L if row < 3 else M)
            img.putpixel((8 + col, 55 + row), shade)

    # Right face (depth strip): x:0-7, y:55-62
    for row in range(8):
        for col in range(8):
            shade = M if col < 6 else D
            img.putpixel((col, 55 + row), shade)

    # Left face (depth strip): x:16-23, y:55-62
    for row in range(8):
        for col in range(8):
            shade = D if col == 0 else M
            img.putpixel((16 + col, 55 + row), shade)

    # Bottom face: x:24-31, y:55-62
    for row in range(8):
        for col in range(8):
            img.putpixel((24 + col, 55 + row), D)

    # Side strip (H=1): y:63
    # Right: x:0-7
    for col in range(8):
        img.putpixel((col, 63), M)
    # Front: x:8-15
    for col in range(8):
        shade = H if col == 0 else (L if col < 6 else M)
        img.putpixel((8 + col, 63), shade)
    # Left: x:16-23
    for col in range(8):
        img.putpixel((16 + col, 63), M)
    # Back: x:24-31
    for col in range(8):
        img.putpixel((24 + col, 63), D)


def paint_front_prong(img):
    """
    Front prong: texOffs(32, 55), addBox(-1,-10,-3, 2,3,2) → W=2, H=3, D=2
    Full UV region: 2*(W+D) = 8 wide, D+H = 5 tall → x:32-39, y:55-59
    Top strip (D=2 rows): [right D | top W | left D | bottom W] = [2|2|2|2]=8 wide
    Side strip (H=3 rows): same width
    """
    # Top strip: y:55-56
    # right depth x:32-33
    for row in range(2):
        img.putpixel((32, 55 + row), M)
        img.putpixel((33, 55 + row), D)
    # top face x:34-35
    for row in range(2):
        img.putpixel((34, 55 + row), H if row == 0 else L)
        img.putpixel((35, 55 + row), H if row == 0 else L)
    # left depth x:36-37
    for row in range(2):
        img.putpixel((36, 55 + row), D)
        img.putpixel((37, 55 + row), M)
    # bottom x:38-39
    for row in range(2):
        img.putpixel((38, 55 + row), D)
        img.putpixel((39, 55 + row), D)

    # Side strip: y:57-59
    for row in range(3):
        y = 57 + row
        # right x:32-33
        img.putpixel((32, y), M)
        img.putpixel((33, y), D)
        # front x:34-35  — gem on top center of front face
        front_l = G if row == 0 else L
        front_r = G if row == 0 else L
        img.putpixel((34, y), front_l)
        img.putpixel((35, y), front_r)
        # left x:36-37
        img.putpixel((36, y), D)
        img.putpixel((37, y), M)
        # back x:38-39
        img.putpixel((38, y), D)
        img.putpixel((39, y), D)


def paint_side_prong(img, u_offset):
    """
    Left/Right prong: texOffs(u,55), addBox 2x2x2 → W=2, H=2, D=2
    Full UV: 8 wide, 4 tall → x:u..u+7, y:55-58
    Top strip (2 rows): [right2 | top2 | left2 | bottom2]
    Side strip (2 rows): same layout
    """
    # Top strip y:55-56
    for row in range(2):
        y = 55 + row
        # right depth
        img.putpixel((u_offset,     y), M)
        img.putpixel((u_offset + 1, y), D)
        # top face
        shade = H if row == 0 else L
        img.putpixel((u_offset + 2, y), shade)
        img.putpixel((u_offset + 3, y), shade)
        # left depth
        img.putpixel((u_offset + 4, y), D)
        img.putpixel((u_offset + 5, y), M)
        # bottom face
        img.putpixel((u_offset + 6, y), D)
        img.putpixel((u_offset + 7, y), D)

    # Side strip y:57-58
    for row in range(2):
        y = 57 + row
        img.putpixel((u_offset,     y), M)
        img.putpixel((u_offset + 1, y), D)
        front_shade = L if row == 0 else M
        img.putpixel((u_offset + 2, y), front_shade)
        img.putpixel((u_offset + 3, y), front_shade)
        img.putpixel((u_offset + 4, y), D)
        img.putpixel((u_offset + 5, y), M)
        img.putpixel((u_offset + 6, y), D)
        img.putpixel((u_offset + 7, y), D)


def main():
    tex_path = os.path.join(
        os.path.dirname(__file__),
        "..", "src", "main", "resources", "assets", "ironhold",
        "textures", "entity", "filcher.png"
    )
    tex_path = os.path.normpath(tex_path)

    img = Image.open(tex_path).convert("RGBA")
    w, h = img.size
    print(f"Opened {tex_path} ({w}x{h})")

    # The model uses a 64x64 sheet. If the image is smaller, we need to expand it.
    if h < 64:
        new_img = Image.new("RGBA", (w, 64), (0, 0, 0, 0))
        new_img.paste(img, (0, 0))
        img = new_img
        print(f"Expanded texture to {w}x64")

    paint_band(img)
    paint_front_prong(img)
    paint_side_prong(img, 40)  # left prong
    paint_side_prong(img, 48)  # right prong

    img.save(tex_path)
    print(f"Saved updated filcher.png to {tex_path}")


if __name__ == "__main__":
    main()
