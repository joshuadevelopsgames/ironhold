# Grand Stair — 5 wide, ceremonial

**Status:** v0
**Provenance:** claude-proposal
**Best for:** great hall main stair, keep entry stair (from courtyard to first-floor entrance), cathedral nave-to-altar approach
**Reads as:** ceremonial, processional, "people come up this stair to do business with the lord"

## Description

A wide straight-flight or L-shaped stair, 5 cells wide, with stone
balustrades on both sides. The lord descends to greet visitors; visitors
ascend with measured steps. Wide enough for a procession to pass.

Standard rise: 1 block per 1 block, so a stair from courtyard to first
floor (typically 5 blocks up) is 5 blocks long. With landings, can extend
further.

## Layout (straight flight, 5 wide × 5 high × 5 long)

Side view:

```
                                  □ landing
                          ▓ ▓ ▓ ▓ ▓
                      ▓ ▓ ▓ stair ▓
                  ▓ ▓ ▓ stair ▓
              ▓ ▓ ▓ stair ▓
          ▓ ▓ ▓ stair ▓
      ▓ ▓ ▓ stair ▓
  ▓ ▓ ▓ floor ▓ ▓ ▓
```

Top view:

```
                              □ landing (5w)
                              ▓ ▓ ▓ ▓ ▓
                              ▓ ▓ ▓ ▓ ▓
  ▓ ▓ ▓ ▓ ▓                   ▓ ▓ ▓ ▓ ▓
  ▓ ▓ ▓ ▓ ▓                   ▓ ▓ ▓ ▓ ▓
  ▓ ▓ ▓ ▓ ▓                   ▓ ▓ ▓ ▓ ▓   ← stair flight (5w)
  ↑ flanking balustrade on both sides
```

## Python snippet

```python
def grand_stair_5w(
    g, x0, base_y, z0, length, *,
    facing="south",         # ascent direction
    width=5,
    stone,                  # solid block under the stair (and floor at top)
    stair_block_id,         # "minecraft:polished_deepslate_stairs", etc.
    balustrade_block,       # block for the balustrade — typically polished_<stone>_wall
    balustrade_post,        # decorative post block at landings — chiseled
):
    """
    Build a straight-flight grand stair, ascending `length` blocks vertically.
    (x0, base_y, z0) is the LOWER-LEFT cell of the stair's BASE.
    `facing` is the direction of ascent.
    """
    # Direction deltas
    if facing == "south":  dx, dz = 0, 1
    elif facing == "north": dx, dz = 0, -1
    elif facing == "east":  dx, dz = 1, 0
    elif facing == "west":  dx, dz = -1, 0

    # Cross-axis (perpendicular to ascent direction)
    cross = (1, 0) if dz else (0, 1)

    for step in range(length):
        # Step y-level
        sy = base_y + step
        # Position of the step start
        sx = x0 + dx * step
        sz = z0 + dz * step

        # Across the full width: place a stair block at this step
        for w in range(width):
            cx = sx + cross[0] * w
            cz = sz + cross[1] * w
            g.setb(cx, sy, cz, P(f"{stair_block_id}[facing={facing},half=bottom,shape=straight]"))

            # Fill the cells UNDER this stair with stone (so the stair isn't floating)
            for fill_y in range(base_y, sy):
                g.setb(cx, fill_y, cz, stone() if callable(stone) else stone)

    # Top landing — 1 wide, full step width, at base_y + length
    landing_y = base_y + length
    for ll in range(width):
        cx = x0 + dx * length + cross[0] * ll
        cz = z0 + dz * length + cross[1] * ll
        g.setb(cx, landing_y, cz, stone() if callable(stone) else stone)

    # Balustrades on both sides of the stair (rising along the slope)
    for step in range(length):
        sy = base_y + step + 1   # balustrade sits 1 above the step surface
        sx = x0 + dx * step
        sz = z0 + dz * step
        # Left edge (cross-axis 0 cell, but offset one outward)
        g.setb(sx - cross[0], sy, sz - cross[1], balustrade_block)
        # Right edge
        g.setb(sx + cross[0] * width, sy, sz + cross[1] * width, balustrade_block)

    # Decorative posts at base and at landing — taller than the wall by 2
    g.setb(x0 - cross[0], base_y, z0 - cross[1], balustrade_post)
    g.setb(x0 - cross[0], base_y + 1, z0 - cross[1], balustrade_post)
    g.setb(x0 + cross[0] * width, base_y, z0 + cross[1] * width, balustrade_post)
    g.setb(x0 + cross[0] * width, base_y + 1, z0 + cross[1] * width, balustrade_post)
```

## Standard accompanying details

- A **carpet runner** down the center of the stair (3 cells wide, contrasting
  color — `red_carpet` is canonical for "ceremonial")
- **Wall torches** or **lanterns on posts** at each end of the stair (4
  lights: 2 at base, 2 at landing)
- **Banners** on the wall flanking the stair (lord's colors, hanging from
  the ceiling at the level above the landing)
- At the base of the stair, a **welcome runner** of additional carpet
  extending 2-3 cells into the room beyond, signaling "this is the
  ceremonial approach"

## Variations

- **L-shaped grand stair**: two flights at right angles meeting at a
  half-landing. Used when the vertical rise is too tall for a single
  flight (8+ blocks). The half-landing is a 5×5 (or larger) platform.
- **Y-shaped** (single flight up, splits into two at landing): two halves
  diverge to form a Y, leading to two doors at the top. The most ceremonial
  shape. Used in palaces, royal manors.
- **Curved grand stair**: in MC, hard to do — fakeable with 5-wide stair
  blocks placed in a coarse arc. Generally not worth the effort; use
  L-shape instead.

## When NOT to use a grand stair

A grand stair is the wrong fit for:
- Tower interiors (use [spiral_3x3.md](spiral_3x3.md))
- Peasant houses (use a simple `oak_stairs` straight flight 1 wide)
- Service / kitchen access (use a single-row narrow stair)
- Anywhere the player won't deliberately walk slowly

## See also

- [spiral_3x3.md](spiral_3x3.md) — the tower alternative
- [../doors/double_door_great_hall.md](../doors/double_door_great_hall.md) — often at the top of the grand stair
- [../_principles/scale_and_proportion.md](../_principles/scale_and_proportion.md) — doorway pacing
