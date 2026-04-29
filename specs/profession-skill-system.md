# Profession Skill System

**Status:** v1 spec, partially calibrated. All design questions resolved; numeric calibration values (career budget, per-tree caps, exact milestone list) ship as v1 targets, adjustable in playtest.

**Companion spec:** [ore-quality-system.md](./ore-quality-system.md) — defines the gear/quality/mine payload that the Blacksmithing and Mining trees actually unlock.

---

## 1. Goal & design constraints

Define the talent/skill-tree subsystem that powers the existing `/menu` Skills tab in Ironhold ([MainMenuScreen.java:73-77](../src/main/java/kingdom/smp/client/screen/MainMenuScreen.java)). The Skills tab currently displays 8 profession placeholders at Lv. 0 with a "coming soon" note; this spec is the design that fills it in.

Constraints — Kingdom SMP design philosophy is binding:

- **Minecraft-native first.** No imported grand-strategy abstractions (no loyalty bars, no tracked-stat dashboards beyond what vanilla already shows).
- **Physical/in-world levers preferred** over GUI dashboards where possible.
- **Skills must thread into the locked political layer** — Blacksmithing into Royal Forge / Royal Smith court appointment, Mining into mine ownership / kingdom power, Trading into the trade tax flow, etc.

---

## 2. Two tree types

A player has **9 trees at any time**:

- **8 profession trees** — one per profession from the existing Skills tab: **Blacksmithing, Mining, Farming, Cooking, Alchemy, Fishing, Enchanting, Trading.** Linear ranks (Novice → Apprentice → Journeyman → Expert → Master).
- **1 class tree** — scoped to whichever of the 27 Ironhold classes the player currently is ([PlayerClass.java](../src/main/java/kingdom/smp/rpg/PlayerClass.java)). Branching nodes. Builds on the existing class-skill catalog ([ClassSkills.java](../src/main/java/kingdom/smp/rpg/ClassSkills.java)).

The two tree types use **separate point pools**. Profession points and class points do not interchange.

---

## 3. Point economy

### 3.1 Profession points — milestone-unlock only

Profession points are **never gained from organic XP grind.** Every point is awarded by a specific in-world milestone. The Skills-tab progress bars in `/menu` will be repurposed to show "X / Y points unlocked," not continuous XP fill.

**Per-character starting allocation:** 3 points (allocated at character creation, free-floating).

**Career budget target:** ~19 total points = 3 starting + ~16 milestones across the character's lifetime.

**Free-floating** — every earned profession point can be spent in any of the 8 profession trees. (Some milestones are tree-pinned exceptions; see §3.3.)

### 3.2 Class points — class-XP gated

Class points come from the existing Ironhold class-progression curve ([RpgProgression.java](../src/main/java/kingdom/smp/rpg/RpgProgression.java), `40 + lvl × 26`). Each class level-up grants 1 class point, spendable only in the current class's tree.

**Promotion behavior:** When a player promotes to a higher-tier class (Squire → Knight, etc.), unspent class points carry over but already-spent points reset (the new class has a different tree). This is implicit because the previous class tree is no longer accessible.

**Class points are not respeccable.** Class identity is permanent; respec means re-rolling the class.

### 3.3 Per-character vs. server-first unlocks

- **Per-character milestones (~12)** — every player can earn each one once. Examples: kill the Wither, kill the Warden, forge your first Mint-tier item, complete N Royal Quests, hold a claim continuously for 30 days.
- **Server-first milestones (~4)** — only the *first player on the server* to achieve each one earns the point. These are political prestige unlocks tied to Kingdom SMP. Examples: first Dragon Egg equipped, first Royal Forge built, first Royal Mine claimed, first kingdom to formally declare war.

The full milestone list is calibration-stage and lives in §6.

### 3.4 Tree-pinned vs. free-floating

Most profession unlocks are **free-floating** (spendable in any profession tree). A subset are **tree-pinned** to thematically-aligned trees:

- *Forge your first Mint item* → +1 Blacksmithing only
- *First Royal Mine claimed* → +1 Mining only
- *Catch a tropical fish in deep ocean* → +1 Fishing only

Tree-pinned exceptions encourage actually engaging with the profession that earned the point. Combat/political milestones stay free-floating.

### 3.5 Respec

**Profession trees:** respec is allowed at the [Royal Forge](./ore-quality-system.md#royal-forge) structure. Each respec refunds **N − 1** points where N is the points being refunded — i.e., **a flat 1-point loss per respec**, regardless of how many points are being refunded in that operation.

**Class tree:** no respec. Class identity is permanent.

**Failure mode:** if a respec would leave the player with fewer than 0 unspent points (e.g. respeccing only 1 point loses it entirely), the operation is allowed but the loss stands.

---

## 4. Profession trees

Each of the 8 professions uses the **same 5-rank linear shape**. Rank costs are uniform across professions:

| Rank | Point Cost | Cumulative |
|---|---|---|
| Novice | 1 | 1 |
| Apprentice | 1 | 2 |
| Journeyman | 2 | 4 |
| Expert | 2 | 6 |
| Master | 3 | 9 |

**Maxing one profession requires 9 points.** Career budget of 19 means a player can comfortably max 2 (18 points spent) with 1 wildcard spare. Maxing 3 (27 points) is unreachable.

### 4.1 Blacksmithing — detailed

The flagship tree. Drives the gear-quality system from [ore-quality-system.md](./ore-quality-system.md).

| Rank | Node | Effect |
|---|---|---|
| Novice | Field Patching | 60% repair efficiency, full fatigue |
| Apprentice | Proper Tools | 75% repair efficiency, −10% fatigue gain |
| Journeyman | Fine Preservation | 85% repair efficiency, −20% fatigue gain. Can refine Fine gear to Pristine. |
| Expert | Mint Handling | 92% repair efficiency, −30% fatigue gain. Can refine Mint gear to Pristine. Required to use a Royal Forge. |
| Master | Masterwork Conservation | 95% repair efficiency, −40% fatigue gain. None of correct mats. |

### 4.2 Mining — detailed stub

| Rank | Node | Effect (calibration TBD) |
|---|---|---|
| Novice | Prospecting | Visible ore-density hint within 8 blocks |
| Apprentice | Claimed Mine Yield | +10% drop count from claimed-tier mines |
| Journeyman | Deep Mine Access | Permits mining Deep-tier mines without speed penalty |
| Expert | Royal Surveying | Drop-chance bonus on Mint ore from Royal mines |
| Master | Veinbreaker | Adjacent ore propagation on first hit (vein-mining) |

### 4.3 Other professions — v2 stubs

Farming, Cooking, Alchemy, Fishing, Enchanting, Trading ship as named placeholders with rank slots reserved. Detailed nodes belong in v2 once Blacksmithing + Mining are validated in playtest.

Initial v2 themes:

- **Farming** — yield, crop quality, animal husbandry
- **Cooking** — meal quality, feast buffs, spoilage mitigation
- **Alchemy** — potion crafting, reagent efficiency, brew failure reduction
- **Fishing** — rare-catch chance, bait conservation, sea economy
- **Enchanting** — enchant stability, gold-quality synergy, reroll handling
- **Trading** — toll efficiency, market discounts, contract fulfillment

---

## 5. Class trees

Class trees are **branching**, with each of the 27 classes defining its own node graph. The existing [ClassSkills.java](../src/main/java/kingdom/smp/rpg/ClassSkills.java) catalog (4 named skills per class for KNIGHT / RANGER / WIZARD / CLERIC, etc.) becomes the seed; each class tree gates 4–8 nodes minimum.

Branching shape lets a class spec into archetypes — e.g. a Knight can pursue **Heavy Plate** (defensive), **Cavalry** (mobility), or **Shield Wall** (support) paths. Mutually exclusive sub-branches are supported via Puffish-style `exclusive_root` per top-level branch.

Class tree rank costs match profession trees (1 / 1 / 2 / 2 / 3) per linear chain. Branching choices are at the player's discretion within the class point budget.

Detailed per-class node graphs are out of scope for this spec — they belong in `class-trees.md` (v2 followup).

---

## 6. Milestone unlock catalog (v1 calibration target)

Total: 16 milestones (12 per-character + 4 server-first). Concrete list is calibration-stage; the *count* is the contract, individual entries are tunable.

### 6.1 Per-character (12) — each player earns once

| Milestone | Points | Pinning |
|---|---|---|
| Kill the Wither | +1 | Free |
| Kill the Warden | +1 | Free |
| Kill the Ender Dragon | +1 | Free |
| Forge your first Mint-tier item | +1 | Blacksmithing |
| Mint a coin (when minting exists) | +1 | Trading |
| Catch the rare-fish set | +1 | Fishing |
| Brew a tier-3 potion | +1 | Alchemy |
| Eat a feast you cooked | +1 | Cooking |
| Harvest 1000 crops | +1 | Farming |
| Reach max Enchanting at a Bookshelf-15 setup | +1 | Enchanting |
| Mine a Mint-tier ore vein | +1 | Mining |
| Hold an active claim continuously 30 in-game days | +1 | Free |

### 6.2 Server-first (4) — only the first player gets it

| Milestone | Points | Pinning |
|---|---|---|
| First Dragon Egg equipped (cosmetic head slot) | +2 | Free |
| First Royal Forge built on the server | +1 | Blacksmithing |
| First Royal Mine claimed | +1 | Mining |
| First kingdom to formally declare war | +1 | Free |

(Server-firsts are the "Dragon Egg equip" example the user specifically called out — these are prestige unlocks tied to Kingdom SMP politics, not random achievements.)

**Server-first total: 5 points across 4 unlocks** (Dragon Egg gives 2 because being literally first to claim the Dragon Egg in a multi-kingdom server is uniquely difficult). Per-character total: 12. Plus starting 3 = **20 max career budget**, 1 above the ~19 target — fine for a player who somehow achieves *every* server-first.

---

## 7. Engine — build our own

We do **not** depend on Puffish Skills or fork PMMO. Reasons:

- **License blockers:** ZsoltMolnarrr/SkillTree (and Puffish content) and Caltinor/Project-MMO-2.0 are both All Rights Reserved. We can study but not redistribute their code.
- **Version blocker:** Puffish targets MC 1.21.1 (NeoForge 21.x). Ironhold targets MC 1.26.1 (NeoForge 26.1+). The API gap is substantial.
- **Scope fit:** Both reference mods diverge from our needs in important ways — Puffish has no XP-from-action profession model, PMMO has no spendable-point tree.

We adopt their *patterns*, not their code. Patterns are listed in §8.

---

## 8. Implementation patterns

Adopted from Puffish Skills (via SkillTree content pack) and Project MMO 2.0. Reference-only; no code is copied.

### 8.1 Datapack JSON layout per tree

```
data/ironhold/skill_trees/<tree_id>/
  category.json       — tree-wide config (title, icon, point cap, line colors)
  nodes.json          — node definitions: { id, icon, point_cost, effects[], description }
  placements.json     — { node_uuid: { definition: id, x, y, root: bool } }
  connections.json    — { bidirectional: [[uuid_a, uuid_b], ...], unidirectional: [...] }
```

`<tree_id>` is one of `blacksmithing`, `mining`, `farming`, `cooking`, `alchemy`, `fishing`, `enchanting`, `trading`, or `class/<class_id>`.

Splitting "what a node does" (nodes.json) from "where it sits" (placements.json) means the same node definition can be reused in multiple trees, and re-layouts don't touch effect data.

### 8.2 Explicit edge arrays for prerequisites

Connections are explicit edge lists, not position-derived. This supports diamond and loop topologies cleanly and avoids brittle nearest-neighbor logic. (Class trees use this; profession trees are linear so connections degenerate to a chain.)

### 8.3 Typed pluggable `effects[]` registry

Every node has `effects: [{type, data}]`. `type` is a `ResourceLocation` resolved via a `DeferredRegister<NodeEffect>`. Built-ins ship with Ironhold:

- `ironhold:attribute` — apply a vanilla attribute modifier
- `ironhold:repair_efficiency` — modify Blacksmithing repair output
- `ironhold:fatigue_gain` — modify Repair Fatigue accumulation
- `ironhold:recipe_unlock` — gate a recipe behind the node
- `ironhold:tag_membership` — add player to a permission tag
- `ironhold:event_unlock` — uncancel a vanilla event the gate would normally cancel

Other content can register more via `IronholdRegistrationEvent` → `NodeEffectRegistry`.

### 8.4 PMMO-style XP-from-action mapping (deprecated for v1)

PMMO's per-object JSON for XP-from-action is **not used** in v1 because our point economy is unlock-only. The data shape is reserved for a future `xp_values` rollout if we later add organic XP as a supplementary point source — but v1 doesn't ship it.

### 8.5 Server-authoritative state via SavedData

Player skill state is stored in a single `IronholdSkillSavedData` (Minecraft `SavedData` attached to the server level), keyed by player UUID:

```
{
  player_uuid: {
    profession_points_unspent: int,
    profession_points_lifetime_earned: int,
    profession_unlocks: { tree_id: Set<NodeId> },
    class_points_unspent: int,
    class_unlocks: { class_id: Set<NodeId> },
    milestones_completed: Set<MilestoneId>
  }
}
```

(SavedData over per-player attachment because: easier admin export/migration, single source of truth, server-first milestones need a shared registry anyway.)

### 8.6 Sync to client

Custom `CustomPacketPayload` packets:

- On player join: `S2C_FullSkillState` — sends the player's full state for UI rendering
- On point spend: `C2S_SpendPoint` (request) → `S2C_NodeUnlocked` (broadcast confirmation)
- On milestone: `S2C_MilestoneAwarded` — toast notification + state update
- On respec: `C2S_RespecRequest` (must be at Royal Forge) → `S2C_RespecResolved`

Client mirrors state in `ClientSkillData` for `MainMenuScreen` rendering.

### 8.7 UI

- **`/menu` Skills tab** — already exists; replaced placeholder bars with "X / Y points unlocked" rows + click-through to per-tree screen.
- **Per-tree screen** — new `Screen` class (`SkillTreeScreen`). Pan/zoom canvas drawn from `placements.json` coordinates; connection lines colored per state (locked/available/unlocked) from `category.json`; tooltips from node `description`.
- **Open routes:** `/menu` Skills tab click, dedicated keybind, and `/skilltree <tree_id>` command.
- **Milestone toast:** in-world chat/toast notification when a milestone is awarded ("⚒ Milestone: First Mint-tier item forged. +1 Blacksmithing point.").

---

## 9. Hard rules (do not violate)

1. **Quality does NOT change vanilla mining tier.** A Standard Diamond Pickaxe still mines obsidian; a Mint Iron Pickaxe still cannot. (Cross-reference: same firewall as in [ore-quality-system.md](./ore-quality-system.md).)
2. **No XP-from-action point gain in v1.** Unlock-only. Any "level up" of a profession only happens when a milestone is awarded.
3. **No class respec.** Class identity is permanent except by re-roll/admin.
4. **Server-first milestones are atomic per server.** Once awarded, no second player can earn the same one until the world is reset. Track in `IronholdSkillSavedData.server_firsts_claimed`.
5. **Free-floating points cannot be reverted to tree-pinned.** Once spent in a profession, a free-floating point can be respec'd freely; once spent in a tree-pinned profession matching its pin, it loses the point-loss penalty's "free choice" credit.

---

## 10. Out of scope (rejected, do not re-propose)

| Idea | Status | Reason |
|---|---|---|
| Organic XP-from-action gives points | Rejected for v1 | User wants unlock-only — milestones are the *only* point source. May revisit in v2 as supplementary. |
| Spendable points convert into class XP | Rejected | Separate pools is the locked decision. |
| Free respec | Rejected | −1 per respec is the locked cost. |
| Class respec | Rejected | Class identity is permanent. |
| Loot-rarity tier expansion (Crude / Exceptional) | Rejected | Three quality tiers only — see ore-quality-system.md. |
| Skill XP grind with 0–50 numeric levels | Rejected | Replaced by milestone-unlock points and rank nodes. |
| Class restricts which professions can be specced | Rejected | No restriction — any class can spec any profession. |
| Per-server-first as the only unlock model | Rejected | Per-character is the primary mode; server-first is the prestige minority. |

---

## 11. Open calibration questions (TBD by playtest)

These do not block v1 implementation but should be revisited after the first playtest cycle:

1. **Exact milestone list** — the §6 entries are a v1 target. Some may be too easy / hard / off-theme; tune in playtest.
2. **Per-tree point cap value** — currently 9 (max one rank chain). May need to allow extra capstone perks beyond Master, capped at e.g. 12 per tree.
3. **Career budget** — currently ~20. May tighten to 16 if "max 1 + splash" feels right, or expand if veteran retention suffers.
4. **Class point grant rate** — currently 1 per class level. May need to scale with promotion tier (Tier 1 classes earn slower than Tier 4).
5. **Respec cost formula** — currently flat −1 regardless of points refunded. May need to scale (−1 per 5 points refunded?) if mass-respec becomes too cheap at high totals.
6. **Tree-pinned vs. free-floating ratio** — currently 7 pinned of 16 milestones (44%). Calibrate based on how often players feel "stuck" with pins they don't want.

---

## 12. References (patterns only — no code reuse)

- **Puffish Skills** by Puffish/CodexAdrian — datapack-driven skill tree engine for MC 1.21.1. License: redistribution-restricted; learn-from only. https://modrinth.com/mod/puffish-skills
- **SkillTree** by ZsoltMolnarrr — content pack driving Puffish, MC 1.21.1. License: All Rights Reserved 2025. https://github.com/ZsoltMolnarrr/SkillTree
- **Project MMO 2.0** by Caltinor (continuation of Harmonised7 / Caltinor 2022) — flat-skill profession-XP system, MC 1.21.1 active. License: All Rights Reserved 2022. https://github.com/Caltinor/Project-MMO-2.0
- **Ironhold class system** — internal: [PlayerClass.java](../src/main/java/kingdom/smp/rpg/PlayerClass.java), [RpgProgression.java](../src/main/java/kingdom/smp/rpg/RpgProgression.java), [ClassSkills.java](../src/main/java/kingdom/smp/rpg/ClassSkills.java), [MainMenuScreen.java:73-77](../src/main/java/kingdom/smp/client/screen/MainMenuScreen.java).
