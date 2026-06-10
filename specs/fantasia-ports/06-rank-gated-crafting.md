# Rank-Gated Crafting Tech-Tree  (feature ⑧)

**Status:** spec. Decisions: `fantasia_port_decisions.md` ⑧. Extends the proven `skill.MiningGating`
pattern from ore-extraction to **production**, so profession ranks ARE the full tech tree. Companion:
[`../profession-skill-system.md`](../profession-skill-system.md).

## 1. Goal
You can't *produce* higher-tier gear/enchants/potions/food until your relevant **`ProfessionRank`**
allows it — mirroring how `MiningGating` already gates ore mining. Each rank-up fires a tutorial quest.

## 2. The gate (mirror MiningGating)
- New `skill.CraftGating` (sibling to `MiningGating`): a `Map<gated output → (Profession, ProfessionRank)>`.
  Read the player's rank from `SkillSavedData` (same source `MiningGating` uses).
- **Enforcement = cancel + action-bar** (same UX as `MiningGating`'s gated-ore message). Hook the
  *output* of each production station so an under-ranked player can't take the result:
  - **Crafting table / inventory craft** — gate the result slot / `ItemCraftedEvent` path. ⚠️ confirm
    the cancellable pre-take hook; fallback = clear output when rank insufficient + message.
  - **Furnace/Blast furnace** — gate smelting of gated ingots/gear inputs (or the take from output slot).
  - **Smithing table** — gate netherite/affix upgrades by Blacksmithing rank.
  - **Brewing stand** — gate stronger potions by Alchemy rank.
  - **Campfire / smoker / cooking** — gate buff-food by Cooking rank.
  ⚠️ Per-station event wiring is the bulk of the work; design is one shared `CraftGating.check(player, result)`.

## 3. Gated professions & tiers (decision ⑧ = all four)
| Profession | Gates | Example rungs (⚠️ tune) |
|---|---|---|
| **Blacksmithing** | crafting/forging weapon+armor tiers | Novice→iron gear · Apprentice→gold/chain · Journeyman→diamond · Expert→netherite |
| **Enchanting** | enchant level/rarity (incl. **Soulbound** apply, see `04`) | rank gates max enchant level + treasure enchants |
| **Alchemy** | potion/tonic tiers (incl. healing items) | rank gates II/extended/splash/lingering + custom tonics |
| **Cooking** | buff-food tiers (feeds `10-diet.md`) | rank gates multi-ingredient meals + diet-buff foods |

Mirror `MiningGating`'s cumulative-point tier mapping; early-game tiers stay open, gating bites at mid/high tiers.

## 4. Quest-as-tutorial (decision ⑧)
- On a gated **rank-up**, fire a short `QuestDef` via `QuestService` explaining what just unlocked
  (the recipes now available). One quest per rung per profession. Reuses the quest board + feedback.
- Keep it a *toast + readable quest entry*, not a forced modal.

## 5. Files (new)
- `skill/CraftGating.java` (map + `check()` + action-bar message)
- per-station handlers (`game/CraftGatingHandlers.java`) subscribing the relevant craft/smelt/brew/cook events
- `quest/` defs for the rank-up tutorial quests (data-driven where possible)

## 6. Open / TBD
- ⚠️ Cancellable hooks for each station (crafting/furnace/smithing/brewing/cooking).
- Exact item→(profession,rank) tables. Whether gating blocks *crafting* or only *equipping/using*
  (lean: blocks taking the crafted output, like MiningGating blocks the drop).
