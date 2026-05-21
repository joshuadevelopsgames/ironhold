# Castles — grand reference study

**Status:** v0 — synthesized study, not yet validated against schematic samples
**Provenance:** claude-proposal — distilled from real-world fortification design + named Minecraft castle builders (see [../_principles/master_builders_compass.md](../_principles/master_builders_compass.md))
**Date:** 2026-05-19
**Source:** general architectural knowledge + the master builders compass. To be upgraded to `reference-image` as castles get ingested via `schem_to_isc.py`.

This file is the answer to the question "how do the greatest Minecraft
players build castles?" It synthesizes the design moves shared across
the strongest castle builders (Westeroscraft, BlueBits, DigsRTU,
WiederDude, GoCreative, Smitey, Adamantis) plus the underlying historical
logic those builders are drawing from.

Read this entire file before generating any castle, fort, or keep.
Read [../_principles/master_builders_compass.md](../_principles/master_builders_compass.md)
first if you haven't already.

---

## 1. The castle's job dictates its shape

Before designing materials or silhouette, decide what the castle is *for*.
Every castle answers a different threat model, and the answer drives the
plan.

| Type | Job | Defining features |
|------|-----|-------------------|
| **Motte-and-bailey** (early) | A single lord's last refuge | One wooden tower on an artificial mound, palisade around a courtyard. Almost no stone. |
| **Norman keep** | Hold against raiders | One enormous square stone tower; entrance one floor up via removable stair; few exterior windows. |
| **Concentric castle** | Hold against an army | Inner ward inside outer ward; two rings of walls; multiple gatehouses staggered to prevent straight assault. |
| **Fortified manor** | Status + comfort, modest defense | Single hall building with a curtain wall; gatehouse, but no real military depth. |
| **Royal seat / palace-castle** | Display power; defense secondary | Many wings, gardens, ceremonial spaces, glass everywhere. Walls are symbolic. |
| **Border fortress** | Hold a strategic point | Aggressive defensive engineering: machicolations, barbican, multiple gatehouses; minimal frills. |
| **Dark keep / ruined seat** | Atmosphere over function | Asymmetry, decay, soul fire, blackstone palette; the build's story is its history. |

A generator should pick one of these on entry and refuse to mix them.
A "concentric royal seat with a Norman keep at the center" is fine —
that's *historical accretion* (see [../_principles/asymmetry.md](../_principles/asymmetry.md)).
A "fortified manor with machicolations on the chapel" is not fine — those
features don't co-occur.

---

## 2. The standard parts of a castle, in build order

A castle is a *program*, not a building. Every great castle build assembles
these parts; many lesser builds skip half of them. Build in this order so
that each later element has a place to go:

### Order matters

1. **Pick the site** — flat plain, cliff edge, river-bend, mountain spur, lake-island. The choice is half the design.
2. **Build the keep** — the oldest, sturdiest single tower. Founding-era materials. This is the part that exists if money runs out.
3. **Build the curtain wall** — the perimeter, with corner towers. Curtain-era materials. Defines the silhouette from a distance.
4. **Build the gatehouse** — the only opening in the curtain wall. The single most heavily defended structure.
5. **Build the inner ward (courtyard) buildings** — chapel, great hall, kitchens, stables, well, garderobe. Refinement-era materials.
6. **Build the outer ward / barbican** — newer, lighter. Modern-era materials. Often skipped on small castles.
7. **Decorate edges** — every door, every corner of every building, every gate gets light + foliage + flags.

If a castle generator runs out of time at step 4, it still reads as a castle
(keep + curtain + gate). If it runs out at step 6, it reads as a great castle.
If you start with step 7 and never do steps 1–4 properly, you have a
decorated cube.

### The minimum viable parts list

A structure must include at least these elements to read as "castle":

- A keep (a single tall building, the visual anchor — even just 7×7×20)
- A curtain wall (perimeter, with crenellations at the top)
- Towers at the corners of the curtain wall (at least the two flanking the gate)
- A gatehouse / fortified entry (two flanking towers + portcullis path + murder hole)
- A great hall (or one substantial roofed building) inside the walls
- A well (literally one block of water in a stone-rimmed shaft)

That minimum is six elements. Anything skipping more than one of them
isn't a castle — it's a "fortified building."

---

## 3. The five layout archetypes

Master builders rarely improvise the castle plan from scratch. They pick
a historical layout and adapt it. The five archetypes that cover ~90% of
medieval castles:

### A. The square keep with bailey

```
            ┌────────────┐
            │            │
            │    KEEP    │   ← keep is the only stone building
            │            │
            └────┬───┬───┘
                 │   │
       ─ palisade │ ─ palisade ─
       │         BAILEY        │
       │   (mostly empty)      │
       │   stable, smithy      │
       ─        ─ ─ ─ ─        ─
                  ↑
              wooden gate
```

Earliest. Just a keep with a wooden palisade around outbuildings. Use for
small frontier castles. Almost no stone curtain wall.

### B. The motte-and-bailey

Like A, but the keep sits on an artificial earthen mound (the motte) with
a connecting bridge to the bailey. The motte is critical — it's the
elevation differential that makes the keep defensible.

### C. The square curtain with corner towers

```
       ┌──T───────────T──┐
       │                  │
       │    courtyard     │
       T   keep+chapel    T
       │   kitchen+stable │
       │                  │
       └──T───────GATE────┘
                  ↑
               main entry
```

Four (or sometimes six) towers, one at each corner of a rectangular
curtain wall. The keep is inside, often offset. The gatehouse is one of
the two long-wall midpoints. Most common medieval form.

### D. The concentric

Two complete curtain walls, one inside the other. The outer is lower,
the inner is taller. Gates are *staggered* — outer gate is on the east
side, inner gate is on the south, so an attacker who breaches the outer
gate must turn 90° in a killing-ground space before facing the inner gate.

```
     ┌──T─────────────T──┐
     │ ┌──T─────────T──┐ │
     │ │  inner ward  │ │
     │ T              T │   inner gate (S)
     │ │              │ │
     │ └──T───G───────┘ │
     │   outer ward     │
     │                  │
     └──T─────GATE──────┘   outer gate (W)
                 ↑
            main entry
```

Use for major castles, royal seats, anything that wants to read as
"impregnable." This is the layout that gives you a "killing ground" space
between the rings — the classic concentric advantage.

### E. The shell keep

One curtain wall around a central courtyard, but no separate keep —
the curtain wall IS the keep. All living quarters are built against
the inside of the curtain wall, facing the courtyard. The center is
open sky.

```
     ┌──T───────────T──┐
     │ ┌────────────┐  │
     │ │            │  │
     │ │  open      │  │
     T │ courtyard  │  T
     │ │            │  │
     │ └────────────┘  │
     │                  │
     └──T──GATE─────T──┘
```

Use for moderate castles where the budget didn't stretch to a separate keep
and curtain. Living and defense are the same walls.

---

## 4. The keep — the heart of the build

The keep is the single most important building in the castle. Get this
right and the rest is texture.

### Dimensions

- **Height** must dominate the curtain wall. Curtain wall ~8–12 blocks tall;
  keep ~18–30 blocks. The keep should be visible *over* the curtain wall
  from outside.
- **Footprint** is square, usually 9×9 to 13×13. Larger keeps split into
  multiple connected blocks (an "L-plan" or "H-plan" keep) rather than
  going wider as a single block.
- **Floors** at least 3: undercroft (storage / dungeon), great hall (lord's
  receiving), solar (lord's private quarters). Plus often a roof level for
  battlements.

### Floor program

| Floor | Function | Decoration density | Light |
|-------|----------|-------------------|-------|
| Ground | undercroft / cellar / dungeon | low (1–3 per 10 m²) | torches sparse, OR soul lanterns in a dungeon |
| 1st | great hall (the public room) | high (12–20 per 10 m²) | hanging lanterns + table candles + fireplace |
| 2nd | solar / lord's private | medium (8–15) | one lantern + candles at desk + small fireplace |
| 3rd / roof | battlement walk | minimal (1 per 10 m²) | wall torches at corners only |

The entrance to the keep is usually on the *first* floor, not the ground —
historically reached by a removable wooden stair. In MC, simulate by:
- Putting the main door on level 1 (the great hall level)
- A small stone staircase leading up from the courtyard
- The undercroft access is internal only (trapdoor in the great hall)

### Walls

- Founding-era material (cobbled deepslate, mossy variants)
- Thicker than other buildings — 2-block walls, not 1-block
- Few windows on the lower 2 storeys; arrow loops only
- Larger windows on the solar level (the lord wanted view + light)
- Highest-grade window is the chapel oriel (if the keep includes a chapel)

---

## 5. The curtain wall — the silhouette from far away

### Dimensions

- **Height**: 8–12 blocks above ground. Lower walls (4–6) read as a fortified
  manor, not a castle. Taller walls (15+) read as a fortress.
- **Thickness**: 2 blocks minimum. The walk-on-top requires interior space.
- **Length** between towers: 20–40 blocks. Towers closer than 20 apart read
  as wall-towers (not corner towers); towers more than 40 apart leave the
  wall undefended.

### Anatomy

From top to bottom:

```
 1. Crenellations (merlons and crenels alternating)
 2. Wall-walk (the path the guards walk along — 2 blocks wide on the inside)
 3. Battlement parapet (the top of the outer wall, where the crenellations sit)
 4. Curtain wall main body (founding/curtain-era stone)
 5. Plinth / base (slightly wider than the wall above, slopes out at 45°)
 6. Foundation / bedrock
```

The plinth (item 5) is the signature mark of a serious castle build. A
curtain wall that meets the ground vertically reads as a stage prop. A
curtain wall with a plinth that splays outward at 45° at the base reads
as defensive engineering. Use one course of stair blocks, slope-outward,
at the bottom of the wall.

### Towers

Corner towers are tall (taller than the curtain wall by 3–6 blocks),
round-cap or conical-cap (see [../roofs/conical_tower_cap.md](../roofs/conical_tower_cap.md)),
and project *outward* from the wall corner so that defenders on the tower
can shoot down the length of the curtain wall on both sides.

Wall towers (midway along a long stretch of curtain) are shorter, often
square, and serve as guard posts.

### Decoration

- Banners hanging from the curtain wall on the courtyard side (the lord's
  colors). Spaced every 4–6 blocks along the inside.
- Wall torches outside on the courtyard face at 4–6 block intervals.
- Vines/moss draping down from the parapet on the *outside* of the wall —
  but only on the north face (damp side, see [../_principles/material_storytelling.md](../_principles/material_storytelling.md))
- Birds: a few campfires inside the wall with smoke rising can simulate
  cooking fires in a populated bailey.

---

## 6. The gatehouse — the most defended single structure

The gatehouse is the only part of a castle that *invites* attack. Every
defensive trick concentrates here.

### Anatomy in entry order (what an attacker sees)

1. **Approach causeway** — narrow, exposed, often turns 90° as it approaches the gate to force attackers to expose their unshielded side
2. **Drawbridge** — wooden, retractable, over a moat or pit. In MC, simulate
   with two trapdoors hinged at the gate side, with water or a pit beneath.
3. **Outer portcullis** — vertically-sliding iron grate at the front of
   the gatehouse passage
4. **Murder hole(s)** — a hole in the ceiling of the gate-passage where
   defenders pour boiling oil / drop rocks / shoot arrows down at anyone
   trapped between the two portcullises
5. **Inner portcullis** — second vertical grate at the back of the gatehouse
6. **Inner door** — heavy oak double doors, inside the inner portcullis
7. **Killing ground** — a small bailey just past the gate, surrounded by
   walls higher than the gatehouse, so defenders can shoot down into it

In Minecraft block terms:
- The portcullises are `iron_bars` walls that fill the gate opening
- The murder hole is a 1-block gap in the ceiling of the gate passage
- The drawbridge is two retractable trapdoors at the front

### Gatehouse construction

- **Two flanking towers** — identical (see P16 in `community_batch1_24_patterns.md`),
  taller than the curtain wall, often round (drum towers) on prestige castles
  and square on older castles
- **Passage between** — 4–6 blocks wide and as long as the towers are
  thick, so a defender has time to act before the attacker is through
- **Top-floor guard room** spanning both towers, with arrow loops looking
  forward and downward
- **Battlement walk** atop the gatehouse, with crenellations matching the
  curtain wall

The gatehouse is the build's main facade. Spend disproportionate decoration
budget here.

---

## 7. The "dark castle" — atmosphere over function

A dark castle is a stylistic variant, not a different program. Almost all
the layout rules above still apply. What changes is:

### Palette swap

Replace founding/curtain/refinement palettes with:

| Era | Standard | Dark variant |
|-----|----------|--------------|
| Founding | cobbled deepslate / mossy | blackstone / cobbled deepslate (no moss) |
| Curtain | deepslate bricks | polished blackstone bricks / cracked deepslate |
| Refinement | polished deepslate / chiseled | polished blackstone + chiseled blackstone |
| Modern | polished blackstone bricks | gilded blackstone accents — sparse, only on important details |

See [../palettes/dark_castle.md](../palettes/dark_castle.md) for the full
palette specification.

### Lighting swap

A dark castle's light vocabulary is exclusively soul-themed:

| Setting | Light |
|---------|-------|
| Curtain wall walk | soul torches at 6-block intervals (sparse) |
| Gatehouse passage | soul lanterns hanging — TWO in the whole passage |
| Great hall | one soul lantern over the throne ONLY; otherwise red candles |
| Chapel / shrine | candle clusters (black or red), NO lantern |
| Crypt / dungeon | soul torches only, large gaps of darkness |
| Watchtower tops | one soul fire campfire at apex (visible from a distance) |
| Banner/flag color | black, with one accent color (deep crimson, royal purple, sickly green) |

Never mix soul fire and normal fire (see [../_principles/lighting_dramaturgy.md](../_principles/lighting_dramaturgy.md)).
A dark castle is *committed* — the only normal fire is the smithy (because
the smithy is mortal-work).

### Decay treatment

Dark castles read as *aged*. Apply decay rules:

- ~10% of blackstone on outer walls replaced with cracked variant (above
  the standard ~5% for non-dark castles)
- Cobwebs in corners of every interior room (1–2 per room, in upper corners)
- One section of curtain wall *partially collapsed* — a 3-block-long gap
  with stair blocks and rubble at the base. This is the build's "story scar."
- One tower with broken-off battlements (see P18 — ruined tower variant)
- Skull-related blocks (wither skeleton skulls on stakes near the gate;
  a single skull on the throne back)

### What NOT to do

- **No purple wool, no purple concrete, no obvious "evil purple" accents.**
  Master builders signal evil through *restraint* and *palette*, not
  cartoonish color. A dark castle is black, gray, soul-blue, with maybe
  *one* desaturated accent.
- **No giant spike walls or skull arches at the gate.** That's children's-show
  evil. Real-feeling evil is *austere*. The gate is plain, well-built, and
  unwelcoming because the materials are wrong, not because of stuck-on horror.
- **No bedrock or obsidian "magical" materials in the structure.** Reserve
  those for a focal point only (the throne, an altar, one specific room).
- **No mixing of soul fire and normal fire**, as above. Commit to one.
- **Don't over-decay.** A castle that's 80% collapsed is a ruin, not a dark
  castle. The dark castle is *functional* and *inhabited* — just by something
  that makes you uneasy. Decay is a 10% effect on the texture, not a defining
  feature of the silhouette.

### The dark-castle "tell" features (use 2–3 of these)

These are the signals a viewer reads as "dark castle":

1. **Blackstone+deepslate palette throughout** (most important)
2. **Soul fire as the dominant lighting** (second most important)
3. **One asymmetric collapsed wall section** (the "story scar")
4. **A throne room with one soul lantern over an obsidian throne**
5. **Skeletal banners or wither skeleton skulls flanking the gate** — sparingly
6. **Approach causeway across a lava moat instead of a water moat**
7. **A central spire / single tall tower visible from miles** — black, soul-fire-topped
8. **A graveyard outside the gate** — slabs of mossy cobble as headstones,
   soul soil ground patches, wither rose plantings

Pick 2–3, not all 8. Restraint is what makes it read as serious.

---

## 8. Common amateur mistakes (and what to do instead)

### Mistake 1: Four identical corner towers on a square plan

Real castles never have four identical anything. Even matched towers
are matched in *type* (drum, drum, drum, drum) but vary in *condition*
(one slightly taller, one with extra battlements, one missing its
roof). See [../_principles/asymmetry.md](../_principles/asymmetry.md).

### Mistake 2: One material from top to bottom

The whole curtain wall is "stone brick." This reads as procedurally
generated even if it's handbuilt. Fix with material storytelling — see
[../_principles/material_storytelling.md](../_principles/material_storytelling.md).

### Mistake 3: No keep, just curtain walls

A castle without a tall central anchor reads as a fort, not a castle.
Even the smallest fortified manor has *something* taller than the curtain.

### Mistake 4: A gate that's just a door in a wall

The gatehouse is a building, not a gap. If the curtain wall is 10
tall, the gatehouse is 14+ tall. The two flanking towers go even higher.
Most amateur castles' gates look like garage doors.

### Mistake 5: The interior of the curtain walls is empty

A real castle's interior is a *village* — chapel, hall, kitchens,
stables, well, garderobe, smithy, storehouse. An empty courtyard reads
as a stage set. Even a small castle needs 3–5 interior buildings.

### Mistake 6: Crenellations placed without rhythm

Crenellations are merlons (solid) alternating with crenels (gaps).
The pattern is always merlon-crenel-merlon-crenel. Amateur builds
sometimes do merlon-merlon-crenel or stair-block tops, which are wrong.
See [../battlements/standard_crenellations.md](../battlements/standard_crenellations.md).

### Mistake 7: Symmetric placement of everything

The well is dead-center of the courtyard; the chapel and hall are
mirror images; the keep is centered on the curtain wall. Real castles
grew over time and look like it. Offset things.

### Mistake 8: Decorating before silhouetting

Spending an hour on perfect wall texture, then realizing the build is
a rectangle. Silhouette first — see [../_principles/silhouette_complexity.md](../_principles/silhouette_complexity.md).

### Mistake 9: Lighting everywhere equally

A castle is mostly dim. The great hall is lit. The chapel is candle-lit.
The corridors are torch-sparse. The dungeon is *dark*. Equal-lighting
everywhere is a museum tell.

### Mistake 10: No connection to terrain

The castle is on a perfectly flat 50×50 patch. Move the castle to a
hill, a cliff edge, a river-bend, or a spur. The site is half the
design — see [../_principles/master_builders_compass.md#impetusbuilds](../_principles/master_builders_compass.md).

---

## 9. A worked example: building a small dark castle

To make this concrete, here's the design recipe for a small dark castle
suitable as a procedural target. Use this as a checklist to validate any
generator output.

**Site**: rocky spur jutting into a swamp; one approach causeway.

**Plan**: archetype C (square curtain + 4 corner towers + keep + gatehouse).

**Footprint**:
- Curtain wall: 32 × 24 blocks
- Curtain height: 10 blocks
- Corner towers: 6×6 footprint, 16 blocks tall, conical cap
- Gatehouse towers: 5×5, 14 blocks tall, square cap
- Keep: 11×11 footprint, 22 blocks tall, offset to the back-right corner

**Eras**:
- Founding (keep base, ground level): cobbled deepslate + mossy + cracked
- Curtain: polished blackstone bricks
- Refinement (keep upper, chapel): polished blackstone + chiseled blackstone
- No modern era — this is a castle that has *not* been kept up

**Buildings in the bailey**:
- Chapel (small, 7×9, on the south face of the curtain wall, with one
  rose-window-style soul-lantern oriel facing in)
- Great hall (attached to the keep base, 9×13, one fireplace at the back)
- Stable (open-sided lean-to, 5×9, against the north wall)
- Well (offset from center, 2×2 stone-rim with water; mossy stone)

**Lighting**:
- Soul torches on the curtain wall walk, 6-block spacing
- ONE soul lantern over the gate entry passage
- Chapel: 4 red candles on the altar, no lantern
- Great hall: one soul lantern over the throne, fireplace with normal fire
  - **Exception to the soul-only rule**: the great hall's hearth fire is
    NORMAL fire to distinguish "this room is lived in" from "this room is
    haunted." It is the only normal fire in the castle. Document this in
    the build's design notes; it's a deliberate single-exception variance.
- Watchtower atop the keep: campfire (smoke visible for miles)

**Decay**:
- 12% of curtain blocks cracked variant
- One 3-block gap in the east curtain wall, with rubble piled at the base
- Cobwebs in every interior corner
- Wither skeleton skull on a pike outside the gate (one, not five)

**Approach decoration**:
- Causeway is 3 blocks wide, polished blackstone, no railing
- Lava moat under causeway (1 wide on each side; netherrack base hides the
  bottom)
- Two skull-on-pike sentries flanking the gate (wither skeleton skulls on
  fence posts)
- A solitary mossy gravestone at the start of the causeway (slab of
  cobbled deepslate on a soul-soil patch)

This entire build is ~32×24×24 = ~18,400 blocks of envelope, of which
maybe 6,000–8,000 are placed blocks. A generator can produce it in a single
pass, but only if the design steps above are computed first.

---

## 10. The recipe-priority queue for castle builds

Recipes specifically needed before a generator can produce a castle of
the quality described here. Each is filed in the appropriate folder:

- ✓ [../roofs/conical_tower_cap.md](../roofs/conical_tower_cap.md)
- TODO: [../battlements/standard_crenellations.md](../battlements/standard_crenellations.md)
- TODO: [../battlements/machicolations.md](../battlements/machicolations.md)
- TODO: [../doors/portcullis_gatehouse.md](../doors/portcullis_gatehouse.md)
- TODO: [../doors/double_door_great_hall.md](../doors/double_door_great_hall.md)
- TODO: [../staircases/spiral_3x3.md](../staircases/spiral_3x3.md)
- TODO: [../staircases/grand_stair_5w.md](../staircases/grand_stair_5w.md)
- TODO: [../fireplaces/great_hall_hearth.md](../fireplaces/great_hall_hearth.md)
- TODO: [../furniture/throne_on_dais.md](../furniture/throne_on_dais.md)
- TODO: [../furniture/banquet_table_long.md](../furniture/banquet_table_long.md)
- TODO: [../furniture/altar.md](../furniture/altar.md)
- TODO: [../palettes/dark_castle.md](../palettes/dark_castle.md)
- TODO: [../palettes/founding_era.md](../palettes/founding_era.md)
- TODO: arrow_loop_window.md (in windows/)
- TODO: drum_tower_round.md (in roofs/ — variant on conical cap, for round towers)
- TODO: drawbridge_trapdoors.md (in doors/)
- TODO: murder_hole.md (in doors/ or atmosphere/)
- TODO: curtain_wall_plinth.md (in columns_and_arches/ — the 45°
  splay-out at wall base)
- TODO: keep_undercroft.md (full room recipe)

---

## Iteration history

- v0 (2026-05-19): initial grand study. Synthesized from training-data
  knowledge of historical castle architecture + named Minecraft castle
  builders. Specific block-count claims are illustrative; needs in-game
  validation. Should be revised to `reference-image` when 2–3 castle
  schematics are ingested via `schem_to_isc.py` and analyzed.

## See also

- [../_principles/master_builders_compass.md](../_principles/master_builders_compass.md)
- [../_principles/material_storytelling.md](../_principles/material_storytelling.md)
- [../_principles/asymmetry.md](../_principles/asymmetry.md)
- [../_principles/lighting_dramaturgy.md](../_principles/lighting_dramaturgy.md)
- [community_batch1_24_patterns.md](community_batch1_24_patterns.md)
