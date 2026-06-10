# Combat Actions — Parry + Dodge  (features ② ③)

**Status:** spec. Covers both ② Parry and ③ Dodge — they share one input/cooldown "footwork"
layer, so they're specced together. Decisions: `fantasia_port_decisions.md` ②③.

## 1. Goal
Add the two missing player-defensive actions on top of vanilla combat. No stamina, no sustained
block, no camera shake. Both are tap-key actives gated by cooldown, surfaced on the ability HUD.

## 2. Shared footwork layer
- **Keybinds** (`client.IronholdKeys`, follow the `SEASHELL_DASH` pattern): add `PARRY` and `DODGE`
  `KeyMapping`s in the `ironhold` category. Suggested defaults: **Parry = `F`**, **Dodge = `Left-Alt`**
  (⚠️ confirm no clash with vanilla swap-hands `F`; fall back to `V`/`G`). Register in
  `IronholdClient#registerKeyMappings`.
- **Cooldowns:** reuse the `AbilityCooldowns` attachment (already *not* copyOnDeath → both actions
  reset on death, matching the ability layer). Add two reserved cooldown ids
  (`ironhold:parry`, `ironhold:dodge`) so they render on the existing cooldown HUD.
- **Client→server:** one new `FootworkPayload {Action action, float yaw, float pitch, boolean forward,
  boolean strafeL, boolean strafeR, boolean back}` (registered in `ModNetworking`). Client reads
  movement-input state at key-press and sends; server is authoritative.

## 3. Parry (②)
- **Window:** on `PARRY` press, set a server-side `parryUntilTick = now + W`. Weapon-class-dependent
  W (ticks): sword 6, dagger 7, axe 5, **BattleHammer/maces 4** (tight but big payoff), default 5.
  Pull the class from item tags (new `ironhold:parry/<class>` item tags) or from
  `BattleHammerItem`/weapon type. ⚠️ values are v1 targets, tune in playtest.
- **Hook:** subscribe `LivingIncomingDamageEvent` (⚠️ confirm exact 1.26 event name; the
  pre-mitigation damage event) on the player. If `now <= parryUntilTick` AND the damage source is
  parryable (melee, or a projectile for the reflect case):
  - **Negate:** `event.setCanceled(true)` (or amount 0).
  - **Stagger attacker:** if `source.getDirectEntity()` is a `LivingEntity`, apply a brief
    "Staggered" state — `MobEffects.MOVEMENT_SLOWDOWN` + a `0`-velocity hit + attack-cooldown reset.
    Reuse a new lightweight `StaggeredEffect` (mirror `BleedingEffect` structure) or an attribute
    freeze; weapon class scales stagger duration (hammer longest).
  - **Reflect projectiles:** if the source is one of our projectile entities (`ArcaneBoltEntity`,
    `TempestArrowEntity`, `HexBoltEntity`, vanilla arrows/fireballs), redirect: set the projectile
    owner = player, invert/aim velocity back at the attacker. (Pattern: vanilla shield-fireball deflect.)
  - **Refund ability cooldown:** call `AbilityCooldowns.reduceAll(player, R)` (new helper) — shave
    R ticks (v1: 40) off the player's active-ability cooldowns. Ties parry into the RPG layer.
  - **Feedback (NO camera shake):** spawn a particle ring + `ModSounds` parry clink via
    `AbilityEffects` (server `sendParticles`); flash the HUD. (Reuse combat-juice VFX entry points.)
- **Cost — asymmetric whiff/success cooldown (makes parry tactical, not spammable):** every press
  immediately starts a **long whiff lockout** (`PARRY_WHIFF_COOLDOWN`, v1: 24t ≈ 1.2s). A *successful*
  parry overwrites it with a **short recovery** (`PARRY_SUCCESS_COOLDOWN`, v1: 8t ≈ 0.4s). So a
  blind/early press eats the full lockout and exposes you to follow-up hits, while well-timed parries
  flow. Client also drains queued key-clicks to one send/tick so mashing can't flood the server.
  *(Implemented v1.167.x; replaced the original flat 12t cooldown that let you blanket-cover ranged spam.)*

## 4. Dodge (③)
- **Movement — short omnidirectional hop:** on `DODGE` press (off cooldown), apply a horizontal
  impulse in the input direction (neutral = backhop). Reuse the **`SeashellDashPayload` / seashell
  dash impulse math** as the precedent (`SeashellItem` already does a directional dash). Magnitude
  smaller than the seashell dash (a hop, not a leap); v1 ≈ 0.5 blocks/tick for ~4 ticks. Cancel fall
  damage during the hop.
- **Perfect-dodge i-frames:** the hop grants invulnerability **only if** an incoming hit lands within
  the first `P` ticks (v1: P=5) of the hop — implement by setting a `dodgeIFrameUntil = now + P` and,
  in the same `LivingIncomingDamageEvent` handler, cancelling damage if `now <= dodgeIFrameUntil`.
  A *late* dodge (hit lands after P) just repositions, no i-frames. Mirrors the timed parry.
- **Perfect-dodge bonus (optional, v1 cheap):** on a successful perfect dodge, grant ~1s of a small
  buff (e.g. `MobEffects.MOVEMENT_SPEED` I) and a soft particle puff — rewards precise timing.
- **Cooldown:** flat **~1.5s (30 ticks)** `ironhold:dodge` cooldown on every press. No charges, no stamina.
- **Animation (the real cost):** vanilla has no player roll. v1: first-person camera dip/tilt during
  the hop (client tick mixin on camera) + a small third-person model lean (`LivingEntity` render
  mixin / pose tweak). Keep it a *hop*, not a full roll → far less animation work (this is why we
  chose the hop). ⚠️ client-render mixin work; can ship mechanic first, animation second.

## 5. Files (new)
- `client/IronholdKeys.java` (+2 KeyMappings) · `client/FootworkInputHandler.java` (client key→payload)
- `net/FootworkPayload.java` (+register in `ModNetworking`)
- `combat/FootworkHandler.java` (server: window/iframe state, the `LivingIncomingDamageEvent` hook)
- `effect/StaggeredEffect.java` (+ `ModEffects` entry) · particle/sound entries in `ModParticles`/`ModSounds`
- `rpg/ability/AbilityCooldowns.java` — add `reduceAll(player, ticks)` + reserve parry/dodge ids
- (client) camera/model lean mixins under `mixin/client/`

## 6. Open / TBD
- Final keybind defaults (clash check). Parryable-source tag list. Per-class W/stagger numbers.
- Confirm 1.26 pre-mitigation damage event name + projectile-deflect API. ⚠️
