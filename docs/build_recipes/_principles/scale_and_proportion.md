# Scale and proportion

Minecraft's block grid lies to the eye. A "1-block-tall" entity is the
player at ~1.8m, but a "1-block-tall wall" reads as ~3m. Most amateur
builds are built at the wrong scale because they trust the block count
instead of the perceived scale.

## The body-block ratio

The most useful constant: **a Minecraft block is ~1.5–2m on a side from
the player's POV**, even though the player avatar is 1.8m tall. This is
because blocks are big in the eye but the player is the camera, not the
subject.

Practical consequence: build at *half scale* compared to real-world
dimensions, and the result reads accurate.

| Real-world thing | Real size | MC size that reads right |
|------------------|-----------|--------------------------|
| Door | 2.0m tall × 0.8m wide | 2 blocks tall × 1 wide |
| Peasant cottage interior height | ~2.4m | 3 blocks |
| Great-hall interior height | 6–10m | 6 blocks (NOT 10) |
| Cathedral nave interior height | 30m | 12–15 blocks (NOT 30) |
| Castle curtain wall | 8–10m | 8–10 blocks (correct ratio because it's tall+vertical) |
| Watchtower | 15–25m | 18–25 blocks |
| Cathedral spire | 50–100m | 30–40 blocks |

The pattern: **horizontal dimensions compress**; **vertical dimensions
stay close to real**. Eye height in MC is 1.6 blocks from the floor, which
is the player at 1.6m. So vertical scale is roughly true. But horizontal
"room size" is compressed by gameplay considerations — a 30m-wide cathedral
nave in real life is 12 blocks wide in MC because that's what fits and
reads as enormous.

## Door scale is the master ratio

Every doorway in a build is the eye's calibration point. The door is
~2 blocks tall, and every other dimension scales from there.

| Door type | Width × Height | Reads as |
|-----------|----------------|----------|
| Peasant single door | 1×2 | Modest, domestic |
| Lord's chamber door | 1×2 with framing | Private but not grand |
| Great-hall double door | 2×3 | Public, ceremonial |
| Gatehouse double door | 2×3 to 3×4 | Fortified, formal |
| Cathedral west door | 3×4 to 4×5 | Awe-inspiring, divine |
| Castle gate (vehicle scale) | 3×4 to 4×6 | Carts and horses pass through |

If the door doesn't scale with the room beyond, the room misreads. A
peasant cottage with a 2×3 cathedral door looks wrong. A throne room
with a 1×2 peasant door looks wrong.

## Room dimensions by function

These dimensions are what reads "right" in Minecraft, not what's
historically accurate.

| Room | Floor (W × D) | Interior height | Notes |
|------|---------------|-----------------|-------|
| Peasant cottage main room | 5×6 to 7×7 | 3 | Single room, hearth on one wall |
| Tavern common room | 9×11 to 11×13 | 4 | Long table, hearth, stairs to rooms |
| Lord's solar | 7×9 | 4 | Bed, desk, fireplace, oriel window |
| Castle great hall | 9×15 to 13×23 | 6 | Long-axis with throne at one end, fireplace at the other |
| Throne room | 9×11 to 11×15 | 6–8 | Dais at the end, axial |
| Chapel | 7×11 to 9×15 | 6–9 | Long-axis, altar at end, pointed-arch windows |
| Cathedral nave | 11×31+ | 12–15 | Side aisles add 3 to each side |
| Watchtower interior | 5×5 or 7×7 | 4 per floor | Spiral stair in corner |
| Keep great hall | 11×11 to 13×13 | 6 | Often square, hearth on inner wall |
| Undercroft | matches the floor above | 3 | Vaulted (1-block-recessed ceiling) |
| Crypt | 5×9 to 7×11 | 4 | Sarcophagi along walls |

## The doorway pacing rule

Don't put multiple doors in a row at the same width. Each doorway should
*change scale* slightly to telegraph the importance of the room beyond:

- Outer gate (3×4) → courtyard → keep entrance (2×3) → great hall (2×3) →
  solar (1×2) → bedchamber (1×2)
- Cathedral west front (4×5) → narthex → nave doorway (3×4) → side chapel
  (1×2 each)

Each step should be visibly smaller than the one before. This compresses
the player toward more-private spaces and signals where the building's
"heart" is.

## The corridor problem

Corridors in MC tend to read as too long because the block grid makes
them feel uniform. Master builders solve this three ways:

1. **Bend the corridor**: a single 90° turn breaks the long-line tunnel
   effect
2. **Punctuate with arches**: every 4–6 blocks, the corridor has an
   archway that breaks the rhythm (use [../columns_and_arches/](../columns_and_arches/))
3. **Vary the ceiling height**: short corridor segments have 3-block
   ceilings, then an "intersection" segment with 5-block ceilings and a
   chandelier

## The window-to-wall ratio

Master builders maintain rough ratios of window opening to wall area,
varying by build type and era:

| Build type | Window-to-wall ratio | Why |
|------------|---------------------|-----|
| Norman keep | 5–10% | Defensive — arrow loops only |
| Curtain wall | 0% | No windows in a curtain wall — arrow loops at the top only |
| Watchtower | 10–15% | Slightly more, but oriented for line-of-sight |
| Castle great hall | 20–30% | Lord wanted light and view |
| Solar (lord's private) | 25–35% | Most-glazed room in a castle, with oriel |
| Peasant cottage | 5–10% | Glass was expensive; small windows |
| Tavern | 15–20% | More than a cottage; light for guests |
| Chapel | 30–50% | Light is divine; rose windows + clerestory |
| Cathedral nave wall | 40–60% | The wall is mostly window above 3 blocks |

A keep with cathedral-scale glazing reads wrong. A cathedral with peasant
windows reads wrong. Calibrate to the era and function.

## Anti-patterns

### The over-scaled door

Cathedral-sized doors on a peasant house. Either the house misreads as
ceremonial, or the door dwarfs the inhabitants visually. Match doors to
rooms.

### The flat-ceiling great hall

A great hall built at 4 blocks tall feels like a kitchen because it
matches kitchen scale. Push great-hall ceilings to 6 blocks even if
that means cutting the floor plan to compensate.

### The wall-of-glass castle

A castle with windows like a cottage on every wall reads as decorative,
not defensive. Reserve glazing for solar / great hall / chapel; keep the
outer walls mostly stone.

### The shrunken cathedral

A 7×11×6 "cathedral" reads as a chapel. Cathedrals need scale to feel
sacred: 11+ wide, 25+ long, 12+ tall minimum. If the build's footprint
budget can't sustain that, build a *chapel*, not a small cathedral.

## How to apply in a generator

```python
class Scale:
    door_unit = (1, 2)   # base unit; all openings derive from this
    great_hall = (11, 17, 6)   # w, d, h
    solar = (7, 9, 4)
    keep_footprint = (11, 11)
    keep_storey_h = 5
    keep_storeys = 4
    curtain_h = 10
    tower_h = 16

# Then every recipe consumes these proportions rather than hardcoding sizes.
```

## Apply this rule

A new generator should pick door scale first, then derive all room sizes
and openings from that. Don't choose room sizes independently; the
relationship between rooms (size ratio, doorway scaling) is what makes
a build read as designed.

## See also

- [silhouette_complexity.md](silhouette_complexity.md)
- [design_language.md](design_language.md)
- [decoration_density.md](decoration_density.md)
- [master_builders_compass.md](master_builders_compass.md)
