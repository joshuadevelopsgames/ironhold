# Pitched Gable Roof — stair-block 45° pitch

**Status:** v0 — derived from reference images, not yet built in-game
**Provenance:** reference-image (Abfielder houses via schemat.io)
**Best for:** houses, taverns, chapels, gatehouses — anything vernacular at human scale
**Reads as:** lived-in, vernacular, period-correct medieval
**Pair with:** [half_timbered_wall.md](../columns_and_arches/half_timbered_wall.md)

## Description

A two-sided pitched roof formed by stair blocks angled at 45°. The roof has
a long ridge (running along one axis) and slopes down toward the eaves on
both sides. Triangular **gable ends** close off the short ends of the
building. Unlike a stepped pyramid (which only works for towers), a gable
roof works for any rectangular footprint.

The crucial detail: stairs blocks must face *inward* (toward the ridge) so
their slope faces *outward*. The block above sits at the top half of the
cell, the block below sits at the bottom half — the boundary between them
forms the 45° face.

## Layout (cross-section of a 7-wide building, viewing along the ridge)

```
ridge →            S       <- single block, the ridge
              s         s   <- stairs, facing inward (toward ridge)
         s                s
    s                          s
beam ━━━━━━━━━━━━━━━━━━━━━━━━━━  <- top of wall (eaves line)
```

Where `s` is a stair block oriented so its slope faces the gutter side
(outward), and `S` is a single full block at the apex (or a stair-top half
if a finer point is wanted).

## Layout (gable end — viewing the triangle from outside)

```
       █             <- apex block
      sBs            <- stairs flanking, infill block in middle (B = wall)
     sBBBs
    sBBBBBs
   sBBBBBBBs
  sBBBBBBBBBs
 sBBBBBBBBBBBs        <- bottom of triangle = top of wall
```

The triangle infill (`B`) is wall material — the gable wall continues up to
the ridge.

## Block list (for a 9 × 9 × 5-tall-roof example over a 9-wide rectangular building)

- Stairs: ~`8 × 2 × N` stairs along each long-side roof slope
- Apex: 1 full block per cell along the ridge
- Gable infill: ~16 wall blocks per gable end

## Python snippet

```python
def pitched_gable_roof(g, x0, y0, z0, x1, z1, ridge_axis,
                       stair_block, apex_block, gable_block,
                       setb=None):
    """
    Build a pitched gable roof over the rectangle (x0,z0)-(x1,z1) starting
    at y0 (which should be one block above the wall top).

    ridge_axis: 'x' (ridge runs east-west) or 'z' (ridge runs north-south).
    stair_block: ID for a stair block — caller passes the correct facing
                 for each slope. We use the same stair_block ID for both
                 sides; if you need different facings you'll have to pass
                 a dict and split.
    apex_block: full block at the peak
    gable_block: wall material to fill the triangular gable ends
    setb: g.setb-style placement function (so we can be called from any
          generator)
    """
    span = (x1 - x0) if ridge_axis == 'z' else (z1 - z0)
    height = span // 2 + 1
    # Slopes
    for layer in range(height):
        y = y0 + layer
        if ridge_axis == 'z':
            wx = x0 + layer
            ex = x1 - layer
            for z in range(z0, z1 + 1):
                # Slope cells at the two outside edges of this layer
                if wx == ex:
                    setb(wx, y, z, apex_block)        # ridge
                else:
                    setb(wx, y, z, west_stair(stair_block))
                    setb(ex, y, z, east_stair(stair_block))
        else:  # ridge_axis == 'x'
            wz = z0 + layer
            ez = z1 - layer
            for x in range(x0, x1 + 1):
                if wz == ez:
                    setb(x, y, wz, apex_block)
                else:
                    setb(x, y, wz, north_stair(stair_block))
                    setb(x, y, ez, south_stair(stair_block))

    # Gable end triangles (only if ridge runs in the perpendicular axis)
    # ... left as caller responsibility for now; we'll add when actually used
```

**Note:** the snippet above is sketched. When this is built in-game the
first time, capture the exact facings used and inline the correct
palette references; right now `west_stair(...)` etc. are placeholders.

## Iteration history

- v0 (2026-05-15): derived from Abfielder house reference. Not built. The
  Python snippet is structural-only; needs the exact stair facings filled
  in on first use.

## See also

- [conical_stair_pitched.md](conical_stair_pitched.md) — for round towers
- [stepped_pyramid.md](stepped_pyramid.md) — for what NOT to use on
  vernacular buildings
- [../reference_builds/medieval_houses_abfielder.md](../reference_builds/medieval_houses_abfielder.md) — source
