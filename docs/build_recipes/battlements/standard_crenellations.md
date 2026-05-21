# Standard Crenellations — merlon-and-crenel parapet

**Status:** v0
**Provenance:** claude-proposal (universal real-world pattern + every MC castle reference)
**Best for:** curtain walls, tower tops, fortified wall tops
**Reads as:** defensive, "this is a wall built for archers"
**Pair with:** [machicolations.md](machicolations.md) below the parapet on gatehouses

## Description

The classic castle-top pattern: solid blocks (**merlons**) alternating
with gaps (**crenels**). The defender hides behind the merlon and shoots
through the crenel. The pattern is always **merlon-crenel-merlon-crenel**;
never two merlons in a row, never two crenels in a row.

### Dimensions that read right

- Merlon: **1 wide × 2 tall** (atop the wall, so the merlon top is at
  wall_top + 2)
- Crenel: **1 wide × 1 deep** (the gap between merlons, 1 block
  lower than the merlon top)
- Wall-walk floor: at wall_top - 1 (the defender walks 1 block below
  the top of the merlon, with the crenel at their chest height)

The wall is 2 blocks thick: outer course (with crenellations on top) and
inner course (also 1 block tall, no crenellations). The walk goes between.

## Layout (cross-section through the wall)

```
y+2     M . M . M . M . M     ← merlons, crenels (.) alternate
y+1     ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓     ← top of wall (every block)
y+0     ─ ─ ─ ─ ─ ─ ─ ─ ─     ← wall-walk floor (defender stands here)
        ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓     ← parapet base (interior face — no crenel)
        ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓
        ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓
        ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓
            (wall continues down)
```

Looking along the wall from above:

```
M . M . M . M . M . M . M     ← outer face — crenellated
▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓     ← wall-walk floor (1 block wide minimum, 2 for comfort)
▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓ ▓     ← inner face (no crenellations — defender's back)
```

## Python snippet

```python
def standard_crenellations(g, x0, y_top, z, length, axis="x", *,
                           stone, accent_slab=None):
    """
    Place crenellations along a wall.
    (x0, y_top, z) is the START cell of the OUTER course at the wall's TOP.
    `axis` = "x" or "z" indicating direction the wall runs.
    `length` = number of blocks along the wall.

    Result:
        outer-face course gets merlon-crenel-merlon-crenel pattern at y_top..y_top+1
        the cell below (y_top - 1) stays as the wall-walk top.
    """
    for i in range(length):
        # Position of this cell on the outer course
        cx = x0 + (i if axis == "x" else 0)
        cz = z  + (i if axis == "z" else 0)
        is_merlon = (i % 2 == 0)
        if is_merlon:
            # 2 blocks tall (the merlon itself)
            g.setb(cx, y_top,     cz, stone() if callable(stone) else stone)
            g.setb(cx, y_top + 1, cz, stone() if callable(stone) else stone)
            # Optional accent slab on the very top of the merlon
            if accent_slab is not None:
                # Some master builders cap each merlon with a slab[type=bottom]
                # for a more refined Italian crenellation look
                g.setb(cx, y_top + 2, cz, accent_slab)
        else:
            # Crenel — 1 block tall (the parapet base only)
            g.setb(cx, y_top, cz, stone() if callable(stone) else stone)
            # The cell above (y_top + 1) is left air — that's the gap
```

## Variations

### Italian "swallow-tail" merlon (Ghibelline)

Each merlon is V-cut on top — the top block is replaced with two opposing
upper-half stairs forming a notch. Reads as Italian Renaissance fortress
(Sforza castle, Verona walls).

```
y+2   /\ . /\ . /\ . /\     ← /\ is upper stair-pair forming notch
y+1   ▓  ▓ ▓  ▓ ▓  ▓ ▓  ▓
```

### Norman square-and-tall merlon

Merlons are 1 wide × 3 tall (not 2). Used on older castles with tall narrow
crenels meant for crossbow rather than longbow defenders.

### Brick / decorative crenellation

The merlons are made of a different (smaller, finer) material than the
wall — `chiseled_polished_blackstone`, for example, over a curtain wall of
`polished_blackstone_bricks`. Used on dark-castle gatehouses for a refined
top-edge.

### Capped merlon

Each merlon's top block is replaced with a slab to soften the silhouette.
Slightly less defensive-looking; more residential / palace.

## Crenellation length and spacing

| Wall length | Crenellation pattern |
|------------|---------------------|
| Short (< 12 blocks) | merlon at each end, alternating — e.g. M.M.M.M.M.M.M = 4 merlons, 3 crenels |
| Medium (12-30) | alternating pattern continued — every other block |
| Long (> 30) | alternating; consider inserting a *wider* merlon (2-block wide) every 8–10 cells to break the rhythm |

The two endpoints of a wall should be merlons (not crenels) — the corner
needs the defender's cover, and a crenel at a corner is structurally weird.

## Anti-patterns

- **Stair-block tops** — using upside-down stair blocks to form the
  crenellations. Looks wrong because crenels should be square 1-block gaps,
  not angled. Save stairs for cornices / slope details.
- **Three crenels in a row** — the wall starts to look like a comb. The
  rhythm requires 1:1 alternation.
- **No crenels at corners** — corners need extra defensive coverage; both
  edges of a corner should terminate in a merlon.
- **Merlon at every other block but only 1 tall** — looks like a low garden
  wall, not a battlement. Merlons MUST be 2 tall to read as defensive.

## See also

- [machicolations.md](machicolations.md)
- [../reference_builds/castles_grand_study.md](../reference_builds/castles_grand_study.md) §5
