# Canopy Bed — lord's bedchamber

**Status:** v0
**Provenance:** claude-proposal
**Best for:** lord's solar / bedchamber, queen's chamber, wizard's quarters
**Reads as:** wealthy, private, status

## Description

A 4-poster canopy bed: a Minecraft `bed` block surrounded by four corner
posts (vertical logs) supporting a fabric canopy (carpet on the underside
of a horizontal stair/slab frame). Side curtains drape from the canopy.

Compared to a peasant bed (just the `bed` block alone): the canopy adds
height, frames the bed as the room's focal point, and signals the
occupant's wealth.

## Layout

```
y+5     ▓▓▓▓       canopy frame (slabs)
y+4    ▓░▓░▓       canopy underside (carpet)
y+3    │ ▓ │       side curtain hint (wool or banner)
y+2    │ ▓ │       posts continue
y+1    │ B │       Bed top half
y+0    │ B │       Bed bottom half + posts at corners
       └───┘
       (footprint is 3 wide × 3 deep — 2-block bed in center, 1-block posts at corners)
```

The bed itself is 1 wide × 2 long. The canopy footprint is 3 × 4 — bed plus
1 cell at each long-side corner + 1 cell at head and foot. Posts go in
the 4 corners.

## Python snippet

```python
def canopy_bed_lord(
    g, anchor_x, anchor_y, anchor_z, facing="south", *,
    bed_color="red",
    post_block=None,           # e.g., dark_oak_log[axis=y]
    canopy_slab=None,          # slab block for the canopy (top type)
    canopy_carpet=None,        # carpet for the canopy underside (use a deep color)
    side_curtain_banner=None,  # wall-banner block for side drapes (optional)
):
    """
    Build a canopy bed.
    Anchor (x,y,z) is the FOOT of the bed (the cell where the player's feet go).
    Bed extends 1 block in `facing` direction toward the head.
    """
    # 1. The bed itself
    head_dx = 0; head_dz = 0
    if facing == "south": head_dz = 1
    elif facing == "north": head_dz = -1
    elif facing == "east":  head_dx = 1
    elif facing == "west":  head_dx = -1

    # Foot half
    g.setb(anchor_x, anchor_y, anchor_z,
        P(f"minecraft:{bed_color}_bed[facing={facing},part=foot,occupied=false]"))
    # Head half
    g.setb(anchor_x + head_dx, anchor_y, anchor_z + head_dz,
        P(f"minecraft:{bed_color}_bed[facing={facing},part=head,occupied=false]"))

    # 2. Four corner posts
    # The posts sit 1 block away from the bed corners on each long side
    if facing in ("south", "north"):
        # Bed is along the z-axis. Posts go at:
        #   (x-1, y, z) and (x+1, y, z) -- foot end posts
        #   (x-1, y, z+head_dz) and (x+1, y, z+head_dz) -- head end posts
        post_positions = [
            (anchor_x - 1, anchor_z),
            (anchor_x + 1, anchor_z),
            (anchor_x - 1, anchor_z + head_dz),
            (anchor_x + 1, anchor_z + head_dz),
        ]
    else:
        post_positions = [
            (anchor_x, anchor_z - 1),
            (anchor_x, anchor_z + 1),
            (anchor_x + head_dx, anchor_z - 1),
            (anchor_x + head_dx, anchor_z + 1),
        ]

    for px, pz in post_positions:
        for dy in (0, 1, 2, 3):
            g.setb(px, anchor_y + dy, pz, post_block)

    # 3. Canopy frame — slabs forming a rectangle at y+4
    canopy_y = anchor_y + 4
    for px, pz in post_positions:
        g.setb(px, canopy_y, pz, canopy_slab)
    # Connect the posts with slab rails
    if facing in ("south", "north"):
        # Long edges
        for dz_off in range(min(p[1] for p in post_positions),
                            max(p[1] for p in post_positions) + 1):
            g.setb(anchor_x - 1, canopy_y, dz_off, canopy_slab)
            g.setb(anchor_x + 1, canopy_y, dz_off, canopy_slab)
        # Short edges
        for dx_off in (-1, 0, 1):
            g.setb(anchor_x + dx_off, canopy_y, anchor_z, canopy_slab)
            g.setb(anchor_x + dx_off, canopy_y, anchor_z + head_dz, canopy_slab)

    # 4. Canopy underside (carpet) — covers the inside cells of the frame
    if canopy_carpet is not None:
        # Carpet goes one block below the frame, attached underneath via the slab top
        # In MC, carpet floats only on the top of a block; we put it
        # ABOVE the lower slab. So carpet at canopy_y is the "underside" visually.
        # (Carpet floats above a slab[type=bottom] looks like a hung cloth.)
        for dx_off in (-1, 0, 1):
            for dz_off in range(min(p[1] for p in post_positions),
                                max(p[1] for p in post_positions) + 1):
                # Only fill the INNER cells (not where the slab frame is)
                if dx_off == 0:  # bed centerline only — leaves an L-shaped fringe
                    g.setb(anchor_x + dx_off, canopy_y, dz_off, canopy_carpet)

    # 5. Optional side curtain — wall banner hung at post height, facing inward
    if side_curtain_banner is not None:
        # Mount a banner on one of the foot-end posts, facing the bed centerline
        side_facing = "west" if facing == "south" else "east"  # rough; tune per build
        g.setb(anchor_x - 1, anchor_y + 2, anchor_z,
               P(f"{side_curtain_banner}[facing={side_facing}]"))
        g.setb(anchor_x + 1, anchor_y + 2, anchor_z,
               P(f"{side_curtain_banner}[facing={'east' if side_facing == 'west' else 'west'}]"))
```

## Variations

- **Peasant bed** (no canopy): just the `bed` block. No posts, no canopy.
- **Wizard's bed** (slight twist): replace `dark_oak_log` posts with
  `stripped_dark_oak_log` topped with `end_rod` (the rod glows = magical
  bed). Canopy carpet is `purple_carpet`.
- **Sickbed** (dramatic): the bed is centered in the room with a single
  candle at the head (lord is dying). No canopy frame — just the bed
  alone with `cobweb` blocks in the upper corners of the room.
- **Empty / haunted bed**: the bed is made (block is placed) but covered
  in `cobweb`, with a `wither_skeleton_skull` on a fence post at the foot
  — the room's previous occupant is gone, dark-castle style.

## See also

- [throne_on_dais.md](throne_on_dais.md)
- [../palettes/refinement_era.md](../palettes/refinement_era.md)
