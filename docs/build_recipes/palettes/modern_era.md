# Modern-era palette

**Status:** v0 — codified from `_principles/material_storytelling.md`
**Provenance:** claude-proposal
**Best for:** barbican, drum-tower rebuilds, the newest spire, last-generation additions
**Reads as:** fashionable, statement-making, showing off

## Description

The newest stone in a castle that's still being built. A descendant of
the founders has the wealth to rebuild the entry to impress visitors. The
"modern" era palette is showy — polished blackstone, gilded accents,
complex roof shapes — but used sparingly, only on the most-visible
additions.

## Palette

| Role | Block | Weight |
|------|-------|--------|
| Primary stone | `polished_blackstone_bricks` | 55% |
| Trim variant | `polished_deepslate` | 20% |
| Accent variant | `chiseled_polished_blackstone` | 13% |
| Tile variant | `deepslate_tiles` (floors) | 7% |
| Showpiece (sparing) | `gilded_blackstone` (corners and key blocks only) | 5% |
| Trim | `polished_blackstone_brick_slab`, `polished_blackstone_brick_stairs` | — |
| Wood | `dark_oak_log[axis=y]` | — |
| Door | `dark_oak_door` | — |
| Glass | `glass_pane` in large openings; complex window shapes (oriels, rose windows) | — |
| Accent | `lantern[hanging=true]` with `chain` drop, banners with strong color | — |

## Python snippet

```python
import random

def stone_modern():
    """Statement stone — the lord wants to impress."""
    r = random.random()
    if r < 0.55: return P("minecraft:polished_blackstone_bricks")
    if r < 0.75: return P("minecraft:polished_deepslate")
    if r < 0.88: return P("minecraft:chiseled_polished_blackstone")
    if r < 0.95: return P("minecraft:deepslate_tiles")
    return P("minecraft:gilded_blackstone")


MODERN = {
    "stone": stone_modern,
    "trim_slab": P("minecraft:polished_blackstone_brick_slab[type=top]"),
    "trim_stair": P("minecraft:polished_blackstone_brick_stairs"),
    "wood": P("minecraft:dark_oak_log[axis=y]"),
    "door": P("minecraft:dark_oak_door"),
    "glass": P("minecraft:glass_pane"),
    "hanging_lantern": P("minecraft:lantern[hanging=true]"),
    "chain": P("minecraft:chain[axis=y]"),
}
```

## How to use sparingly

The modern palette is *not* a whole-castle palette. Reserve it for:

- The barbican (the newest gatehouse, often added late)
- One rebuilt drum tower (replacing an older square tower)
- The main facade of the great hall
- A new spire added by a descendant lord

If the entire castle is built in this palette, it reads as a Disney
recreation, not a medieval castle.

## See also

- [refinement_era.md](refinement_era.md)
- [../_principles/material_storytelling.md](../_principles/material_storytelling.md)
