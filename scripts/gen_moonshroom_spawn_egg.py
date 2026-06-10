#!/usr/bin/env python3
"""Generate the moonshroom spawn egg texture by hue-shifting the vanilla mooshroom egg.

The moonshroom is the moon's take on the mooshroom, so its spawn egg reuses the
vanilla mooshroom egg silhouette/shading and simply recolours the red body to the
moonshroom entity's blue (textures/entity/moonshroom/moonshroom.png). The neutral
grey spots are left untouched, matching the entity's grey mushroom caps.

Source art is read straight from the Minecraft client jar in the Gradle cache, so
no vanilla asset is vendored into the repo.
"""

import colorsys
import glob
import os
import zipfile

from PIL import Image

HERE = os.path.dirname(__file__)
OUT = os.path.join(
    HERE, "..", "src", "main", "resources", "assets", "ironhold",
    "textures", "item", "moonshroom_spawn_egg.png",
)

# Sampled from textures/entity/moonshroom/moonshroom.png (dominant body swatch
# #4769A7): hue 218.7 deg, saturation ~0.58. The vanilla mooshroom red is ~0.9
# saturated, so we also scale saturation down to land on the same blue.
HUE_TARGET = 218.7 / 360.0
SAT_SCALE = 0.63

VANILLA_ENTRY = "assets/minecraft/textures/item/mooshroom_spawn_egg.png"


def find_vanilla_egg():
    """Return raw PNG bytes of the vanilla mooshroom spawn egg from the Gradle cache."""
    home = os.path.expanduser("~")
    roots = [os.path.join(home, ".gradle", "caches")]
    candidates = []
    for root in roots:
        candidates += glob.glob(os.path.join(root, "**", "client-extra.jar"), recursive=True)
        candidates += glob.glob(os.path.join(root, "**", "*client*.jar"), recursive=True)
    # newest first so we track the version the project currently builds against
    for jar in sorted(set(candidates), key=os.path.getmtime, reverse=True):
        try:
            with zipfile.ZipFile(jar) as z:
                if VANILLA_ENTRY in z.namelist():
                    return z.read(VANILLA_ENTRY)
        except zipfile.BadZipFile:
            continue
    raise SystemExit(
        "Could not find the vanilla mooshroom spawn egg in any client jar under "
        "~/.gradle/caches. Run a Gradle build first so the client jar is cached."
    )


def recolour(src):
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            # Red body -> moonshroom blue; neutral grey spots (s~0) stay grey.
            h = HUE_TARGET
            s *= SAT_SCALE
            nr, ng, nb = colorsys.hsv_to_rgb(h, s, v)
            op[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return out


if __name__ == "__main__":
    import io

    src = Image.open(io.BytesIO(find_vanilla_egg())).convert("RGBA")
    recolour(src).save(OUT)
    print(f"Saved {os.path.relpath(OUT, HERE)}")
