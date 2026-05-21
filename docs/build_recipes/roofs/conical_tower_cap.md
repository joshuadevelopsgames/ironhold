# Conical Tower Cap — stair-block point

**Status:** v0 — observed in references, not yet built
**Provenance:** reference-image (synthesized from r04, r07, r10, r14L, r17, r22, r23)
**Best for:** corner towers, gatehouses, fantasy/Disney medieval, witch's house
**Reads as:** fairytale, vertical-aspiring, identifying landmark
**Pair with:** [pitched_gable_stair_45.md](pitched_gable_stair_45.md), corner-tower walls

## Description

A pointed cone or pyramid roof that caps a tower. Distinct from a stepped
pyramid (which has visible "steps") — this uses stair blocks angled inward
on each level so the SURFACE is a smooth 45° face, narrowing to a single
block apex.

The classic fairy-tale silhouette comes from extending the cone TALLER than
its base width — a 5×5 base typically gets a 6-7 block tall cone.

## Visual reference

A 5×5 tower base produces this cone (looking from the side):

```
            ▲           y = base + 6 (apex block)
           ▲ ▲          y = base + 5 (two stair tips)
          ▲   ▲         y = base + 4
         ▲     ▲        y = base + 3
        ▲       ▲       y = base + 2
       ▲         ▲      y = base + 1 (slope begins)
      ▒ ▒ ▒ ▒ ▒ ▒ ▒     y = base (top of tower wall, slab/roof line)
```

Each `▲` is a stair-block angled inward (south-facing stair on south side,
north-facing on north side, etc.). The interior layers may use solid blocks
or be hollow (only the outer-shell cells matter visually).

## Algorithm

```
Given a square tower top at (cx, cy, cz) with half-width R:
For each ring layer y in [base_y..base_y+R+1]:
    layer_R = R - (y - base_y)
    if layer_R <= 0:
        place apex block at (cx, y, cz); break
    For each cell (x,z) on the perimeter of the layer_R-radius square:
        Pick stair block whose slope-face points outward (away from cx, cz)
        Place at (x, y, z)
```

## Python snippet

```python
def conical_tower_cap(g, cx, base_y, cz, half_width,
                      stair_n, stair_s, stair_e, stair_w, apex_block):
    """
    Build a conical roof centered at (cx, cz), base at base_y, tower top size
    (2*half_width+1) x (2*half_width+1).

    Stair blocks must be the BOTTOM-half variants; the stair shape's high-side
    must face outward (away from the centre).
    """
    R = half_width
    for layer in range(R + 1):
        y = base_y + layer
        r = R - layer
        if r < 0:
            break
        if r == 0:
            g.setb(cx, y, cz, apex_block)
            continue
        # Perimeter of square at half-width r
        for d in range(-r, r + 1):
            # North edge (z = cz - r): stair facing north (slope up to north)
            g.setb(cx + d, y, cz - r, stair_n)
            # South edge (z = cz + r): stair facing south
            g.setb(cx + d, y, cz + r, stair_s)
            # West edge (x = cx - r), skipping corners
            if -r < d < r:
                g.setb(cx - r, y, cz + d, stair_w)
                g.setb(cx + r, y, cz + d, stair_e)
        # Corner blocks — could be full blocks or special outer-corner stairs
        # Easiest: just use the apex block style at the four corners
        for dx, dz in [(-r, -r), (r, -r), (-r, r), (r, r)]:
            g.setb(cx + dx, y, cz + dz, apex_block)
    # Optional: 1-2 more blocks above apex for a spike/lightning rod
    # (caller can add separately)
```

## Variations

- **Witch-tower / overgrown** (r11): leaves carpet or vines draped over
  one or two of the cone faces.
- **Belfry conical** (NE bell tower idea): cone sits above an open belfry
  (no walls in the top floor), pillars at corners hold the cone up.
- **Disney exaggerated**: scale the cone to 2× base width tall — even more
  fairy-tale.

## Iteration history

- v0 (2026-05-15): synthesized from 7 reference images. Untested. The
  corner-block treatment is the weakest part — using stair-block "outer
  corner" shape variants would look better than the apex_block hack.

## See also

- [../reference_builds/community_batch1_24_patterns.md](../reference_builds/community_batch1_24_patterns.md) — P2 in the catalog
- [stepped_pyramid.md](stepped_pyramid.md) (when written) — the alternative
- [pitched_gable_stair_45.md](pitched_gable_stair_45.md) — for non-tower roofs
