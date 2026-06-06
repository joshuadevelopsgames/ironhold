from PIL import Image
import numpy as np

# Load textures
base_img = Image.open('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo.png').convert('RGBA')
glow_img = Image.open('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo_glowmask.png').convert('RGBA')

base = np.array(base_img)
glow = np.array(glow_img)

# We want to find purple/blue pixels in the base texture
# Purple is high R, high B, low G.
# Let's say if B > G + 20 and R > G + 20 it's purple? Or maybe it's more blue-purple.
# Or if B > 80 and R > 80 and G < max(R, B) - 30.

h, w, c = base.shape

for y in range(h):
    for x in range(w):
        r, g, b, a = base[y, x]
        if a > 0:
            # Check if it's not brown/yellow. Brown usually has R > G > B.
            # Purple usually has B > G and R > G.
            if b > g and r > g - 20 and (int(r)+int(b) > int(g)*1.5) and b > 40:
                glow[y, x] = [r, g, b, a]

Image.fromarray(glow).save('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo_glowmask.png')
print("Updated glowmask!")
