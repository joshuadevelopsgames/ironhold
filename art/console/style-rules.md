# Style Rules — King's Console

These rules catch ~80% of "doesn't look Minecraft" problems before they
reach the screen. Treat as non-negotiable.

## Scale

- **1 mod-pixel = 1 GUI pixel.** Sprites are painted at their final
  display size. A 16×16 icon means 16 actual pixels — no upscaling, no
  downscaling later.
- The whole screen is rendered at vanilla GUI scale. If a sprite looks
  blurry or too soft, you upscaled — start over.

## Outlines

- **1px solid black outline** on every icon and on all tax/toll/decree
  boxes. Outline color: `#2A2A2A` (palette index 0).
- Outlines are unbroken — no diagonal-only corners, no missing pixels.
- Wax seals get a 1px `#8B1F1F` (wax_red) outline instead, since pure
  black would clash with the warm tone.

## Anti-aliasing

- **Forbidden.** No alpha gradients, no soft edges. A pixel is either
  fully on a palette color or fully transparent.
- If your AI generator produces soft edges, run it through Aseprite's
  *Sprite → Color Mode → Indexed* with a locked palette to harden them,
  then hand-fix any outline gaps.

## Light source

- **Top-left** for icons and most chrome. Highlights on top + left
  edges, shadows on bottom + right edges.
- Title bar uses **top** light only (bilateral symmetry).
- Wax seals use **center-radial** light — bright spot in upper-center.

## Shading

- **Two tones max** on icons ≤ 16×16: a base color + one shadow OR one
  highlight, picked from the same hue family in the palette.
  *Example for coin: gold_mid base, gold_light highlight on top-left,
  gold_dark shadow on bottom-right outline.*
- Three tones allowed on larger panels (frame, parchment).
- Never use a status color (ok_green, bad_red) for shading. Status
  colors are *meaning*, not *form*.

## Dithering

- **Never on icons under 16×16.** A 6×6 coin has no room to dither
  without looking like noise.
- **Allowed on parchment** at low density (2px gap minimum) to imply
  paper texture.
- **Forbidden on stone frames** — stone is a solid mid + bevel only.

## Tile-ability (9-slice sprites)

Frame and parchment are painted as 9-slice tilesets:

```
+----+--------+----+
| TL |   TM   | TR |
+----+--------+----+
| ML |   MM   | MR |
+----+--------+----+
| BL |   BM   | BR |
+----+--------+----+
```

- **TL/TR/BL/BR** are fixed corner sprites. Paint them once.
- **TM/BM** must tile horizontally — left edge of the sprite must match
  right edge so multiple copies butt cleanly.
- **ML/MR** must tile vertically — top edge matches bottom edge.
- **MM** is the fill — must tile both directions.

Test by tiling 4× in each direction in Aseprite's preview. Any visible
seam = redo.

## Outline-vs-fill rule for icons

For 16×16 item-style icons, follow vanilla MC convention:

- 1px black outline forms the silhouette
- Inside the outline: base color
- 1–3 highlight pixels on the top-left edge (using the *light* palette
  shade)
- 1–2 shadow pixels on the bottom-right edge (using the *dark* shade)
- 1 specular pixel (single brightest tone) optional, placed near the
  top-left

## Forbidden visual patterns

- Gradients (linear, radial, any kind)
- Drop shadows (the "blur" kind — pixel-perfect 1-step shadows are fine)
- Glow effects
- Particle/sparkle decoration on icons
- Text rendered into a sprite (text is drawn by the screen at runtime)
- Drop-shadow on text (Minecraft's font already shadows; don't paint it in)

## Acceptance test

Before submitting a sprite, view it at 1× and 4× zoom in Aseprite:
- At 1×: silhouette readable, outline solid, no seams.
- At 4×: every pixel intentional. No mystery anti-alias pixels. Color
  count ≤ 6 per sprite (8 for chrome).
