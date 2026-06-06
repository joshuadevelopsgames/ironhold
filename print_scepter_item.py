from PIL import Image
import numpy as np

img = Image.open('src/main/resources/assets/ironhold/textures/item/arcane_scepter.png').convert('RGBA')
arr = np.array(img)
h, w, c = arr.shape
print(f"arcane_scepter.png is {w}x{h}")

for y in range(h):
    for x in range(w):
        r, g, b, a = arr[y, x]
        if a > 0:
            print(f"Row {y}, Col {x}: ({r},{g},{b})")
            break
    else:
        continue
    if y > 10:
        break
