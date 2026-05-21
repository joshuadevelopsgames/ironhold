# Portcullis Gatehouse — fortified entry passage

**Status:** v0 — synthesized for the castle reference study
**Provenance:** claude-proposal
**Best for:** castle main gate, fortified manor entry, dark castle gate
**Reads as:** serious defense, "this castle expects to be attacked"
**Pair with:** [../battlements/machicolations.md](../battlements/machicolations.md) (above the passage), [../battlements/standard_crenellations.md](../battlements/standard_crenellations.md)

## Description

A gatehouse is the single most defended building in a castle. Its job is
to be the only opening in the curtain wall, and to be so hostile to attack
that breaching it costs the attacker more than the castle is worth.

This recipe builds the *passage* through a gatehouse — two flanking towers
flank a vaulted passage that contains, in order:

1. **Drawbridge** (front) — retractable wooden bridge or trapdoor planks
2. **Outer portcullis** — vertical iron grate at the outer face
3. **Murder hole** — gap in the passage ceiling
4. **Inner portcullis** — second vertical grate
5. **Inner double doors** — heavy oak doors, normally closed

The passage is 4 wide × 6 long × 4 tall. The two flanking towers (built
separately, see [../reference_builds/castles_grand_study.md](../reference_builds/castles_grand_study.md))
hold the portcullis-raising mechanisms above.

## Layout (top-down)

```
              outside (attacker)
                    ↓
        ┌──┐ drawbridge ┌──┐
        │T1│ ═══════════ │T2│   ← towers flank
        ├──┤             ├──┤
        │  │  ▓▓▓▓▓▓▓▓   │  │   ← outer portcullis (iron_bars)
        │  │  passage    │  │
        │  │  ░ MURDER   │  │   ← murder hole in ceiling
        │  │  ░░░ HOLE   │  │
        │  │  passage    │  │
        │  │  ▓▓▓▓▓▓▓▓   │  │   ← inner portcullis
        │  │  ░░░░░░░░   │  │
        │  │  ║║║║║║║║   │  │   ← inner double doors (dark_oak)
        ├──┤             ├──┤
        │T1│             │T2│
        └──┘             └──┘
                    ↑
              inside (courtyard)
```

## Side view (passage cross-section)

```
y+4    ███ ROOF/MACHICOLATION ███
y+3    ▓ ▓ ░ MURDER HOLE ░ ▓ ▓   <- ceiling drops a block at the hole
y+2    ░   ░               ░  ░  <- normal ceiling height (passage open)
y+1    ░   ░               ░  ░
y+0    ▓▓▓▓▓ floor (stone) ▓▓▓▓
       ↑     ↑     ↑     ↑
       outer outer murder inner
       face  portc hole   portc
```

## Python snippet

```python
def portcullis_gatehouse_passage(
    g, anchor_x, anchor_y, anchor_z, *,
    stone, dark_wood_door, passage_w=4, passage_len=6, passage_h=4
):
    """
    Build the gate PASSAGE only (not the flanking towers).
    Anchor is at the floor of the inside-front-left corner of the passage.

    Required blocks:
      - stone: a block (or block factory) for the passage walls and ceiling
      - dark_wood_door: door block for the inner doors
    """
    # 1. Floor — slightly recessed (1 deep below entry) for drawbridge gap effect
    for x in range(passage_w):
        for z in range(passage_len):
            g.setb(anchor_x + x, anchor_y, anchor_z + z, stone() if callable(stone) else stone)

    # 2. Side walls (the inner faces of the flanking towers)
    # Caller's tower builds will produce these; we only set ceilings here.

    # 3. Ceiling (with a murder hole in the middle of the passage)
    ceil_y = anchor_y + passage_h
    hole_z = anchor_z + passage_len // 2
    hole_x = anchor_x + passage_w // 2
    for x in range(passage_w):
        for z in range(passage_len):
            if x == passage_w // 2 and (z == passage_len // 2 - 1 or z == passage_len // 2):
                continue  # leave the 1x2 murder hole open to the floor above
            g.setb(anchor_x + x, ceil_y, anchor_z + z, stone() if callable(stone) else stone)

    # 4. Outer portcullis (at the front)
    for x in range(passage_w):
        for y in range(anchor_y + 1, anchor_y + passage_h):
            g.setb(anchor_x + x, y, anchor_z, P("minecraft:iron_bars"))

    # 5. Inner portcullis (at the back-but-not-quite — 1 block from the back)
    inner_portc_z = anchor_z + passage_len - 2
    for x in range(passage_w):
        for y in range(anchor_y + 1, anchor_y + passage_h):
            g.setb(anchor_x + x, y, inner_portc_z, P("minecraft:iron_bars"))

    # 6. Inner double doors (at the back)
    door_z = anchor_z + passage_len - 1
    door_left_x = anchor_x + passage_w // 2 - 1
    door_right_x = anchor_x + passage_w // 2
    # In MC, doors are 2 blocks tall; placed pair-wise (left-hinge + right-hinge)
    g.setb(door_left_x, anchor_y + 1, door_z,
        P("minecraft:dark_oak_door[half=lower,hinge=left,facing=south,open=false]"))
    g.setb(door_left_x, anchor_y + 2, door_z,
        P("minecraft:dark_oak_door[half=upper,hinge=left,facing=south,open=false]"))
    g.setb(door_right_x, anchor_y + 1, door_z,
        P("minecraft:dark_oak_door[half=right,hinge=right,facing=south,open=false]"))
    # (Door state syntax depends on your Ironhold block-id helper — adapt accordingly.)

    # 7. Drawbridge: two trapdoors flush with floor, attached at the inside
    # of the gate (so they hinge UP toward the outside to form a wall when closed).
    for x in range(passage_w):
        g.setb(anchor_x + x, anchor_y, anchor_z - 1,
            P("minecraft:oak_trapdoor[half=top,facing=south,open=false]"))
```

## Variations

- **Single portcullis** (lighter gate, manor scale): drop the inner portcullis
  and just have outer portcullis + inner doors. No murder hole.
- **Double-passage** (huge castle): TWO parallel passages side by side, one
  for foot traffic and one for carts. Each gets its own pair of portcullises.
- **Drum-tower flanks**: replace the square flanking towers with round drum
  towers (see drum_tower_round.md, TODO). More modern-era.
- **Lava moat** (dark castle): replace the water moat with lava. Drawbridge
  trapdoors are the only way to cross safely.

## Iteration history

- v0 (2026-05-19): designed from historical gatehouse anatomy. Not yet built
  in-game. Concerns:
  - Door-block state syntax depends on the project's block-id helper
  - Trapdoor drawbridge may need 2 trapdoors wide × 3 long for proper visuals
  - Murder hole 1-block gap may be too small to read from inside; try 1x2

## See also

- [../reference_builds/castles_grand_study.md](../reference_builds/castles_grand_study.md) §6
- [../battlements/machicolations.md](../battlements/machicolations.md)
- [double_door_great_hall.md](double_door_great_hall.md) — the inner-door doors recipe
