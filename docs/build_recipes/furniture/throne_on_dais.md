# Throne on Dais — focal point of a throne room

**Status:** v0
**Provenance:** claude-proposal
**Best for:** castle throne room, dark-castle inner sanctum, lord's audience chamber
**Reads as:** authority, hierarchy, "the room is about THIS"
**Pair with:** [../doors/double_door_great_hall.md](../doors/double_door_great_hall.md) (the audience door), single hanging lantern above

## Description

A raised platform (the dais) with a single seat upon it, lit from above
by ONE lantern. The throne room's geometry funnels toward this point
(see [../_principles/master_builders_compass.md](../_principles/master_builders_compass.md)
— "every great religious space has one focal point").

The dais is 2 blocks high (the lord sits ABOVE eye level when standing
courtiers approach), 5×5 footprint. The throne is one block tall, on top.

## Layout (perspective)

```
        ▼ single hanging lantern
        |
        |
        ░░░░░░  ← back wall: banner with lord's colors
        ▓THRONE▓
        ▓THRONE▓     ← throne is 1 wide × 2 tall (back is 2 tall)
       ░▓THRONE▓░
      ░░░ DAIS ░░░    ← dais: 5 wide × 5 deep × 2 tall
     ░░░░ DAIS ░░░░
    ░░░░░ DAIS ░░░░░
    ───────────────  ← floor of room
```

## Python snippet

```python
def throne_on_dais(
    g, anchor_x, anchor_y, anchor_z, facing="south", *,
    dais_block,           # stone for the dais (curtain or refinement era)
    throne_block,         # block for the throne seat (e.g., obsidian for dark castle, polished blackstone for normal)
    throne_back_block,    # block for the throne high back (often chiseled)
    accent_block,         # block for the dais step lip (often slab top, contrasting)
    lantern_block,        # lantern type (soul_lantern for dark, lantern for normal)
    banner_block,         # wall banner block (lord's colors)
):
    """
    Build a 5x5x2 dais with a throne in the middle, facing 'facing'.
    Anchor is the lower-front-left corner of the dais (NOT the throne).
    """
    dais_w = 5
    dais_d = 5
    dais_h = 2

    # 1. Solid dais
    for dx in range(dais_w):
        for dz in range(dais_d):
            for dy in range(dais_h):
                g.setb(anchor_x + dx, anchor_y + dy, anchor_z + dz,
                       dais_block() if callable(dais_block) else dais_block)

    # 2. Step lip (slab on top of the front edge — front face of dais)
    for dx in range(dais_w):
        g.setb(anchor_x + dx, anchor_y + dais_h, anchor_z, accent_block)

    # 3. Throne (centered on dais)
    tx = anchor_x + dais_w // 2
    tz = anchor_z + dais_d // 2
    # Seat
    g.setb(tx, anchor_y + dais_h, tz, throne_block)
    # High back (2 blocks tall behind the seat)
    back_dz = +1 if facing == "south" else -1 if facing == "north" else 0
    back_dx = +1 if facing == "east" else -1 if facing == "west" else 0
    for dy in (1, 2):
        g.setb(tx - back_dx, anchor_y + dais_h + dy, tz - back_dz, throne_back_block)

    # 4. Hanging lantern, 5 blocks above the throne seat
    g.setb(tx, anchor_y + dais_h + 5, tz, lantern_block)
    # Chain drop between lantern and the ceiling (caller may extend)
    for cy in range(anchor_y + dais_h + 6, anchor_y + dais_h + 8):
        g.setb(tx, cy, tz, P("minecraft:chain[axis=y]"))

    # 5. Banner behind the throne (wall-mounted, facing forward)
    facing_for_banner = {
        "south": "north", "north": "south", "east": "west", "west": "east",
    }[facing]
    # The wall block is 2 cells behind the throne back; banner mounts on it
    wall_dz = +2 if facing == "south" else -2 if facing == "north" else 0
    wall_dx = +2 if facing == "east" else -2 if facing == "west" else 0
    g.setb(tx + wall_dx, anchor_y + dais_h + 1, tz + wall_dz,
           P(f"{banner_block.split('[')[0]}[facing={facing_for_banner}]"))

    # 6. Wall sconces flanking the throne (one each side)
    g.setb(tx + 1, anchor_y + dais_h + 1, tz + (back_dz or 0) + 2,
           P("minecraft:wall_torch[facing=south]"))
    g.setb(tx - 1, anchor_y + dais_h + 1, tz + (back_dz or 0) + 2,
           P("minecraft:wall_torch[facing=south]"))
```

## Throne-block choices by build type

| Build type | Seat block | Back block |
|------------|-----------|-----------|
| Normal castle (peace-time) | `polished_blackstone` | `chiseled_polished_blackstone` |
| Royal palace | `gold_block` (the actual throne is gold-clad) | `chiseled_quartz_block` |
| Dark castle | `obsidian` | `crying_obsidian` or `chiseled_polished_blackstone` |
| Druid / pagan court | `mossy_cobblestone` with `vine` draped | a living tree (`oak_log[axis=y]`) as the throne back |
| Skeletal / lich court | `bone_block[axis=y]` | `wither_skeleton_skull[powered=false]` on top of bone block |

## The "rule of one light"

The throne should be the *only* well-lit point in the room. The dais is
lit by the single hanging lantern (or chain of lanterns). The rest of
the room is lit dimly by wall torches at the sides — never another
lantern at throne height.

The viewer's eye should be drawn to the throne *because it is the brightest
point in an otherwise-shadowed room.* This is the "light reveals the
important block" rule from
[../_principles/lighting_dramaturgy.md](../_principles/lighting_dramaturgy.md).

## See also

- [banquet_table_long.md](banquet_table_long.md) — sometimes pairs with a throne in a great hall
- [../_principles/master_builders_compass.md](../_principles/master_builders_compass.md) — Adamantis axial design
- [../reference_builds/castles_grand_study.md](../reference_builds/castles_grand_study.md) §7 — dark-castle throne specifics
