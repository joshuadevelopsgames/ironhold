# Ore Quality System

**Status:** v2 spec, partially calibrated. **Tier count expanded from 3 to 4** (added Good = vanilla baseline) so unmarked vanilla items keep their durability unchanged. All design questions resolved; numeric multipliers and mine drop chances ship as v2 targets, adjustable in playtest.

**Companion spec:** [profession-skill-system.md](./profession-skill-system.md) — defines the skill-tree framework whose Blacksmithing and Mining trees this gear/quality system gates against.

**Spreadsheet:** [kingdom_smp_gear_economy_system_v3.xlsx](../../../Downloads/kingdom_smp_gear_economy_system_v3.xlsx) — calibration tables, full Durability Model, and per-material × tier matrix. This spec is the rules; the spreadsheet is the numbers.

---

## 1. Goal & design constraints

Define a 3-tier gear quality system that creates real political stakes around mineral control without breaking baseline Minecraft survival. Standard tier gives wilderness/peasant gear teeth (and weakness); Fine tier preserves the vanilla baseline; Mint tier is the prestige output of Royal mines that kingdoms fight over.

Constraints — Kingdom SMP design philosophy is binding:

- **Vanilla survival must remain playable.** A solo player who never joins a kingdom must still be able to progress through Minecraft's standard loop. The mod must not gate the core game behind kingdom infrastructure.
- **Quality is a *physical* lever, not a stat dashboard.** Visible textures, durability bars, and item-tooltip readouts are the only surfaces. No tracked-stat menus.
- **Vanilla mining progression is sacrosanct.** Quality must never change which tier a tool can mine. (See §8 hard rule #1 — this is the firewall.)

---

## 2. Tier system

Four gear quality tiers:

| Quality | Multiplier × | Repair Eff × | Pristine? | Source |
|---|---|---|---|---|
| Poor | 0.5 | 0.6 | No | Wilderness, mob drops, Wild mines, low-roll yields |
| Fine | 0.8 | 0.9 | Yes | Mid-yield mines (Claimed-mine average rolls), unskilled smith |
| Good | 1.0 *(vanilla baseline)* | 1.0 | Yes | Competent smith from Claimed-mine high rolls or Deep-mine output |
| Mint | 1.2 | 1.2 | Yes | Royal mines + Expert Blacksmith. Prestige tier. |

**Default = Good.** Items without an explicit quality component are treated as Good, so vanilla unmarked items keep their stats exactly. Fine is now an *intentional tag* below the baseline (representing average-quality output), not the baseline itself.

**Naming note:** the *gear quality* tier is "Mint." The *mine tier* "Royal" is a separate kingdom-political concept — a Royal Mine is a kingdom-controlled mine that *produces* Mint-quality ore. The naming aligns with the real-world "Royal Mint" idiom: a kingdom-owned facility that produces high-grade output.

## 2.1 Quality scope

Quality applies **only** to:

- **Armor** — helmet, chestplate, leggings, boots (vanilla and modded).
- **Weapons** — swords, axes (when used as weapons), bows, crossbows, tridents, maces.
- **Tools** — pickaxes, shovels, hoes (axes scale via the weapon path).
- **Ores and ingots** — items in the `ironhold:ore_or_ingot` tag. Vanilla v2 list: raw_iron, iron_ingot, iron_nugget, raw_gold, gold_ingot, gold_nugget, raw_copper, copper_ingot, diamond, emerald, netherite_scrap, netherite_ingot, lapis_lazuli, redstone, coal, charcoal, quartz, amethyst_shard.

Quality does **NOT** apply to:

- **Utility gear** — shield, elytra, fishing rod, shears, flint and steel, brush, carrot on a stick, warped fungus on a stick, wolf armor. These keep vanilla durability and stats unchanged. The tooltip handler and `/k2 gear setquality` command both reject them.
- **Plain items** — blocks, food, sticks, dyes, banners, signs, books, maps, music discs, anything else not damageable and not in the ore tag.

Implementation: [QualityScope.java](../src/main/java/kingdom/smp/gear/QualityScope.java) (whitelist with utility blacklist) and the [ore_or_ingot.json](../src/main/resources/data/ironhold/tags/item/ore_or_ingot.json) tag.

---

## 3. Per-stat scaling

The tier multiplier applies **uniformly** to every relevant stat — single value per tier, easy to remember. Different item categories scale different stat sets:

### 3.0 Stats by item category

| Item category | Scaled stats |
|---|---|
| **Armor** | durability, armor points, armor toughness, knockback resistance |
| **Weapons** (sword, mace, trident, bow, crossbow, axe-as-weapon) | durability, attack damage |
| **Tools** (pickaxe, axe, shovel, hoe) | durability, attack damage |
| **Utility gear** (shield, elytra, fishing rod, shears, flint and steel, brush, carrot/warped fungus on a stick, wolf armor) | **none — vanilla unchanged** |
| **Ores / ingots** | quality is *carrier metadata* only — propagates to crafted gear; no direct stat effect |

### 3.0.1 Per-stat rounding rules

| Stat | How it scales | Rounding | Floor |
|---|---|---|---|
| Durability | × multiplier | nearest int | 1 (so quality nerf can't produce 0-dura items) |
| Armor points (per piece) | × multiplier | nearest int | 0 |
| Armor toughness | × multiplier | nearest int | 0 |
| Knockback resistance | × multiplier | float, no rounding | 0.0 |
| Attack damage | × multiplier | float, no rounding | 0.0 |

### 3.0.2 NOT quality-scaled (out of scope by design)

| Stat | Reason |
|---|---|
| **Attack speed** | Keeps weapon swing rhythm vanilla so PvP timing doesn't depend on tier. |
| **Mining speed** | Gathering pace stays vanilla; controlled mines should reward you with quality, not faster mining. |
| **Mining tier** | Firewalled by §8 hard rule — quality must NEVER change which tier a tool can mine. |
| **Enchantability** | Vanilla table behavior preserved across all tiers. Gold's high vanilla enchantability remains its inherent niche. |

### 3.1 Material × tier baseline (Good = vanilla)

The values in this table are the Good (vanilla baseline) numbers. Apply the tier multiplier to derive Poor / Fine / Mint values.

#### Armor base

| Material | Helm | Chest | Legs | Boots | Toughness | Knockback Resist |
|---|---|---|---|---|---|---|
| Leather | 55 | 80 | 75 | 65 | 0 | 0 |
| Gold | 77 | 112 | 105 | 91 | 0 | 0 |
| Chainmail | 165 | 240 | 225 | 195 | 0 | 0 |
| Iron | 165 | 240 | 225 | 195 | 0 | 0 |
| Diamond | 363 | 528 | 495 | 429 | 2 | 0 |
| Netherite | 407 | 592 | 555 | 481 | 3 | 0.1 |

#### Tools/weapons base

| Material | Durability | Mining Speed | Damage Bonus | Mining Tier | Enchantability |
|---|---|---|---|---|---|
| Wood | 59 | 2 | 0 | 0 | 15 |
| Gold | 32 | 12 | 0 | 0 | 22 |
| Stone | 131 | 4 | 1 | 1 | 5 |
| Iron | 250 | 6 | 2 | 2 | 14 |
| Diamond | 1561 | 8 | 3 | 3 | 10 |
| Netherite | 2031 | 9 | 4 | 4 | 15 |

### 3.2 Worked example — Diamond chestplate

| Stat | Poor (×0.5) | Fine (×0.8) | Good (×1.0) | Mint (×1.2) |
|---|---|---|---|---|
| Durability | 264 | 422 | 528 | 633 |
| Armor points | 4 | 6 | 8 | 10 |
| Toughness | 1 | 2 | 2 | 2 |
| Knockback resist | 0 | 0 | 0 | 0 |

Note: low-base-value stats (toughness 2, knockback resist 0) round to identical values across multiple tiers. Acceptable for v2 — the dominant differentiation lever is durability and armor points. If toughness/KBR differentiation matters in playtest, see §11 calibration question.

### 3.3 Worked example — Netherite chestplate

| Stat | Poor | Fine | Good | Mint |
|---|---|---|---|---|
| Durability | 296 | 474 | 592 | 710 |
| Armor points | 4 | 6 | 8 | 10 |
| Toughness | 2 | 2 | 3 | 4 |
| Knockback resist | 0.05 | 0.08 | 0.10 | 0.12 |

Higher base values (netherite toughness 3, KBR 0.1) produce real differentiation across all four tiers.

### 3.4 Worked example — Iron sword

| Stat | Poor | Fine | Good | Mint |
|---|---|---|---|---|
| Durability | 125 | 200 | 250 | 300 |
| Attack damage | 3.0 | 4.8 | 6.0 | 7.2 |
| Attack speed | -2.4 *(unchanged)* | -2.4 | -2.4 | -2.4 |

Attack speed is preserved at the vanilla value across all tiers — quality affects the damage of each swing, not the rhythm.

### 3.5 Worked example — Diamond pickaxe

| Stat | Poor | Fine | Good | Mint |
|---|---|---|---|---|
| Durability | 781 | 1249 | 1561 | 1873 |
| Attack damage | 1.0 | 1.6 | 2.0 | 2.4 |
| Mining speed | 8 *(unchanged)* | 8 | 8 | 8 |
| Mining tier | 3 *(immutable — firewall)* | 3 | 3 | 3 |

Mining speed and mining tier are preserved across all four quality tiers — see §8 hard rules.

### 3.6 Gold niche

Gold's high vanilla enchantability (22) is preserved at every tier — quality does not amplify or reduce it. Gold's distinct quality perk is **durability loss reduction at Mint**:

| Gold Quality | Durability loss reduction per hit |
|---|---|
| Poor | 0% |
| Fine | 0% |
| Good | 0% |
| Mint | +10% |

This perk is a flat additive applied at hit time, not part of the multiplier system. Combined with gold's vanilla enchantability ceiling, it gives Mint Gold a real specialization niche (the enchanter's metal that also lasts a bit longer) without making gold a tank-armor replacement.

---

## 4. Condition system

Conditions are the **durability bar interpreted as stages.** Same single bar players already see in vanilla — but stat output degrades in steps as the bar drains, instead of staying flat until 0. No second display layer.

| Condition | Durability Range | Protection Modifier | Toughness Modifier |
|---|---|---|---|
| Pristine | 100% – 90% | +3% | +1 |
| Worn | 89% – 60% | baseline | baseline |
| Damaged | 59% – 30% | −8% | −1 |
| Battered | 29% – 10% | −20% | −2 |
| Broken | <10% | −60% | severe |

Pristine is **not** an automatic outcome of repair — it requires explicit refinement (§7).

---

## 5. Mine system

Mines are the geographic sources of quality ore. **Two zones**, with the in-mine zone graduated by depth:

| Zone | Source | Poor % | Fine % | Good % | Mint % | Notes |
|---|---|---|---|---|---|---|
| Wild | Any ore broken outside a mine structure | 65% | 30% | 5% | 0% | Vanilla everywhere; Mint unreachable |
| Mine — Shallow | Upper band of a generated mine | 25% | 50% | 25% | 0% | Roughly the original "Claimed" table |
| Mine — Mid | Mid band | 10% | 35% | 50% | 5% | Roughly the original "Deep" table |
| Mine — Deep | Bottom band | 5% | 20% | 50% | 25% | Roughly the original "Royal" table — Mint is the modal outcome |

**Geographic model:** Wild ore generates as vanilla everywhere — nothing in worldgen is altered for it. Mines are *single discoverable structures* with an open-mouth surface mineshaft visible at terrain level, sloping down through three depth bands. Deeper = better; reaching Mint requires committing to a deep descent. There is no separate "Claimed" or "Royal" structure type — all mines are the same structure, and depth is the tier lever.

**Politics is out-of-code.** A kingdom "owns" a mine by defending its entrance with their members and banners — there is no claim-the-mine mechanic in the mod itself. The geographic asymmetry is the only lever; player politics turns it into kingdom-level conflict.

**Solo player path.** Wild rare-Good keeps unaffiliated survival viable: a patient solo miner can craft Good gear from wilderness ore, just slowly. Mint is gated behind discovering and surviving a deep mine, which can be done solo or as a kingdom raid.

**Mine depletion is deferred.** Mines do not exhaust output in v1. (Risk of "kingdom collapses because mine ran out" is too punishing for a first cut.)

**Worldgen rarity** is the new primary calibration knob — too common and Mint is everywhere, too rare and kingdoms can't form around mines. Detailed worldgen is in a v2 followup (`worldgen-mines.md`).

---

## 6. Crafting & smithing

### 6.1 Quality at craft time — weakest-link drag algorithm

When a player crafts gear, the result quality is computed from the *average* of the eligible-input qualities, with a small "weakest-link drag" that pulls the result toward the lowest input. Pure batches preserve their tier exactly; mixed batches degrade gracefully without falling all the way to the worst input.

**Formula:**

```
avg      = mean of eligible-input multipliers
min      = lowest eligible-input multiplier
weighted = avg − (avg − min) × DRAG       // DRAG = 0.25
result   = tier whose multiplier is closest to weighted
```

The 0.25 drag means a single bad ingot pulls the result down somewhat, but the average still dominates. Inputs that aren't quality-eligible (sticks, planks, dyes, redstone-as-circuit) are ignored — they don't drag the result down. If a recipe has no quality-eligible inputs at all, the result keeps its default quality (Good).

**Worked examples** (tier multipliers: Poor 0.5, Fine 0.8, Good 1.0, Mint 1.2):

| Recipe | Avg | Min | Weighted | Result |
|---|---|---|---|---|
| 8× Mint | 1.20 | 1.20 | 1.20 | **Mint** *(pure batch unchanged)* |
| 7× Mint + 1× Poor | 1.11 | 0.50 | 0.96 | **Good** *(one bad ingot drops 1 tier)* |
| 4× Mint + 4× Poor | 0.85 | 0.50 | 0.76 | **Fine** *(heavy contamination drops 2)* |
| 1× Mint + 7× Poor | 0.59 | 0.50 | 0.57 | **Poor** *(mostly Poor → Poor)* |
| 4× Mint + 3× Good + 1× Poor | 1.04 | 0.50 | 0.90 | **Good** *(slight drop from Mint)* |
| 2× Fine + 1× Poor | 0.70 | 0.50 | 0.65 | **Fine** *(small drop)* |
| 1× Mint + 1× Good + 1× Fine + 1× Poor | 0.88 | 0.50 | 0.78 | **Fine** *(mixed → mid-low)* |
| 8× Good | 1.00 | 1.00 | 1.00 | **Good** *(vanilla, unchanged)* |

**Live preview:** the crafting result slot shows the actual quality of the would-be craft *before* the player takes it — the algorithm runs at result-computation time (every input change), not at consumption time. So players can see what they'll get and adjust ingredients if they want a different tier.

**Calibration knob:** the `WEAKEST_LINK_DRAG` constant is currently 0.25. Lower values (0.1) make the average dominate more (mixed batches stay closer to the average); higher values (0.5) make the lowest input pull harder (closer to "worst-case wins").

**Implementation:**
- Algorithm: [QualityPropagation.java](../src/main/java/kingdom/smp/gear/QualityPropagation.java)
- Crafting hook: [CraftingMenuMixin.java](../src/main/java/kingdom/smp/mixin/CraftingMenuMixin.java) — Mixins into `CraftingMenu.slotChangedCraftingGrid` at TAIL, applies quality to the result container so the preview slot reflects the actual outcome. Covers both the standalone crafting table and the inventory 2×2 grid.
- **Smelting** propagates quality the same way (Mint raw iron → Mint iron ingot) but uses a separate hook (furnace block-entity Mixin, TBD).

### 6.2 Smithing-table upgrade path

Mirroring the vanilla Netherite upgrade pattern: an existing piece of gear can be upgraded one quality tier at a smithing table by combining it with the appropriate higher-grade ingot + smithing template.

```
Standard Iron Chestplate + Fine Iron Ingot   + Smithing Template → Fine Iron Chestplate
Fine Iron Chestplate     + Mint Iron Ingot   + Smithing Template → Mint Iron Chestplate (Royal Forge required)
```

This means:
- You always pay the Standard-tier crafting cost first
- Mint upgrades are infrastructure-gated (requires a Royal Forge — §6.3)
- Pristine refinement (§7) is a separate operation on top of upgrades

### 6.3 Royal Forge

The Royal Forge is a placeable multiblock structure that gates **Mint-tier smithing and refinement.**

| Aspect | Rule |
|---|---|
| Blocks required | Anvil + Smithing Table + Blast Furnace + Lava/Heat Source + Royal Core block (kingdom-themed decorative) |
| Placement | Must be inside a claimed kingdom zone or royal district |
| Who can use | Player must have **Expert** Blacksmithing rank (see [profession-skill-system.md §4.1](./profession-skill-system.md#41-blacksmithing--detailed)) |
| Royal Smith court appointment? | **Open question.** Two options: (a) any Expert Blacksmith with Royal Forge access can use it; (b) only the king's appointed Royal Smith can use it. Calibration TBD — see §11. |
| Unlocks | Mint-tier upgrade recipes, Mint Pristine refinement, repair-without-quality-loss when correct mats are used |
| Failure mode | If multiblock requirements are missing, the station works as a normal forge only — no hard error |

The Royal Forge is also the only place **respec** of profession trees can occur (per [profession-skill-system.md §3.5](./profession-skill-system.md#35-respec)).

---

## 7. Pristine refinement

Pristine is a paid premium state, not a default repair outcome. It applies an extra protection / toughness bonus (§4) at the top of the durability bar.

| Quality | Required Material | Min Skill Rank | Cost |
|---|---|---|---|
| Fine | Fine ingot | Apprentice Blacksmithing | 25–40% of crafting cost |
| Good | Good ingot | Journeyman Blacksmithing | 25–40% of crafting cost |
| Mint | Mint ingot | Expert Blacksmithing (Royal Forge) | 25–40% of crafting cost |

**Sticky behavior:** Pristine drops to Worn when durability drops below 90%, but **does NOT drop on first combat hit.** This is the locked design — preserves the Pristine "buff state" as something worth paying for, without feel-bad UX where one bad swing wastes the refinement.

Poor quality is **not** Pristine-eligible. Wilderness/scrappy gear cannot be ceremonially refined.

---

## 8. Hard rules (do not violate)

These are the firewall rules that protect vanilla survival from the quality system.

1. **QUALITY DOES NOT AFFECT MINING TIER.** A Standard Diamond Pickaxe still mines obsidian; a Mint Iron Pickaxe still cannot. *This is the most important rule in the spec.* If a player crafts a Standard Diamond Pickaxe, they can still complete vanilla progression. The quality system is a horizontal axis (durability, repair efficiency), not a vertical axis (tier-up).
2. **No cross-tier preservation.** Mint gear cannot be repaired or refined with Fine materials and stay Mint — it tier-drops to Fine. Repair material quality must meet or exceed gear quality to preserve quality (see §9).
3. **Quality cannot be discovered after crafting.** When an item is crafted, its quality is set deterministically from inputs (§6.1) and never changes except via explicit upgrade (§6.2), repair fatigue tier-drop (§9), or refinement (§7).
4. **Vanilla generation untouched.** Vanilla ore generation continues exactly as in stock Minecraft. The mine system (§5) layers on top — it does not subtract from baseline ore distribution.
5. **Pristine does not drop on first hit.** Sticky until 90% durability.

---

## 9. Repair fatigue & material rules

### 9.1 Material requirement to preserve quality

| Gear Quality | Min material to preserve quality | If lower-quality material used |
|---|---|---|
| Poor | Poor+ | preserved (Poor floor) |
| Fine | Fine+ | restores durability but tier-drops to Poor at next fatigue tick |
| Good | Good+ | restores durability but tier-drops to Fine at next fatigue tick |
| Mint | Mint | restores durability but tier-drops to Good at next fatigue tick |

You can always *restore durability* with a lower-quality material — but you cannot *preserve quality* without matching-or-better material.

### 9.2 Repair fatigue

Each repair on the same item accumulates fatigue. Fatigue caps maximum durability (vanilla anvil "Too Expensive!" replacement).

| Fatigue Level | Max Durability Penalty | Effect |
|---|---|---|
| 0 | 0% | Fresh |
| 1 | −2% | Minor |
| 2 | −5% | Small |
| 3 | −10% | Noticeable |
| 4 | −15% | Heavy |
| 5 | Tier Drop | Quality downgrade: Mint → Good → Fine → Poor → discard recommended |

Master Blacksmiths reduce fatigue gain (see [profession-skill-system.md §4.1](./profession-skill-system.md#41-blacksmithing--detailed)) — at Master rank, fatigue gain is 60% of normal.

---

## 10. Integration with profession-skill-system

This system threads into the profession skill tree at exactly two trees:

- **Blacksmithing** ([profession-skill-system.md §4.1](./profession-skill-system.md#41-blacksmithing--detailed)) — repair efficiency, fatigue reduction, quality preservation, Mint refinement, Royal Forge access.
- **Mining** ([profession-skill-system.md §4.2](./profession-skill-system.md#42-mining--detailed-stub)) — mine yield, drop quality bonuses, Royal Mine surveying, vein-mining at Master.

Other professions interact tangentially:

- **Trading** — trade tax flow on quality goods (Mint ingots = high-value export)
- **Enchanting** — gold-quality synergy (§3 gold niche)
- **Cooking** — feast buffs at the king's table (orthogonal)

Profession milestones that are tree-pinned to Blacksmithing or Mining (e.g. "Forge your first Mint item") feed back into this system as +1 in the relevant tree, accelerating mastery of these mechanics.

---

## 11. Open calibration questions (TBD by playtest)

These do not block v2 implementation:

1. **Multiplier tightening or widening.** Current spread is 0.5 / 0.8 / 1.0 / 1.2. Mint→Poor ratio is 2.4×. If kingdoms with Mint equipment stomp non-kingdom players too hard, tighten Mint to 1.15. If the spread feels insignificant, widen Mint to 1.3.
2. **Per-stat multiplier override.** Currently a single multiplier applies to all five stats. Low-base-value stats (toughness 2, KBR 0.1) round to identical values across tiers — e.g., diamond toughness reads 2 across Fine/Good/Mint. If toughness/KBR differentiation matters in playtest, switch those two stats to additive deltas (Poor −1 / Fine 0 / Good 0 / Mint +1 for toughness; ±0.05 for KBR).
3. **Mine worldgen rarity** — replaces the old "Deep vs Royal merge" question (resolved: depth bands inside one structure, see §5). The live knob is now: how often do mine structures generate? Calibrate so a kingdom can plausibly find one within its claim radius, but rare enough that controlling one matters.
4. **Royal Smith court appointment requirement** — Royal Forge usable by *any* Expert Blacksmith, or *only* the king's appointed Royal Smith? The latter is more politically textured; the former is friendlier to non-court members.
5. **Mine depletion** (deferred from v1) — should mines exhaust over time? When this ships, the rate must be slow enough that "kingdom collapses because mine ran out" is rare, but fast enough that conquering a fresh mine has marginal value over holding an old one.
6. **Silk-touched ore-block loophole** — Silk Touch drops the ore *block*, which isn't in the `ore_or_ingot` tag, so it falls through to Good default at smelt time. Fix options: (a) include ore-block items in the tag and stamp them at break time; (b) track break geography on the block-form item via a transient component; (c) leave it — Silk Touch becomes the "ignore quality" path for players who don't care.
7. **Wild Good rate** — currently 5%. If solo wilderness progression feels too easy, push toward 0% (matching the original spec). If too punishing, push to 10%.

---

## 12. Out of scope (rejected, do not re-propose)

| Idea | Status | Reason |
|---|---|---|
| 5-tier quality system (Crude / Standard / Fine / Exceptional / Mint) | Rejected | Loot-rarity creep, philosophy violation. v1 collapsed to 3 tiers; v2 expanded to 4 (Poor/Fine/Good/Mint) for vanilla-baseline preservation. |
| 3-tier system with Fine = vanilla baseline | Superseded by v2 | Fine = vanilla forced unmarked items to take a 20% durability nerf when 4-tier was introduced. v2 makes Good = vanilla baseline; Fine is now an intentional tag below baseline. |
| Pristine drops on first combat hit | Rejected | Feel-bad UX. Sticky until 90% durability. |
| Mine depletion in v1 | Deferred to v2 | Useful later, but adds maintenance burden and frustration vector to a v1 design that already has a lot of new mechanics. |
| Cross-tier Mint preservation | Rejected | Mint requires Mint materials. No "free upgrade via clever material mixing." |
| Quality affects mining level | **Hard rejected** | This is the firewall. See §8 rule #1. |
| Numeric blacksmith levels 0–50 with XP grind | Rejected | Replaced by skill-tree node spend through `/menu` Blacksmithing tree. |
| Quality on fixed-material items (bow, crossbow, trident, mace, shield, elytra, etc.) | Deferred | Could be added later via crafting/source rule, but not in v1 to keep scope manageable. |

---

## 13. References

- **Spreadsheet (calibration tables):** [kingdom_smp_gear_economy_system_v3.xlsx](../../../Downloads/kingdom_smp_gear_economy_system_v3.xlsx)
- **Companion skill spec:** [profession-skill-system.md](./profession-skill-system.md)
- **Kingdom SMP design philosophy:** `/Users/joshua/.claude/projects/-Users-joshua-Kingdom-SMP-2-0/memory/kingdom_smp_design_philosophy.md`
- **Vanilla durability values:** https://minecraft.wiki/w/Durability
- **Vanilla armor values:** https://minecraft.wiki/w/Armor
- **Vanilla tier table:** https://minecraft.wiki/w/Tiers
