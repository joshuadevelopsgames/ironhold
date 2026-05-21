# Curtain-era palette

**Status:** v0 — codified from `_principles/material_storytelling.md`
**Provenance:** claude-proposal
**Best for:** curtain walls, gatehouses, second-generation keeps, the bulk of a working castle
**Reads as:** competent, organized, mature defensive engineering

## Description

The dominant stone of a working castle. Built a generation after the
founding keep, by a lord with workers and time. Dressed stone is now
available; cobbled stone is reserved for the base courses. First small
glass appears in important windows only.

## Palette

| Role | Block | Weight |
|------|-------|--------|
| Primary stone | `deepslate_bricks` | 55% |
| Crust variant | `cobbled_deepslate` (lower courses, weather damage) | 20% |
| Crack variant | `cracked_deepslate_bricks` | 15% |
| Upgrade variant | `polished_deepslate` (around important openings) | 10% |
| Trim | `deepslate_brick_slab[type=top]` for string courses, `deepslate_brick_stairs` for cornices | — |
| Wood | `oak_log[axis=y]`, `oak_planks` | — |
| Door | `oak_door` (banded with iron — represented by oak with a `tripwire_hook` accent above) | — |
| Glass | `glass_pane` (clear) in the gatehouse guard windows; arrow loops elsewhere | — |
| Accent | `lantern[hanging=false]`, `iron_bars` (portcullises), `chain` | — |

## Python snippet

```python
import random

def stone_curtain():
    """The working stone of a mature castle."""
    r = random.random()
    if r < 0.55: return P("minecraft:deepslate_bricks")
    if r < 0.75: return P("minecraft:cobbled_deepslate")
    if r < 0.90: return P("minecraft:cracked_deepslate_bricks")
    return P("minecraft:polished_deepslate")


CURTAIN = {
    "stone": stone_curtain,
    "trim_slab": P("minecraft:deepslate_brick_slab[type=top]"),
    "trim_stair": P("minecraft:deepslate_brick_stairs"),
    "wood": P("minecraft:oak_log[axis=y]"),
    "planks": P("minecraft:oak_planks"),
    "door": P("minecraft:oak_door"),
    "glass": P("minecraft:glass_pane"),
    "lantern": P("minecraft:lantern[hanging=false]"),
    "iron_bars": P("minecraft:iron_bars"),
}
```

## See also

- [founding_era.md](founding_era.md)
- [refinement_era.md](refinement_era.md)
- [../_principles/material_storytelling.md](../_principles/material_storytelling.md)
