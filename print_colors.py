from PIL import Image
import numpy as np

base_img = Image.open('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo.png').convert('RGBA')
base = np.array(base_img)

h, w, c = base.shape
for y in range(h):
    row_colors = []
    for x in range(w):
        r, g, b, a = base[y, x]
        if a > 0:
            row_colors.append((r, g, b))
    if row_colors:
        print(f"Row {y}: {row_colors[:3]} ... (total {len(row_colors)} px)")
    if y > 20:
        break
