# Half-Timbered Wall (Fachwerk)

**Status:** v0 — derived from reference images, not yet built in-game
**Provenance:** reference-image (Abfielder houses via schemat.io)
**Best for:** house upper floors, tavern walls, guild buildings, any non-fortified vernacular building
**Reads as:** lived-in, period-correct, German/English vernacular
**Pair with:** [../roofs/pitched_gable_stair_45.md](../roofs/pitched_gable_stair_45.md), stone-base ground floor

## Description

A wall pattern that mimics historical timber-frame construction. The wall
is a **grid of dark wooden beams** with **lighter wood-plank infill** filling
each grid cell. The visual rhythm of dark beam → light infill → dark beam
is what makes the wall read as "constructed" rather than "extruded."

Three variants from least to most detailed:

1. **Vertical-only**: dark oak logs every 3 cells, plank infill between.
2. **Vertical + horizontal**: as above, plus a horizontal log course at
   each floor height.
3. **Vertical + horizontal + cross-brace**: add diagonal cross-braces in
   selected panels (made from stair blocks angled across the panel).

The Abfielder tavern uses variant 3 on its prominent front face.

## Block recipe

For a wall section that's `W` blocks wide and `H` blocks tall:

```
beam_block       = minecraft:dark_oak_log[axis=y]            # vertical beams
horiz_beam       = minecraft:dark_oak_log[axis=x]            # horizontal beams (axis depends on wall orientation)
infill_block     = minecraft:stripped_oak_planks (or birch_planks, jungle_planks)
brace_stair      = stairs block matching beam_block color, angled
```

## Layout (variant 2 — 7-wide × 4-tall section)

```
y+3   ▓ ░ ░ ▓ ░ ░ ▓     <- vertical beams at x=0,3,6; infill between
y+2   ▓ ░ ░ ▓ ░ ░ ▓
y+1   ▓ ░ ░ ▓ ░ ░ ▓
y+0   ═ ═ ═ ═ ═ ═ ═     <- horizontal beam at floor line (or use horiz log)
```

`▓` = vertical log, `░` = plank infill, `═` = horizontal log

## Layout (variant 3 — same, with cross-braces on alternating panels)

```
y+3   ▓ ░ ╱ ▓ ╲ ░ ▓     <- diagonal stair-block braces
y+2   ▓ ╱ ░ ▓ ░ ╲ ▓
y+1   ▓ ░ ░ ▓ ░ ░ ▓
y+0   ═ ═ ═ ═ ═ ═ ═
```

`╱` and `╲` are stairs blocks angled to suggest a diagonal beam crossing
the panel.

## Python snippet

```python
def half_timbered_wall(g, x0, y0, z0, x1, y1, z, facing, *,
                        beam_v, beam_h, infill, brace_lt=None, brace_rt=None,
                        variant=2, beam_spacing=3):
    """
    Build a half-timbered wall along z (one cell deep, spanning x0..x1, y0..y1).
    facing = 'north' | 'south' | 'east' | 'west'  — picks the right brace stair facings.

    variant: 1 (vertical only), 2 (vertical + horizontal), 3 (with diagonal braces)
    beam_spacing: every Nth column is a vertical log (default 3 — so panels are 2 wide)
    """
    # Fill the wall with infill first
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            g.setb(x, y, z, infill)

    # Vertical beams every `beam_spacing` columns
    for x in range(x0, x1 + 1, beam_spacing):
        for y in range(y0, y1 + 1):
            g.setb(x, y, z, beam_v)

    # Horizontal beams at top and bottom (variant 2+)
    if variant >= 2:
        for x in range(x0, x1 + 1):
            g.setb(x, y0, z, beam_h)
            g.setb(x, y1, z, beam_h)

    # Diagonal cross-braces on alternating panels (variant 3)
    if variant == 3 and brace_lt is not None and brace_rt is not None:
        panel_idx = 0
        for px in range(x0 + 1, x1, beam_spacing):
            panel_w = min(beam_spacing - 1, x1 - px)
            panel_h = y1 - y0
            # Pick direction by panel index
            for i in range(min(panel_w, panel_h)):
                bx = px + (i if panel_idx % 2 == 0 else (panel_w - 1 - i))
                by = y0 + 1 + i
                if by >= y1: break
                g.setb(bx, by, z, brace_lt if panel_idx % 2 == 0 else brace_rt)
            panel_idx += 1
```

## Iteration history

- v0 (2026-05-15): captured from Abfielder reference. Untested. The brace
  facing convention may need flipping per wall orientation.

## See also

- [../reference_builds/medieval_houses_abfielder.md](../reference_builds/medieval_houses_abfielder.md) — source
- [../_principles/material_storytelling.md](../_principles/material_storytelling.md) — vertical change rule
