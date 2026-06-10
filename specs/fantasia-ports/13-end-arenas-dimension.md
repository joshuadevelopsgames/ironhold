# Boss-Gated End + Boss Arenas + Story Dimension  (feature ⑱)

**Status:** spec — large, content-heavy. Decisions: `fantasia_port_decisions.md` ⑱. Three linked
content systems. Heavy content/worldgen; framework here, set-pieces are authored content (⚠️ TBD).

## 1. Boss-gated End
- **Goal:** End access requires defeating bosses for their eyes (fewer than 12, ~4 long-term; **2 now**
  from King Enderman + Stone Golem — decision ⑱).
- **Mechanic:** bosses drop a custom eye item on death (reuse/extend `entity.KingEnderEyeEntity` /
  a `BossEyeItem`). The End portal is gated so it only activates with the boss-eyes placed:
  - Option A (cleanest): a **custom End-portal frame** (our block) that accepts N boss-eyes instead of
    vanilla ender eyes; vanilla stronghold portal stays inert. ⚠️ or override vanilla eye placement to
    require boss-eyes.
  - Launch with **2 required eyes** (the live bosses); raise the count + add eyes as bosses ship.
- **Drop hook:** boss `LivingDeathEvent` in `IronholdGameEvents`, 100% first-kill (track earned set, cf. boss accessories).

## 2. Boss arenas
- **Goal:** each boss lives in a themed set-piece arena you discover & clear — the home for the #1 boss
  accessories. Bosses become destinations, not random spawns.
- **Build on RTF structures** (`rtf/structure/*` + `rtf/feature/template/*` template paste system).
  Each arena = a structure template + a spawn/trigger that summons the boss when the player enters/
  activates it, with anti-cheese (cf. Fantasia's inhibitor) — e.g. seal the arena during the fight.
- v1: arenas for **King Enderman** and **Stone Golem**. Each new boss = a new arena template + boss spawn rule.
- ⚠️ Structure authoring (build the arenas) is the bulk of the work; see `docs/build_recipes/` + `scripts/schem_to_isc.py`.

## 3. Story dimension (Hall-of-Decimus analog)
- **Goal:** a bespoke narrative dimension beyond the Moon, reached via a portal/gem, housing a climax
  boss/structure — the endgame destination.
- **Reuse:** the Moon dimension + immersive-portal tech (`portal/*`, `moon/*`, `MoonPortalBlock`) as the
  template for a new dimension + portal. New `dimension`/`dimension_type` data, a portal block/activation
  item, custom sky (cf. `client/moon`).
- **Content (⚠️ TBD — needs a design pass):** theme, layout, the climax boss, the reward for clearing it,
  and how it slots after the End gate narratively. This is a mini-project; spec the *framework* now,
  author content later.

## 4. Files (new)
- `item/BossEyeItem.java` (or extend KingEnderEye) + custom End-portal frame block (`block/…`)
- `structure/` arena templates + boss-spawn/seal handlers (on RTF structure system)
- new dimension: `dimension`/`dimension_type` json, portal block + activation, `client` sky effects
- drop hooks in `IronholdGameEvents`

## 5. Open / TBD
- ⚠️ Custom-portal-frame vs vanilla-eye-override approach. Required-eye count curve as bosses ship.
- Arena authoring per boss. **Story dimension content design (theme/boss/reward) — its own pass.**
- Which mobs become bosses #3+ (candidates: Void Invoker/Null Stalker, Siren, Plague Doctor/Cemetery
  Watcher, Skeleton King).
