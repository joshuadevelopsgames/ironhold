# Bard Instruments (atmosphere)  (feature ⑯)

**Status:** spec. Decisions: `fantasia_port_decisions.md` ⑯. **Bard NPCs only** — no player-held
instrument items. Your `music` reactive engine already covers the adaptive score; this is the diegetic
in-world layer.

## 1. Goal
Bard NPCs play instruments in taverns/villages for ambiance — music emanates from them, not from a
global track.

## 2. Approach
- `entity.BramBardEntity` (+ any tavern NPCs) gains a "performing" behavior: when idle near a
  gathering spot (tavern marker / players nearby), enter a play state.
- **Audio:** loop short instrument tracks (lute/flute `.ogg` in `ModSounds`) as **positioned** sounds
  at the bard's location (attenuated by distance) so it's diegetic. ⚠️ provide ogg assets. Optional:
  drive song selection through the existing `music` engine so it ducks/coordinates with the reactive
  score rather than clashing.
- **Animation:** GeckoLib play pose (cf. existing GeoEntity mobs) — strum/hold-instrument; a held
  instrument item model on the NPC. A simple looped pose is fine v1.
- **Trigger/stop:** play while players are within range and the bard is unthreatened; stop on combat.

## 3. Files (new)
- bard "perform" goal (`entity/goal/BardPerformGoal.java`) + state on `BramBardEntity`
- instrument `.ogg` sounds in `ModSounds` · held-instrument model/texture · GeckoLib play animation

## 4. Open / TBD
- Source the instrument audio (ogg). Coordinate with `music` engine vs standalone positioned sound.
- Which NPCs perform beyond BramBard.
