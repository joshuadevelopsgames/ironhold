# Silhouette complexity

The shape of the build, read against the sky, is the first thing a viewer
sees and the last thing they remember. Master builders solve the silhouette
before they place a single texture.

## The rule

Squint at the build. If you only see a rectangle, the build will read as
amateur no matter how carefully it's textured. A serious build has at
least:

1. **Three different heights** visible in the silhouette
2. **One sharp vertical accent** (spire, tower, chimney, gable peak)
3. **One asymmetric break** (one wing taller than the other, one tower
   shorter, one corner truncated)
4. **One horizontal accent** that crosses the verticals (a curtain wall
   roofline, a cantilevered eave, a long covered walk)

Real-world buildings get these for free from history and budget; Minecraft
builds have to add them deliberately.

## The silhouette test

Before placing detail blocks, look at the structure from a distance with
the texture pack reduced to black-on-sky. Squint until the build is just
an outline. Walk the outline mentally:

- Does the outline have at least 3 different y-heights?
- Is there at least one peak / point / sharp vertical?
- Is there at least one asymmetry that can't be explained by "the camera
  is just at an angle"?
- Does the outline tell you the *function* of the building (a spire =
  sacred or watch; a row of merlons = defensive; a chimney = domestic)?

If the answer to any of those is no, the silhouette needs work *before*
any texturing.

## Common silhouettes by build type

| Build type | Required silhouette features |
|------------|------------------------------|
| Peasant house | Pitched gable peak, 1 chimney rising above ridge |
| Tavern / inn | 2 chimneys at gable ends; multiple dormers; jetty cantilever shadow |
| Watchtower | Tall vertical, conical or square cap, often visibly leaning or asymmetric |
| Village (multi-building) | Multiple peaks at different heights; one spire (church) above all |
| Castle keep | Tall square block with crenellated top; dominates a longer-but-lower curtain wall |
| Castle curtain | Long horizontal punctuated by 2–4 vertical towers |
| Gatehouse | Pair of taller verticals flanking a slightly lower middle block |
| Chapel | One spire higher than the nave; cross-shape (transept) visible from above |
| Cathedral | Two-tower west front; central spire; flying buttresses casting horizontal shadows |
| Dark castle | One central spire visible from miles; one asymmetric "scar" in the curtain wall |
| Windmill | Tall cylinder with 4 protruding sails forming an X |

## The "three heights" rule

Three distinct y-levels visible in silhouette is the minimum to read as
designed:

- **Base** — the main wall height
- **Mid** — secondary masses (lower wings, attached chapels, gate-towers)
- **Peak** — the dominant vertical (keep, spire, central tower)

A build with all three feels like it has hierarchy. A build with only base
and peak feels like a building plus a wart. A build with only base feels
like a box.

## How to add a height when the build is "too flat"

If the silhouette is too rectangular, the highest-leverage moves are:

1. **Make ONE thing taller**: pick the most-important room (great hall,
   throne, master bedroom) and let its roof poke above the surrounding
   roof line.
2. **Add a chimney**: even on a flat-roofed building, a brick chimney
   stack reaches 4+ blocks above the parapet and breaks the rectangle.
3. **Add a turret**: a 3×3 corner turret rising 4 blocks above the wall
   converts a square plan into a fortified plan.
4. **Stack a small attic dormer or a cupola**: a tiny 3×3 roof rider with
   its own micro-pitched roof, centered on the main roof.

## How to add a horizontal accent

If everything is "tall and skinny" without horizontal anchors:

1. **String-course trim**: a 1-block band of slabs all the way around the
   building at floor-level breaks (e.g., between ground and second storey)
2. **Eave overhang**: roof extends 1 block beyond the wall on all sides,
   casting a strong shadow line
3. **Cantilevered jetty**: second floor extends 1 block out from the
   ground floor (Abfielder-style)
4. **Crenellated parapet**: even on a non-defensive building, a low
   crenellated wall around a flat roof gives a horizontal rhythm

## Anti-patterns

### "The decorated box"

The build is a perfect rectangle in silhouette but has gorgeous wall
textures, intricate windows, beautiful materials. Reads as a box that
someone painted, not a designed building. Fix the silhouette before
the texture.

### "The Christmas tree"

The opposite: every roof, peak, chimney, turret, spire stacked on every
other. Reads as undecided. Pick a *dominant* vertical and let it be the
focus; everything else is secondary.

### "Mirror-symmetric"

The left half is a perfect mirror of the right half. Reads as a stage prop
or video game asset. See [asymmetry.md](asymmetry.md) for fixes.

## How to apply in a generator

```python
class Silhouette:
    base_y = 12        # the dominant wall height
    mid_y = 16         # secondary masses (one or two)
    peak_y = 22        # dominant vertical (one)
    horizontal_accent = "jetty"  # or "string_course" or "eave"
    asymmetry_source = "site"    # or "accretion" or "function"

# Then EVERY room/feature in the generator gets assigned a y-tier:
# the throne hall is at peak; the great hall is at mid; the curtain
# walls are at base. The function that places the structure must respect
# the tier of each piece.
```

## Apply this rule

A new generator must compute its silhouette *first*. Sketch base, mid,
and peak heights before any room layout. If the silhouette has fewer
than 3 heights, force one — pick the most-important room and lift its
roof.

A new generator must also pre-decide ONE asymmetric move (see
[asymmetry.md](asymmetry.md)) and one horizontal accent before placing
walls. These cannot be afterthoughts.

## See also

- [design_language.md](design_language.md)
- [asymmetry.md](asymmetry.md)
- [scale_and_proportion.md](scale_and_proportion.md)
- [master_builders_compass.md](master_builders_compass.md) — section IV.1
