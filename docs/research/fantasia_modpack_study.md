# Fantasia (Beta 11.9h) — Design Study for Ironhold

> Source: `Fantasia Beta 11.9h.zip` (CurseForge pack, author **waccyy**).
> Forge **47.4.10** / MC **1.20.1**, **350 mods**, ~150 custom KubeJS scripts.
> Studied 2026-06-07. The pack ships *no jars* — just `manifest.json` + `overrides/`
> (configs, KubeJS, CustomNPCs, FancyMenu). The transferable value is the **design**,
> not the code (Ironhold is NeoForge 1.26 Java — the KubeJS patterns become Java).

Ironhold should treat Fantasia as a **north-star reference for a Dark-Souls-flavored
medieval-fantasy survival RPG**: deliberate weighty combat, deep blacksmithing
progression, boss "souls" as power rewards, class-based starts, heavy roleplay framing,
and atmospheric immersion. Below: what it is, how it's built, and what to port.

---

## 1. The identity (what "very similar to how I want Ironhold to be" means)

waccyy's own words from the in-game dev message quest:

> "This modpack was made with a huge emphasis on **ROLEPLAYING**, so have a little
> whimsy in your life… Things are only refined up to the Diamond Tier… the main
> storyline ends at **Chapter II – The Hall of Decimus**."

Design pillars, in priority order:

1. **Deliberate, heavy medieval combat** — slow attack speeds, directional attacks,
   blocking/parry, dodge-rolls, stamina-like commitment. Not Minecraft click-spam.
2. **Blacksmithing as the core progression spine** — ore → ingot → alloy → blade/head
   part → reforge → temper → assemble. Tiering is Copper → Iron → Steel → Diamond →
   Netherite, gated by an "Age of Advancement" advancement tree.
3. **Bosses as milestones that grant permanent power** — kill a boss, eat its **Soul**,
   gain a permanent buff suite (lost on death). Dark Souls references everywhere
   (Ornstein & Smough boss, "Gwyn Lord of Cinder" music, boss souls, Hall of Decimus).
4. **Class-based RPG starts** — pick a class kit at spawn (Knight, Mage, Thief…),
   each with themed gear, weapon, and a one-line backstory.
5. **Survival depth / friction** — no natural regen, harder healing, diet variety
   requirement, temperature, "you were slain" death screen, corpse-runs (Corpse mod).
6. **Atmospheric immersion** — seasons, dynamic weather/surroundings, aurora & realistic
   night skies, diegetic music (instruments play MIDI), custom main menu, clean tooltips.
7. **Exploration & structures** — dense structure mods + custom dimensions
   (Hall of Decimus) reached via a Teleport Gem.

---

## 2. Mod stack mapped to pillars (reference — what delivers each feel)

These are 1.20.1 Forge mods; many have NeoForge 1.26 equivalents or are worth
reimplementing natively in Ironhold. Grouped by what they *do for the experience*.

**Combat feel (the most important cluster):**
- **Better Combat** (Daedelus) — directional combo melee, weapon-specific movesets. The
  backbone of the "weighty swings" feel.
- **Combat Roll** (Daedelus) — dodge roll on a keybind with i-frames + cooldown.
- **Sword Blocking Mechanics** (Fuzs) — right-click block; the pack builds **parry** on
  top of its `SwordBlockingHandler.onLivingAttack` → `INTERRUPT` result.
- **Spartan Weaponry** — the weapon *archetype* system (longsword, katana, rapier,
  saber, greatsword, battleaxe, warhammer, flanged mace, battle hammer). Fantasia wraps
  these into its custom **stance** items.
- **Shield Expansion / Archery Expansion / Combat Nouveau / Better Mob Combat / Unified
  Combat / Horse Combat Controls** — round out blocking, ranged, mounted, and smarter
  enemy AI.
- **Epic Knights** (+ Slavic Armory, Open Helms, addon) — realistic plate/mail armor;
  the entire armor aesthetic and the "light vs heavy" weight system is built on it.
- **Apothic Attributes / AttributeFix / Attribute Tooltip Fix** — expanded RPG stats.

**Bosses & dangerous mobs:**
- **L_Ender's Cataclysm** (Ignis, Maledictus, Scylla, draugr, koboleton, black steel) —
  the headline bosses whose Souls grant powers.
- **Mowzie's Mobs** (Ferrous Wroughtnaut — gets anti-cheese), **Mine Cells** (Dead Cells
  dungeon + enemies), **Wither: Reincarnated**, **Savage & Ravage**, **Cataclysm**.
- **Iron's Spells 'n Spellbooks** — the magic system (mana, spell books, staffs); mages
  get it in their kit, and boss souls grant `irons_spellbooks:max_mana` etc.

**Progression / blacksmithing:**
- **Overgeared** (StirDrem) — the blacksmith mod: tongs, blueprints, heating, tempering,
  blade/head parts. Fantasia's 1000-line reforging script is built on it.
- **Mining Master** (Infernal Studios) — gems with enchant-on-gem + forging recipes;
  Fantasia layers custom gems on top.
- **Sewing Kit / Tannery** — leather/cloth crafting tier.

**Quests / NPCs / RPG framing:**
- **Questlog** (Infernal Studios) — the quest/chapter UI (chapters: tutorial, main,
  sailing, blacksmithing). Quests are JSON: requirements → objectives → rewards.
- **Custom NPCs** (Noppes) + **Blabber** (dialogue trees) + **MCA** (villager lives) +
  **Guard Villagers** + **Goblin Traders** + **Numismatic Overhaul** (coin currency).
- **Starter Kit** (Rick South) — class selection at first spawn.

**Survival friction:**
- **Harder Natural Healing / Medieval Healing / Saturation Plus / Diet / Natural
  Temperature / You Were Slain / Corpse** — no free regen; heal via tonics/food; eat a
  varied diet; manage temperature; recover your corpse on death.

**Atmosphere / juice:**
- **Dynamic Surroundings, Ecliptic Seasons, Auroras, Astrocraft (night skies), Cool
  Rain, Delayed Thunder, Particular, Subtle Effects** — ambient world feel.
- **Immersive Melodies** (instruments play `.mid` files — incl. *Gwyn*, *Harvest Dawn*,
  *Concerning Hobbits*) + **Medieval Music** — diegetic + ambient score.
- **FancyMenu** — fully custom main menu / loading screen / UI themes.
- **Shoulder Surfing Reloaded** — third-person RPG camera (FOV in options is 0.5,
  gamma 0.15 — a deliberately cinematic, slightly dark look).

**Performance baseline (so the heavy pack runs):** Embeddium, Oculus, ModernFix,
FerriteCore, Krypton, ImmediatelyFast, BadOptimizations, ServerCore, AllTheLeaks.

---

## 3. The custom KubeJS layer — transferable mechanic patterns

This is the real gold: ~150 scripts under `overrides/kubejs/{startup,server,client}_scripts/`,
namespaced `Fantasia - <category>`. Each is a self-contained mechanic. The patterns below
translate directly to Ironhold Java (events, persistent data, attribute modifiers).

### 3.1 Stance weapons (two-handed ↔ one-handed swap)
- Every weapon archetype is registered in **two forms** (`*_one_handed`, `*_two_handed`)
  via Spartan Weaponry's `SwordBaseItem`, tagged `fantasia:stance_one_handed/two_handed`.
- **Crouch + use** triggers `startUsingItem`; the `LivingEntityUseItem.Start` event is
  cancelled and the item is swapped in-hand to its other-stance variant (NBT copied,
  10-tick cooldown applied to both ids). Namespace routing by tag
  (`cataclysm_weapons` / `minecraft_weapons` / `overgeared_weapons`).
- **Ironhold takeaway:** a single weapon can carry two movesets/stat profiles toggled by
  a stance key. In Java this is cleaner: one item + a `stance` data component, swap the
  AttributeModifier set + animation on toggle (no item-swap hack needed). Ironhold already
  has a `BattleHammerCombatHandler` / stance-ish combat — this validates the direction.

### 3.2 Parry (built on blocking, not a new system)
- `LivingAttackEvent` → call `SwordBlockingHandler.onLivingAttack`; if it returns
  `INTERRUPT` (a successful timed block), fire **parry feedback**: particles, an animation
  packet to the client, and special cases (reflect ghast fireballs back).
- **Takeaway:** parry = "block within a tight window" layered on an existing block
  mechanic + juicy feedback (particle ring + sound + brief stagger). Cheap, high-impact.
  Note: **user has rejected camera shake** — keep feedback to particles/sound/flash.

### 3.3 Boss Souls (kill → eat → permanent buff, lost on death)
- Each Cataclysm boss drops a **Soul** food item (`kubejs:soul_of_ignis/maledictus/scylla`).
- `ItemEvents.foodEaten` sets a `persistentData` flag (`IgnisPower=true`), grants +20 max
  health (named attribute modifier), plays a totem-style activation (sound + particles +
  `displayItemActivation`).
- A `PlayerEvents.tick` loop (every 20 ticks) **re-applies** a buff suite while the flag is
  set: Ignis → fire res + resistance; Maledictus → speed/jump + huge mana/mana-regen;
  Scylla → aquatic buffs + charged. `EntityEvents.death` clears all flags (you lose the
  powers when you die — Souls-like risk).
- **Takeaway for Ironhold:** this is a fantastic, low-cost "boss reward = build identity"
  system. Each major boss grants a *playstyle*, not just loot. Permanent-until-death keeps
  stakes high. Implement as a player capability/attachment + a server tick that refreshes
  short-duration effects, cleared on death.

### 3.4 "Ermira's Favor" — a soulbound second-chance revive (signature item)
- A used-item with **N charges / revives**, configured centrally (`itemUseCharges:3`,
  `maxDistance:100`, a heal amount, and an effect suite on revive).
- On player death: if the player owns the favor and dies **within range of their bound
  spot in the same dimension**, cancel death, heal, apply effects (water breathing, fire
  res, absorption, slow-fall), teleport to the bound spot with a two-stage particle/sound
  flourish (blastwave at death spot → totem burst at revive spot), decrement revives.
  Out-of-range or out-of-dimension → fail with a translated red message; exhausted →
  consume. Stored in `server.persistentData.ErmirasFavor[uuid]`.
- **Takeaway:** a "bind a sanctuary, get rescued there" item is a memorable RPG artifact
  and a soft difficulty knob. Great Ironhold candidate (pairs with the lockable-doors /
  home-base theme already in the repo).

### 3.5 Corpse Compass (find your death loot)
- On `corpse:corpse` spawn, record `{uuid,name,x,y,z,dim}` into two server lists
  (active-with-items vs empty), spawn an invisible `corpse_core` marker entity carrying
  the uuid + empty flag. A compass item points the player back to their corpse.
- **Takeaway:** corpse-runs make death meaningful without full hardcore; a compass that
  guides you back is the QoL that makes it fair. Ironhold has keep-inventory tweaks
  already (`fantasia-additional-keepinv` equivalent) — this is the opposite design lever.

### 3.6 Custom gems (combine → enchanted accessory)
- Built on Mining Master: `forging_recipe` (gem + catalyst → new gem) then
  `gem_smithing_recipe` (gem → carries specific enchantments, e.g. `leeching`, `reeling`,
  `launch`, `acrobat`, `reforming`, `death_wish`). A gem-fusion tree with named results
  (Alluring Sunstone, Nimble Spinel, Vengeful Onyx…).
- Life gems: eating `heart_rhodonite` grants strong Regeneration (the *only* sanctioned
  burst-heal, since natural regen is off).
- **Takeaway:** gems = socketable/consumable stat carriers with evocative names and a
  small fusion crafting tree. Names matter for roleplay.

### 3.7 Survival friction scripts (small, sharp rules)
- **No regen:** `EntityEvents.hurt` strips `minecraft:regeneration` from players on every
  hit (so passive regen never trivializes combat; heals come from items only).
- **Anti-cheese:** when within 80 blocks of a designated boss (Ferrous Wroughtnaut), apply
  `moderninhibited:inhibited` (blocks teleport/cheese tactics); clear when you leave.
- **Diet:** giant item-tag sheets sorting every food into vegetables/grains/fruits/… so the
  Diet mod can require a balanced diet for buffs.
- **Lumber mechanic:** logs → planks only via stonecutter or **axe (consumes durability)**,
  not free crafting; sticks compress into stick-blocks. Makes early wood deliberate.
- **Spartan combatry:** mass `ItemEvents.modification` setting attack speeds (swords -2.5,
  hoes/knives -2.8, scythe -3.0) — the **slow, weighty swing** baseline.
- **Armor weights:** `ItemAttributeModifierEvent` reclassifies every armor piece into
  light/heavy lists to attach movement/stat tradeoffs — a real armor-class system.

### 3.8 NPC dialogue (UUID-pinned Blabber trees)
- `ItemEvents.entityInteracted` checks the target's hard-coded UUID and runs
  `blabber dialogue start fantasia:<name>_dialogue` as the nearest player. Named NPCs
  (Aaren, Yuhiri, Neifon) are placed in the world and wired by UUID.
- **Takeaway:** Ironhold already has voiced AI NPCs (Kangarude, Warden Halric) which is a
  *more advanced* version of this. Fantasia's lesson is breadth: many small fixed-dialogue
  NPCs giving the world population + quest hooks, not just a few deep AI ones.

### 3.9 Central config objects (`global.specialItemsConfig`, `global.FantasiaStanceWeapons`)
- Every tunable mechanic reads from one literal config object in a startup script, so
  balance lives in one place. Good discipline to mirror in Ironhold (a `BalanceConfig`).

---

## 4. Progression & onboarding design

- **Class kits at spawn** (Starter Kit): 14 classes — Adventurer, Alchemist, Chef, Farmer,
  Fisherman, Highlander, Hunter, Knight, Mage, Mayor, Thief, Toolsmith, Vagabond, Vile
  Prisoner. Each `.txt` is a full inventory NBT dump (armor with banner/dye decoration,
  a signature weapon, themed consumables, a compass pre-set to a location) + a one-line
  flavor description. *Knight* = decorated XIV-century plate + lion banner + iron spear +
  kite shield + cross necklace + bread + herbal tonics. *Mage* = magician helm + leather
  tunic w/ mana upgrade + graybeard staff + firebolt spellbook + golden carrots.
- **Quest spine** (Questlog) split into chapters:
  - *Tutorial* — cheat-death, crystal, death, dimension-tears, enchant, fodder, merchants,
    sluice, tanning, village (teaches each mechanic).
  - *Beginner 1-3 → Main Quest 1-3* — the story (ends at "Hall of Decimus").
  - *NFTS* (sailing) — compass, healing, mapping, mounts, rowboat, sloop.
  - *TKB* (blacksmithing) — alloy, blueprints, grindstone, ironworks, kiln, reforging,
    steelworks — each quest is a *tutorial page* gated behind an advancement
    (`age_of_advancement/phase_2/steel_age`).
- **Advancement-gated tiers:** "Age of Advancement" phases (copper→iron→steel→…) act as
  the real tech tree; quests unlock as you hit each age.
- **Takeaway for Ironhold:** pair every new system with (a) a starter-context that
  introduces it and (b) a short read-quest that explains it, gated behind the advancement
  that proves you reached that tier. Onboarding is treated as content, not an afterthought.

---

## 5. Concrete, prioritized takeaways for Ironhold

Ranked by impact-to-effort for a NeoForge 1.26 Java mod:

1. **Boss Souls system** (§3.3) — highest payoff. Each Ironhold boss (e.g. the dragon
   work, Mowzies-derived bosses) drops a consumable Soul granting a permanent-until-death
   buff suite that defines a playstyle. Player attachment + server tick refresh + clear on
   death. Cheap, deeply "souls-like," reuses existing combat-juice particle infra.
2. **Weighty combat baseline** (§3.7) — slow attack speeds + no passive regen + heal-only-
   from-items. This single trio is what makes the whole pack feel like an RPG instead of
   vanilla. Tune via attribute modifiers; expose in a central `BalanceConfig`.
3. **Parry on top of blocking** (§3.2) — timed-block window → particle ring + sound +
   brief enemy stagger (NO camera shake per user preference). Reuse combat-juice VFX.
4. **Class-based start** (§4) — a spawn screen offering 4-6 Ironhold classes with themed
   gear + a line of backstory. Immediate identity + roleplay hook.
5. **Signature artifacts** (§3.4-3.6) — port "Ermira's Favor" (bind-a-sanctuary revive,
   pairs with lockable home bases) and a Corpse Compass; add a named gem-fusion tree.
6. **Blacksmithing progression** (§3.1, §4) — if Ironhold wants depth, the reforge chain
   (part + heat + temper + assemble, advancement-gated ages) is the proven spine. Big
   effort; do after combat feel is locked.
7. **Quest-as-tutorial + advancement-gated tiers** (§4) — wrap each shipped system in a
   read-quest unlocked by the advancement proving you reached it.
8. **Atmosphere** — seasons, diegetic instrument music (Ironhold's reactive-music engine
   already exceeds Immersive Melodies' concept), cinematic FOV/gamma defaults, custom main
   menu. Ironhold's `kingdom.smp.music` and dynamic-lighting (`Ironglow`) already align.

**Where Ironhold is already ahead of Fantasia:** voiced *AI* NPCs (vs fixed Blabber
trees), a reactive music *engine* (vs MIDI playback), native dynamic lights, immersive
portals, and being real Java (vs KubeJS) — so the mechanics above can be cleaner and more
performant than the modpack's script versions.

---

## 6. Where the raw material lives (if re-reading is needed)
Extracted at `/Users/joshua/Downloads/fantasia_extract/overrides/` (regenerate from the
zip in Downloads). Highest-signal files:
- Combat: `kubejs/server_scripts/Fantasia - Core/fantasia-parry.js`,
  `.../stance_weapons_registry/register_functions.js`,
  `kubejs/startup_scripts/Fantasia - Core/useitemstart/handle_weapon_stance_swap.js`,
  `kubejs/startup_scripts/Fantasia - Game Balance/fantasia-spartan-combatry.js`.
- Souls/items: `kubejs/server_scripts/Fantasia - Unique Features/fantasia-boss-souls*.js`,
  `.../Fantasia - Unique Items/fantasia-efavor.js`, `fantasia-corpse-compass.js`,
  `fantasia-custom-gems.js`; central config `.../Fantasia - Items/items_config.js`.
- Friction: `.../Gameplay Changes/fantasia-noregen.js`, `fantasia-anti-cheese.js`,
  `fantasia-diet.js`, `fantasia-lumber-mechanic.js`.
- Progression: `config/starterkit/kits/*.txt`, `config/questlog/{chapters,quests}/*.json`.
- World/NPC: `.../Core/fantasia-npc-dialogue.js`, `.../Entities Goals/fantasia-factions.js`.
