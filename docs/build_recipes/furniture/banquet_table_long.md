# Banquet Table — long, with benches

**Status:** v0
**Provenance:** claude-proposal
**Best for:** great hall, banquet hall, large tavern, monastic refectory
**Reads as:** feasting, hospitality, communal life

## Description

A long table running down the long axis of a great hall, with benches
(not chairs) on both sides. The lord's seat is at the head, usually
slightly elevated. Tableware, candles, and food are placed at intervals
along the surface.

Typical dimensions: 9–15 blocks long × 2 wide × 1 tall (the table top is
1 block above the floor; that's the height of a `stripped_log` or
`crafting_table`).

## Layout (top-down)

```
       head (lord's seat) →  ▓        ▓
                          ░░░T░T░T░T░T░░░
                          ░░░T░T░T░T░T░░░   ← 2-wide table
                          ░░░T░T░T░T░T░░░
                             ▓ ▓ ▓ ▓ ▓     ← benches on both sides
                             ▓ ▓ ▓ ▓ ▓
```

Each "T" is a 2-block table top. The `T░T` pattern alternates table top
with a small gap for trenchers / plates (decorative item frames or
`pot` blocks).

## Python snippet

```python
def banquet_table_long(
    g, anchor_x, anchor_y, anchor_z, *,
    length=11,            # total length in blocks
    facing="south",       # the long-axis of the table
    table_block,          # block for the table top (commonly stripped_oak_log[axis=x])
    bench_block,          # block for benches (commonly oak_slab[type=top])
    candle_block,         # candle for the tabletop (use 'red' or 'white')
    centerpiece_blocks=None,   # list of decorative blocks to scatter along the centerline
):
    """
    Place a long table + benches.
    Anchor is the floor cell directly under the head of the table.
    """
    if centerpiece_blocks is None:
        centerpiece_blocks = [
            P("minecraft:cake[bites=0]"),
            P("minecraft:flower_pot"),
            P("minecraft:decorated_pot[facing=north,waterlogged=false]"),
        ]

    # Direction along the table
    if facing == "south":   dz = 1; dx = 0
    elif facing == "north": dz = -1; dx = 0
    elif facing == "east":  dz = 0; dx = 1
    elif facing == "west":  dz = 0; dx = -1

    # Place table top — 2 wide along the cross-axis
    cross_dx = 1 if dz else 0
    cross_dz = 1 if dx else 0

    for i in range(length):
        table_x = anchor_x + i * dx
        table_z = anchor_z + i * dz
        # Two cells wide (the table itself)
        g.setb(table_x, anchor_y, table_z, table_block)
        g.setb(table_x + cross_dx, anchor_y, table_z + cross_dz, table_block)

        # Every 3rd cell, place a centerpiece (candle, plate, etc.)
        if i % 3 == 1:
            # Candle centered on table at this slice
            g.setb(table_x, anchor_y + 1, table_z,
                   P(f"{candle_block}[lit=true,candles=1]"))
        elif i % 3 == 2:
            # Decoration item from rotating set
            deco = centerpiece_blocks[i % len(centerpiece_blocks)]
            g.setb(table_x, anchor_y + 1, table_z, deco)

    # Benches — flush against each long side of the table
    bench_offset_a = (-1 if dz else cross_dx + 1) * (1 if dz else 0)
    # Easier: benches sit at (table_x, anchor_y, table_z - 1) for south-facing tables
    # Two parallel rows of slabs:
    for i in range(length):
        tx = anchor_x + i * dx
        tz = anchor_z + i * dz
        # Bench 1
        g.setb(tx - cross_dx, anchor_y, tz - cross_dz, bench_block)
        # Bench 2 (other side of the 2-wide table)
        g.setb(tx + 2 * cross_dx, anchor_y, tz + 2 * cross_dz, bench_block)
```

## Standard decoration

A 9-long banquet table should have:
- 3 lit candles, spaced along the centerline (3-block intervals)
- 2 decorated pots / dyed pottery
- 1 cake at the head (the lord's place)
- Item frames on the wall behind the lord's seat showing the lord's crest
  (or a wall banner)
- Hanging chains-with-lanterns above the table at 4-block intervals

## Variations

- **Refectory** (monastic dining): single long table, simple wooden benches,
  no candles (lit by overhead lanterns only), reading lectern at one end
  for the prelector
- **Tavern common-room table**: smaller (5–7 long × 2 wide), surrounded by
  stools (`oak_pressure_plate` on log) instead of benches
- **Outdoor feast table** (wedding, harvest): same recipe but in a courtyard,
  with `flower_pot` centerpieces and `lantern` poles at each corner
- **U-shaped high table**: for the largest halls, the lord's table sits
  perpendicular at the head, forming a T or U with the main table — used
  for state banquets

## See also

- [throne_on_dais.md](throne_on_dais.md) — for the lord's seat at the head
- [../fireplaces/great_hall_hearth.md](../fireplaces/great_hall_hearth.md) — at the opposite end
