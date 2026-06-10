# Boss-Dropped Signature Accessories  (feature ①)

**Status:** spec. Decisions: `fantasia_port_decisions.md` ①. Replaces Fantasia "Boss Souls" — each
boss drops a unique equippable accessory instead of a consumable soul.

## 1. Goal
Each boss drops one guaranteed signature accessory that grants a playstyle-defining passive (+ an
optional active on flagships) while worn in an accessory slot. Build identity comes from the 5-slot cap.

## 2. Reuses (almost everything exists)
- `accessory.AccessoryItem` (override `onAccessoryTick`/`onEquipped`/`onUnequipped`/`getAccessoryTooltip`)
- `accessory.AccessorySlot` (5 slots), `accessory.AccessoryInventory` attachment (**copyOnDeath** →
  artifacts kept on death, decision ①), `game.AccessoryTickHandler` (efficient equip-flip modifier apply)
- `rpg.ability.AbilityCooldowns` for the optional active; `effect.*` for passives.

## 3. Pattern per artifact
Each is an `AccessoryItem` subclass:
- **Passive:** apply via `AccessoryTickHandler` (attribute modifier added on equip, removed on unequip —
  follow the `HERMES_MOVE` cached-flip pattern so it's not re-applied every tick).
- **Optional active (flagships):** a keybind/use trigger → `AbilityCooldowns`-gated effect. Reuse the
  ability cast path; surface on the cooldown HUD.
- **Drop:** boss `LivingDeathEvent` grants the artifact at 100% on **first kill** (track via a
  per-player "boss artifacts earned" set so it isn't farmable; later kills drop normal loot). Hook in
  `IronholdGameEvents` alongside the existing special-mob-drop handlers.

## 4. First two (live bosses)
| Boss | Artifact | Passive | Optional active |
|---|---|---|---|
| **King Enderman** | *Ender Regalia* | Endermen ignore you; immune to void-rift pull | Sneak-tap **blink** (reuse `CloudJump`/`TeleportBurstEffect`), cooldown |
| **Stone Golem** | *Stoneblood Amulet* | Flat damage reduction + knockback & slowness immunity | — (passive-only) |

Future bosses (Void Invoker/Null Stalker, Siren, Plague Doctor/Cemetery Watcher, Skeleton King) each add
one `AccessoryItem` subclass + a drop hook + art — **no system work**.

## 5. Files (new, per artifact)
- `item/EnderRegaliaItem.java`, `item/StonebloodAmuletItem.java` (+`ModItems`, models/textures, lang)
- `game/AccessoryTickHandler` — add the two passive modifier blocks (or per-item `onAccessoryTick`)
- `ModAttachments` — `BOSS_ARTIFACTS_EARNED` (set of boss ids, copyOnDeath) to gate guaranteed drops
- drop hooks in `IronholdGameEvents`

## 6. Open / TBD
- Exact passive magnitudes + active cooldowns (balance pass). Blink distance/obstruction rules.
- Whether artifacts are tradeable/droppable when un-earned via creative.
