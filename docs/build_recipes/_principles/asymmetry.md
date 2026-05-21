# Asymmetry: how to break symmetry meaningfully

Real castles are not symmetric. Even castles that were planned to be symmetric
ended up asymmetric because the ground was uneven, money ran out, fashions
changed, or one wing collapsed and was rebuilt differently a generation later.

## The three legitimate sources of asymmetry

### 1. Site response

The terrain forced an irregularity:
- One side is on a cliff edge → no curtain wall on that face, just a sheer drop
- One corner sits on bedrock → that corner is the oldest, sturdiest, ugliest stone
- A spring/well forced a building to wrap around it

In MC, simulate by:
- Skipping the wall on one face entirely if pasting on cliffside terrain
- Placing the well first in a non-central location, then building the keep around it

### 2. Historical accretion

Built in pieces over time:
- Original Norman keep (smallest, cobblestone, most defensible)
- Curtain wall added a generation later (mixed-quality dressed stone)
- A chapel wing added by a pious heir (finer materials, glass, candle-rich)
- A barbican added when siege engines became a threat (newest, biggest)

Each addition has slightly different materials, orientation, and quality
because each was built by different people in different eras.

In MC, simulate by:
- Different palette mix functions for each "construction era" (we have
  `stone_old`, `stone_curtain`, `stone_dressed` in the v2 generator)
- Slight offset of additions from the main grid (e.g., chapel wing is rotated
  3° relative to keep — in MC, offset by 1 block at an angle)

### 3. Functional bias

The building knows where its enemies and friends are:
- Stronger walls and more towers on the *threat* side
- The main gate faces the road / town, not the wilderness
- The lord's solar faces the *view*, not the road
- The garderobe drops over an exterior wall, not a courtyard
- Service entries (kitchen, stable) on the side, never the front

In MC, simulate by:
- Define a "threat direction" once per build (e.g., south = open plain)
- Build the most fortified side toward threat
- Build the most decorative facade toward the secondary direction
- Service buildings tucked against the opposite wall

## Anti-pattern

The dark_castle_v2 has four identical corner towers placed at perfect 90°
intervals on a square plan. There's no story for why each tower exists or
why they're identical. Real castles never look like this.

## Easy improvements

1. Make ONE corner tower noticeably taller than the others — that's the
   watch tower with the best view of the threat direction.
2. Make ONE corner tower noticeably *shorter* or in cruder stone — that's
   the original Norman corner that nobody bothered to rebuild.
3. Skip ONE wall section entirely (e.g., south-east face) on grounds of
   "this side is on a cliff."
4. Make the gate offset from center along the wall (e.g., 1/3 in from the
   east end of the south wall, not dead center).
5. Add a noticeable but small wing (chapel, kitchen) to ONE side of the
   keep — not the back, the side. The side facing the courtyard.
