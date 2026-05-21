# Double Door — great-hall entrance

**Status:** v0
**Provenance:** claude-proposal (universal pattern in great-hall references)
**Best for:** great hall, chapel west door, throne room, banquet hall
**Reads as:** ceremonial, public, important

## Description

A pair of doors, hinged at opposite sides, with a stone arch / hood above.
Standard for any room where multiple people may enter at once and where
the room is *public* (vs. a lord's private chamber, which gets a single
door).

The door pair is centered in a 3-block-wide opening framed by stone trim.

## Layout

```
y+4   ╔═══T═══╗     T = trim block (stair, slope-down outward)
y+3   ║ ▓ ▓ ▓ ║     ▓ = chiseled stone or trim
y+2   ║▓▓D D▓▓║     D = upper door half
y+1   ║▓▓D D▓▓║     D = lower door half
y+0   ▓▓▓▓▓▓▓▓▓     floor + threshold
        ↑ ↑
        L R         hinge directions (left-hinge / right-hinge pair)
```

Width: 3 blocks (1 left trim + 1 left door + 1 right door + 1 right trim
... actually the doors are each 1 wide, so the opening is 2 doors wide,
plus 1 trim each side = 4 blocks total wall section, with the 2 inner
columns being the door pair).

Let's just be clear: opening is **2 blocks wide × 2 blocks tall** for the
doors themselves; the FRAME extends 1 block in each direction.

## Python snippet

```python
def double_door_great_hall(
    g, anchor_x, anchor_y, anchor_z, facing="south", *,
    door_block_id="minecraft:dark_oak_door",
    trim_block=None, hood_stair=None
):
    """
    Place a double door at the anchor.
    anchor = lower-left block of the LEFT door (NOT the trim).
    facing: which way the doors face when opening outward
            ("north"/"south"/"east"/"west")

    Trim is placed in the columns 1-left and 1-right of the doors.
    Hood is a stair-block above the doors, pointing down-and-outward.
    """
    facing_dir_offset = {
        "south": (0, +1), "north": (0, -1),
        "east": (+1, 0), "west": (-1, 0),
    }[facing]

    # Doors (left and right)
    dx, dz = anchor_x, anchor_z
    # Left door: hinge=left
    g.setb(dx, anchor_y, dz,
        P(f"{door_block_id}[half=lower,hinge=left,facing={facing},open=false]"))
    g.setb(dx, anchor_y + 1, dz,
        P(f"{door_block_id}[half=upper,hinge=left,facing={facing},open=false]"))
    # Right door: hinge=right
    g.setb(dx + 1, anchor_y, dz,
        P(f"{door_block_id}[half=lower,hinge=right,facing={facing},open=false]"))
    g.setb(dx + 1, anchor_y + 1, dz,
        P(f"{door_block_id}[half=upper,hinge=right,facing={facing},open=false]"))

    # Trim (sides)
    if trim_block is not None:
        for dy in range(0, 2):
            g.setb(dx - 1, anchor_y + dy, dz, trim_block() if callable(trim_block) else trim_block)
            g.setb(dx + 2, anchor_y + dy, dz, trim_block() if callable(trim_block) else trim_block)

    # Hood (above the doors)
    if hood_stair is not None:
        # 4-wide stair hood (sloping downward from the wall, outward)
        for dxoff in range(-1, 3):
            g.setb(dx + dxoff, anchor_y + 2, dz, hood_stair)
```

## Variations

- **Cathedral west door**: scale up to 3 blocks wide × 3 tall, but MC doors
  don't natively go that tall. Substitute: hide the door behind a 3×3 trim
  opening, with two normal 1×2 doors centered as a "smaller door within the
  great door" gesture. Real cathedrals do this — a "pedestrian door" inset
  in the great west door.
- **Throne-room door**: same recipe but flanked by 2 hanging banners
  (lord's colors) and 2 wall torches (or soul torches for dark castle).
- **Dark-castle inner door**: same recipe but `dark_oak_door`, hood stair
  is `polished_blackstone_brick_stairs`, no decorative banners — austerity
  reads as ominous.

## See also

- [portcullis_gatehouse.md](portcullis_gatehouse.md) — for the outer gate
- [peasant_plank_door.md](peasant_plank_door.md) — for single-door rooms
- [../_principles/scale_and_proportion.md](../_principles/scale_and_proportion.md) — doorway pacing rule
