from PIL import Image
import numpy as np

for name in ['soluna_staff_sun.png', 'soluna_staff_moon.png']:
    img = Image.open(f'src/main/resources/assets/ironhold/textures/item/{name}').convert('RGBA')
    arr = np.array(img)
    h, w, c = arr.shape
    colors = set()
    for y in range(h):
        for x in range(w):
            r, g, b, a = arr[y, x]
            if a > 0:
                colors.add((r, g, b))
    
    print(f"{name} ({w}x{h}) has {len(colors)} colors")
    # Print a few brightest colors
    brightest = sorted(list(colors), key=lambda x: x[0]+x[1]+x[2], reverse=True)[:10]
    print(f"Brightest: {brightest}")
