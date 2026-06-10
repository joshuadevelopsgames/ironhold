# Soulbound Enchantment  (feature ⑥)

**Status:** spec. Decisions: `fantasia_port_decisions.md` ⑥. Default death-loot model = **vanilla
drops**; Soulbound is the opt-in per-item protection. Replaces both "corpse compass" and Fantasia's
"keep gear, gear takes damage" (skipped/merged).

## 1. Goal
A custom enchantment that keeps the enchanted item with the player through any revive, at the cost
of a small, **never-fatal** durability hit.

## 2. The enchantment
- **1.26 enchantments are data-driven** (JSON in `data/ironhold/enchantment/soulbound.json`), not a
  `class`. ⚠️ confirm exact 1.26 enchantment JSON schema. Define:
  - `supported_items`: a new item tag `#ironhold:soulbound_applicable` = weapons + tools + armor
    (NOT shields/elytra/etc., matching `ItemQuality` scope).
  - `max_level: 1`, `weight` low (treasure-ish), `anvil_cost` moderate.
  - No vanilla effect components — the behavior is driven by our event handler keyed on
    `EnchantmentHelper.getItemEnchantmentLevel(SOULBOUND, stack) > 0`.
- A `DeferredHolder`/`ResourceKey<Enchantment>` constant `IronholdEnchantments.SOULBOUND` for lookups.

## 3. Acquisition (decision ⑥ = BOTH)
- **Findable early:** Soulbound enchanted books in dungeon/boss **loot tables** (`#minecraft` chest
  pools via a loot modifier; low weight). Like Mending — never on the enchant table.
- **Mastered via progression:** *applying/upgrading* Soulbound to gear is gated:
  - Requires **high Enchanting `ProfessionRank`** (see `06-rank-gated-crafting.md`) to apply at all, AND
  - Is **"blessed" at the Ender Shrine** (`03-ender-shrine.md`) — consumes an Ender Totem or boss
    material. The shrine block is the application surface; loot books are the discovery surface.
- ⚠️ Decide whether anvil-combining a found book also works pre-rank, or whether the shrine is the
  *only* application path. Lean: shrine-only application keeps the theme tight; books are the recipe input.

## 4. Death retention + durability cost
- **Hook `LivingDeathEvent` / the drops path** (`IronholdGameEvents` already handles
  `LivingDropsEvent`/`PlayerRespawnEvent` — extend there). On player death:
  1. Scan inventory (all slots incl. armor/offhand) for items with Soulbound.
  2. **Remove them from the drop set** so vanilla doesn't drop them.
  3. Apply a **small fixed durability hit** per saved item: `dmg = min(SOULBOUND_HIT, maxDmg − curDmg − 1)`
     so the result **always leaves ≥1 durability** (never breaks). v1 `SOULBOUND_HIT = 15` points
     (flat, item-agnostic) ⚠️ tune. Skip unbreakable/0-durability items.
  4. **Stash** the saved stacks on a transient `SOULBOUND_STASH` attachment (server-only, not
     copyOnDeath — we restore manually).
- **Restore on respawn:** in `PlayerRespawnEvent`, return stashed stacks to the (new) player entity,
  preferring original slots; overflow → inventory; full → drop at feet. Clear the stash.
- **Consistency across revive paths:** the same stash/restore runs for normal respawn, **Ender Shrine**
  revive (`03`), and **vanilla Totem** pop — Soulbound items never drop regardless of how you come back.

## 5. Tooltip
- `GearTooltipHandler` (or item tooltip): show `✦ Soulbound` (purple) + "Returns with you on death
  (takes minor wear)". Surface the rank/shrine requirement in the application UI, not the item.

## 6. Files (new)
- `data/ironhold/enchantment/soulbound.json` · item tag `soulbound_applicable.json`
- `enchant/IronholdEnchantments.java` (holder/key) · loot modifier for book drops
- `enchant/SoulboundDeathHandler.java` (stash on death, restore on respawn, durability clamp)
- `ModAttachments` — `SOULBOUND_STASH` (transient list-of-stacks attachment)
- Ender-Shrine "bless" interaction lives in `03-ender-shrine.md`

## 7. Open / TBD
- ⚠️ 1.26 enchantment JSON schema + loot-modifier API. Shrine-only vs anvil application.
- Final `SOULBOUND_HIT` value + whether it scales with item max durability instead of flat.
