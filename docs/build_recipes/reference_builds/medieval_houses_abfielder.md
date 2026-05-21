# Medieval Houses — three Abfielder builds (study notes)

**Status:** reference study — patterns extracted, not yet codified as runnable recipes
**Provenance:** reference-image
**Source:** schemat.io re-uploads of original builds by Abfielder
- Medieval House → https://schemat.io/schematics/pq0S4r (preview only — full file behind login)
- The Tavern → https://schemat.io/schematics/w14NWo
- Big Medieval House → https://schemat.io/schematics/lOeOp7
**Creator:** Abfielder (original) — schemat.io is the re-host
**License:** unverified — schemat.io re-hosts; original Abfielder terms unknown. Use as
visual reference only. Do not redistribute the .schem files.
**Date fetched:** 2026-05-15
**Reads as:** vernacular medieval European (English / German / Bavarian)

## Why these matter

These three buildings contain **at least eight patterns my own generators have
never used.** They demonstrate the difference between "blocky castle" and
"built object." I'm noting each pattern below as a candidate recipe.

## Patterns observed

### P1 — Pitched gable roof with angled stair-blocks (NOT stepped pyramids)

All three buildings have steeply-pitched gable roofs formed by stairs blocks
oriented to make a 45° face. Both the long sides (rake) and gable ends use
stairs at the slope, with the apex a single block.

This is the **single biggest correction** to my prior work. Every roof I've
built has been a stepped pyramid — squared and chunky. Real player builds use
stairs blocks to make smooth 45° slopes. The difference is dramatic.

→ See proposed recipe: [../roofs/pitched_gable_stair_45.md](../roofs/pitched_gable_stair_45.md)

### P2 — Half-timbered walls (oak log frame + plank infill)

All three buildings use the half-timbered (Fachwerk) pattern: dark vertical
oak logs every 2–3 cells, horizontal cross-beams at floor/ceiling lines,
infill of lighter planks (stripped oak, birch, or jungle planks). The tavern
adds diagonal cross-braces between the vertical timbers for additional
texture.

The visual rhythm is: 2-block infill, 1-block beam, 2-block infill, 1-block
beam — never just a flat plank wall.

→ See proposed recipe: [../columns_and_arches/half_timbered_wall.md](../columns_and_arches/half_timbered_wall.md)

### P3 — Stone base, wood upper (vertical material change)

All three buildings have stone (cobblestone, stone bricks) for the ground
floor (1–3 blocks tall), transitioning to half-timbered wood for upper
floors. The transition course is a stair block (upside-down stair facing
inward) — creating a small projecting lip where stone meets wood.

This is "material storytelling" applied at the per-floor scale: the stone
base is the load-bearing element, the wood is the lighter living quarters.

### P4 — Cantilevered upper floor (jetty)

The medieval house and the tavern both have upper floors that overhang the
ground floor by 1 block. The overhang is supported visually by **diagonal
wooden brackets** (single stair blocks projecting outward and angled
upward) at corners.

This was a real medieval feature — the upper floor jetty maximized usable
floor area within crowded streets, and structurally distributed roof load.
In MC, it adds a strong horizontal shadow line that breaks up the facade.

### P5 — Moss / leaves on roof ridges and edges

The tavern's roof has **red-tinted leaves or vines** scattered along the
ridge and at the gable ends. This is moss-carpet or azalea-leaves used as a
texture overlay. Adds weathering and color contrast against the dark roof.

→ See proposed recipe: [../atmosphere/roof_moss_weathering.md](../atmosphere/roof_moss_weathering.md)

### P6 — Multiple chimneys and dormers

The tavern has **two chimney stacks** at the apex of the roof. The big house
has a chimney protruding from mid-roof. Both buildings have **dormer
windows** — small protruding gables in the middle of the roof slope, each
with its own tiny pitched roof.

These break up the long roof line and tell the player "things happen up
here" (fireplaces, attic rooms).

### P7 — Arched stone window surrounds

The big medieval house shows windows with stone arches above them (formed
by stairs blocks meeting at an apex, sitting above the window). The arch
surround is *separate from the wall material* — a visual frame distinct
from the wall behind it.

This is the same gothic-pointed-arch idea I've used, but applied at house
scale (smaller windows) and integrated with the wall rather than as a
standalone feature.

### P8 — Mixed window styles in a coherent language

The medieval house shows at least three different window sizes/shapes on
the same facade — a tall pane window, a small panel window, and what looks
like a bay window. But they all share: dark-wood frames, white glass panes,
and same vertical proportions. The result is visual variety without
chaos.

This contradicts my earlier "pick one window and repeat it" rule — but
critically, the windows are unified by their **frame and glass treatment**,
not by being identical. The design language is "dark-wood-framed white
panes," and within that language different sizes are fine.

## What to do with this

1. Cash out P1 (pitched gable roofs) and P2 (half-timber walls) into
   runnable Python recipes — these are the highest-leverage corrections.
2. Add P3 (vertical material change) as a `_principle` since it's a rule
   about composition rather than a placeable thing.
3. Hold P4–P8 as known patterns; codify when needed.

## Images

![Medieval House](../_images/schemat_io/medieval_house_pq0S4r.png)
![Tavern](../_images/schemat_io/tavern_w14NWo.png)
![Big Medieval House](../_images/schemat_io/big_medieval_house_lOeOp7.png)
