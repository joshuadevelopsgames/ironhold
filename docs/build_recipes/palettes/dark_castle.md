# Dark castle palette

**Status:** v0 — synthesized for the dark-castle archetype
**Provenance:** claude-proposal
**Best for:** the villain's seat, an evil keep, a dread-faction stronghold
**Reads as:** austere, malevolent, *not* cartoonish — restraint is the point

## Description

The palette for a "dark castle." Distinct from a normal castle palette
by:
- Total commitment to blackstone + deepslate (no warm stone)
- No moss (moss reads as life — wrong feeling here)
- Soul-fire lighting throughout
- Gilded accents ONLY in the throne room, not the curtain wall
- Decay treatment is higher than normal (~12% cracked variant)

The goal: a viewer reads "evil" from the absence of warmth, not from
horror props.

## Palette

| Role | Block | Weight |
|------|-------|--------|
| Primary stone | `polished_blackstone_bricks` | 50% |
| Crust variant | `cobbled_deepslate` | 18% |
| Crack variant | `cracked_polished_blackstone_bricks` | 12% |
| Cracked-deeper | `cracked_deepslate_bricks` | 10% |
| Tile variant (floors) | `deepslate_tiles` | 8% |
| Showpiece (throne room ONLY) | `gilded_blackstone` | 2% |
| Trim | `polished_blackstone_brick_slab[type=top]`, `polished_blackstone_brick_stairs` | — |
| Wood | `dark_oak_log[axis=y]`, `dark_oak_planks` | — |
| Door | `dark_oak_door` (banded; gate uses iron_bars portcullis layer too) | — |
| Glass | NONE in the curtain wall; `tinted_glass` in the throne-room oriel only | — |
| Primary light | `soul_torch`, `soul_lantern[hanging=true]` | — |
| Accent light | `red_candle[lit=true]`, `black_candle[lit=true]` | — |
| Banner color | `black_banner` with one accent stripe in `red_banner` or `purple_banner` | — |
| Decay accents | `cobweb`, occasional `wither_skeleton_skull[powered=false]` on a fence post | — |

## Python snippet

```python
import random

def stone_dark_castle():
    """Austere blackstone palette. No moss; decay-heavy."""
    r = random.random()
    if r < 0.50: return P("minecraft:polished_blackstone_bricks")
    if r < 0.68: return P("minecraft:cobbled_deepslate")
    if r < 0.80: return P("minecraft:cracked_polished_blackstone_bricks")
    if r < 0.90: return P("minecraft:cracked_deepslate_bricks")
    if r < 0.98: return P("minecraft:deepslate_tiles")
    return P("minecraft:gilded_blackstone")


DARK_CASTLE = {
    "stone": stone_dark_castle,
    "trim_slab": P("minecraft:polished_blackstone_brick_slab[type=top]"),
    "trim_stair": P("minecraft:polished_blackstone_brick_stairs"),
    "wood": P("minecraft:dark_oak_log[axis=y]"),
    "door": P("minecraft:dark_oak_door"),
    "throne_glass": P("minecraft:tinted_glass"),

    "primary_light": P("minecraft:soul_torch"),
    "hanging_light": P("minecraft:soul_lantern[hanging=true]"),
    "altar_candle": P("minecraft:red_candle[lit=true,candles=3]"),
    "private_candle": P("minecraft:black_candle[lit=true,candles=1]"),

    "banner_main": P("minecraft:black_banner"),
    "banner_accent": P("minecraft:red_banner"),

    "decay_web": P("minecraft:cobweb"),
    "decay_skull": P("minecraft:wither_skeleton_skull"),
}
```

## Rules of restraint

These are the "don't" rules — fail any of these and the castle reads as
parody:

1. **Never use purple wool / concrete / glazed-terracotta** as bulk material
2. **Never put a giant skull on the gate.** One skull on a fence post by the
   gate is enough.
3. **Never use end-stone or end-rods as bulk material.** These are for a
   single magical focal point (the throne or an altar), not walls.
4. **Never mix soul-fire and normal-fire lighting** *except* the great-hall
   hearth (a deliberate single exception — see castles_grand_study.md §7)
5. **Never make the gate a "scary" shape.** Make it a *competent* shape
   built in the wrong materials. The horror is the austerity, not the prop.
6. **Always include one collapsed wall section.** A perfectly intact dark
   castle reads as new construction; a 50%-ruined castle reads as just a
   ruin. The sweet spot is ~10% damage concentrated in one signature scar.
7. **No decoration on the outer curtain.** The dark castle's exterior is
   *bare*. Banners and decoration live on the *interior* facing the
   courtyard, where the inhabitants see them.

## See also

- [../reference_builds/castles_grand_study.md](../reference_builds/castles_grand_study.md) §7
- [../_principles/lighting_dramaturgy.md](../_principles/lighting_dramaturgy.md)
- [../_principles/material_storytelling.md](../_principles/material_storytelling.md)
