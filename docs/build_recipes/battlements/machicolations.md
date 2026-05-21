# Machicolations — overhanging floor with drop-holes

**Status:** v0
**Provenance:** claude-proposal (real-world medieval engineering + MC castle references)
**Best for:** gatehouse tops, drum-tower tops, key sections of curtain wall (over a wall-base attacker is trying to climb)
**Reads as:** *serious* fortification, distinct from a "decorative castle"

## Description

A machicolation is an *overhanging* parapet floor with gaps in it,
projecting from the wall on a row of corbels (angled supports). Defenders
on the wall-walk drop rocks, hot oil, or arrows through the gaps onto
attackers right below the wall base.

In Minecraft block terms: a course of stair-blocks projects outward from
the top of the wall, supporting a 1-block-thick floor above the wall.
Every other cell of that floor is *missing* — those are the drop-holes.
The crenellated parapet sits on the outer edge of this projecting floor.

This is the visual mark of a real fortress. Castles without
machicolations read as "show castles." A small build can do machicolations
over the gatehouse only, which is the minimum for the build to look
serious.

## Layout (cross-section, looking along the wall)

```
                          OUTSIDE
y+5                       M . M . M     ← crenellations on the overhang
y+4                       ▓ ░ ▓ ░ ▓     ← overhang floor with drop-holes (░)
y+3                       /          ← corbel (stair-block, slope outward)
y+2                       ▓ wall_top  ← top of the wall (defender stands here)
y+1                       ▓
y+0                       ▓ (wall continues down)
                          INSIDE
```

Looking down from above onto the overhang:

```
←OUTSIDE                           ←INSIDE
M . M . M . M . M . M . M     ← crenellations on outer edge
▓ ░ ▓ ░ ▓ ░ ▓ ░ ▓ ░ ▓ ░ ▓     ← overhang floor (1 wider than wall) — ░ are drop-holes
▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓     ← top of wall, defender's walk
```

## Python snippet

```python
def machicolated_parapet(g, x0, y_wall_top, z, length, axis="x", *,
                         stone, corbel_stair, hole_every=2):
    """
    Build a machicolated overhang on top of an existing wall.
    `(x0, y_wall_top, z)` is the OUTER cell of the wall's TOP, where the
    corbel will attach.
    `axis` = "x" or "z".
    `length` = number of blocks of wall to machicolate.

    Result:
        Row of corbels (stair blocks, sloping outward) at y_wall_top
        Overhang floor at y_wall_top + 1, with drop-holes every `hole_every` cells
        Crenellations on the outer edge of the overhang (call separately)
    """
    # 1. Corbel row — stair blocks projecting outward
    for i in range(length):
        cx = x0 + (i if axis == "x" else 0)
        cz = z  + (i if axis == "z" else 0)
        # Corbel sits 1 block OUTSIDE the wall top, slope-facing inward
        if axis == "x":
            cor_z = cz - 1   # assume "outside" is -z; flip for opposite face
            g.setb(cx, y_wall_top, cor_z, P(f"{corbel_stair}[facing=south,half=top]"))
        else:
            cor_x = cx - 1
            g.setb(cor_x, y_wall_top, cz, P(f"{corbel_stair}[facing=east,half=top]"))

    # 2. Overhang floor at y_wall_top + 1, with drop-holes every `hole_every` cells
    for i in range(length):
        cx = x0 + (i if axis == "x" else 0)
        cz = z  + (i if axis == "z" else 0)
        if axis == "x":
            over_z = cz - 1
            if i % hole_every == 1:
                # Drop-hole: leave air at this cell
                continue
            g.setb(cx, y_wall_top + 1, over_z,
                   stone() if callable(stone) else stone)
        else:
            over_x = cx - 1
            if i % hole_every == 1:
                continue
            g.setb(over_x, y_wall_top + 1, cz,
                   stone() if callable(stone) else stone)

    # 3. Caller follows up with standard_crenellations on the outer edge
    # of the overhang floor (at y_wall_top + 2 / +3)
```

## Where to use machicolations

Machicolations are *expensive* in real-world terms (more stone, more
masonry skill). Medieval castles used them selectively:

- **Always**: above every gatehouse, on all 3 sides facing outward
- **Usually**: drum-tower tops (full ring around the tower)
- **Sometimes**: long stretches of curtain wall where attackers can approach
  the base (e.g., the wall facing the road)
- **Rarely**: full perimeter machicolation — only on the richest, most
  modern fortresses

In Minecraft, a sensible rule: machicolate the gatehouse top + the 4
corner tower tops. Leave the rest of the curtain wall with standard
crenellations only.

## Variations

- **Hourding** (wooden machicolation): the same overhang made of wood
  instead of stone — used in real life by less wealthy castles or as
  a quick add-on. In MC: use `dark_oak_planks` for the overhang floor,
  `dark_oak_stairs` for corbels. Add `dark_oak_slab` shingles on a small
  pitched roof above. Looks like a wooden gallery encircling the tower
  top.
- **Decorative machicolation** (refinement era): the drop-holes are
  closed by inset chiseled blocks, and the corbels become purely
  decorative. Tells the viewer the castle has retired from active defense
  but kept the silhouette.

## Anti-patterns

- **Drop-holes too rare** (every 4 cells): the overhang reads as a flat
  ledge, not a fighting platform. 1-in-2 cell holes is the visual minimum.
- **No corbels — overhang floats**: looks like a stage prop. Corbels are
  the structural detail that makes the overhang readable.
- **Machicolation around an entire small castle**: overkill — every part of
  the castle reads as "most defended part." Use selectively.

## See also

- [standard_crenellations.md](standard_crenellations.md) — placed on top of the machicolation
- [../doors/portcullis_gatehouse.md](../doors/portcullis_gatehouse.md) — the gatehouse below the machicolation
- [../reference_builds/castles_grand_study.md](../reference_builds/castles_grand_study.md) §6
