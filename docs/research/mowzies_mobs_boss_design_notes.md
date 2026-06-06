# Mowzie's Mobs — boss design notes (reverse-engineered reference)

> Source: `mowziesmobs-1.21.1-1.8.2.jar` (NeoForge, GeckoLib, **custom license — reference only, do not ship their assets/code verbatim**).
> Decompiled with Vineflower for study. These are *patterns to re-implement in our own code*, not copy-paste.

Mowzie's is the gold standard for "boss feel" in modded MC. The takeaways below are what actually makes their bosses read as deliberate, weighty, and readable.

---

## 1. The core architecture: Ability + AbilitySection state machine

Every attack (mob *and* player) is an **`Ability`** = a state machine over a `sectionTrack[]` of **`AbilitySection`s**. This is the single most important thing to steal.

- `AbilitySection.AbilitySectionType`: **`STARTUP` → `ACTIVE` → `RECOVERY`** (+ `MISC`). Exactly windup → hit-frames → cooldown, but generalized.
- Section kinds: `AbilitySectionDuration(type, ticks)`, `AbilitySectionInstant`, `AbilitySectionInfinite` (waits for a condition).
- `Ability.tick()` counts `ticksInSection`; when it exceeds the section's `duration` it calls `nextSection()`. `jumpToSection(i)` allows branching (combos, feints, phase changes).
- Hooks per ability: `start()`, `tickUsing()` / `tickNotUsing()`, `beginSection()/endSection()`, `playAnimation(RawAnimation)`, **`codeAnimations(model, partialTick)`** (procedural bone driving on top of the clip), `onTakeDamage(event)` (react to hits mid-attack — armor/parry windows), cooldown (`getMaxCooldown`), `canUse()` (gated by effects like FROZEN).

**Why it's better than our current per-goal `switch(phase)`**: damage, particles, sounds, knockback, and animation are all keyed to *named sections and exact ticks* in one place, and the same `Ability` works for any entity (even the player). Combos/feints are just `jumpToSection`. Reaction windows (parry) are `onTakeDamage` checking the current section.

**Adopt for Ironhold:** build a small `Ability`/`AbilitySection` framework (or fold the concept into our existing attack goals). Our `StoneGolemAttackGoal` already does STARTUP/ACTIVE/RECOVER by hand — this is the reusable version. Each boss = a registry of abilities; AI picks one; the ability owns its timeline.

## 2. The two attack-timing styles they use

1. **Ability-section** (newer: Wroughtnaut, Sculptor, Umvuthi) — see above.
2. **Animation-tick switch** (older: Frostmaw) — `if (getAnimation()==ICE_BREATH && getAnimationTick()==13) spawnBreath();`. Damage/effects fire on specific frames of the playing animation. This is exactly our golem's strike-frame approach.

Frostmaw concrete frame timings (great calibration reference for "heavy"):
- `SLAM`: impact at **tick 82** — a *huge* windup (~4s). Giants telegraph enormously.
- `ICE_BREATH`: starts at tick 13, sustained breath cone.
- `ROAR`: effect at tick 10, sustained tick 8–65 (crowd-control + buff window).
- `ICE_BALL`: charge `< tick 12`, release after.
- `DODGE`: tick 2 (iframe/leap), tick 6 (land).
- **Parry window**: `SWIPE` checks `player.isBlocking()` at tick 21 → if blocking, the boss is staggered. Reading the *player's* state mid-attack is the counterplay hook.

**Takeaway:** big bosses get 40–90 tick windups; bury the punish/parry check on a specific frame; sustain crowd-control attacks over a long active window.

## 3. Procedural animation > keyframes alone

Mowzie's bosses are renowned for smoothness because they layer **procedural code-driven motion** over (or instead of) keyframes:

- **`codeAnimations(MowzieGeoModel, partialTick)`** — every ability can rotate/translate bones in code each frame (look-at, lean-into-attack, bre-athing, recoil), reading `getCurrentSectionIndex()` + `ticksInSection` for phase-accurate motion. This is how the hammer "follows through" feels analog instead of stepped.
- **`DynamicChain`** — a spring/verlet physics chain. The Naga's tail uses it: the *static* tail bones (`tailOriginal`) are hidden and a *dynamic* copy (`tailDynamic`) is simulated so the tail whips, lags, and settles on its own. Same idea for capes, tentacles, wing-tips, chains.
- `ControlledAnimation` / `IntermittentAnimation` — eased 0→1 timers for blend-in/out of poses (e.g. "open mouth" weight) and randomized idle fidgets.
- `LegArticulator` / `RigUtils` / `SimplexNoise` — IK-ish leg planting and noise-driven jitter.

**Adopt:** we already added molang breathing + a charge-driven glow. The next level is (a) a `codeAnimations`-style per-attack procedural layer in the renderer keyed to the attack section/tick, and (b) a spring chain for anything dangly (the golem's hammer already wants this; a dragon tail/wings *need* it).

## 4. Particles — a data-driven, texture-billboard library

`assets/mowziesmobs/particles/*.json` are trivial (`{ "textures": ["mowziesmobs:ring"] }`) — the **behavior lives in `client/particle/types/` Java classes** spawned from code with parameters (lifetime, scale curve, color, velocity, gravity, trail). The *texture library* is the reusable asset idea:

| Texture | Use |
|---|---|
| `ring`, `ring_big`, `ring_0/1/2`, `sparks_ring` | expanding shockwave rings on impact/slam |
| `ribbon_flat/glow/squiggle/streaks` | motion-trail ribbons (swings, dashes, projectiles) |
| `orb`, `orb_0`, `glow`, `flare`, `flare_radial` | charge-up glows, energy cores, muzzle flashes |
| `sun`, `sun_nova`, `moon_*` | telegraph/finisher set-pieces (Umvuthi/Sculptor) |
| `sparkle`, `pixel`, `snowflake`, `crack`, `eye` | ambient sparkle, frost, ground cracks, eye-glints |

Techniques worth copying:
- **Expanding ring on every heavy impact** — a flat ground-aligned `ring` particle that scales up and fades. Instantly sells weight (we already spawn block-dust; add a ring).
- **Ribbon trails** on fast weapon swings and projectiles (billboarded quad strip following the path).
- **Charge orb + radial flare** that grows during STARTUP and flashes on ACTIVE — exactly our golem's eye-glow-swell idea, but as world particles too.
- **Custom particle JSON + a ParticleType** is cheap; the win is a small library of reusable spawn helpers (`spawnRing(pos, color, size)`, `spawnRibbon(...)`, `spawnFlare(...)`).

## 5. Telegraphed area effects via "effect entities"

Heavy hits aren't instant hitscans — they spawn **effect entities** that resolve on a delay, so the player can read & dodge the ground:
- `server/entity/effects/` + `effects/geomancy/` — earth spikes, geomancy pillars, rising-block columns, boulders. An attack spawns a *telegraph* (cracks/particles at target tiles), then the effect entity erupts a few ticks later dealing the damage.
- Earth-spike line: spawn a row of spike entities marching outward from the boss (each with its own startup), so the AoE has travel time and a dodge lane.

**Adopt:** our golem slam could spawn a short-lived "shockwave" effect entity that expands outward (ring particle + delayed damage at radius), instead of one instantaneous AoE — much more readable.

## 6. Stages / phases, boss bar, music

- **Boss bar**: `ServerBossEvent` with `BossBarColor`; custom bar textures in `textures/gui/boss_bar/`.
- **Custom boss music**: `BossMusicPlayer` (e.g. `SCULPTOR_MUSIC`) — fades combat music in on aggro, out on death. Big atmosphere multiplier.
- **Activation / phases**: bosses have an `ACTIVATE_ANIMATION` (dormant → awake set-piece — Wroughtnaut/Sculptor sit inert until approached), and gate move-sets behind state (`getActive()`, crystal-present variants, frozen state). Sculptor has multi-phase fights with different ability sets per phase.
- **Frozen/CC**: a shared `EffectHandler.FROZEN` effect disables ability use (`canUse()` returns false) — clean way to implement hard CC both ways.

**Adopt:** (1) a dormant-until-approached intro for set-piece bosses; (2) HP-threshold phase switches that swap the ability pool and play a transition animation + bar color change; (3) wire our `BossMusicPlayer` equivalent (we have a reactive-music engine already — hook boss aggro into it).

## 7. A build checklist for an Ironhold boss

1. **Rig** in GeckoLib with real joints (we do this). Add spring chains for dangly bits.
2. **Ability framework**: define attacks as `STARTUP/ACTIVE/RECOVERY` section tracks; AI selects by range/target-count/phase.
3. **Telegraph everything**: 40–90 tick windups for heavies; held apex pose; particle/sound markers on the windup; the punish window is a specific recovery tick.
4. **Readable AoE**: spawn delayed effect entities (ring telegraph → eruption) instead of instant hits.
5. **Juice**: impact ring particle + dust + low sound + brief held pose (no screen shake per project pref); charge orb/flare during windup that flashes on hit; ribbon trails on big swings.
6. **Procedural layer**: `codeAnimations`-style look-at/lean/recoil keyed to the current section; eased blend timers for poses.
7. **Phases**: dormant intro, HP-threshold ability-pool swaps with a transition beat, boss bar + music.
8. **Counterplay**: read player state mid-attack (blocking → stagger), poise/stagger punish windows (we have this on the golem).

---

### File map (in the decompiled jar, for future digging)
- `server/ability/Ability.java`, `AbilitySection.java`, `AbilityHandler.java` — the framework.
- `server/entity/frostmaw/EntityFrostmaw.java` — frame-tick attack style + ice effects.
- `server/entity/sculptor/EntitySculptor.java`, `umvuthi/` — Ability-style multi-phase + boss music.
- `server/entity/effects/`, `effects/geomancy/` — delayed AoE effect entities.
- `client/particle/types/`, `particles/*.json`, `textures/particle/*.png` — particle library.
- `client/model/tools/` — `DynamicChain`, `ControlledAnimation`, `LegArticulator`, `MMModelAnimator`, `RigUtils`, `SimplexNoise` (procedural-animation toolkit).
