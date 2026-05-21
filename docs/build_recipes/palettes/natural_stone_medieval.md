# Natural-stone medieval palette

**Status:** v1 — block-truthed from Onctaaf's castle (332-entry palette)
**Provenance:** reference-image (extracted from `structures/external/onctaaf_castle.litematic` block counts)
**Best for:** stately castle, royal seat, peace-time fortified manor, a great noble's residence
**Reads as:** light gray, weathered, warmly lit, NOT a dark/evil keep
**Pair with:** [../reference_builds/onctaaf_castle_study.md](../reference_builds/onctaaf_castle_study.md)

## Description

The dominant castle palette in the master-builder corpus that I was
*completely missing*: an **andesite + smooth_stone + stone_bricks**
build with heavy slab trim, moss + cracked stone for aging, and
*zero* deepslate. This is the "natural medieval" palette — the
opposite stylistic pole from `dark_castle.md`.

Distinguishing features:
- Andesite (raw + polished) is **a primary material**, not an accent
- Slab cells outnumber stair cells ~10:1 (the slab-trim signature)
- Moss + cracked together carry the weathering (not cracked alone)
- Lighting is torch-dominant; lanterns are reserved
- ~0% glass — the windows are arrow loops or simply unglazed

## Palette

Block weights below are derived from Onctaaf's actual usage,
normalized so the wall fabric is the sum:

| Role | Block | Weight (of wall fabric) |
|------|-------|------------------------|
| Primary fill stone | `stone` | 35% |
| Primary brick | `stone_bricks` | 14% |
| Smooth surface | `smooth_stone` | 13% |
| Natural variant 1 | `andesite` | 12% |
| Natural variant 2 | `polished_andesite` | 10% |
| Modern variant (1.21) | `tuff_bricks` | 8% |
| Crack-and-age | `cracked_stone_bricks` | 5% |
| Old-section variant | `cobblestone` | 3% |

| Role | Block |
|------|-------|
| Primary trim slab | `smooth_stone_slab[type=top]` |
| Secondary trim slab | `stone_brick_slab[type=top]` |
| Modern trim slab | `tuff_brick_slab[type=top]` |
| Andesite trim slab | `polished_andesite_slab[type=top]` |
| Stair (used sparingly) | `polished_andesite_stairs`, `stone_brick_stairs` |
| Wall blocks | `stone_brick_wall`, `cobblestone_wall` |
| Moss accent | `moss_block` |
| Vine accent | `vine[north=true]` etc. |
| Wood (sparing) | `dark_oak_log[axis=y]`, `stripped_oak_log[axis=y]` |
| Wood trim | `dark_oak_trapdoor`, `spruce_trapdoor` (battens) |
| Glass (chapel/solar only) | `white_stained_glass` |
| Primary light | `torch` (yellow), `wall_torch` |
| Accent light | `lantern[hanging=true]` |
| Smoke effect | `campfire[lit=true,signal_fire=false]` |
| Terrain podium | `dirt`, `coarse_dirt`, `grass_block`, `moss_block` |
| Water feature | `water[level=0]` (moats, lakes) |

## Python snippet

```python
import random

def stone_natural_castle():
    """Andesite-and-smooth-stone wall fabric. Light gray, warm, weathered."""
    r = random.random()
    if r < 0.35: return P("minecraft:stone")
    if r < 0.49: return P("minecraft:stone_bricks")
    if r < 0.62: return P("minecraft:smooth_stone")
    if r < 0.74: return P("minecraft:andesite")
    if r < 0.84: return P("minecraft:polished_andesite")
    if r < 0.92: return P("minecraft:tuff_bricks")
    if r < 0.97: return P("minecraft:cracked_stone_bricks")
    return P("minecraft:cobblestone")


NATURAL_STONE_CASTLE = {
    "stone": stone_natural_castle,
    "trim_slab": P("minecraft:smooth_stone_slab[type=top,waterlogged=false]"),
    "trim_slab_alt": P("minecraft:stone_brick_slab[type=top,waterlogged=false]"),
    "trim_slab_tuff": P("minecraft:tuff_brick_slab[type=top,waterlogged=false]"),
    "trim_stair": P("minecraft:polished_andesite_stairs[half=bottom,shape=straight]"),
    "wall_low": P("minecraft:stone_brick_wall"),
    "wall_cobble": P("minecraft:cobblestone_wall"),
    "moss": P("minecraft:moss_block"),
    "wood": P("minecraft:dark_oak_log[axis=y]"),
    "wood_stripped": P("minecraft:stripped_oak_log[axis=y]"),
    "trapdoor_batten": P("minecraft:dark_oak_trapdoor[half=top,facing=south,open=false]"),
    "glass": P("minecraft:white_stained_glass"),
    "primary_light": P("minecraft:torch"),
    "wall_torch": P("minecraft:wall_torch[facing=north]"),
    "accent_lantern": P("minecraft:lantern[hanging=true,waterlogged=false]"),
    "smoke_campfire": P("minecraft:campfire[lit=true,signal_fire=false,facing=north,waterlogged=false]"),

    # Terrain podium materials
    "podium_dirt": P("minecraft:dirt"),
    "podium_grass": P("minecraft:grass_block[snowy=false]"),
    "podium_moss": P("minecraft:moss_block"),
    "moat_water": P("minecraft:water[level=0]"),
}
```

## The slab-trim rule

This palette is defined by *slabs as primary trim*. Apply the rule:

- Every floor break gets a slab-top string course
- Every wall-top cap is a slab (NOT a stair-block crenellation, unless
  it's a *defensive* castle)
- Window sills are slab-top
- Stair-block trim is reserved for **explicit slope features**:
  the corner of a pitched roof, a curtain-wall plinth splay-out, a
  hood above a door

If a generator uses this palette but defaults to stair-block trim,
the output will read as the wrong style (rustic peasant) instead of
"natural-stone castle."

## The "no glass" rule (for this palette)

Onctaaf's castle uses only 45 glass cells in 76,869 placed. **For the
natural-stone castle palette, glass is essentially absent.** Reserve
it for:
- The chapel apse (small window)
- The solar / lord's bedchamber (one window per wall, small panes)
- The watchtower's lookout (1–2 small panes)
- Every other window is unglazed (just an opening framed by trim) or
  is a single iron_bars cell

## The moss + cracked together rule

For weathering, use **moss AND cracked stone together** in approximately
equal proportions, scattered across the same surfaces. Onctaaf used
1,536 moss + 1,132 cracked = 1.36:1 ratio. Approximate this:

```python
def weathered_natural_stone():
    r = random.random()
    if r < 0.04: return P("minecraft:moss_block")
    if r < 0.07: return P("minecraft:cracked_stone_bricks")
    return stone_natural_castle()
```

7% combined weathering, slightly more moss than cracks. The result
reads as "ancestral seat" — old but living, not ruined.

## Variations

- **Cathedral wing addition**: swap `polished_andesite` for `polished_diorite`,
  add `quartz_stairs` and `chiseled_quartz_block` accents. Use the
  cathedral stained-glass program from [cathedral.md](cathedral.md).
- **Smithy / outbuilding**: lean toward `cobblestone` + raw `stone`,
  drop andesite, swap trapdoors for actual `oak_planks`.
- **Watchtower**: same palette but increase `stone_bricks` proportion
  (a defensive-coded tower has more masonry, less rough stone).

## See also

- [../reference_builds/onctaaf_castle_study.md](../reference_builds/onctaaf_castle_study.md) — the source data
- [dark_castle.md](dark_castle.md) — the opposite stylistic pole
- [peasant_village.md](peasant_village.md) — the smaller-scale relative
