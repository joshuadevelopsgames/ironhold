# Cathedral palette

**Status:** v0 — synthesized from gothic cathedral references
**Provenance:** claude-proposal (drawing on Smitey + Adamantis cathedral style)
**Best for:** cathedrals, great chapels, cardinal seats, large monastic complexes
**Reads as:** sacred, light-filled, vertically aspiring

## Description

Cathedral architecture is light made stone. Everything in the palette
emphasizes:
- Pale stone (light reflects, never absorbs)
- Maximum glazing (the wall above 3 blocks is mostly window)
- Vertical stone members (columns, ribs, pointed arches)
- Wooden choir / pews as the only horizontal element

Distinct from a refinement-era palette: the cathedral palette uses
*polished diorite / quartz / calcite* — much lighter than deepslate.

## Palette

| Role | Block | Weight |
|------|-------|--------|
| Primary stone | `polished_diorite` | 50% |
| Trim variant | `smooth_stone` | 18% |
| Accent variant | `polished_diorite` (light, organic) | 14% |
| Showpiece (vault ribs, key openings) | `quartz_block` or `chiseled_quartz_block` | 10% |
| Floor | `polished_diorite` or `calcite` (lighter) | 5% |
| Polished | `smooth_quartz` for plinths and altar surfaces | 3% |
| Trim | `polished_diorite_slab[type=top]`, `quartz_stairs` | — |
| Wood | `dark_oak_planks`, `dark_oak_stairs` (choir, pews — high contrast against the pale stone) | — |
| Door | `dark_oak_door[hinge=left|right]` paired for west-front double door | — |
| Glass | colored stained-glass programs — see "stained-glass program" below | — |
| Primary light | `soul_lantern[hanging=true]` with `chain` drops (cool, sacred) OR `lantern[hanging=true]` (warm, more domestic chapel) | — |
| Accent light | `white_candle[lit=true]` clusters on altar; `soul_torch` along ambulatory | — |

## Stained-glass program

A cathedral picks ONE color story and repeats it across its glazing:

| Color story | Use | Reads as |
|-------------|-----|----------|
| Blue + white | clerestory windows (high windows) | Light of heaven, sky |
| Red + yellow | rose window + side aisles | Pentecost, fire, martyrs |
| Green + brown | low windows in monastic cathedral | Earthly works, harvest |
| Purple + gold | clerestory + apse | Royal, advent, lordship |

Use `<color>_stained_glass_pane`. The clerestory has the lightest, most
saturated panes (light passes through them and lands on the nave floor).
The side-aisle windows have darker, smaller panes.

## Python snippet

```python
import random

def stone_cathedral():
    """Pale, light-reflective stone for cathedrals."""
    r = random.random()
    if r < 0.50: return P("minecraft:polished_diorite")
    if r < 0.68: return P("minecraft:smooth_stone")
    if r < 0.82: return P("minecraft:polished_diorite")  # repeats — palette is intentionally low-variance
    if r < 0.92: return P("minecraft:quartz_block")
    if r < 0.97: return P("minecraft:calcite")
    return P("minecraft:smooth_quartz")


CATHEDRAL = {
    "stone": stone_cathedral,
    "trim_slab": P("minecraft:polished_diorite_slab[type=top]"),
    "trim_stair": P("minecraft:quartz_stairs"),
    "wood": P("minecraft:dark_oak_planks"),
    "door": P("minecraft:dark_oak_door"),

    "stained_glass_blue": P("minecraft:blue_stained_glass_pane"),
    "stained_glass_white": P("minecraft:white_stained_glass_pane"),
    "stained_glass_red": P("minecraft:red_stained_glass_pane"),
    "stained_glass_yellow": P("minecraft:yellow_stained_glass_pane"),
    "stained_glass_purple": P("minecraft:purple_stained_glass_pane"),

    "hanging_lantern": P("minecraft:lantern[hanging=true]"),
    "hanging_soul_lantern": P("minecraft:soul_lantern[hanging=true]"),
    "chain": P("minecraft:chain[axis=y]"),
    "altar_candle": P("minecraft:white_candle[lit=true,candles=4]"),
}
```

## Anti-pattern

A cathedral built in deepslate or blackstone reads as a *dark* sacred
space — fine for the crypt or a dread shrine, but wrong for the main
nave. The cathedral wants light *passing through* the building. Deepslate
absorbs light. Use it only for the foundation course and the crypt.

## See also

- [refinement_era.md](refinement_era.md) — for chapels (smaller scale than cathedrals)
- [../_principles/master_builders_compass.md](../_principles/master_builders_compass.md) — Smitey, Adamantis
- [../_principles/scale_and_proportion.md](../_principles/scale_and_proportion.md) — cathedral nave dimensions
