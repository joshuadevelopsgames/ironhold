# Peasant village palette

**Status:** v0 — codified from `community_batch1_24_patterns.md`
**Provenance:** reference-image (extracted from SWORDSELF_MC, Abfielder, ChronoComet, CraftingMatthew houses)
**Best for:** vernacular medieval houses, taverns, mills, village buildings
**Reads as:** lived-in, warm, working-class

## Description

The dominant palette of the master-builder peasant-house corpus. Stone base,
half-timber (Fachwerk) upper, pitched-stair roof, brick chimneys.

## Palette

| Role | Block | Weight |
|------|-------|--------|
| Stone base | `cobblestone` | 50% |
| Stone variant | `mossy_cobblestone` | 25% |
| Stone variant | `stone_bricks` | 15% |
| Stone variant | `cracked_stone_bricks` | 10% |
| Wood frame | `dark_oak_log[axis=y]` (vertical posts) | — |
| Wood plank | `birch_planks` (light infill between dark posts) | — |
| Wood diagonal | `dark_oak_stairs` (cross-bracing in half-timber) | — |
| Roof | `spruce_stairs` for pitched roofs (dark slate look) | — |
| Roof variant (richer) | `deepslate_tile_stairs` (mossy-spotted) | — |
| Door | `oak_door` (peasant) or `spruce_door` (slightly nicer) | — |
| Window glass | `glass_pane` plain; `white_stained_glass_pane` for slightly nicer | — |
| Window frame | `dark_oak_stairs` as window hood + corbels | — |
| Chimney | `bricks` with `chiseled_stone_bricks` cap | — |
| Lighting | `torch` (exterior), `lantern[hanging=true]` (interior good rooms) | — |
| Decoration | `flower_pot` (windowsill), `hay_block` (haystacks), `barrel`, `cauldron`, `bell` | — |

## Python snippet

```python
import random

def stone_peasant():
    """Mixed cobble for vernacular medieval houses."""
    r = random.random()
    if r < 0.50: return P("minecraft:cobblestone")
    if r < 0.75: return P("minecraft:mossy_cobblestone")
    if r < 0.90: return P("minecraft:stone_bricks")
    return P("minecraft:cracked_stone_bricks")


PEASANT = {
    "stone": stone_peasant,
    "wood_post": P("minecraft:dark_oak_log[axis=y]"),
    "wood_plank": P("minecraft:birch_planks"),
    "wood_cross": P("minecraft:dark_oak_stairs"),
    "roof_stair": P("minecraft:spruce_stairs"),
    "roof_slab": P("minecraft:spruce_slab[type=top]"),
    "door": P("minecraft:oak_door"),
    "glass": P("minecraft:glass_pane"),
    "window_hood": P("minecraft:dark_oak_stairs"),
    "chimney_brick": P("minecraft:bricks"),
    "chimney_cap": P("minecraft:chiseled_stone_bricks"),
    "torch_wall": P("minecraft:wall_torch"),
    "lantern_hanging": P("minecraft:lantern[hanging=true]"),

    "deco_flowerpot": P("minecraft:flower_pot"),
    "deco_hay": P("minecraft:hay_block"),
    "deco_barrel": P("minecraft:barrel[facing=up,open=false]"),
}
```

## Variations

- **Wealthier villager** (a craftsman, the miller): swap `birch_planks`
  for `oak_planks`, use `dark_oak_stairs` for roof (instead of spruce).
- **Tavern**: increase footprint, add a second floor with jetty (cantilever),
  add a second chimney at the opposite gable end, add a bay window.
- **Mill**: keep palette but add a `waterwheel` block sketch (slabs + fence)
  attached to a stream-side wall.
- **Smithy**: add `magma_block` cube (the forge), `anvil`, swap door wood
  for `oak` (worn). Heavily scorched, no flower pots.

## See also

- [../reference_builds/community_batch1_24_patterns.md](../reference_builds/community_batch1_24_patterns.md)
- [../columns_and_arches/half_timbered_wall.md](../columns_and_arches/half_timbered_wall.md)
- [../roofs/pitched_gable_stair_45.md](../roofs/pitched_gable_stair_45.md)
- [../atmosphere/brick_chimney_stack.md](../atmosphere/brick_chimney_stack.md)
