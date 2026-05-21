# Raaamseeel Dark Fortress — full block-by-block reference study

**Status:** v1 — block-level data extracted via `scripts/analyze_isc.py`
**Provenance:** reference-image (LITEMATIC ACTUAL DATA — not visual estimation)
**Source:** `structures/external/raaamseeel_dark_fortress.litematic` (litematic file supplied by Joshua, 2026-05-19, original via abfielder.com share)
**Creator:** **Raaamseeel** (Author field on the litematic). abfielder is the hosting site, not the creator. Use as visual + structural reference only; do not redistribute.
**Minecraft data version:** 4325 (MC 1.21.4 era)
**Date analyzed:** 2026-05-19
**Reads as:** "vast dark fortress on a colossal red-rock cliff in a snowy mountain biome" — extreme verticality + extreme contrast

This is the second block-truthed castle study in the catalog. Compared
to Onctaaf's natural-stone castle, this is a much larger, much darker,
much more *stylized* build that uses techniques the Onctaaf study didn't
reveal.

## Headline numbers

| Metric | Value |
|--------|-------|
| Enclosing volume | 97 × 153 × 104 = 1,543,464 m³ (1.88x larger than Onctaaf) |
| Placed blocks | 200,546 (13% fill) |
| Unique block types | 110 |
| Palette size (full state) | 417 unique block states |
| Vertical height | **153 blocks** — extreme |
| Silhouette layers (y with >5 cells) | 146 |
| Era diversity | ALL 4 categories present (founding/curtain/refinement/modern each 20-30%) |

## What's surprising

### Surprise 1: 38.7% of the build is RED WOOL

77,642 cells of `red_wool` — almost two-fifths of every placed block.
Looking at where it sits in the Y-profile:

| red_wool by Y zone | Cells | % of all red wool |
|--------------------|------:|-----:|
| y = 0–29 (base + podium) | 77,176 | 99.4% |
| y = 30+ (the actual building) | 466 | 0.6% |

The red wool is **not the castle**. It's a vast colored podium / cliff /
mountain face that the castle sits upon. Centroid y=15.2 means it's
concentrated in the bottom 1/10th of the build's height (which goes up
to y=152).

This is a technique my catalog has never documented: **the litematic
SHIPS WITH ITS OWN MOUNTAIN**. Raaamseeel built a 97×104 footprint of
red-wool earth, ~30 blocks deep, with snowy patches on top (29,150
snow_block cells, also at y=0–15), and placed the actual fortress atop
this artificial cliff.

Why red? Several possibilities:
- A "blood-stained earth" aesthetic (the fortress is hostile)
- A literal red-sandstone cliff (Petra / Bryce Canyon analog)
- A "place-holder" the creator intends viewers to swap for terracotta
- A deliberate Mordor-style desolate-red landscape

In any case, **the lesson is the technique**: ship a podium *inside the
litematic*, so when the user pastes it onto flat ground they get the
hillside for free. Onctaaf does this with dirt+grass+moss (22.6%);
Raaamseeel does it with red_wool+snow at *enormous* scale (53% of
the build is podium).

### Surprise 2: 34.8% of the actual building is GREEN WOOL

When you exclude the wool podium and look only at y>=30 (the building
proper), `green_wool` becomes the dominant block: **16,391 of 47,123
upper-structure cells = 34.8%**.

Green wool's centroid is y=45.2 — exactly building-roof altitude. Top
Y-layers for green wool are y=33–38 (665 cells per layer for several
consecutive layers). This reads unmistakably as **green-copper-dome
roofing** — the patinated bronze look of European cathedrals.

The lesson: **bright colored wool is a valid roofing material at
building scale, not just an accent.** A 16,000-cell green roof gives the
fortress its silhouette identity. My existing roof recipes all assume
wood-stair or stone-slab — green wool roofs are an entirely new
category.

### Surprise 3: Extreme palette diversity

In the upper structure (y >= 30, excluding wool):

| Stone family | Cells | % |
|--------------|------:|---|
| polished_blackstone | 2,965 | 6.3% |
| polished_tuff | 2,768 | 5.9% |
| deepslate_tiles | 2,750 | 5.8% |
| tuff | 2,701 | 5.7% |
| cobbled_deepslate | 2,629 | 5.6% |
| gray_concrete_powder | 2,356 | 5.0% |
| mossy_stone_bricks | 2,335 | 5.0% |
| deepslate_bricks | 2,267 | 4.8% |
| polished_blackstone_wall | 1,782 | 3.8% |

**Nine different stone-family materials each contributing ~5%.** No
single block dominates. The wall fabric is *all* of them at once,
shuffled. This is *much* more diverse than the recipe I gave in
[../palettes/dark_castle.md](../palettes/dark_castle.md), which had one
dominant primary (`polished_blackstone_bricks` at 50%) and only ~5
contributing blocks.

The era breakdown (from my analyzer's classifier):
- founding: 29.2%
- curtain: 28.3%
- refinement: 20.4%
- modern: 22.1%
- decay: 5.4% (within healthy 5-12% target)

**This is the perfect material-storytelling result.** All 4 eras visible
in approximately equal measure. `_principles/material_storytelling.md`
specifies exactly this, and Raaamseeel's build achieves it. None of
my generators have ever produced this distribution.

### Surprise 4: Cyan terracotta as accent (and red_glazed_terracotta at the spire)

| Accent block | Cells | Location centroid |
|--------------|------:|-------------------|
| cyan_terracotta | 2,501 | y=48 (building level) |
| red_glazed_terracotta | 314 | concentrated y=100+ (spire) |
| black_glazed_terracotta | 271 | y=50+ |

The **cyan** is a recurring decorative color throughout the building —
likely window-trim, banner-substitute, or floor-tile accent. 2,500
cells is substantial.

**Red glazed terracotta** is the highest-count block in the spire (y>=100).
200 cells in the spire alone = it's the visual climax of the build.
The fortress's whole silhouette ascends to a red-glazed peak.

The compositional logic: **the build's color story is "red base →
gray-black walls → green roof → red glazed spire."** Color forms a
narrative axis from bottom to top.

### Surprise 5: A nether-themed interior shrine

Found in the upper structure:
- 266 `netherrack`
- 223 `lava`
- 251 `crimson_stem`
- 194 `red_mushroom_block`
- 503 `dripstone_block`

That's ~1,400 cells of distinctly Nether-coded material *inside* a
fortress otherwise built in deepslate + tuff + blackstone. The implication
is a dedicated room — a forge, a shrine, a hellfire altar, or a
crimson-themed wing.

**Lesson:** a dark fortress can include a single "Nether-themed" room
(probably 5×5×5 to 10×10×10) as a focal interior set-piece. Don't let
the Nether materials leak into the rest of the build.

### Surprise 6: No lighting in the litematic

Total light-emitting cells: **109** (76 shroomlight + 33 magma_block).
Zero torches, zero lanterns, zero candles, zero soul fire.

In a 200,546-block build, that's 0.054% lighting. Compared to Onctaaf's
462 lights (0.6%) or even my dark_castle's 15 soul lanterns (0.12%), this
is *much* lower.

**Hypothesis:** Raaamseeel built without lighting and expects the user
pasting the litematic to add their own. Or the lighting is provided
entirely by environmental fire (lava 223 cells) and the natural sky.

**Lesson:** when sharing a build for pasting, *not* baking in lighting
is a legitimate choice. The user can decide between soul lanterns
(dark dread) and normal lanterns (lived in) at paste time.

### Surprise 7: Mangrove wood for warm structural accents

| Wood | Cells |
|------|------:|
| dark_oak_planks | 315 |
| stripped_mangrove_log | 277 |
| mangrove_planks | 193 |
| spruce_planks | 117 |
| stripped_acacia_wood + acacia_planks + stairs | 127 |

Mangrove (warm red-brown) + acacia (warm orange) are deliberate WARM
wood choices against the cool gray-black stone. This carries the red
color story up into the timber-framed sections of the upper building.

**Lesson:** wood choice is part of the palette. Mangrove + acacia is the
"warm sub-palette" for a dark fortress; dark_oak would be the safer
default. Raaamseeel uses both *together* to mix temperatures.

### Surprise 8: Heavy wall use (2,184 cells)

`polished_blackstone_wall` alone: 1,782 cells. Plus other wall variants:
~400 more cells.

These are NOT crenellations (which would be merlon-crenel stair patterns).
These are *low parapet rails* — wall-blocks that are about 1 block tall
and visually thin, used along walkways, balconies, and battlement edges.
2,184 cells = a LOT of railing.

**Lesson:** in a tall fortress with many walkways and balconies, the
wall family does more visual work than the crenellation family. Slab tops
for walkways + wall blocks for the safety rails along their edges.

## Y-profile summary

Compressed view:

```
y=  0 ████████████████████████████████████████████████  9055   massive base (terrain + red wool)
y=  5 ████████████████████████████████████████████      8012   base + snow patches
y= 10 ████████████████████████████                      5893
y= 15 ███████████████████                               3599   platform mid-height (red wool)
y= 20 ██████████████████                                3392
y= 25 █████████████████                                 3275
y= 30 ██████████                                        1983   ← building begins
y= 35 ████████                                          1463   ← green roof level
y= 40 █████                                             1072
y= 45 █████                                             1073   ← tower bodies
y= 55 ████                                              849
y= 65 *                                                 331    ← upper tower
y= 75 *                                                 278
y= 85 *                                                 137
y= 95 *                                                 131    ← spire shaft
y=105 *                                                 160    ← top of spire body
y=115 (32)                                              32
y=125 (16)                                              16
y=135 (24)                                              24     ← isolated spire tip
y=145 (62)                                              62
y=152 (9)                                               9
```

The Y-profile reveals at least **6 distinct height tiers**:

1. **y=0–14**: solid base / red-wool podium / snow biome (tapering 9000→3500)
2. **y=15–29**: platform mid-section (~3300/layer, narrow taper)
3. **y=30–39**: main building floor (tower bases, green-roof underside)
4. **y=40–65**: upper tower bodies (~1000/layer, then dropping)
5. **y=66–110**: spire shaft (~150–600/layer, much narrower footprint)
6. **y=132–152**: isolated spire tip (a separate small mass at the very top)

That's **6 height tiers**, far above the "3-heights minimum" rule in
[../_principles/silhouette_complexity.md](../_principles/silhouette_complexity.md).

The front silhouette (visible in `/tmp/darkfortress_analysis.txt`) shows
twin towers at upper levels, a central spire breaking the rooflinks, and
a free-standing pinnacle ~30 blocks above the main mass.

## Patterns to codify (the catalog needs these)

### R1 — Wool podium / colored-cliff base

A castle ships with its own large colored-block podium (red wool here)
that simulates a cliff or rock outcrop. The podium is enormous — 50%+
of the build's volume. When the user pastes the litematic onto flat
land, they get the dramatic setting for free.

→ Propose new principle in
[../_principles/master_builders_compass.md](../_principles/master_builders_compass.md):
the "podium-in-litematic" pattern. Or as a recipe:
`atmosphere/colored_cliff_podium.md`.

**Note:** wool is fragile in lit areas (it burns from fire). For a build
to be permanent, the user should replace red_wool with `red_terracotta`,
`red_concrete`, or `red_sandstone` after pasting. Document this warning.

### R2 — Bright colored wool ROOFS at building scale

Green wool covers ~35% of the upper structure as the dominant roofing
material. This is the "patinated copper dome" look in MC terms.

→ Propose recipe: `roofs/colored_wool_dome.md`.
→ Color variants worth catalogging: green (copper patina), red
(spire-tile), blue (royal), purple (mage). Each gives a different
faction signature.

### R3 — All-four-eras palette mix (no single dominant)

The biggest revelation for `material_storytelling.md`: a build can have
ALL FOUR ERAS at roughly equal contribution and read as MORE coherent,
not less. The key is that no single block exceeds ~7% of the wall
fabric — everything is mixed at low concentrations.

→ Update [../_principles/material_storytelling.md](../_principles/material_storytelling.md)
with a new section: *"the egalitarian-era mix"* — a build with all 4
eras at 20-25% each, with no single material exceeding 7%, reads as
genuinely ancient in a way that a 50%-primary palette cannot.

### R4 — Color narrative axis (vertical)

The build has a vertical color story:
- Base: red (earth, blood, foundation)
- Walls: gray-black (deepslate / tuff / blackstone)
- Roof: green (patinated copper / dome)
- Spire: red-glazed (climax, focal accent)

→ Propose new principle: `_principles/color_narrative.md` documenting
the vertical-color-axis technique. Or fold into
[../_principles/master_builders_compass.md](../_principles/master_builders_compass.md)
cross-cutting lesson.

### R5 — A single Nether-themed room embedded in a non-Nether build

1,400 cells of crimson_stem + netherrack + lava + dripstone, all clustered
in one zone of the upper building, suggests a dedicated "infernal" room.
The discipline of *not letting the Nether palette leak* into the rest
of the build is the lesson.

→ Propose recipe variant in `furniture/altar.md`: add a "wither-altar
chamber" sub-recipe that uses these blocks.

### R6 — Walls (wall blocks) as railings, not just crenellations

`polished_blackstone_wall` × 1,782 cells used as low walkway-edge rails.
This is the FUNCTIONAL use of wall blocks — keeping defenders from
falling off battlements — separate from the DECORATIVE use as merlon
caps.

→ Update [../battlements/standard_crenellations.md](../battlements/standard_crenellations.md)
to document the inner-wall-walk rail as a wall-block course on the
opposite side from the crenellations.

### R7 — Lighting decoupling for litematic distribution

A litematic shared without lighting is a legitimate choice — it lets the
user choose between dread (soul) and lived-in (normal) at paste time.

→ Propose convention: ship two .litematic versions when sharing — one
with no lighting, one with the "creator's intended" lighting.

### R8 — Mixed warm-wood + cool-stone within one palette

Mangrove + acacia (warm reds and oranges) used as accents *within* a
deepslate + blackstone (cool grays and blacks) building. The warm wood
isn't a separate room — it's the timber framing, the trim, the
shutters scattered throughout.

→ Update [../palettes/dark_castle.md](../palettes/dark_castle.md) to add
mangrove + acacia as accent wood options (currently only specifies
`dark_oak`).

## Patterns confirmed (existing rules survived contact with real data)

- **Era diversity** ✓ — but the "balanced 4-way mix" is more aggressive
  than the doc currently prescribes
- **Silhouette complexity** ✓ — 6 tiers, exceeds the 3-minimum
- **Asymmetry** ✓ — Y-profile shows uneven tier counts (no symmetric
  matching)
- **Material storytelling** ✓ — strongly confirmed
- **Decay percentage** ✓ — 5.4% is within healthy range
- **No iron_bars** ✓ — like Onctaaf, defense isn't via portcullis
  (probably terrain-defended)

## Direct comparison to Onctaaf's castle

| Metric | Onctaaf (Castle) | Raaamseeel (Dark Fortress) |
|--------|------------------|----------------------------|
| Placed blocks | 76,869 | 200,546 (2.6×) |
| Height | 81 | 153 (1.9×) |
| Footprint | ~10,000 m² | ~10,000 m² |
| Unique block types | 137 | 110 |
| Slab cells | 7,394 | 377 (dramatic reversal) |
| Stair cells | 759 | 687 |
| Wall cells | 724 | 2,184 (3×) |
| Era distribution | curtain-heavy | all-4-balanced |
| Decay % | 23.5% (heavy) | 5.4% (light) |
| Glass | 45 cells | ~0 |
| Lighting | 462 cells | 109 cells |
| Color story | natural gray | red→gray→green→red vertical |
| Defense pattern | moat | terrain + walls |
| Style mode | weathered peace-time | fresh-built dark-fortress |

**Different stylistic poles, same level of mastery.** Onctaaf optimizes
for *natural weathered realism*. Raaamseeel optimizes for *dramatic
silhouette + color contrast*. Both are valid; the catalog needs both.

## Action items (catalog deltas)

1. **Write `atmosphere/colored_cliff_podium.md`** — the wool/terracotta
   podium recipe
2. **Write `roofs/colored_wool_dome.md`** — green-wool/red-wool roof tile
   technique
3. **Update `_principles/material_storytelling.md`** — the egalitarian
   4-era mix as an explicit option (not just "use multiple eras")
4. **Write or fold-in `_principles/color_narrative.md`** — vertical
   color axis as a composition tool
5. **Update `palettes/dark_castle.md`** — add mangrove + acacia accent
   wood options + the gray_concrete_powder modern variant
6. **Update `battlements/standard_crenellations.md`** — document the
   inner-wall-walk rail as a wall-block course
7. **Update `_principles/master_builders_compass.md`** — Raaamseeel
   as a confirmed reference (block-truthed), specializing in
   "dramatic vertical fortress with color narrative"

## Reproduce

```bash
.recipes_venv/bin/python scripts/analyze_isc.py \
  structures/external/raaamseeel_dark_fortress.litematic > /tmp/raaamseeel_analysis.txt
```

## See also

- [onctaaf_castle_study.md](onctaaf_castle_study.md) — the natural-stone counterpart
- [castles_grand_study.md](castles_grand_study.md)
- [../_principles/master_builders_compass.md](../_principles/master_builders_compass.md)
- [../palettes/dark_castle.md](../palettes/dark_castle.md) — needs updates per R8
