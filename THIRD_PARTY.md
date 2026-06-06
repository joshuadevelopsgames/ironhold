# Third-Party Assets & Dependencies

## LambDynamicLights (technique attribution; no code reused)
- **Author:** LambdAurora
- **Source:** https://github.com/LambdAurora/LambDynamicLights
- **Modrinth:** https://modrinth.com/mod/lambdynamiclights
- **Usage:** Ironhold ships its own minimal dynamic-lighting system at `src/main/java/kingdom/smp/dynlight/` (clean-room, no LambDynamicLights source incorporated). The lightmap-injection technique it uses — sampling vanilla per-block light at `LevelRenderer#getLightCoords`, comparing against a per-source falloff `(1 - dist/7.75) * luminance`, replacing the block-light bits when the dynamic value wins — is documented openly by LambdAurora in their "How does it work?" reference and was originally suggested by MaryWeeb; credit for the approach goes to them. The JSON file layout under `assets/<ns>/dynamiclights/{item,entity}/` mirrors LambDynamicLights so existing data packs remain readable, but the loader is our own.
- **Scope (Ironhold-specific):** Held items light up only if they are registered in `assets/ironhold/dynamiclights/item/` or are `BlockItem`s with a positive light emission. Entities light up only if their type is registered in `assets/ironhold/dynamiclights/entity/`. No automatic glow for blazes, magma cubes, TNT, particles, etc.

## Ebonwood Hollow Ambient Sound
- **Author:** CreativeMD
- **License:** LGPL-3.0
- **Source:** https://github.com/CreativeMD/AmbientSounds
- **File:** `sounds/ambient/ebonwood_hollow.ogg` (originally `suspense/pale-garden.ogg`)
- **Usage:** Looping ambient sound for the Ebonwood Hollow biome.

## Reactive Music Pack — "Adventure Redefined"
- **Composers:** Vindsvept, Adrian von Ziegler, and Mufaya (as credited in the original pack).
- **Repackaged from:** CircuitLord's ReactiveMusic — https://github.com/CircuitLord/ReactiveMusic (mod code is GPL-3.0; the bundled songpack `src/main/resources/musicpack/` carries the credit line *"Music from Vindsvept, Adrian von Ziegler, and Mufaya."*).
- **Engine:** Ironhold's reactive-music system (`src/main/java/kingdom/smp/music/`) is a clean-room implementation — **no ReactiveMusic code was used**, only the music tracks. The trigger→track mapping in `ReactiveSongbook` is our own re-expression of the pack's `ReactiveMusic.yaml` intent.
- **Files:** 61 tracks under `assets/ironhold/sounds/music/*.ogg`, transcoded from the original `.mp3` to Ogg Vorbis (Minecraft requires Ogg). Registered as streamed `music.ironhold.*` sound events in `ModSounds`.
- **Usage:** Situational soundtrack (menu / day / night / weather / dimension / depth / boss / PvP), selected per-tick and played via NeoForge `SelectMusicEvent`.
- **⚠️ Licensing — verify before distribution:** these are third-party composers' works, not CircuitLord's to relicense, and the repo's GPL covers its *code*, not the music. Each composer sets their own terms:
  - **Vindsvept** — generally Creative Commons BY 4.0 (attribution required). https://www.youtube.com/@Vindsvept
  - **Adrian von Ziegler** — free to use with credit, but historically restricts redistribution / monetization; confirm current terms. https://www.youtube.com/@AdrianvonZiegler
  - **Mufaya** — confirm the composer's usage terms.
  Bundling these in Ironhold (All Rights Reserved, distributed via Modrinth/Folium) should be cleared against each composer's license — at minimum keep this attribution intact; ideally obtain explicit permission or swap any track whose license forbids redistribution. Tracked here so the obligation isn't lost.
