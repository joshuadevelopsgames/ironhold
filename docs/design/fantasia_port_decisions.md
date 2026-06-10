# Fantasia Port — Feature Decisions (living doc)

> Working record of the "what should we port from Fantasia" walkthrough.
> Source diff: `docs/research/fantasia_modpack_study.md` (Beta 11.9h — confirmed current).
> Each feature below is reframed to fit Ironhold's **existing** infra rather than copied.
> Status legend: ✅ locked · ✳️ partially decided · 🅿️ parked / needs thought · ❌ skipped.
>
> **Implementation specs:** [`../../specs/fantasia-ports/`](../../specs/fantasia-ports/00-index.md)
> — one spec file per feature, grounded in real Ironhold classes.

---

## ✅ ① Boss-dropped signature accessories  (replaces Fantasia "Boss Souls")
Decision: each boss drops **one guaranteed signature accessory**, not a consumable soul.
- Slots into existing `AccessoryInventory` / `AccessorySlot` (5 slots) / `AccessoryTickHandler`.
- **Passive + optional active** on flagship artifacts (active via `AbilityCooldowns`).
- **Kept on death** (accessory inventory is already `copyOnDeath`).
- Build identity comes from the **5-slot cap** — you can't wear every boss artifact at once.
- First two:
  - **King Enderman → *Ender Regalia*** — endermen ignore you + sneak-tap blink (reuse
    `CloudJump`/`TeleportBurstEffect`), immune to void-rift pull.
  - **Stone Golem → *Stoneblood Amulet*** — flat damage reduction + knockback & slowness immunity.
- Each future boss = a new `AccessoryItem` subclass + loot hook + art. No system work.

## ✅ ② Parry  (new — no player block exists today)
- **Tap-parry key** = brief ~6-tick active stance. No sustained blocking, no stamina.
- **Weapon-class-dependent** timing (swords lenient/low-stagger; hammers tight/high-stagger).
- Success reward: **negate hit + stagger attacker**, **reflect projectiles** (reuse our
  projectile entities), **refund ability cooldown**. *No riposte. No camera shake.*
- Reuses `BattleHammerCombatHandler` pattern + `AbilityEffects` VFX + `LivingIncomingDamageEvent`.

## ✅ ③ Dodge  (new — sibling to parry, same input philosophy)
- **Short omnidirectional hop** (not a full roll → less animation work).
- **Perfect-dodge**: i-frames only if dodging just before a hit lands; late dodge just
  repositions. Mirrors the timed parry; perfect dodge may grant a small bonus.
- **Flat ~1.5s cooldown.** No charges, no stamina.
- Real cost = client animation (camera tilt + 3rd-person model pitch).

## ✅ ④ Additive healing  (redirected — regen UNCHANGED)
- **Do NOT touch vanilla passive regen.** Ironhold stays accessible, not hardcore-survival.
  → Fantasia's "weighty no-regen survival" pillar is explicitly **out**.
- Add instead:
  - **Healer-class active abilities** — Cleric/Medic/Saint/Bishop/Redeemer get
    heal/cleanse/regen via `AbilityRegistry` (gives those classes a real party role).
  - **Rest / campfire healing** — safe-haven restore (sit by campfire / sleep).

## ✅ ⑤ Ender Shrine  (the bind-a-sanctuary revive, reworked)
- **Ender Totem** (new item) = **Totem of Undying + Ender Pearl** (ender pearl = teleport-home flavor).
- **Ender Shrine** (new placed block) = bound sanctuary; right-click with Ender Totems to **stock charges**.
- **Death resolution order:**
  1. Holding a normal **Totem of Undying** → vanilla totem fires in place (shrine untouched).
  2. No totem + shrine has a charge → **cancel death, consume 1 charge, teleport to shrine,
     heal, brief fire-res + slow-fall + absorption + flourish** (`TeleportBurstEffect`).
  3. No totem + no charge → normal death.
- **Reach: same dimension, any distance.** (Cross-dimension = later upgrade tier.)
- Defaults: right-click-to-own (one shrine/player, `LOCK_OWNER` pattern); multi-charge cap.

## ✅ ⑥/⑬ Death-loot model — Vanilla drops + **Soulbound enchantment**
(Replaces both "Corpse Compass" and Fantasia's "keep gear, gear takes damage".)
- **Default:** vanilla — full inventory drops where you die.
- **Soulbound** (new custom enchant, gear/weapons/tools): item is **not dropped**; it
  **returns with you on any revive** (respawn, Ender Shrine, or totem — consistent).
- **Cost:** a **small fixed durability hit** per soulbound save, **clamped so it can never
  fully break** the item (always left usable).
- ❌ **Corpse Compass skipped** — Soulbound protects what matters; the rest is acceptable loss.

### ✅ RESOLVED: how is Soulbound obtained?  — **BOTH paths**
- **Rare books trickle from loot** (dungeon/boss), so Soulbound is *findable* early before you've
  built a shrine.
- **Applying / upgrading it wants progression:** a **high Enchanting rank** (from ⑧) to apply,
  and it's **"blessed" onto gear at the Ender Shrine** (cost: Ender Totem / boss material).
- Net: discoverable early, mastered through the progression tree + shrine. No enchant-table dilution.

---

## ✅ ⑦ Coin economy — add a purse  (currency already exists)
Already built: `GOLD_COIN` item + `GoldCoinTradeHandler` (re-skins villager economy emerald→coin).
Decision — add only the missing convenience layer:
- **Right-click purse** item — bank loose coins into it (stored as a number), withdraw anytime.
- **Single gold coin** (no copper/silver/gold tiers).
- **Keep vanilla trade leveling** (restock + price variation untouched — no Fantasia trade rework).

## ✅ ⑧ Rank-gated crafting tech-tree  (extend existing `MiningGating`)
Already built & better than Fantasia: `MiningGating` gates ore-mining behind **Mining
profession ranks** (Novice→iron … Expert→netherite … Master→Veinbreaker). Decision — extend
the same pattern to production, so **profession ranks ARE the full tech tree**:
- Gate crafting tiers by rank for **all four** production professions:
  **Blacksmithing** (gear tiers), **Enchanting** (enchant tiers), **Alchemy** (potion/tonic
  tiers), **Cooking** (food tiers).
- **Quest-as-tutorial:** each rank-up fires a short explanatory quest via `QuestService`
  (Fantasia's best onboarding idea — teaching as content).
- 🔗 **Resolves the parked Soulbound-source question (⑥):** Soulbound can be gated behind a
  **high Enchanting rank** (+ optionally blessed at the Ender Shrine). Folds death-safety into
  the progression tree. *Promote this to the lead Soulbound option.*

## ✅ ⑨ Gear affix system + reroll  (NEW — the big one; replaces Fantasia "Reforged")
Note: existing `ItemQuality` (Poor/Fine/Good/Mint) is a durability/repair multiplier, NOT affixes.
Decision — add a new affix layer on top:
- **Random stat affixes + special abilities** on gear (offensive, defensive, utility/movement,
  **and** special on-hit abilities — leeching, bleed (reuse `BleedingEffect`), launch, chain-lightning).
- **Affix count = ItemQuality tier:** Poor 0 / Fine 1 / Good 2 / **Mint 3**. Unifies the two gear
  systems — Mint = "best metal AND most magic slots" = the prestige goal.
- **Reroll by talking to the blacksmith NPC** (Tobias) — **targeted: lock the affixes you like,
  reroll the rest** for escalating coin/material cost. Higher **Blacksmithing rank → more locks**.
  Ties reforge into NPC + coin + rank layers (no crafting block).

### ✅ Affix pool (locked) — 21 affixes
- **Roll model:** **rolled ranges** (each affix rolls a value in its range → loot chase + reroll-for-high-roll).
- **Gear mapping:** **by category** (offensive + on-hit → weapons; defensive → armor; utility → tools/armor).
  No nonsensical rolls.
- **On-hit set:** **all five** kept (Voltaic needs one small new chain-shock effect; rest reuse existing).

| Category | Affix | Effect (rolled range) | Gear |
|---|---|---|---|
| ⚔️ Offensive | Keen | +5–15% attack damage | weapon |
| | Swift | +5–12% attack speed | weapon |
| | Piercing | ignore 10–25% target armor | weapon |
| | Brutal | +15–30% bonus dmg on fully-charged hits | weapon |
| | Savage | +bonus dmg vs mob family (undead/arthropod/illager) | weapon |
| 🛡️ Defensive | Stalwart | +1–3 armor | armor |
| | Vital | +1–3 max health | armor |
| | Bulwark | +10–30% knockback resist | armor |
| | Warded | 3–8% flat damage reduction | armor |
| | Thorns | reflect 10–25% melee dmg | armor |
| 🧭 Utility | Fleet | +4–10% movement speed | boots |
| | Prospector | +8–20% mining speed | tool |
| | Lucky | +1–2 luck / loot quality | tool |
| | Scholar | +5–15% XP gain | any |
| | Reaching | +0.5–1.0 block reach | any |
| | Enduring | −10–25% durability loss | any |
| ✨ On-hit | Leeching | heal 5–12% of dmg dealt | weapon |
| | Serrated | 20–40% chance → `BleedingEffect` | weapon |
| | Concussive | 15–25% chance → launch/knockback | weapon |
| | Voltaic | chance → chain shock to nearby foes *(new effect)* | weapon |
| | Soulrending | builds a `KillBurstEffect` charge on kill | weapon |

*Per-affix final values/weights = balance pass during implementation.*

## ❌ ⑩ Gems — SKIPPED
The new affix system already owns "magic stats on gear." A second socket system = over-engineering.
(If ever revisited: the non-redundant role would be *deterministic socketed affixes* = intent vs
reforge's luck, sourced from Mining, + consumable life gems. Explicitly deferred/dropped for now.)

## ✅ ⑪ Diet variety  (reward-only)
- **Carrot, never stick:** eating a varied/balanced diet grants temporary buffs (hearts, regen,
  etc.); poor diet just = no bonus, never a penalty. Fits the accessibility lean.
- **Tied to Cooking:** home-cooked meals give stronger/longer diet buffs than raw ingredients,
  and higher **Cooking rank** unlocks better bonuses → makes Cooking a real build choice.

## ❌ ⑫ Temperature survival — SKIPPED
Most friction-heavy survival mechanic; clashes with the accessibility line. Seasons stay
atmospheric/visual only. (If revisited: a soft seasons-flavored "Chilled/Warm" cozy-buff
version, never deadly — but dropped for now.)

## ✅ ⑭ Class promotion kits  (fits earned-progression, NOT spawn-pick)
Ironhold = start `PEASANT`, promote up the 27-class tree at class stones (earned identity),
NOT Fantasia's chosen-at-spawn. So kits port as:
- **Themed starter gear + weapon + one-line backstory granted on promotion**, at
  **major branch points only** (entering Knight/Mage/Archer/Medic lines + advanced/Divine
  tiers) — not every step. Reuses class stones; gives each class immediate identity.

## ✅/❌ ⑮ Presentation — menu DONE, camera SKIPPED
- Custom main menu **already exists** (`MainMenuScreen` + full custom UI suite) — Fantasia's
  FancyMenu need is already met. Nothing to build.
- ❌ **Third-person shoulder camera: skipped** — Ironhold stays first-person; combat (parry/dodge)
  is designed first-person.
- ❌ Cinematic FOV/gamma defaults: **leave vanilla** (no forced look).

## ✅ ⑯ Diegetic instruments — bards only
Reactive music engine already > Fantasia's ambient score. Add only the diegetic layer:
- **Bard NPCs play instruments in-world** (`BramBardEntity` + tavern NPCs) for tavern/village
  ambiance. **No player instrument items** (no note UI / playback item complexity).

## ✅ ⑰ Atmosphere polish — ALL, but DEFERRED
Want all four, built in a later polish pass (after core combat/RPG/death systems):
- **Auroras** (cold/night skies, pairs w/ seasons) · **Enhanced night sky** (stars/nebulae/moon,
  fits Moon dim) · **Ambient particles & sounds** (biome motes, fireflies, falling leaves,
  nature audio — "Dynamic Surroundings" feel) · **Weather polish** (cool rain, delayed thunder).
- Priority: **deferred** — garnish, not the meal.

## ✅ ⑱ Dungeons + boss-gated End + story dimension  (ambitious content)
You have the bones: Moon dim + immersive portals, custom bosses, RTF structures, a
`KingEnderEyeEntity`. All three approved:
- **Boss-gated End:** End access requires defeating bosses for their eyes (fewer than 12,
  Fantasia-style ~4). Builds on the existing King Ender Eye. Bosses become portal keys.
- **Boss-arena dungeons:** each boss gets a themed set-piece arena (via RTF structures) you
  discover & clear — the home for the #1 boss accessories. Bosses = destinations, not random spawns.
- **New story dimension:** a Hall-of-Decimus analog beyond the Moon, reached via portal/gem,
  housing a climax boss/structure. Reuses immersive-portal + dimension tech. Endgame destination.

## ✅ ⑲ Villager relationship lifecycle  (extends `PlayerNpcBonds`)
Already built: `PlayerNpcBonds` (Stardew rapport + gifts = befriending) + `NpcFollowOwnerGoal`
+ `NpcStation*` goals + voiced AI NPCs. Add the lifecycle stages on top — ALL three:
- **Hire (employment):** pay NPCs in **coins** to staff base stations / run production
  (reuses `NpcStation` goals + coin economy → village labor economy).
- **Recruit (combat companion):** befriended NPC follows as a fighting partner
  (reuses `NpcFollowOwnerGoal` + knight/guard AI).
- **Marry (spouse) — mechanical benefits:** shared base access (ties to the **lock system**),
  a home buff, helps with chores/defense, gifts coins/items. Pairs with voiced AI NPCs for
  characterful romance. NOT flavor-only.

---

## Summary — what we're building vs skipping

**BUILD (locked):** ① boss accessories · ② parry · ③ dodge · ④ additive healing (healer
abilities + campfire) · ⑤ Ender Shrine revive · ⑥ Soulbound enchant · ⑦ coin purse ·
⑧ rank-gated crafting tree + tutorial quests · ⑨ gear affix system + blacksmith reroll ·
⑪ diet (reward) · ⑭ class promotion kits · ⑯ bard instruments · ⑰ atmosphere (deferred) ·
⑱ boss-gated End + arenas + story dimension · ⑲ villager hire/recruit/marry.

**SKIP:** no-regen survival (kept regen; replaced w/ additive healing) · corpse compass
(Soulbound covers it) · gems (affixes cover it) · temperature · shoulder camera + cinematic
defaults (menu already exists) · trade rework (kept vanilla leveling) · coin denominations.

**ALREADY HAD (no work / minor):** coin currency (`GoldCoinTradeHandler`) · custom main menu
(`MainMenuScreen`) · mining tier-gating (`MiningGating`) · reactive music engine · befriending
(`PlayerNpcBonds`) · NPC follow/station AI.

---

## Recommended build order

- **Phase 1 — Combat feel** (fast, high-impact, independent): ② parry → ③ dodge.
  Establishes the action layer; they share input + `AbilityCooldowns`.
- **Phase 2 — Death & safety net** (self-contained): ⑥ Soulbound enchant · ⑤ Ender Shrine +
  Ender Totem · ⑦ coin purse. Small, satisfying, independent wins.
- **Phase 3 — Boss rewards:** ① boss accessories (Ender Regalia + Stoneblood Amulet for the two
  live bosses). Quick once parry/effect infra is in.
- **Phase 4 — Progression depth** (the big gear loop): ⑧ rank-gated crafting + tutorial quests,
  THEN ⑨ affix system + blacksmith reroll (affixes are gated by Blacksmithing rank, so ⑧ first).
- **Phase 5 — RPG flavor:** ④ healer abilities + campfire · ⑭ class promotion kits · ⑪ diet.
- **Phase 6 — Big content** (longest): ⑲ villager lifecycle → ⑱ boss-gated End + arenas + story dim.
- **Phase 7 — Polish:** ⑯ bard instruments · ⑰ atmosphere.

### ✅ Open questions — RESOLVED
- **Soulbound acquisition:** BOTH — rare books trickle from loot (findable early) + Enchanting
  rank & Ender Shrine blessing to apply/upgrade. (See ⑥.)
- **Affix pool:** 21-affix table locked (see ⑨) — rolled ranges, by-category, all 5 on-hit.
- **End-gate bosses:** **King Enderman + Stone Golem drop the eyes now**; gate expands as more
  bosses ship. Arenas for the two existing bosses first.

### 🅿️ Remaining (implementation-time, not blocking)
- Per-affix final values + roll weights (⑨) — balance pass while coding.
- Voltaic chain-shock = one new effect to author.
- Future boss roster for the End gate / arenas: candidates from the existing mob set —
  Void Invoker / Null Stalker (void), Siren (water), Plague Doctor / Cemetery Watcher (death),
  a Skeleton King (`SkeletonKingModel` already exists). Promote as developed.
