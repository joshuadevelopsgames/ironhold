# Spiral Stair — 3×3 newel

**Status:** v0
**Provenance:** claude-proposal (universal pattern in MC tower interiors)
**Best for:** tower interiors, keep ascents, watchtower spiral, dark-castle keep
**Reads as:** real medieval vertical circulation; defensive — fights can be funneled into the stair

## Description

A spiral (newel) staircase fits inside a 3×3 footprint. The central column
(the "newel") is a solid stone pillar; the steps spiral around it. Each
full revolution rises 4 blocks (4 stairs × 1 block tall, completing a
360° turn).

This is the standard vertical circulation in any medieval tower. Real
spiral stairs were typically clockwise-ascending (so right-handed
defenders going down had their sword arm free against attackers coming
up, who were jammed by the central column). In MC, clockwise vs.
counter-clockwise is a stylistic choice; pick one per build and stick.

## Layout (one revolution = 4 blocks tall)

Looking down at each layer:

```
Layer y+0:     Layer y+1:     Layer y+2:     Layer y+3:
░ S ░          S ░ ░          ░ ░ S          ░ S ░
░ N ░    →     ░ N ░    →     ░ N ░    →     ░ N ░
░ ░ ░          ░ ░ ░          ░ ░ ░          ░ ░ ░
                                                ↑
where S = stair block, N = newel column, ░ = air
```

So at each y, one stair-block is placed adjacent to the newel column, in
a new cell going clockwise. After 4 cells (4 blocks of height), the stair
returns to the starting orientation, and the next revolution begins.

## Python snippet

```python
def spiral_stair_3x3(g, cx, base_y, cz, height, *,
                     stone, stair_block_id, clockwise=True):
    """
    Build a spiral stair filling a 3x3 footprint, ascending `height` blocks.
    `(cx, base_y, cz)` is the CENTER cell of the bottom layer
    (i.e., the newel column footprint).
    Stairs spiral around the newel column.
    Each y-layer places ONE stair block adjacent to the newel.

    `stair_block_id` is a string like "minecraft:cobbled_deepslate_stairs".
    `stone` is the newel-column block (or factory).
    """
    # The 4 positions around the newel, in clockwise order, starting from south:
    # Each tuple: (dx, dz, stair_facing_when_ascending_into_this_cell)
    cw_positions = [
        ( 0, +1, "north"),   # south of newel — stair faces north (ascends back toward newel)
        (+1,  0, "west"),    # east of newel
        ( 0, -1, "south"),   # north of newel
        (-1,  0, "east"),    # west of newel
    ]
    ccw_positions = list(reversed(cw_positions))
    positions = cw_positions if clockwise else ccw_positions

    for layer in range(height):
        y = base_y + layer

        # 1. Newel column (always solid at the center)
        g.setb(cx, y, cz, stone() if callable(stone) else stone)

        # 2. The single stair block for this layer
        dx, dz, facing = positions[layer % 4]
        g.setb(cx + dx, y, cz + dz, P(f"{stair_block_id}[facing={facing},half=bottom,shape=straight]"))

        # 3. Optional: fill the OPPOSITE cell (the one the stair just came from)
        # with the "previous" stair as a top-half — this creates the smooth
        # surface where the previous step meets the current step.
        # Actually in MC, a 1-step-per-block spiral works without this;
        # leaving it as a simple version for now.
```

## What to do at the floor breaks

When the spiral arrives at a new floor (every 4 layers), you have options:

1. **Continue straight up** — the spiral keeps going without interruption.
   Each floor's door opens off the spiral at a specific orientation. Use
   when towers are skinny / pure-circulation.

2. **Landing + door** — at floor breaks, replace the spiral stair-block
   layer with a flat landing (3×3 solid floor) and the next floor's door
   opens off this landing. Use when each floor is a *destination*, not
   just a place to pass through.

3. **Vertical-only ladder gap** — leave a 1-block gap at the floor break
   and require the player to use a ladder for that one block. Use in
   undercroft-to-ground-floor situations where the lord doesn't want
   easy access from the dungeon up.

## The light placement question

Spiral stairs are dark by default. Two options:

- **Wall torches every 3 layers** on the outer wall — gives even but dim
  light up the spiral
- **One lantern at each landing** — leaves the spiral itself nearly dark,
  with bright pools of light at each floor break

Master builders favor option 2 for dramatic effect. Option 1 is more
"functional / lived in."

## Variations

- **Wider spiral (5×5 footprint)**: same algorithm but the steps go in a
  larger ring. Used in major towers, palace stairs. Rises 8 blocks per
  revolution (8 step-blocks per loop).
- **Square spiral (no newel)**: 5×5 footprint, no central column —
  the stair runs around the outer ring, with an open shaft in the middle.
  Looks more "Disney" than medieval; use for palace / refinement.
- **Stone spiral with `cobbled_deepslate_stairs`**: dark, founding-era
- **Spruce / dark-oak spiral**: wooden stair, used in older or poorer towers

## See also

- [grand_stair_5w.md](grand_stair_5w.md) — for ceremonial / public stairs
- [../reference_builds/castles_grand_study.md](../reference_builds/castles_grand_study.md) §4 (the keep)
