# Peasant Plank Door — single-door entrance

**Status:** v0
**Provenance:** claude-proposal (universal in peasant-house references)
**Best for:** peasant houses, cottage entrances, stable doors, private rooms in a castle
**Reads as:** modest, domestic, single-occupant entry

## Description

A single 1×2 wooden door in a wall, framed by simple stone trim and a small
stair-block hood above. The most common door in any vernacular medieval
build.

Compared to a great-hall double door: this is **single-leaf, narrower,
shorter hood, no banners**. The hood is the decoration; the door itself is
plain.

## Layout

```
y+3     ╲═══╱           hood (stair-block, downward-outward)
y+2     ▓▓D▓▓           upper door half (D), trim on sides
y+1     ▓▓D▓▓           lower door half
y+0     ▓▓▓▓▓           threshold (stone step or plain ground)
```

## Python snippet

```python
def peasant_plank_door(
    g, x, y, z, facing="south", *,
    door_id="minecraft:oak_door",
    trim_block=None,
    hood_stair=None,
    hinge="left"
):
    """
    Single-leaf door + frame + hood.
    anchor (x,y,z) is the lower door cell.
    """
    g.setb(x, y, z,
        P(f"{door_id}[half=lower,hinge={hinge},facing={facing},open=false]"))
    g.setb(x, y + 1, z,
        P(f"{door_id}[half=upper,hinge={hinge},facing={facing},open=false]"))
    # Side trim (optional — many peasant houses don't bother)
    if trim_block is not None:
        for dy in (0, 1):
            g.setb(x - 1, y + dy, z, trim_block() if callable(trim_block) else trim_block)
            g.setb(x + 1, y + dy, z, trim_block() if callable(trim_block) else trim_block)
    # Hood
    if hood_stair is not None:
        for dx in (-1, 0, 1):
            g.setb(x + dx, y + 2, z, hood_stair)
```

## Variations

- **No frame, no hood** (poorest cottage): just the door in a wall. The
  wall blocks around it ARE the frame.
- **Stair-hood only** (most common): no side trim; just a single course
  of stair blocks as a roof above the door.
- **Plank-door over arrow loop** (castle interior): in a hallway with side
  arrow-loop windows, the door + windows share a stair-block string course.
- **Stable door** (split top + bottom, "Dutch door"): represented with two
  trapdoors stacked, hinged at opposite sides — top half opens
  independently. Use `oak_trapdoor` × 2.

## See also

- [double_door_great_hall.md](double_door_great_hall.md)
- [../palettes/peasant_village.md](../palettes/peasant_village.md)
