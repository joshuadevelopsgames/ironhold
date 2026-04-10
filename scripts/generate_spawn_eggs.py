#!/usr/bin/env python3
"""Generate mob-silhouette spawn egg textures in the modern MC 1.21.4+ style.

Each egg is a 16x16 RGBA PNG with the mob's shape and identifying features.
Style reference: vanilla eggs use an egg-shaped silhouette with the mob's key
visual traits, darker outline, highlight top-left, shadow bottom-right.
"""

from PIL import Image
import os

OUT = os.path.join(os.path.dirname(__file__),
    "..", "src", "main", "resources", "assets", "ironhold", "textures", "item")

T = (0, 0, 0, 0)  # transparent


def img_from_rows(rows):
    """Build a 16x16 RGBA image from a list of 16 rows, each a list of 16 color tuples."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            img.putpixel((x, y), c)
    return img


def save(name, rows):
    img = img_from_rows(rows)
    path = os.path.join(OUT, f"{name}_spawn_egg.png")
    img.save(path)
    print(f"  Saved {name}_spawn_egg.png")


def c(r, g, b):
    """Shorthand for opaque color."""
    return (r, g, b, 255)


# ---------- Color palettes ----------

# Arcane Mage - dark robes, purple magic accents (evoker-like shape)
def arcane_mage():
    _ = T
    # outline
    o = c(0x18, 0x0e, 0x2a)  # very dark purple
    # dark robe
    d = c(0x2a, 0x1a, 0x45)
    # mid robe
    m = c(0x3d, 0x28, 0x62)
    # light/highlight
    h = c(0x5b, 0x3a, 0x8e)
    # magic purple glow
    g = c(0x9b, 0x59, 0xd6)
    # bright accent
    a = c(0xc8, 0x7a, 0xf0)
    # skin/face
    s = c(0xbe, 0x9e, 0x80)
    # shadow
    w = c(0x12, 0x08, 0x1e)
    return [
        [_,_,_,_,_,_,o,o,o,o,_,_,_,_,_,_],
        [_,_,_,_,_,o,d,h,h,d,o,_,_,_,_,_],
        [_,_,_,_,o,d,h,h,h,h,d,o,_,_,_,_],
        [_,_,_,o,d,h,s,s,s,s,h,d,o,_,_,_],
        [_,_,_,o,m,s,s,s,s,s,s,d,w,_,_,_],
        [_,_,o,d,h,g,a,h,h,g,a,d,m,w,_,_],
        [_,_,o,m,h,h,m,m,m,m,h,m,d,w,_,_],
        [_,_,o,h,g,h,m,d,d,m,h,m,d,w,_,_],
        [_,_,o,m,g,a,d,d,d,d,m,d,d,w,_,_],
        [_,_,o,d,m,h,d,d,d,d,m,d,d,w,_,_],
        [_,_,o,d,d,m,d,d,d,d,d,m,d,w,_,_],
        [_,_,o,w,d,d,o,d,d,o,d,d,w,w,_,_],
        [_,_,_,o,w,d,o,m,m,o,d,w,w,_,_,_],
        [_,_,_,_,w,o,o,d,d,o,o,w,_,_,_,_],
        [_,_,_,_,_,w,w,w,w,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Void Invoker - very dark, void energy, similar to evoker but darker
def void_invoker():
    _ = T
    o = c(0x08, 0x04, 0x12)  # near-black outline
    d = c(0x14, 0x0a, 0x22)  # very dark purple
    m = c(0x22, 0x14, 0x3a)  # dark
    h = c(0x38, 0x20, 0x55)  # mid
    g = c(0x6a, 0x0d, 0xad)  # void purple glow
    a = c(0x9a, 0x30, 0xe0)  # bright void
    s = c(0x60, 0x50, 0x70)  # pale face
    w = c(0x05, 0x02, 0x0a)  # deep shadow
    return [
        [_,_,_,_,_,_,o,o,o,o,_,_,_,_,_,_],
        [_,_,_,_,_,o,d,h,h,d,o,_,_,_,_,_],
        [_,_,_,_,o,d,h,h,h,h,d,o,_,_,_,_],
        [_,_,_,o,d,h,s,s,s,s,h,d,o,_,_,_],
        [_,_,_,o,m,s,s,s,s,s,s,d,w,_,_,_],
        [_,_,o,d,h,g,a,h,h,g,a,d,m,w,_,_],
        [_,_,o,m,h,h,m,m,m,m,h,m,d,w,_,_],
        [_,_,o,h,g,h,m,d,d,m,h,m,d,w,_,_],
        [_,_,o,m,a,g,d,d,d,d,m,d,d,w,_,_],
        [_,_,o,d,m,h,d,d,d,d,m,d,d,w,_,_],
        [_,_,o,d,d,m,d,d,d,d,d,m,d,w,_,_],
        [_,_,o,w,d,d,o,d,d,o,d,d,w,w,_,_],
        [_,_,_,o,w,d,o,m,m,o,d,w,w,_,_,_],
        [_,_,_,_,w,o,o,d,d,o,o,w,_,_,_,_],
        [_,_,_,_,_,w,w,w,w,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Null Stalker - tall enderman-like, dark gray/ghostly, glowing white eyes
def null_stalker():
    _ = T
    o = c(0x15, 0x15, 0x1e)  # dark outline
    d = c(0x28, 0x28, 0x32)  # dark gray
    m = c(0x3c, 0x3c, 0x48)  # mid gray
    h = c(0x55, 0x55, 0x62)  # lighter gray
    e = c(0xc8, 0xc8, 0xd8)  # glowing white eyes
    g = c(0xe0, 0xe0, 0xf0)  # eye glow
    w = c(0x0a, 0x0a, 0x12)  # shadow
    return [
        [_,_,_,_,_,_,o,o,o,o,_,_,_,_,_,_],
        [_,_,_,_,_,o,m,h,h,m,o,_,_,_,_,_],
        [_,_,_,_,o,m,h,h,h,h,m,o,_,_,_,_],
        [_,_,_,o,m,h,h,h,h,h,h,d,o,_,_,_],
        [_,_,_,o,h,h,h,h,h,h,h,d,w,_,_,_],
        [_,_,o,m,h,e,g,h,h,e,g,m,d,w,_,_],
        [_,_,o,m,h,g,e,h,h,g,e,d,d,w,_,_],
        [_,_,o,d,m,h,h,d,d,h,m,d,d,w,_,_],
        [_,_,o,d,m,m,d,d,d,d,m,d,d,w,_,_],
        [_,_,_,o,d,m,d,d,d,d,m,d,w,_,_,_],
        [_,_,_,o,d,d,d,d,d,d,d,d,w,_,_,_],
        [_,_,_,o,w,d,d,d,d,d,d,w,w,_,_,_],
        [_,_,_,_,o,d,o,d,d,o,d,w,_,_,_,_],
        [_,_,_,_,w,o,_,o,o,_,o,w,_,_,_,_],
        [_,_,_,_,_,w,_,w,w,_,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Pink Deer - soft pink body, small antlers on top
def pink_deer():
    _ = T
    o = c(0x8a, 0x5a, 0x62)  # dark pink outline
    d = c(0xc0, 0x7a, 0x85)  # darker pink
    m = c(0xd8, 0x90, 0x9c)  # mid pink
    h = c(0xf0, 0xb0, 0xb8)  # light pink
    b = c(0xf8, 0xc8, 0xce)  # highlight pink
    a = c(0x9a, 0x6e, 0x5c)  # antler brown
    e = c(0x30, 0x18, 0x18)  # eye
    n = c(0xe8, 0xa0, 0xa8)  # nose
    w = c(0x70, 0x48, 0x50)  # shadow
    return [
        [_,_,_,a,_,_,_,_,_,_,_,_,a,_,_,_],
        [_,_,a,a,a,_,_,_,_,_,_,a,a,a,_,_],
        [_,_,_,a,_,_,o,o,o,o,_,_,a,_,_,_],
        [_,_,_,_,_,o,m,h,h,m,o,_,_,_,_,_],
        [_,_,_,_,o,m,b,b,b,h,m,o,_,_,_,_],
        [_,_,_,o,m,b,b,b,b,h,h,d,o,_,_,_],
        [_,_,o,m,h,e,h,b,h,e,h,d,d,w,_,_],
        [_,_,o,m,h,h,h,n,n,h,h,d,d,w,_,_],
        [_,_,o,d,m,h,h,h,h,h,m,d,d,w,_,_],
        [_,_,o,d,d,m,h,h,h,m,d,d,d,w,_,_],
        [_,_,o,d,d,d,m,m,m,d,d,d,d,w,_,_],
        [_,_,o,w,d,d,d,d,d,d,d,d,w,w,_,_],
        [_,_,_,o,w,d,o,d,d,o,d,w,w,_,_,_],
        [_,_,_,_,w,o,o,w,w,o,o,w,_,_,_,_],
        [_,_,_,_,_,w,w,_,_,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Rare Pink Deer - brighter/more vibrant pink, small sparkle
def rare_pink_deer():
    _ = T
    o = c(0x9a, 0x40, 0x58)  # vibrant dark pink
    d = c(0xd0, 0x58, 0x70)  # rich pink
    m = c(0xe8, 0x70, 0x8a)  # vibrant mid
    h = c(0xf4, 0x98, 0xb0)  # bright pink
    b = c(0xfc, 0xc0, 0xd0)  # highlight
    a = c(0xc0, 0x88, 0x60)  # golden antler
    g = c(0xff, 0xd7, 0x00)  # gold sparkle
    e = c(0x30, 0x18, 0x18)
    n = c(0xf0, 0x80, 0x98)
    w = c(0x78, 0x30, 0x44)
    return [
        [_,_,_,a,_,_,_,_,_,g,_,_,a,_,_,_],
        [_,_,a,a,a,_,_,_,_,_,_,a,a,a,_,_],
        [_,_,_,a,_,_,o,o,o,o,_,_,a,_,_,_],
        [_,_,_,_,_,o,m,h,h,m,o,_,_,_,_,_],
        [_,_,_,_,o,m,b,b,b,h,m,o,_,_,_,_],
        [_,_,_,o,m,b,b,b,b,h,h,d,o,_,_,_],
        [_,_,o,m,h,e,h,b,h,e,h,d,d,w,_,_],
        [_,_,o,m,h,h,h,n,n,h,h,d,d,w,_,_],
        [_,_,o,d,m,h,h,h,h,h,m,d,d,w,_,_],
        [_,_,o,d,d,m,h,h,h,m,d,d,d,w,_,_],
        [_,_,o,d,d,d,m,m,m,d,d,d,d,w,_,_],
        [_,_,o,w,d,d,d,d,d,d,d,d,w,w,_,_],
        [_,_,_,o,w,d,o,d,d,o,d,w,w,_,_,_],
        [_,_,_,_,w,o,o,w,w,o,o,w,_,_,_,_],
        [_,_,_,_,_,w,w,_,_,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Mom Pink Deer - larger/more mature, warmer rose pink
def mom_pink_deer():
    _ = T
    o = c(0x8a, 0x50, 0x5c)
    d = c(0xb8, 0x6e, 0x7c)
    m = c(0xd0, 0x88, 0x96)
    h = c(0xe4, 0xa4, 0xb0)
    b = c(0xf2, 0xc0, 0xc8)
    a = c(0xa0, 0x78, 0x58)  # mature antlers
    e = c(0x30, 0x18, 0x18)
    n = c(0xd8, 0x98, 0xa4)
    w = c(0x68, 0x3e, 0x48)
    return [
        [_,_,a,a,_,_,_,_,_,_,_,_,a,a,_,_],
        [_,a,a,a,a,_,_,_,_,_,_,a,a,a,a,_],
        [_,_,a,a,_,_,o,o,o,o,_,_,a,a,_,_],
        [_,_,_,_,_,o,m,h,h,m,o,_,_,_,_,_],
        [_,_,_,_,o,m,b,b,b,h,m,o,_,_,_,_],
        [_,_,_,o,m,b,b,b,b,h,h,d,o,_,_,_],
        [_,_,o,m,h,e,h,b,h,e,h,d,d,w,_,_],
        [_,_,o,m,h,h,h,n,n,h,h,d,d,w,_,_],
        [_,_,o,d,m,h,h,h,h,h,m,d,d,w,_,_],
        [_,_,o,d,d,m,h,h,h,m,d,d,d,w,_,_],
        [_,_,o,d,d,d,m,m,m,d,d,d,d,w,_,_],
        [_,_,o,w,d,d,d,d,d,d,d,d,w,w,_,_],
        [_,_,_,o,w,d,o,d,d,o,d,w,w,_,_,_],
        [_,_,_,_,w,o,o,w,w,o,o,w,_,_,_,_],
        [_,_,_,_,_,w,w,_,_,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Purple Allay - vanilla allay shape but purple (wings out to sides)
def purple_allay():
    _ = T
    o = c(0x30, 0x18, 0x48)  # dark purple outline
    d = c(0x4a, 0x28, 0x6e)  # dark purple
    m = c(0x6a, 0x3e, 0x96)  # mid purple
    h = c(0x8b, 0x5c, 0xf6)  # bright purple
    b = c(0xc4, 0xb5, 0xfd)  # lavender highlight
    g = c(0xe0, 0xd0, 0xff)  # glow
    e = c(0x10, 0x08, 0x20)  # eye
    w = c(0x20, 0x10, 0x38)  # wing shadow
    return [
        [_,_,_,_,o,o,_,_,_,_,o,o,_,_,_,_],
        [_,_,_,o,h,o,_,_,_,_,o,h,o,_,_,_],
        [o,o,o,d,h,o,_,_,_,_,o,h,d,o,o,o],
        [o,h,m,d,m,o,o,o,o,o,o,m,d,m,h,o],
        [_,w,m,d,d,o,m,h,h,m,o,d,d,m,w,_],
        [_,_,w,d,o,m,g,h,h,h,d,o,d,w,_,_],
        [_,w,o,o,o,g,g,h,h,h,m,w,o,o,w,_],
        [_,w,m,o,m,g,m,d,d,m,m,d,w,m,w,_],
        [_,_,w,w,h,h,d,d,d,d,m,d,w,w,_,_],
        [_,_,_,o,d,d,d,d,d,d,d,d,w,_,_,_],
        [_,_,_,o,d,d,d,d,d,d,d,d,w,_,_,_],
        [_,_,_,o,d,d,d,d,d,d,d,d,w,_,_,_],
        [_,_,_,_,w,d,d,d,d,d,d,w,_,_,_,_],
        [_,_,_,_,_,w,w,w,w,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Kingdom Villager - villager shape with kingdom colors (regal brown/gold)
def kingdom_villager():
    _ = T
    o = c(0x3c, 0x28, 0x14)  # dark brown outline
    d = c(0x5a, 0x3e, 0x20)  # dark brown robe
    m = c(0x7a, 0x58, 0x30)  # mid brown
    h = c(0x9a, 0x78, 0x48)  # light brown
    b = c(0xbe, 0x9e, 0x68)  # highlight tan
    g = c(0xd4, 0xb8, 0x50)  # gold accent
    s = c(0xbe, 0x9e, 0x80)  # skin
    e = c(0x20, 0x14, 0x0a)  # eye
    n = c(0xb0, 0x88, 0x68)  # nose
    w = c(0x28, 0x1a, 0x0c)  # shadow
    return [
        [_,_,_,_,_,_,o,o,o,o,_,_,_,_,_,_],
        [_,_,_,_,_,o,h,b,b,h,o,_,_,_,_,_],
        [_,_,_,_,o,h,b,b,b,b,h,o,_,_,_,_],
        [_,_,_,o,h,b,s,s,s,s,b,h,o,_,_,_],
        [_,_,_,o,m,s,s,s,s,s,s,m,w,_,_,_],
        [_,_,o,m,b,e,s,n,n,s,e,m,d,w,_,_],
        [_,_,o,m,b,s,s,n,n,s,s,d,d,w,_,_],
        [_,_,o,h,g,b,h,m,m,h,b,m,d,w,_,_],
        [_,_,o,m,g,g,m,d,d,m,h,d,d,w,_,_],
        [_,_,o,d,m,h,d,d,d,d,m,d,d,w,_,_],
        [_,_,o,d,d,m,d,d,d,d,d,m,d,w,_,_],
        [_,_,o,w,d,d,d,d,d,d,d,d,w,w,_,_],
        [_,_,_,o,w,d,o,d,d,o,d,w,w,_,_,_],
        [_,_,_,_,w,o,o,d,d,o,o,w,_,_,_,_],
        [_,_,_,_,_,w,w,w,w,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Filcher - small goblin-like, big yellow eyes, dark green/brown body
def filcher():
    _ = T
    o = c(0x12, 0x18, 0x0c)  # dark outline
    d = c(0x24, 0x30, 0x18)  # dark green-brown
    m = c(0x38, 0x48, 0x28)  # mid green
    h = c(0x50, 0x60, 0x38)  # lighter green
    b = c(0x68, 0x78, 0x48)  # highlight
    e = c(0xff, 0xee, 0x22)  # bright yellow eyes
    p = c(0xd0, 0xb8, 0x10)  # pupil/darker eye
    w = c(0x0a, 0x0e, 0x06)  # shadow
    t = c(0x44, 0x3a, 0x20)  # teeth/mouth
    return [
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
        [_,_,_,_,_,_,o,o,o,o,_,_,_,_,_,_],
        [_,_,_,_,o,o,m,h,h,m,o,o,_,_,_,_],
        [_,_,_,o,m,h,h,b,b,h,h,m,o,_,_,_],
        [_,_,o,m,h,b,b,b,b,b,h,m,d,o,_,_],
        [_,o,m,h,e,p,h,b,h,e,p,h,d,o,_,_],
        [_,o,d,h,p,e,b,h,h,p,e,d,d,w,_,_],
        [_,_,o,m,h,h,t,t,t,h,h,d,w,_,_,_],
        [_,_,o,d,m,h,h,h,h,h,m,d,w,_,_,_],
        [_,_,o,d,d,m,m,m,m,m,d,d,w,_,_,_],
        [_,_,o,d,d,d,d,d,d,d,d,d,w,_,_,_],
        [_,_,_,o,w,d,d,d,d,d,d,w,_,_,_,_],
        [_,_,_,_,o,w,o,d,d,o,w,w,_,_,_,_],
        [_,_,_,_,_,w,w,w,w,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Possessed Armor - iron armor silhouette with red glowing eyes/visor
def possessed_armor():
    _ = T
    o = c(0x3a, 0x3a, 0x40)  # dark iron outline
    d = c(0x58, 0x58, 0x60)  # dark iron
    m = c(0x78, 0x78, 0x82)  # mid iron
    h = c(0x9a, 0x9a, 0xa4)  # light iron
    b = c(0xb8, 0xb8, 0xc2)  # highlight
    r = c(0xc0, 0x20, 0x20)  # red glow (eyes)
    g = c(0xe0, 0x40, 0x30)  # bright red
    w = c(0x28, 0x28, 0x30)  # shadow
    return [
        [_,_,_,_,_,_,o,o,o,o,_,_,_,_,_,_],
        [_,_,_,_,_,o,h,b,b,h,o,_,_,_,_,_],
        [_,_,_,_,o,h,b,b,b,b,h,o,_,_,_,_],
        [_,_,_,o,m,b,b,b,b,b,h,m,o,_,_,_],
        [_,_,_,o,m,m,m,m,m,m,m,d,w,_,_,_],
        [_,_,o,d,m,r,g,m,m,r,g,d,d,w,_,_],
        [_,_,o,d,m,m,m,d,d,m,m,d,d,w,_,_],
        [_,_,o,m,b,h,m,m,m,h,b,m,d,w,_,_],
        [_,_,o,d,h,b,h,m,m,h,b,d,d,w,_,_],
        [_,_,o,d,m,h,m,d,d,m,h,d,d,w,_,_],
        [_,_,o,d,d,m,d,d,d,d,m,d,d,w,_,_],
        [_,_,o,w,d,d,d,d,d,d,d,d,w,w,_,_],
        [_,_,_,o,w,d,o,d,d,o,d,w,w,_,_,_],
        [_,_,_,_,w,o,o,d,d,o,o,w,_,_,_,_],
        [_,_,_,_,_,w,w,w,w,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Siren - teal/aquatic creature, humanoid upper body with fish tail
def siren():
    _ = T
    o = c(0x0e, 0x30, 0x2e)  # dark teal outline
    d = c(0x1a, 0x4a, 0x46)  # dark teal
    m = c(0x2a, 0x6a, 0x64)  # mid teal
    h = c(0x40, 0x8a, 0x82)  # light teal
    b = c(0x60, 0xb0, 0xa6)  # highlight
    s = c(0x90, 0xc8, 0xb8)  # skin/light
    f = c(0x18, 0x58, 0x50)  # fin/scale
    e = c(0xd0, 0x40, 0x50)  # red eyes (luring)
    w = c(0x08, 0x20, 0x1e)  # shadow
    return [
        [_,_,_,_,_,_,o,o,o,o,_,_,_,_,_,_],
        [_,_,_,_,_,o,m,h,h,m,o,_,_,_,_,_],
        [_,_,_,_,o,m,h,b,b,h,m,o,_,_,_,_],
        [_,_,_,o,m,h,s,s,s,s,h,d,o,_,_,_],
        [_,_,_,o,h,s,s,s,s,s,s,d,w,_,_,_],
        [_,_,o,d,h,e,s,s,s,e,s,d,d,w,_,_],
        [_,_,o,m,h,h,s,s,s,h,h,d,d,w,_,_],
        [_,_,o,d,m,h,h,m,m,h,m,d,d,w,_,_],
        [_,_,o,d,f,m,m,d,d,m,f,d,d,w,_,_],
        [_,_,_,o,d,f,m,m,m,f,d,d,w,_,_,_],
        [_,_,_,o,d,d,f,f,f,d,d,d,w,_,_,_],
        [_,_,_,_,o,d,d,d,d,d,d,w,_,_,_,_],
        [_,_,_,o,w,o,d,d,d,d,o,w,o,_,_,_],
        [_,_,o,w,_,_,o,w,w,o,_,_,w,o,_,_],
        [_,_,w,_,_,_,_,w,w,_,_,_,_,w,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Shipwreck Mimic - waterlogged chest with barnacles, dark wood + moss green
def shipwreck_mimic():
    _ = T
    o = c(0x28, 0x1e, 0x10)  # dark wood outline
    d = c(0x3e, 0x30, 0x1a)  # dark wood
    m = c(0x5c, 0x48, 0x28)  # mid wood
    h = c(0x78, 0x60, 0x38)  # light wood
    b = c(0x44, 0x58, 0x2e)  # barnacle/moss green
    g = c(0x58, 0x70, 0x3a)  # lighter moss
    l = c(0x8a, 0x7a, 0x48)  # lid highlight
    t = c(0xd0, 0xd0, 0xc8)  # teeth white
    r = c(0xb0, 0x30, 0x20)  # red tongue/inner
    w = c(0x18, 0x12, 0x08)  # shadow
    k = c(0x6e, 0x6e, 0x50)  # metal latch
    return [
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
        [_,_,_,_,_,o,o,o,o,o,o,_,_,_,_,_],
        [_,_,_,_,o,l,h,h,h,h,l,o,_,_,_,_],
        [_,_,_,o,h,l,b,h,h,b,h,m,o,_,_,_],
        [_,_,o,h,l,h,g,h,h,g,h,m,d,o,_,_],
        [_,_,o,m,h,k,h,h,h,h,k,d,d,w,_,_],
        [_,o,t,t,o,o,o,o,o,o,o,o,t,t,w,_],
        [_,_,o,r,r,t,r,r,r,t,r,r,o,w,_,_],
        [_,_,o,m,h,h,h,m,m,h,h,d,d,w,_,_],
        [_,_,o,d,m,h,b,m,m,b,m,d,d,w,_,_],
        [_,_,o,d,d,m,g,d,d,g,d,d,d,w,_,_],
        [_,_,o,d,d,d,b,d,d,b,d,d,d,w,_,_],
        [_,_,_,o,w,d,d,d,d,d,d,w,w,_,_,_],
        [_,_,_,_,w,o,o,o,o,o,o,w,_,_,_,_],
        [_,_,_,_,_,w,w,w,w,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Mimic - chest with teeth and tongue, wood brown
def mimic():
    _ = T
    o = c(0x3a, 0x28, 0x12)  # dark wood outline
    d = c(0x58, 0x40, 0x20)  # dark wood
    m = c(0x7a, 0x5e, 0x30)  # mid wood
    h = c(0x9a, 0x7a, 0x42)  # light wood
    b = c(0xb8, 0x98, 0x58)  # highlight
    l = c(0xc8, 0xa8, 0x68)  # lid light
    t = c(0xe8, 0xe0, 0xd0)  # teeth
    r = c(0xc0, 0x30, 0x28)  # tongue/red
    k = c(0xd0, 0xb0, 0x30)  # gold latch
    e = c(0xe0, 0xc0, 0x18)  # eye glow
    w = c(0x24, 0x18, 0x0a)  # shadow
    return [
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
        [_,_,_,_,_,o,o,o,o,o,o,_,_,_,_,_],
        [_,_,_,_,o,l,b,b,b,b,l,o,_,_,_,_],
        [_,_,_,o,b,l,h,b,b,h,b,m,o,_,_,_],
        [_,_,o,b,l,b,h,b,b,h,h,m,d,o,_,_],
        [_,_,o,m,h,k,h,h,h,h,k,d,d,w,_,_],
        [_,o,t,t,o,o,o,o,o,o,o,o,t,t,w,_],
        [_,_,o,r,r,t,r,r,r,t,r,r,o,w,_,_],
        [_,_,o,m,h,h,e,m,m,e,h,d,d,w,_,_],
        [_,_,o,d,m,h,h,m,m,h,m,d,d,w,_,_],
        [_,_,o,d,d,m,h,d,d,h,d,d,d,w,_,_],
        [_,_,o,d,d,d,d,d,d,d,d,d,d,w,_,_],
        [_,_,_,o,w,d,d,d,d,d,d,w,w,_,_,_],
        [_,_,_,_,w,o,o,o,o,o,o,w,_,_,_,_],
        [_,_,_,_,_,w,w,w,w,w,w,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


# Baby Mimic - small cute chest, same style as mimic but smaller/rounder
def baby_mimic():
    _ = T
    o = c(0x3a, 0x28, 0x12)
    d = c(0x58, 0x40, 0x20)
    m = c(0x7a, 0x5e, 0x30)
    h = c(0x9a, 0x7a, 0x42)
    b = c(0xb8, 0x98, 0x58)
    l = c(0xc8, 0xa8, 0x68)
    t = c(0xe8, 0xe0, 0xd0)  # teeth
    r = c(0xc0, 0x30, 0x28)  # tongue
    k = c(0xd0, 0xb0, 0x30)  # gold latch
    e = c(0xe0, 0xc0, 0x18)  # eye
    w = c(0x24, 0x18, 0x0a)
    return [
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
        [_,_,_,_,_,_,o,o,o,o,_,_,_,_,_,_],
        [_,_,_,_,_,o,l,b,b,l,o,_,_,_,_,_],
        [_,_,_,_,o,b,h,b,b,h,m,o,_,_,_,_],
        [_,_,_,o,h,b,k,h,h,k,h,d,o,_,_,_],
        [_,_,_,t,o,o,o,o,o,o,o,o,t,_,_,_],
        [_,_,_,o,r,t,r,r,r,t,r,r,w,_,_,_],
        [_,_,_,o,m,e,h,m,m,h,e,d,w,_,_,_],
        [_,_,_,o,d,m,h,d,d,h,m,d,w,_,_,_],
        [_,_,_,o,d,d,d,d,d,d,d,d,w,_,_,_],
        [_,_,_,_,o,w,d,d,d,d,w,w,_,_,_,_],
        [_,_,_,_,_,w,o,o,o,o,w,_,_,_,_,_],
        [_,_,_,_,_,_,w,w,w,w,_,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
        [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    ]


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    print("Generating spawn egg textures...")
    save("arcane_mage", arcane_mage())
    save("void_invoker", void_invoker())
    save("null_stalker", null_stalker())
    save("pink_deer", pink_deer())
    save("rare_pink_deer", rare_pink_deer())
    save("mom_pink_deer", mom_pink_deer())
    save("purple_allay", purple_allay())
    save("kingdom_villager", kingdom_villager())
    save("filcher", filcher())
    save("possessed_armor", possessed_armor())
    save("siren", siren())
    save("shipwreck_mimic", shipwreck_mimic())
    save("mimic", mimic())
    save("baby_mimic", baby_mimic())
    print("Done! 14 spawn egg textures generated.")
