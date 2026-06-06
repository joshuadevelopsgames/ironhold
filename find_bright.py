from PIL import Image
import numpy as np

img = Image.open('src/main/resources/assets/ironhold/textures/item/soluna_staff_sun.png').convert('RGBA')
arr = np.array(img)
h, w, c = arr.shape

bright_pixels = []
for y in range(h):
    for x in range(w):
        r, g, b, a = arr[y, x]
        if a > 0 and r > 200:
            bright_pixels.append((x, y, r, g, b))

print(f"Found {len(bright_pixels)} bright yellow/orange pixels:")
for px in bright_pixels:
    print(f"x={px[0]}, y={px[1]} : RGB({px[2]}, {px[3]}, {px[4]})")
