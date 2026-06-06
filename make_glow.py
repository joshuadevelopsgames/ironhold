from PIL import Image
import numpy as np

base_img = Image.open('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo.png').convert('RGBA')
base = np.array(base_img)

h, w, c = base.shape
glow = np.zeros((h, w, 4), dtype=np.uint8)

orb_pixels = 0
for y in range(h):
    for x in range(w):
        r, g, b, a = base[y, x]
        if a > 0:
            # We want the purple orb. Purple/Blue has B > G and B > R or at least R > G.
            # Brown has R > G > B.
            # Greys are R ~ G ~ B.
            # The orb colors printed earlier: B is always higher than G. 
            # In fact, B is almost always the highest component for the purple orb.
            # Let's say if B > G + 5, it's the orb.
            if int(b) > int(g) + 5:
                glow[y, x] = [r, g, b, a]
                orb_pixels += 1
            # What if it's the bright pink/magenta? R might be higher than B.
            elif int(r) > int(g) + 10 and int(b) > int(g) + 10:
                glow[y, x] = [r, g, b, a]
                orb_pixels += 1

Image.fromarray(glow).save('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo_glowmask.png')
print(f"Generated new glowmask with {orb_pixels} emissive pixels!")
