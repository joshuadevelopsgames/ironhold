# Ender Shrine — Sanctuary Revive  (feature ⑤)

**Status:** spec. Decisions: `fantasia_port_decisions.md` ⑤. The bind-a-sanctuary revive, reworked
around vanilla totems.

## 1. Goal
A placed, bound block that catches a player on death **when they have no handheld totem**, teleporting
them home, healing, with a protective buff — fueled by player-stocked charges.

## 2. New content
### 2.1 Ender Totem (item)
- New item `ENDER_TOTEM` (`item.EnderTotemItem`). Crafted from **Totem of Undying + Ender Pearl**
  (shapeless recipe). Flavor: ender pearl = "teleport-home" fuel. Stacks to 16.
- Sole use: stocking the shrine (§2.2). It is NOT a handheld revive itself (that's the vanilla totem).

### 2.2 Ender Shrine (block + block entity)
- `block.EnderShrineBlock` + `EnderShrineBlockEntity` (cf. existing `ClassStoneBlock`/`GuillotineBlock`
  + BE patterns). GeckoLib model optional (cf. gargoyle/guillotine); a static model is fine v1.
- **BE state:** `int charges` (cap **CHARGES_MAX = 5** ⚠️), `String ownerUuid`, `String boundDim`,
  `BlockPos boundPos` (its own pos). Serialized to NBT.
- **Binding = right-click-to-own** (mirror the `LOCK_OWNER` lock pattern): first empty-hand right-click
  by a player with no shrine sets `ownerUuid` + binds that player's `BOUND_SHRINE` attachment
  (`{dim, pos}`) to this block. One shrine per player; re-binding a new shrine clears the old link.
  ⚠️ Decide multiplayer: can two players bind the same shrine? v1 = one owner per shrine, one shrine per player.
- **Stocking:** right-click with an `ENDER_TOTEM` in hand → `charges = min(charges+1, MAX)`, consume one
  totem, particle/sound feedback. Right-click empty hand shows charge count on the action bar.

## 3. Player data
- `ModAttachments.BOUND_SHRINE` — `{ String dim, long packedPos, int charges-cache }`, **copyOnDeath**
  (so the bound link survives the death it's about to rescue), server-only (not synced) or synced for a
  future HUD. Stores enough to validate + teleport without loading the BE.

## 4. Death resolution order  (the core)
Hook the **same death path** as Soulbound, BUT ordering matters. In `LivingDeathEvent` (cancellable) /
the totem-check path:
1. **Vanilla Totem of Undying in hand/offhand** → let vanilla handle it (revive in place). Do nothing;
   shrine untouched. (Vanilla checks totem before death finalizes; our handler only acts if no totem fires.)
2. **Else, if** the player has a `BOUND_SHRINE` with `dim == player.dimension` (⚠️ **same dimension,
   any distance** — decision ⑤) AND the bound shrine has `charges > 0`:
   - `event.setCanceled(true)`; set health to full (or `REVIVE_HEAL` ⚠️).
   - Apply buff suite: Fire Resistance + Slow Falling + Absorption (v1: 10s / 10s / +4 absorption ⚠`)
     so you don't instantly re-die on arrival.
   - **Teleport** to the bound shrine pos (top of block). Two-stage flourish (reuse `TeleportBurstEffect`):
     blast at death spot → totem-burst at shrine.
   - Decrement the shrine's `charges` (update BE; the attachment cache + BE are reconciled on next load).
   - Run the **Soulbound restore** path too (items already kept; this is just a different "came back" route).
3. **Else** → normal death.
- Edge: shrine block destroyed/missing → treat as no charge (fail to normal death); clear stale link.

## 5. Cross-dimension = later upgrade
Reach is **same-dimension** v1. A future "Greater Ender Shrine" or an upgrade material lifts the
`dim == player.dimension` check to cross-dimension. Leave a `boolean crossDim` flag on the BE, default false.

## 6. Files (new)
- `item/EnderTotemItem.java` (+`ModItems`, recipe json) 
- `block/EnderShrineBlock.java` + `block/EnderShrineBlockEntity.java` (+`ModBlocks`, BE type)
- `block/wardheart/…`-style handler or `game/EnderShrineDeathHandler.java` (death-order logic)
- `ModAttachments.BOUND_SHRINE` · model/texture assets · loot/recipe data
- "Bless Soulbound" interaction hook (see `04-soulbound-enchant.md` §3) added to the shrine right-click menu

## 7. Open / TBD
- One-owner-per-shrine vs shared. REVIVE_HEAL amount + buff durations. CHARGES_MAX.
- Whether the shrine also doubles as the Soulbound application station (lean: yes — unifies death theme).
