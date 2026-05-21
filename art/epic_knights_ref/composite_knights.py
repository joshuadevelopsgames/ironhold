"""
Composite Epic Knights armor layer textures into:
  1) 64x64 humanoid mob base skin   → OUT/{name}.png
  2) 64x32 armor layer 1 texture    → OUT/{name}_layer_1.png  (helm + body/arms/boots)
  3) 64x32 armor layer 2 texture    → OUT/{name}_layer_2.png  (legs)

Armor layer UV (64x32) — the format HumanoidArmorLayer / PLAYER_ARMOR expects:
  y=0-15:  helmet faces
  y=16-31: body + arm + boot faces

64x64 mob skin UV (for base texture):
  y=0-15:  inner head
  y=16-31: inner body (x=16-39), right arm (x=40-55), right leg (x=0-15)
  y=32-47: OUTER layer — keep TRANSPARENT or dark will show
  y=48-63: left leg (x=16-31), left arm (x=32-47)
"""
from PIL import Image
import numpy as np
import os

REF = "/Users/joshua/Kingdom SMP/ironhold/art/epic_knights_ref"
OUT = "/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity"


def load(name, tw=64, th=32):
    path = os.path.join(REF, name)
    if not os.path.exists(path):
        return None
    img = Image.open(path).convert("RGBA")
    if img.size != (tw, th):
        img = img.resize((tw, th), Image.NEAREST)
    return img


def has_pixels(img):
    if img is None:
        return False
    return np.array(img)[:, :, 3].sum() > 0


def alpha_paste(base, overlay, pos=(0, 0)):
    if overlay is None:
        return
    base.paste(overlay, pos, overlay)


def composite(helm_file, layer1_file, layer1_overlay_file,
              layer2_file, layer2_overlay_file, out_name):

    helm  = load(helm_file)
    l1    = load(layer1_file)
    l1_ov = load(layer1_overlay_file) if layer1_overlay_file else None
    l2    = load(layer2_file)
    l2_ov = load(layer2_overlay_file) if layer2_overlay_file else None

    stem = out_name.replace(".png", "")

    # ------------------------------------------------------------------ #
    # 1) MOB BASE SKIN (64x64)                                            #
    # ------------------------------------------------------------------ #
    mob = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    inner_base = Image.new("RGBA", (64, 32), (25, 25, 25, 255))
    mob.paste(inner_base, (0, 0))

    if has_pixels(helm):
        alpha_paste(mob, helm.crop((0, 0, 64, 16)), (0, 0))

    if has_pixels(l1):
        alpha_paste(mob, l1.crop((0, 16, 64, 32)), (0, 16))

    if has_pixels(l1_ov):
        body_ov = l1_ov.crop((0, 16, 64, 32))
        alpha_paste(mob, body_ov, (0, 16))
        head_ov = l1_ov.crop((0, 0, 64, 16))
        if np.array(head_ov)[:, :, 3].sum() > 0:
            alpha_paste(mob, head_ov, (0, 0))

    rleg_img = None
    if has_pixels(l2):
        rleg_img = l2.crop((0, 16, 16, 32))
        if np.array(rleg_img)[:, :, 3].sum() > 0:
            alpha_paste(mob, rleg_img, (0, 16))
        if has_pixels(l2_ov):
            rleg_ov = l2_ov.crop((0, 16, 16, 32))
            if np.array(rleg_ov)[:, :, 3].sum() > 0:
                alpha_paste(mob, rleg_ov, (0, 16))

    if has_pixels(l2):
        lleg = l2.crop((16, 16, 32, 32))
        if np.array(lleg)[:, :, 3].sum() > 0:
            alpha_paste(mob, lleg, (16, 48))
        elif rleg_img is not None and np.array(rleg_img)[:, :, 3].sum() > 0:
            alpha_paste(mob, rleg_img.transpose(Image.FLIP_LEFT_RIGHT), (16, 48))

    if has_pixels(l1):
        rarm = l1.crop((40, 16, 56, 32))
        alpha_paste(mob, rarm.transpose(Image.FLIP_LEFT_RIGHT), (32, 48))
        if has_pixels(l1_ov):
            rarm_ov = l1_ov.crop((40, 16, 56, 32))
            if np.array(rarm_ov)[:, :, 3].sum() > 0:
                alpha_paste(mob, rarm_ov.transpose(Image.FLIP_LEFT_RIGHT), (32, 48))

    mob.save(os.path.join(OUT, out_name))
    print(f"  ✓ {out_name}")

    # ------------------------------------------------------------------ #
    # 2) ARMOR LAYER 1 (64x32) — helm + body/arms/boots                  #
    #    This is the texture fed to HumanoidArmorLayer / PLAYER_ARMOR.   #
    #    y=0-15 = head armor, y=16-31 = body+arms+boots armor            #
    # ------------------------------------------------------------------ #
    armor1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    # Head region from helm
    if has_pixels(helm):
        alpha_paste(armor1, helm.crop((0, 0, 64, 16)), (0, 0))

    # Body/arms/boots region from layer_1
    if has_pixels(l1):
        alpha_paste(armor1, l1.crop((0, 16, 64, 32)), (0, 16))

    # Overlays on top
    if has_pixels(l1_ov):
        body_ov = l1_ov.crop((0, 16, 64, 32))
        alpha_paste(armor1, body_ov, (0, 16))
        head_ov = l1_ov.crop((0, 0, 64, 16))
        if np.array(head_ov)[:, :, 3].sum() > 0:
            alpha_paste(armor1, head_ov, (0, 0))

    armor1_name = f"{stem}_layer_1.png"
    armor1.save(os.path.join(OUT, armor1_name))
    print(f"  ✓ {armor1_name}")

    # ------------------------------------------------------------------ #
    # 3) ARMOR LAYER 2 (64x32) — legs only                               #
    #    y=16-31 contains both right and left leg UV.                     #
    # ------------------------------------------------------------------ #
    armor2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    if has_pixels(l2):
        alpha_paste(armor2, l2.crop((0, 16, 64, 32)), (0, 16))
        if has_pixels(l2_ov):
            leg_ov = l2_ov.crop((0, 16, 64, 32))
            alpha_paste(armor2, leg_ov, (0, 16))

    armor2_name = f"{stem}_layer_2.png"
    armor2.save(os.path.join(OUT, armor2_name))
    print(f"  ✓ {armor2_name}")


print("Compositing knight mob textures + armor layers...")

variants = [
    # helmet                   layer1                   layer1_overlay                    layer2                   layer2_overlay                  output
    ("kettlehat_layer_1.png",  "gambeson_layer_1.png",  "gambeson_layer_1_overlay.png",   "gambeson_layer_2.png",  None,                           "knight_recruit.png"),
    ("bascinet_layer_1.png",   "chainmail_layer_1.png", None,                             "chainmail_layer_2.png", None,                           "knight_man_at_arms.png"),
    ("barbute_layer_1.png",    "brigandine_layer_1.png","brigandine_layer_1_overlay.png", "gambeson_layer_2.png",  None,                           "knight_crossbowman.png"),
    ("bascinet_layer_1.png",   "knight_layer_1.png",    None,                             "knight_layer_2.png",    None,                           "knight_armored.png"),
    ("greathelm_layer_1.png",  "crusader_layer_1.png",  "crusader_layer_1_overlay.png",   "crusader_layer_2.png",  "crusader_layer_2_overlay.png", "knight_crusader.png"),
    ("gold_armet_layer_1.png", "gothic_layer_1.png",    None,                             "gothic_layer_2.png",    None,                           "knight_gothic.png"),
    ("gold_armet_layer_1.png", "gold_knight_layer_1.png",None,                            "gold_knight_layer_2.png",None,                          "knight_gold.png"),
]

for args in variants:
    try:
        composite(*args)
    except Exception as e:
        import traceback
        print(f"  ✗ {args[-1]}: {e}")
        traceback.print_exc()

print("Done.")
