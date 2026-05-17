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
