# Gear Affix System + Reroll  (feature ⑨)

**Status:** spec — the largest new system. Decisions: `fantasia_port_decisions.md` ⑨. Replaces
Fantasia "Reforged". Sits ON TOP of the existing `ItemQuality` durability system (does not replace it).

## 1. Goal
Random stat affixes + special on-hit abilities on gear, rerollable by talking to the blacksmith NPC.
Affix count = item quality tier → Mint becomes the prestige goal.

## 2. Data model
- **New data component** `ironhold:affixes` = `List<AffixInstance>`, registered exactly like
  `GearComponents.QUALITY` (`DeferredRegister.createDataComponents`, persistent + networkSynchronized).
  Put it in `GearComponents` (or a sibling `AffixComponents`).
- `AffixInstance` = `{ Identifier affixId, float roll }` (the rolled value within the affix's range).
- `gear.Affix` — a static registry of the **21 affixes** (see decisions ⑨ table). Each entry:
  `id`, `Category` (OFFENSIVE/DEFENSIVE/UTILITY/ON_HIT), `gear predicate` (weapon/armor/tool/boots/any),
  `roll range [min,max]`, `apply hook`, and a **name fragment** (prefix or suffix) for item renaming.

## 3. Slot count = quality tier  (the unification)
- `affixCapacity(stack) = switch(GearComponents.getQuality(stack)) { POOR→0, FINE→1, GOOD→2, MINT→3 }`.
- Rolling N affixes = pick N distinct affixes whose gear-predicate matches the item, each rolled in range.
  Category restriction by gear (decision ⑨ = by-category): OFFENSIVE+ON_HIT→weapons, DEFENSIVE→armor,
  UTILITY→tools/armor. Roll model = **rolled ranges** (decision ⑨).

## 4. Applying affix effects
- **Attribute affixes** (Keen, Swift, Piercing, Stalwart, Vital, Bulwark, Warded, Fleet, Prospector,
  Reaching, …): inject `AttributeModifier`s in **`GearAttributeHandler`** (extend it, or add
  `AffixAttributeHandler` listening to the same `ItemAttributeModifierEvent`). This composes cleanly
  with quality scaling already there. Map: Keen→ATTACK_DAMAGE, Swift→ATTACK_SPEED, Stalwart→ARMOR,
  Vital→MAX_HEALTH, Bulwark→KNOCKBACK_RESISTANCE, Fleet→MOVEMENT_SPEED, Prospector→mining speed (⚠️
  via dig-speed event, NOT an attribute, to respect the ore-quality firewall), Reaching→reach attrs.
- **Special on-hit affixes** (weapons): hook the player's melee-hit path
  (`AttackEntityEvent` / `LivingDamageEvent.Post` where attacker == player) and roll the affix chance:
  - **Leeching** → heal attacker `roll%` of damage dealt.
  - **Serrated** → `roll%` chance to apply `effect.BleedingEffect`.
  - **Concussive** → `roll%` chance to add knockback impulse to target.
  - **Voltaic** → `roll%` chance to chain a small shock to nearby mobs — **new** `effect`/projectile
    `VoltaicArcEntity` or an AoE damage tick (the one genuinely new effect to author).
  - **Soulrending** → on kill, add a `effect.KillBurstEffect` charge to the player.
- **Warded / Thorns / Brutal / Savage** → conditional damage modifiers in the player's
  incoming/outgoing damage handler (`LivingIncomingDamageEvent` for Warded; outgoing for Brutal/Savage;
  Thorns reflects in the same incoming hook). Scholar/Lucky/Enduring → XP/loot/durability hooks.

## 5. Reroll at the blacksmith  (decision ⑨)
- `entity.BlacksmithTobiasEntity#mobInteract` (cf. `AbstractVoicedNpcEntity#mobInteract → openMenu`)
  opens a new **`ReforgeMenu`** (`MenuType` + `client.screen.ReforgeScreen`, cf. `ForgeMinigameScreen`/
  `AccessoryMenu`). Networking via an `OpenReforgePayload` (cf. `OpenAccessoryPayload`).
- **Targeted lock + reroll the rest** (decision ⑨): the screen shows the item's affixes with a lock
  toggle each. Locked affixes persist; unlocked ones reroll. **Blacksmithing `ProfessionRank` → number
  of locks allowed** (Novice 0 … Master all-but-one). Cost = **gold coins** (+ optional material),
  **escalating** per reroll on the same item (track a reroll counter component, or flat-per-tier ⚠️).
- Reroll never changes affix *count* (that's quality-bound) — only which affixes occupy the slots.

## 6. Tooltip + naming
- `GearTooltipHandler`: list each affix as a colored line (value shown), grouped under an "Affixes" header.
- Optional flavor: compute a display name from the highest-tier prefix + suffix fragments
  ("Keen Iron Sword of Leeching"). ⚠️ v1 may skip renaming and just show affix lines.

## 7. Where affixes come from (initial roll)
- Gear gains affixes when it acquires non-default quality (forge minigame output / loot). A new
  `AffixRoller.roll(stack)` runs on: forge-minigame success (`BlacksmithingMinigameManager`), and on
  qualified loot generation. Vanilla-found Good/Mint gear without affixes can be first-rolled at the blacksmith.

## 8. Files (new)
- `gear/Affix.java` (registry of 21), `gear/AffixInstance.java`, `GearComponents.AFFIXES` component
- `gear/AffixAttributeHandler.java` (ItemAttributeModifierEvent), `gear/AffixCombatHandler.java` (on-hit)
- `gear/AffixRoller.java` · `effect/VoltaicArc*` (the one new effect) · `gear/AffixTooltip` lines
- `inventory/ReforgeMenu.java` + `client/screen/ReforgeScreen.java` + `net/OpenReforgePayload.java` + `ReforgeActionPayload`
- hook `BlacksmithTobiasEntity#mobInteract`

## 9. Open / TBD (balance pass)
- Per-affix final value ranges + roll weights. Reroll cost curve + lock counts per rank.
- Voltaic effect implementation (arc entity vs AoE). Item-renaming on/off for v1.
