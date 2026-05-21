# Build Recipes Library

A growing reference of medieval Minecraft architecture patterns. Used to
inform every new structure generator instead of reinventing from scratch.

## How to use

**Before generating any structure:**
1. Read this index.
2. Read [_principles/](_principles/) — the design rules every build follows.
3. For each room/feature, grep the matching subfolder for an applicable recipe.
4. Compose using existing recipes; only invent when nothing in the library fits.
5. When a new pattern works in-game, add it as a new recipe with provenance.

## Provenance — every recipe must declare its source

| Tag | Meaning | Trust |
|-----|---------|-------|
| `user-built` | Joshua scanned a hand-built room with `/k2 struct scan` | Highest — captures personal aesthetic |
| `reference-public` | Extracted from a public Minecraft build under explicit license | High — proven community pattern |
| `reference-image` | Distilled from a screenshot with credit/license | Medium — depends on reproduction quality |
| `claude-proposal` | I designed it from architectural knowledge | Lowest until in-game tested |

## Index

### Principles (read first, every build)

| File | Topic |
|------|-------|
| [_principles/master_builders_compass.md](_principles/master_builders_compass.md) | Named architects + what each teaches; the "whose style?" cheat sheet |
| [_principles/design_language.md](_principles/design_language.md) | Pick 3 motifs and repeat them at every scale |
| [_principles/silhouette_complexity.md](_principles/silhouette_complexity.md) | The shape against the sky — solve it first |
| [_principles/scale_and_proportion.md](_principles/scale_and_proportion.md) | Door scale → room scale → silhouette scale |
| [_principles/decoration_density.md](_principles/decoration_density.md) | Target blocks-per-room — when to stop adding |
| [_principles/asymmetry.md](_principles/asymmetry.md) | How to break symmetry meaningfully |
| [_principles/lighting_dramaturgy.md](_principles/lighting_dramaturgy.md) | What each light source signifies |
| [_principles/material_storytelling.md](_principles/material_storytelling.md) | Building age signaled through block choice |

### Catalog by category

| Folder | Description | Count |
|--------|-------------|-------|
| [palettes/](palettes/) | Named consistent material sets | 8 |
| [windows/](windows/) | Window styles (arched, gothic, arrow loop, rose) | 1 |
| [doors/](doors/) | Entrance designs (single, double, portcullis, murder hole) | 3 |
| [fireplaces/](fireplaces/) | Hearths and chimneys | 2 |
| [furniture/](furniture/) | Thrones, beds, tables, altars | 4 |
| [columns_and_arches/](columns_and_arches/) | Vertical members and arch types | 1 |
| [battlements/](battlements/) | Crenellation patterns, machicolations, hourds | 2 |
| [roofs/](roofs/) | Roof shapes (gable, conical, hipped, stepped pyramid) | 2 |
| [staircases/](staircases/) | Vertical circulation patterns | 2 |
| [atmosphere/](atmosphere/) | Sculk, cobwebs, banners, candle groupings | 1 |
| [reference_builds/](reference_builds/) | Whole-build studies | 5 |

**Master pattern catalog: [reference_builds/community_batch1_24_patterns.md](reference_builds/community_batch1_24_patterns.md)** — 24 patterns observed across 22 player builds; read this before any vernacular medieval design.

**Grand castle study: [reference_builds/castles_grand_study.md](reference_builds/castles_grand_study.md)** — definitive reference for castle layout, gatehouses, keeps, and the dark-castle variant. Read this before any fortified structure.

**Block-truthed castle study (Onctaaf): [reference_builds/onctaaf_castle_study.md](reference_builds/onctaaf_castle_study.md)** — first whole-build study grounded in actual placed-block counts. Distills the "natural-stone medieval" palette. Source litematic preserved at `structures/external/onctaaf_castle.litematic`.

**Block-truthed dark-fortress study (Raaamseeel): [reference_builds/raaamseeel_dark_fortress_study.md](reference_builds/raaamseeel_dark_fortress_study.md)** — 200,546-block dark fortress on a wool-podium. Distills the egalitarian-4-era palette mix, vertical color narrative, podium-in-litematic technique, and colored-wool dome roofs. Source litematic preserved at `structures/external/raaamseeel_dark_fortress.litematic`.

**Master builders compass: [_principles/master_builders_compass.md](_principles/master_builders_compass.md)** — who to study (Swordself, Smitey, Adamantis, Westeroscraft, Onctaaf, etc.) and what each one teaches.

### Sources

See [_sources.csv](_sources.csv) for the canonical license/credit record
of every external reference used.

## Adding a new recipe

1. Pick a category subfolder (or create a new one if nothing fits — record reasoning in the README).
2. Filename: `<descriptive_name>.md`. Lowercase, underscores. Include key dimensions if useful (`gothic_pointed_3w5h.md`).
3. Use the template at [_template.md](_template.md).
4. If based on an external source, add a row to `_sources.csv`.
5. Update the count column above (manually for now; can script later).

## Recipe template

See [_template.md](_template.md) for the canonical shape.
