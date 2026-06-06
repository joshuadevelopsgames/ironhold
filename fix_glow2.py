from PIL import Image
import numpy as np

base_img = Image.open('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo.png').convert('RGBA')
glow_img = Image.open('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo_glowmask.png').convert('RGBA')

base = np.array(base_img)
glow = np.array(glow_img)

h, w, c = base.shape

# Find the first row that contains a "brown" pixel
brown_row = h
for y in range(h):
    found_brown = False
    for x in range(w):
        r, g, b, a = base[y, x]
        if a > 0:
            # Brown usually has R > G > B and some decent intensity.
            if int(r) > int(g) + 10 and int(g) > int(b) and r > 50:
                brown_row = y
                found_brown = True
                break
    if found_brown:
        break

print(f"Found brown handle starting at row {brown_row} out of {h}")

# Now make EVERYTHING above brown_row emissive if it has alpha > 0
for y in range(brown_row):
    for x in range(w):
        r, g, b, a = base[y, x]
        if a > 0:
            glow[y, x] = [r, g, b, a]

Image.fromarray(glow).save('src/main/resources/assets/ironhold/textures/item/arcane_scepter_geo_glowmask.png')
print("Updated glowmask with top orb!")
