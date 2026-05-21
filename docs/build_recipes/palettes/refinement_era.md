# Refinement-era palette

**Status:** v0 — codified from `_principles/material_storytelling.md`
**Provenance:** claude-proposal
**Best for:** solar additions, chapel additions, banquet halls, lord's chambers
**Reads as:** wealthy, comfortable, peace-time prosperity

## Description

A wealthy era's stone. The lord wanted to live well, not just survive.
Polished surfaces, chiseled accents, large glazed windows. Often added as
a *wing* onto a curtain-era keep rather than rebuilding from scratch.

## Palette

| Role | Block | Weight |
|------|-------|--------|
| Primary stone | `polished_deepslate` | 55% |
| Trim variant | `deepslate_bricks` (visual contrast to the polished primary) | 20% |
| Accent variant | `chiseled_deepslate` (around openings, at corners) | 10% |
| Tile variant | `deepslate_tiles` (floors, ceremonial paths) | 10% |
| Upgrade variant | `polished_blackstone_bricks` (for a chapel-attached oriel only) | 5% |
| Trim | `polished_deepslate_slab`, `polished_deepslate_stairs` | — |
| Wood | `dark_oak_log[axis=y]`, `dark_oak_planks` (finer than oak) | — |
| Door | `dark_oak_door` (often paired — see double_door recipes) | — |
| Glass | `glass_pane` in larger panes; `white_stained_glass_pane` in the chapel | — |
| Accent | `lantern[hanging=true]`, `wall:lit candle`, `banner` (lord's colors) | — |
| Floor | `polished_deepslate` (matched to walls) or `deepslate_tiles` (ceremonial) | — |

## Python snippet

```python
import random

def stone_refinement():
    """Peacetime stone — wealthy, comfortable."""
    r = random.random()
    if r < 0.55: return P("minecraft:polished_deepslate")
    if r < 0.75: return P("minecraft:deepslate_bricks")
    if r < 0.85: return P("minecraft:chiseled_deepslate")
    if r < 0.95: return P("minecraft:deepslate_tiles")
    return P("minecraft:polished_blackstone_bricks")


REFINEMENT = {
    "stone": stone_refinement,
    "trim_slab": P("minecraft:polished_deepslate_slab[type=top]"),
    "trim_stair": P("minecraft:polished_deepslate_stairs"),
    "wood": P("minecraft:dark_oak_log[axis=y]"),
    "planks": P("minecraft:dark_oak_planks"),
    "door": P("minecraft:dark_oak_door"),
    "glass": P("minecraft:glass_pane"),
    "chapel_glass": P("minecraft:white_stained_glass_pane"),
    "hanging_lantern": P("minecraft:lantern[hanging=true]"),
    "candle_lit_white": P("minecraft:white_candle[lit=true,candles=1]"),
}
```

## See also

- [curtain_era.md](curtain_era.md)
- [modern_era.md](modern_era.md)
- [cathedral.md](cathedral.md) — when the refinement wing is a chapel, escalate to cathedral palette
