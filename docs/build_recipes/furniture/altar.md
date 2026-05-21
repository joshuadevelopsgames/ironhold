# Altar — sacred focal point

**Status:** v0
**Provenance:** claude-proposal
**Best for:** chapel, cathedral apse, shrine, dark-castle inner sanctum
**Reads as:** sacred, the room's gravity well

## Description

A raised stone block, flanked by symmetric candles, with one focal element
behind it (cross, idol, single hanging lantern, or stained-glass window).
The altar is the chapel's *only* fully decorated surface — everything else
in the room serves to direct the eye here.

Standard altar: 2 blocks wide × 1 deep × 1 tall, on a small dais (3×2×1).
For a cathedral, scale up to 3×2×1 on a 5×4×1 dais.

## Layout (front elevation)

```
y+4              ◯              ← stained glass / hanging lantern (single focal)
y+3        ▓ ✚ ▓                ← cross (or icon, or skull for dark altar) on plinth
y+2        ▓ ▓ ▓                ← altar back plinth
y+1     ░╞ALTAR╡░               ← altar block (top is a slab)
y+0     ░░DAIS░░                ← dais step
        ─────────────
```

## Python snippet

```python
def altar(
    g, anchor_x, anchor_y, anchor_z, facing="south", *,
    altar_block,           # e.g., polished_diorite for cathedral, blackstone for dark
    dais_block,            # same as altar or one step darker
    altar_top_slab,        # slab block, top-type, finer than altar_block
    candle_block,          # candle name ("white" for chapel, "red" for dark)
    candle_count_each_side=3,
    focal_block=None,      # cross block (often a vertical pair of stripped logs + horizontal slab)
    overhead_light_block=None,
):
    """
    Build an altar centered on (anchor_x, anchor_y, anchor_z).
    Anchor is the FRONT-CENTER cell of the dais (where worshippers approach).
    """
    # Direction-aware deltas
    back_dx, back_dz = 0, 0
    if facing == "south": back_dz = 1
    elif facing == "north": back_dz = -1
    elif facing == "east":  back_dx = 1
    elif facing == "west":  back_dx = -1

    # Dais (3 wide × 2 deep × 1 tall)
    for dx in range(-1, 2):
        for dd in range(2):
            g.setb(anchor_x + dx, anchor_y, anchor_z + dd * back_dz + dd * back_dx,
                   dais_block() if callable(dais_block) else dais_block)

    # Altar block (2 wide × 1 deep × 1 tall, on top of dais, 1 block back from front edge)
    altar_y = anchor_y + 1
    for dx in (-1, 0, 1):
        g.setb(anchor_x + dx, altar_y, anchor_z + back_dx + back_dz,
               altar_block() if callable(altar_block) else altar_block)
    # Top slab gives the altar a finer surface
    for dx in (-1, 0, 1):
        g.setb(anchor_x + dx, altar_y + 1, anchor_z + back_dx + back_dz,
               altar_top_slab)

    # Candles flanking — at each end of the altar top
    for i in range(candle_count_each_side):
        # Stacked candles read as a single multi-flame
        pass  # MC candles stack via the `candles` block-state property
    g.setb(anchor_x - 1, altar_y + 1, anchor_z + back_dx + back_dz + 1,
           P(f"minecraft:{candle_block}_candle[candles={min(4, candle_count_each_side)},lit=true]"))
    g.setb(anchor_x + 1, altar_y + 1, anchor_z + back_dx + back_dz + 1,
           P(f"minecraft:{candle_block}_candle[candles={min(4, candle_count_each_side)},lit=true]"))

    # Focal element (cross or alternative) — 1 block back of the altar, 2 tall
    if focal_block is not None:
        focal_x = anchor_x
        focal_z = anchor_z + 2 * back_dz
        focal_dx = 2 * back_dx
        for dy in (0, 1):
            g.setb(focal_x + focal_dx, altar_y + dy, focal_z, focal_block)

    # Overhead light (1 hanging lantern centered above altar)
    if overhead_light_block is not None:
        g.setb(anchor_x, altar_y + 4, anchor_z + back_dx + back_dz, overhead_light_block)
```

## Focal-element vocabulary by tradition

| Tradition | Focal-element above/behind altar |
|-----------|----------------------------------|
| Christian-medieval chapel | A cross — vertical `stripped_dark_oak_log[axis=y]` 2 tall + horizontal `stripped_dark_oak_log[axis=x]` 3 wide at top |
| Cathedral high altar | A large rose window — see windows/ (rose_window TODO) — backlit by stained glass |
| Dark altar (heresy / cult) | A `wither_skeleton_skull` on a `dark_oak_fence` post (the post centered on the altar back) |
| Druid / pagan stone | A single `mossy_cobblestone` standing-stone (3 blocks tall, irregular) behind the altar |
| Sun / solar shrine | A `glowstone` block above the altar with a chiseled gold motif (`gold_block` flanked by `chiseled_quartz_block`) |
| Skeletal / lich altar | `bone_block[axis=y]` standing stones — 4 of them flanking, with red candles |

## Standard accompanying detail

- A `book_and_quill`-style item-display (use `item_frame[fixed=true,invisible=false]` with a written book) at the foot of the altar
- A `lectern` 2 blocks in front of the altar (where the celebrant stands)
- Stained-glass windows (cathedral palette) in the wall behind, programmed
  to match the season / saint
- A small kneeling stool / `oak_pressure_plate` on a slab, centered on the
  axis approach to the altar (1 block from the dais front)

## Variations

- **Side-chapel altar**: a smaller altar in a side aisle. Footprint 1×1
  altar block on a 3×2 dais. One candle on each end. No focal-element-behind.
- **Travel altar** (mage / pilgrim chapel): a `chiseled_polished_blackstone`
  block on a 1×1 dais with a single `lit_candle`. No flanking candles.
- **Sacrifice altar** (dark): replace altar top slab with `redstone_block`
  (looks like blood-stained stone), add `redstone_torch` cluster at the
  base, use `dripstone_block` standing stones (the dripstone above the
  altar suggests blood-drip).

## The "axial approach" rule

Altars are *always* centered on the room's long axis, at the far end from
the door. The viewer enters → walks the long axis → arrives at the altar.
Everything else in the room (pews, side chapels, decorative arches) sits
to the sides of this axis.

If a chapel has the altar off-axis, it reads as wrong — even non-religious
viewers feel the shape is off because all our cultural sacred-space
references are axial.

## See also

- [../_principles/master_builders_compass.md](../_principles/master_builders_compass.md) — Adamantis axial focal-point design
- [../palettes/cathedral.md](../palettes/cathedral.md)
- [../palettes/dark_castle.md](../palettes/dark_castle.md)
