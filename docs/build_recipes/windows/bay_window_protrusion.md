# Bay Window — single-cell protrusion

**Status:** v0 — observed in references, not yet built
**Provenance:** reference-image (r00, r02, r07, r17, r18, Abfielder houses)
**Best for:** house second-floor windows, tavern fronts, urban facades
**Reads as:** middle-class wealth, occupied space, urban
**Pair with:** [../columns_and_arches/half_timbered_wall.md](../columns_and_arches/half_timbered_wall.md)

## Description

A window that protrudes 1 block outward from the wall plane, supported
below by a corbel (stair-block jutting outward). The window itself sits
in the protruded cell; the wall above continues flat or has a small "roof"
over the bay.

Bay windows give a wall depth and shadow — without them, a half-timbered
facade reads as a flat texture. With them, the building looks inhabited.

## Layout (side view, facing the wall)

```
y+3       ▓ ▓ ▓               <- wall continues above
y+2       ▓▶G◀▓                <- bay window: stairs hood + glass in protruded cell
y+1       ▓▶◤◢◀▓                <- glass below, with small "shelf" stair-block hoods
y+0       ▓ ▟ ▓                <- corbel (stair-block jutting outward, pointed up)
y-1       ▓ ▓ ▓                <- main wall plane
```

Where `▓` is wall, `G` is glass pane, `▟` is a stair-block corbel (acts as
the supporting bracket), `▶◀` are stair-blocks framing the bay.

## Block recipe (for one bay window — 1 wide × 2 tall)

- 1 stair-block corbel (facing outward, half=top, so the slope ramps up
  toward the wall)
- 2 stair-blocks framing the sides of the bay (one on each side, half=top,
  facing inward)
- 2 glass panes (NS or EW orientation depending on wall direction)
- 1 stair-block hood on top (facing outward, half=bottom, slope away from
  wall — sheds rain)

Total: 6 blocks, projecting 1 cell from wall.

## Python snippet

```python
def bay_window(g, wall_x, anchor_y, wall_z, facing, *,
                corbel_stair, side_stair, glass_pane, hood_stair):
    """
    Build a single-bay window protruding 1 cell from the wall.

    facing: 'south' means the wall faces south (the protrusion goes south).
    corbel_stair, side_stair, hood_stair: stair blocks with the appropriate
    facing+half pre-set. Caller is responsible for picking the right ones
    for the wall direction.
    """
    # Direction vector
    dx, dz = {'north': (0, -1), 'south': (0, 1),
              'east':  (1, 0),  'west':  (-1, 0)}[facing]
    out_x = wall_x + dx
    out_z = wall_z + dz

    # y+0: corbel support (sticking out one cell)
    g.setb(out_x, anchor_y, out_z, corbel_stair)
    # y+1: glass + flanking stairs (the bay opening)
    g.setb(out_x, anchor_y + 1, out_z, glass_pane)
    # Flanking stairs (left and right of the bay, on the wall plane)
    # Note: callers can skip these if they want a "floating" bay
    # y+2: glass + small hood
    g.setb(out_x, anchor_y + 2, out_z, glass_pane)
    # y+3: hood block above (sheds rainwater)
    g.setb(out_x, anchor_y + 3, out_z, hood_stair)
```

## Variations

- **Single-glass bay** (simplest, r18): just 1 protruding glass cell with
  stair corbel below and hood above. Tiny and cottage-like.
- **2-wide bay** (r02 multiple, r07): 2 cells wide of glass, two corbels
  below, a larger hood above with stairs meeting at a small peak.
- **3-wide oriel** (r07 town-house centers): 3 cells wide protruding bay
  with its own miniature gabled roof — for important rooms / lord's
  chamber facing the street.
- **Triangular oriel** (r05 castle window): bay window with stair-blocks
  forming a triangular roof above it. Common on cathedral towers.

## Anti-pattern

A wall with too many bay windows reads as a greenhouse. Use bays for:
- The principal/street-facing facade
- Windows above the front door
- Lord's chamber / important rooms

Side and back walls usually have flat windows.

## Iteration history

- v0 (2026-05-15): synthesized from 6 reference images.

## See also

- [diamond_lattice_window.md](diamond_lattice_window.md) — common infill for bay glass
- [../reference_builds/community_batch1_24_patterns.md](../reference_builds/community_batch1_24_patterns.md) — P11
