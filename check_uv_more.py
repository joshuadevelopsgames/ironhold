from PIL import Image
import numpy as np

img = Image.open('src/main/resources/assets/ironhold/textures/item/soluna_staff_sun.png').convert('RGBA')
arr = np.array(img)

for y in range(4):
    for x in range(4):
        print(f"[{x}, {y}]: {arr[y, x]}")
