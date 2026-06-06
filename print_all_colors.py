from PIL import Image
import numpy as np

base_img = Image.open('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo.png').convert('RGBA')
base = np.array(base_img)

h, w, c = base.shape
colors = set()
for y in range(h):
    for x in range(w):
        r, g, b, a = base[y, x]
        if a > 0:
            colors.add((r, g, b))

# Sort colors by hue or something to group them
colors = list(colors)
colors.sort(key=lambda x: (x[2]-x[1], x[0]))

print("Found colors:", len(colors))
for color in colors:
    print(color)
