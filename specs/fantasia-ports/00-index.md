# Fantasia Ports — Implementation Specs

Implementation specs for the features green-lit in the Fantasia walkthrough.
**Decisions of record:** [`../../docs/design/fantasia_port_decisions.md`](../../docs/design/fantasia_port_decisions.md).
**Style/companion specs:** [`../ore-quality-system.md`](../ore-quality-system.md), [`../profession-skill-system.md`](../profession-skill-system.md).

> Each spec is design-level: it names the real Ironhold classes/patterns to build against, the
> new registry objects, data model, server/client logic, integration points, and balance knobs.
> Exact numeric values + 1.26 API signatures are confirmed at implementation time (flagged ⚠️).

## Build order & status

| Phase | # | Feature | Spec | Size |
|---|---|---|---|---|
| P1 | ②③ | Parry + Dodge (combat actions) | [01-combat-actions.md](01-combat-actions.md) | M | ✅ **built v1.165.0** |
| P2 | ⑥ | Soulbound enchantment | [04-soulbound-enchant.md](04-soulbound-enchant.md) | S | ✅ **built v1.167.0** |
| P2 | ⑤ | Ender Shrine revive | [03-ender-shrine.md](03-ender-shrine.md) | M | ✅ **built v1.167.0** |
| P2 | ⑦ | Coin purse | [05-coin-purse.md](05-coin-purse.md) | S | ✅ **built v1.167.0** |
| P3 | ① | Boss-dropped accessories | [02-boss-accessories.md](02-boss-accessories.md) | S | ✅ **built v1.168.0** |
| P4 | ⑧ | Rank-gated crafting tree | [06-rank-gated-crafting.md](06-rank-gated-crafting.md) | M | 🟡 **reforge rank-gate built v1.170.0** (5-station gate deferred to runtime-verify) |
| P4 | ⑨ | Gear affix system + reroll | [07-gear-affixes.md](07-gear-affixes.md) | L | ✅ **built v1.174.0** — all 21 affixes + lock-and-reroll GUI (rank-gated locks, escalating cost) |
| P5 | ④ | Healer abilities + campfire heal | [08-healer-abilities.md](08-healer-abilities.md) | M | ✅ **built v1.171.0** (Mend/Sanctuary/Cleanse + rest) |
| P5 | ⑭ | Class promotion kits | [09-class-promotion-kits.md](09-class-promotion-kits.md) | S | ✅ **built v1.171.0** (8 class kits; tier 3/4 + Divine kits skipped by decision 2026-06-10) |
| P5 | ⑪ | Diet (reward) | [10-diet.md](10-diet.md) | M | ✅ **built v1.173.0** (cooked tag + Cooking-rank scaling shipped) |
| P6 | ⑲ | Villager lifecycle (hire/recruit/marry) | [14-villager-lifecycle.md](14-villager-lifecycle.md) | L | ⏸️ deferred — NPC-identity + AI + lock design; needs runtime |
| P6 | ⑱ | Boss-gated End + arenas + story dimension | [13-end-arenas-dimension.md](13-end-arenas-dimension.md) | L | 🟡 **End gate built v1.172.0** (arenas + story dimension deferred — content) |
| P7 | ⑯ | Bard instruments | [11-bard-instruments.md](11-bard-instruments.md) | S | ⏸️ deferred — needs .ogg audio assets |
| P7 | ⑰ | Atmosphere polish | [12-atmosphere.md](12-atmosphere.md) | M | ⏸️ deferred — custom sky/aurora renderers need runtime iteration |

Size: S ≈ ½–1 day · M ≈ 2–4 days · L ≈ 1–2 weeks.

## Shared infrastructure these specs reuse
- **Abilities:** `rpg.ability.Ability` (interface), `AbilityRegistry` (Z/X/C/V slots/class),
  `AbilityCooldowns` attachment (not copyOnDeath → resets on death), `net.AbilityCastPayload`.
- **Keybinds:** `client.IronholdKeys` (category + `KeyMapping`s), registered in `IronholdClient#registerKeyMappings`.
- **Networking:** `net.ModNetworking` registers payloads (cf. `SeashellDashPayload`, `ForgeMinigameStartPayload`).
- **Attachments:** `ModAttachments` (`AttachmentType.builder(...).serialize(CODEC).sync(STREAM).copyOnDeath()`).
- **Accessories:** `accessory.AccessoryItem` + `AccessorySlot` (5 slots) + `game.AccessoryTickHandler`.
- **Gear:** `gear.ItemQuality` (Poor/Fine/Good/Mint), `GearComponents`, `GearAttributeHandler`, `GearTooltipHandler`.
- **Professions:** `skill.Profession`, `ProfessionRank` (Novice→Master), `MiningGating` (the gate template), `SkillSavedData`.
- **Effects:** `effect.*` (`BleedingEffect`, `KillBurstEffect`, `TeleportBurstEffect`, …).
- **Quests:** `quest.QuestService`/`QuestDef`/`QuestReward`.
- **NPCs:** `entity.BlacksmithTobiasEntity`, `npc.PlayerNpcBonds`, `entity.goal.NpcFollowOwnerGoal`/`NpcStation*`.
