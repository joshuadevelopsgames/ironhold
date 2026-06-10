# Atmosphere Polish  (feature ⑰)

**Status:** spec — **DEFERRED** to a polish pass after core systems. Decisions:
`fantasia_port_decisions.md` ⑰. All four pieces wanted; all client-side, independent, low-risk.

## 1. Goal
Ambient "alive world" polish leveraging the existing Seasons + Moon dimension + `client/particle`.

## 2. Pieces (independent; build any order)
- **Auroras** — a sky-layer renderer drawing northern-lights ribbons in cold biomes / clear nights
  (and tie to Seasons: winter auroras). Client-only sky render (cf. `client/moon` custom sky work).
  Driven by biome temperature + time + weather.
- **Enhanced night sky** — richer stars / nebulae / larger moon; especially fitting given the Moon
  dimension. Custom `DimensionSpecialEffects` / sky renderer. ⚠️ ensure it composes with auroras.
- **Ambient particles & sounds** — biome-reactive motes, fireflies (warm nights), falling leaves
  (Seasons-aware), gentle nature audio loops. A client `AmbientParticleHandler` ticking around the
  player by biome/season; reuse `ModParticles`.
- **Weather polish** — softer rain visuals, **delayed thunder** (lightning flash → rumble after a
  distance-scaled delay), gentler weather transitions. Client weather/sound tweaks.

## 3. Files (new, client)
- `client/sky/AuroraRenderer.java`, `client/sky/NightSkyEnhancer.java` (or `DimensionSpecialEffects`)
- `client/AmbientParticleHandler.java` · `client/DelayedThunderHandler.java`
- particle/sound assets

## 4. Open / TBD
- Performance budget (these run every client tick — gate by config + distance). Visual direction/art.
- Integration with shaders if players run them.
