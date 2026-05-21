# Brick Chimney Stack

**Status:** v0 — observed in references, not yet built
**Provenance:** reference-image (universal across r00, r02, r07, r08, r16, r22, r23 and Abfielder builds)
**Best for:** any vernacular building with a fireplace, especially houses and taverns
**Reads as:** lived-in, working hearth, period-correct
**Pair with:** [../fireplaces/](../fireplaces/) interior fireplace recipes

## Description

A narrow vertical brick stack projecting from the roof of a building.
Visually identifies "there's a fireplace here." Almost universally present
in player-built medieval houses — its absence reads immediately as "this
isn't a real house."

Three crucial details:

1. **Material is brick or red-tinted** — distinctly different from the
   wall stone and roof material. Most commonly `minecraft:bricks`. Tudor-style
   builds use red-tinted blocks like `red_terracotta` or `red_concrete` for
   warmer tones.
2. **It has a cap** — top course is usually `chiseled_stone_bricks`,
   `chiseled_polished_blackstone`, or a stone slab — distinct from the
   bricks below.
3. **It extends well above the roof line** — at least 3-4 blocks above where
   it emerges from the roof. Squat chimneys look wrong.

## Layout

```
y+8     [cap]              <- chiseled block or slab top
y+7     [brick]
y+6     [brick]
y+5     [brick]            <- well above roof line
y+4     [brick]
y+3     [brick]   ── roof line ──
y+2     [brick]            <- emerges from roof
y+1     [brick]
y+0     [brick]            <- attached to a wall or rising from inside
```

## Variations

- **Single chimney mid-roof** (r08, r18): one chimney rising through the
  ridge or centre of the roof
- **Twin chimneys at gable ends** (Abfielder tavern, r00): two chimneys
  symmetric at each gable peak — implies a great hall with hearths at
  both ends
- **Multiple chimneys** (r07, r22): 3-4 chimneys staggered across the
  roof — implies a multi-room house with separate fireplaces per room
- **External wall chimney** (r18): runs UP THE OUTSIDE WALL of the building
  rather than through the roof. Stone-based, brick top. Distinctive feature
  for cold-climate cottages.
- **Chimney with smoke**: in MC, a `campfire` block embedded in the cap
  produces continuous smoke particles. Adds life.

## Python snippet

```python
def brick_chimney_stack(g, cx, base_y, cz, top_y, *,
                         brick_block, cap_block, smoke=False):
    """
    Build a single-block chimney from (cx, base_y, cz) up to top_y.
    The top block is cap_block; everything below is brick_block.
    If smoke=True, embed a campfire in the cap_block cell for animated smoke.
    """
    for y in range(base_y, top_y):
        g.setb(cx, y, cz, brick_block)
    if smoke:
        # Campfire produces smoke. It sits in the cap cell; cap_block is
        # placed in the cell above the campfire as a "pot lip" so it reads
        # as a chimney top rather than a campfire on a roof.
        g.setb(cx, top_y, cz, P("minecraft:campfire[facing=north,lit=true,signal_fire=true,waterlogged=false]"))
        # signal_fire=true makes the smoke rise extra-high
    else:
        g.setb(cx, top_y, cz, cap_block)


def twin_gable_chimneys(g, x0, x1, z_ridge, base_y, top_y, **kwargs):
    """Two chimneys, one at each gable peak (matching pattern in Abfielder tavern)."""
    brick_chimney_stack(g, x0, base_y, z_ridge, top_y, **kwargs)
    brick_chimney_stack(g, x1, base_y, z_ridge, top_y, **kwargs)
```

## Rule of thumb

- Houses up to 7×7 footprint: 1 chimney
- 7×7 to 12×12: 2 chimneys (often at opposing gable ends)
- Larger / mansion-scale: 3-4 chimneys, irregularly placed (NOT symmetric)

## Iteration history

- v0 (2026-05-15): synthesized from 7+ reference images. Untested.
  Smoke effect (campfire embed) is theoretical — needs in-game verification.

## See also

- [../reference_builds/community_batch1_24_patterns.md](../reference_builds/community_batch1_24_patterns.md) — P21
