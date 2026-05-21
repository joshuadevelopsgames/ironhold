# Cottage Hearth — modest fireplace for a peasant home

**Status:** v0
**Provenance:** claude-proposal
**Best for:** peasant cottage, smithy hearth, tavern back room, single-family tavern
**Reads as:** working, warm, life-of-the-house

## Description

A small recessed hearth, 1 wide × 2 tall opening, with a stair-block hood
(not a stone mantle — wealth-marker difference) and a simple brick chimney
above. The cottage's ONE warm spot in a cold-stone-floored room.

The cooking pot (`cauldron`) sits on the hearth or directly beside it.

## Layout

```
y+4     ╲═══╱           hood (stair, downward-outward)
y+3     ▓ ░ ▓           opening top
y+2     ▓ ░ ▓           fire space
y+1     ▓ 🔥 ▓           fire / netherrack
y+0     ▓▓▓▓▓           hearth surround
        column|column
```

## Python snippet

```python
def cottage_hearth(
    g, x, y, z, facing="south", *,
    stone,                          # peasant stone (cobble + mossy mix)
    hood_stair,                     # stair-block hood
    cauldron_offset=(-1, 0, 0)      # where the cooking pot sits (relative offset)
):
    """
    Single-opening (1 wide x 2 tall) hearth + hood + chimney stub.
    Anchor (x, y, z) = lower cell of the opening.
    """
    # Back of recess (1 deep)
    for dy in range(2):
        g.setb(x, y + dy, z - 1, stone() if callable(stone) else stone)

    # Hearth floor
    g.setb(x, y, z, P("minecraft:netherrack"))
    g.setb(x, y, z - 1, P("minecraft:netherrack"))

    # Fire
    g.setb(x, y + 1, z, P("minecraft:fire"))

    # Side frames of opening
    g.setb(x - 1, y, z, stone() if callable(stone) else stone)
    g.setb(x - 1, y + 1, z, stone() if callable(stone) else stone)
    g.setb(x + 1, y, z, stone() if callable(stone) else stone)
    g.setb(x + 1, y + 1, z, stone() if callable(stone) else stone)

    # Hood (single stair block above the opening, slope-down outward)
    g.setb(x, y + 2, z, hood_stair)

    # Chimney stub — caller's brick_chimney_stack picks up from here
    g.setb(x, y + 3, z - 1, P("minecraft:bricks"))

    # Cauldron / cooking pot beside the hearth
    cx, cy, cz = x + cauldron_offset[0], y + cauldron_offset[1], z + cauldron_offset[2]
    g.setb(cx, cy, cz, P("minecraft:cauldron"))
```

## Standard accompanying decoration

- 1 `cauldron` immediately beside the hearth (cooking)
- 1 `iron_trapdoor[facing=south,half=top]` mounted on the wall above the
  cauldron, representing a pot rack or a shutter to the cooking corner
- A small pile of `hay_block` or `dried_kelp_block` as kindling — beside
  the hearth, not in it
- A stool: `oak_pressure_plate` on top of a `dark_oak_log[axis=y]` (a 1-block
  high "seat" beside the fire)
- Vine or `dried_kelp` hanging from the hood (cured-meat strip suggestion)

## Variations

- **Smithy hearth**: replace the wood hood with stone, replace fire with
  `magma_block`, add `anvil` adjacent. No cauldron.
- **Tavern back-kitchen hearth**: 2 opening units wide instead of 1, with
  two `cauldron`s side by side. Tavern needs to cook for multiple guests.
- **Bakers' oven** (variant): hood is a dome of stair-blocks arching over
  the opening. Cube-shaped iron-door represents the oven door.

## See also

- [great_hall_hearth.md](great_hall_hearth.md) — the ceremonial scale
- [../atmosphere/brick_chimney_stack.md](../atmosphere/brick_chimney_stack.md)
- [../palettes/peasant_village.md](../palettes/peasant_village.md)
