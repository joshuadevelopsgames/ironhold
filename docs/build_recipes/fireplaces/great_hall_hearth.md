# Great-Hall Hearth — central fireplace for a ceremonial room

**Status:** v0
**Provenance:** claude-proposal
**Best for:** castle great hall, banquet hall, large tavern common room, manor great room
**Reads as:** ceremonial center, the room's social gravity well
**Pair with:** [../atmosphere/brick_chimney_stack.md](../atmosphere/brick_chimney_stack.md) (the external expression)

## Description

The dominant heat source in a public room. Recessed into a wall (not
free-standing), framed by stone columns or chiseled blocks, with a stone
mantle and an iron firedog/andiron suggestion. Big enough to be visually
dominant; the throne/banquet table addresses it.

Critical detail: the fire is *visible* from across the room. Don't bury
it in a tiny opening. The fireplace opening is 3 wide × 3 tall.

## Layout (front elevation)

```
y+5     ▓▓▓▓▓▓▓▓▓▓▓     mantle (stone slab, top)
y+4     ▓        ▓     opening top trim (chiseled)
y+3     ▓ ░░░░░░ ▓     fire space
y+2     ▓ ░ 🔥 ░ ▓     fire (`fire` on `netherrack` OR campfire)
y+1     ▓ ░░██░░ ▓     fire base (campfire OR netherrack hearth)
y+0     ▓▓▓▓▓▓▓▓▓▓▓     hearth floor / stone surround
        column |column
```

## Python snippet

```python
def great_hall_hearth(
    g, anchor_x, anchor_y, anchor_z, *,
    facing="south",       # which wall the hearth is in (fire points this way)
    stone,                # bulk stone block / factory
    trim_block,           # chiseled or polished trim
    mantle_slab,          # slab block for the mantle (top type)
    use_campfire=True,    # campfire (lit) gives smoke particles
):
    """
    Recessed hearth in the wall at z = anchor_z + 1 (1 block recess).
    Opening is 3 wide x 3 tall. Anchor is the lower-left of the OPENING (not the wall).
    """
    # Carve the 3w x 3h x 1d recess (caller must have placed wall around it already)
    for dx in range(3):
        for dy in range(3):
            g.setb(anchor_x + dx, anchor_y + dy, anchor_z - 1,
                   stone() if callable(stone) else stone)  # back wall of recess

    # Hearth floor (bottom of opening)
    for dx in range(3):
        g.setb(anchor_x + dx, anchor_y, anchor_z, P("minecraft:netherrack"))
        g.setb(anchor_x + dx, anchor_y, anchor_z - 1, P("minecraft:netherrack"))

    # Fire
    if use_campfire:
        # Campfire centered in opening produces smoke + flicker
        g.setb(anchor_x + 1, anchor_y, anchor_z,
               P(f"minecraft:campfire[facing={facing},lit=true,signal_fire=false,waterlogged=false]"))
    else:
        # Classic: netherrack + fire on top
        g.setb(anchor_x + 1, anchor_y + 1, anchor_z, P("minecraft:fire"))

    # Andirons / firedogs — anvil or iron-bars suggestion on each side
    g.setb(anchor_x, anchor_y, anchor_z,
           P("minecraft:campfire[facing=south,lit=false]"))  # unlit = log placeholder; replace with whatever your block id is for "iron andiron"
    # (If you want pure logs: substitute `minecraft:stripped_oak_log[axis=z]` here.)

    # Opening trim (vertical columns flanking the opening)
    for dy in range(4):
        g.setb(anchor_x - 1, anchor_y + dy, anchor_z, trim_block)
        g.setb(anchor_x + 3, anchor_y + dy, anchor_z, trim_block)

    # Mantle — runs across the top of the opening, 1 block wider on each side
    for dx in range(-1, 4):
        g.setb(anchor_x + dx, anchor_y + 4, anchor_z,
               mantle_slab if not callable(mantle_slab) else mantle_slab())

    # Chimney throat — opens above into the chimney (caller routes the chimney up)
    # We just leave the cell at (anchor_x+1, anchor_y+3, anchor_z-1) as netherrack
    # so the fire above can rise. The actual chimney shaft is placed by
    # brick_chimney_stack().
    g.setb(anchor_x + 1, anchor_y + 3, anchor_z - 1, P("minecraft:netherrack"))
```

## Decoration above the mantle

The mantle is a display surface. Standard treatments:
- **Lord's banner** centered above the mantle (a wall banner)
- **Two candlesticks** flanking the banner (lit white candles in a 1-block-tall
  position)
- **Hunting trophy substitute** (mob head — `skeleton_skull` for hunting,
  `wither_skeleton_skull` for dark castle, or `decorated_pot` for a peaceful
  household)
- **Painting** centered above the mantle (use `painting` block with the
  appropriate motive — the larger paintings work for a great hall)

## Variations

- **Twin-hearth great hall**: two hearths at opposite ends of the long
  axis, banquet table runs between them. Both chimneys break the roof
  as twin gable-end stacks.
- **Open hearth in the floor** (older, more vernacular): a square campfire
  pit in the *center* of the room, with smoke rising through a louvre in
  the ceiling. No mantle, no recess. Used in early-medieval keeps before
  fireplaces moved to the wall.
- **Walk-in hearth** (Tudor banquet hall): the opening is 5 wide × 4 tall;
  you can literally walk into the fireplace. Has seats inside the recess.

## See also

- [cottage_hearth.md](cottage_hearth.md) — smaller, peasant scale
- [../atmosphere/brick_chimney_stack.md](../atmosphere/brick_chimney_stack.md)
- [../furniture/banquet_table_long.md](../furniture/banquet_table_long.md) — the thing the hearth talks to
