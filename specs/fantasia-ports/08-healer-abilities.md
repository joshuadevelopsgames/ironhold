# Healer Abilities + Campfire Heal  (feature ④)

**Status:** spec. Decisions: `fantasia_port_decisions.md` ④. **Regen is UNCHANGED** — this is purely
additive healing. Gives the healer classes a real party role.

## 1. Goal
Two additive healing avenues: (a) active heal/cleanse/regen abilities for the healer classes, (b) a
safe-haven rest/campfire heal. No nerf to vanilla regen, no heal-item gating.

## 2. Healer class abilities (reuse the Ability system)
- Implement the `rpg.ability.Ability` interface (id/cooldownTicks/unlockLevel/classes/translationKey/
  `cast(ServerPlayer)`) and register in `AbilityRegistry` slots (Z/X/C/V) for the healer classes:
  **`CLERIC`, `MEDIC`, `SAINT`, `BISHOP`, `REDEEMER`** (and the divine line as appropriate).
- Cast = server-authoritative via existing `AbilityCastPayload`; cooldowns via `AbilityCooldowns`;
  HUD already renders class ability slots.
- Starter kit (⚠️ tune cooldowns/values):
  | Ability | Effect | Classes |
  |---|---|---|
  | **Mend** | heal the nearest ally / self in cone (instant or short regen) | MEDIC, CLERIC |
  | **Cleanse** | strip negative effects (poison/wither/bleed) from an ally | CLERIC, BISHOP |
  | **Sanctuary** | ground a small AoE that regen-pulses allies inside for a few s | SAINT, BISHOP |
  | **Martyr's Gift** | transfer your health to a critically-low ally (REDEEMER flavor) | REDEEMER, MARTYR |
- Targeting: nearest valid ally in look-cone / radius; self if none. Reuse `effect.PassiveAuraEffect`
  for the AoE pulse pattern; particles via `AbilityEffects`.

## 3. Campfire / rest heal
- A `PlayerTickEvent`-driven handler: when the player is **near a lit campfire/soul campfire** (or
  sleeping) AND **out of combat** (no damage taken for ~8s), apply gentle regen (e.g. Regeneration I
  while in range). A "resting" indicator (action-bar / icon). ⚠️ radius + rate are tunable.
- This is a *bonus* lane stacking on vanilla regen, not a replacement.

## 4. Files (new)
- `rpg/ability/Mend.java`, `Cleanse.java`, `Sanctuary.java`, `MartyrsGift.java` (+register in `AbilityRegistry`)
- `game/CampfireRestHandler.java` (PlayerTick proximity + out-of-combat → regen)
- lang keys `ability.ironhold.*`

## 5. Open / TBD
- Final ability list per healer class + values/cooldowns. Ally vs enemy detection in PvP context.
- Campfire heal radius/rate + out-of-combat window.
