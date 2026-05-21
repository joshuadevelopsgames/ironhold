# Founding-era palette

**Status:** v0 — codified from `_principles/material_storytelling.md`, not yet in-game tested
**Provenance:** claude-proposal
**Best for:** original keep walls, bedrock-base courses, the oldest part of any castle
**Reads as:** rough, fearful, defensive, hand-built by frightened settlers
**Pair with:** any other era palette via a visible seam (see material_storytelling.md)

## Description

The oldest stone in a castle. Built by a lord who feared his neighbors,
in a hurry, with whatever rock could be quarried locally. No dressed stone,
no chiseled accents, no glass. Everything is heavy and crude.

## Palette

| Role | Block | Weight |
|------|-------|--------|
| Primary stone | `cobbled_deepslate` | 55% |
| Crust variant | `mossy_cobblestone` | 20% |
| Crack variant | `cobbled_deepslate` (different facing — visual variety) | 13% |
| Upgrade variant | `deepslate_bricks` (sparing — only where a wall was patched) | 8% |
| Crack-and-age | `cracked_deepslate_bricks` | 4% |
| Trim (sparing) | `deepslate_brick_slab` (top, for door lintels only) | — |
| Wood | `oak_log` (raw, not stripped) | — |
| Door | `oak_door` (the heaviest wood door available) | — |
| Glass | none — windows are arrow loops only | — |
| Accent | `torch` (yellow), `iron_bars` (gates), `chain` (gibbets, lamp hangs) | — |

## Python snippet

```python
import random

def stone_founding():
    """Crude, heavy, fearful stone. For the oldest courses only."""
    r = random.random()
    if r < 0.55: return P("minecraft:cobbled_deepslate")
    if r < 0.75: return P("minecraft:mossy_cobblestone")
    if r < 0.88: return P("minecraft:cobbled_deepslate")  # repeats — that's intentional
    if r < 0.96: return P("minecraft:deepslate_bricks")
    return P("minecraft:cracked_deepslate_bricks")


FOUNDING = {
    "stone": stone_founding,
    "trim": P("minecraft:deepslate_brick_slab[type=top]"),
    "wood": P("minecraft:oak_log[axis=y]"),
    "door": P("minecraft:oak_door"),
    "torch": P("minecraft:torch"),
    "glass": None,   # no glass in this era
}
```

## When NOT to use this palette

- For a whole building, except the smallest motte-and-bailey keep. The
  founding palette is a *seam* — it should be visible at the BASE of a
  newer building, not on the whole building.
- On a refinement-era room (solar, chapel) — those have moved on to
  finer materials.

## See also

- [curtain_era.md](curtain_era.md) — the next era up
- [../_principles/material_storytelling.md](../_principles/material_storytelling.md)
