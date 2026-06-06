# Mirrors

A standalone NeoForge mod that adds a single craftable **Mirror** — a wall-hung pane that places like
a painting (auto-fitting the largest shape the wall allows) and renders a **true real-time planar
reflection** of the world.

Up to four nearby mirrors render their own live reflection at once; mirrors beyond that show a static
glassy surface so they never read as a see-through hole. The reflection is captured each frame with an
off-axis projection from the player's reflected eye, into a per-mirror off-screen framebuffer.

## Gallery

Promo screenshots live in `gallery/` (upload these to the project's Gallery tab on Modrinth):

- `gallery/mirror-framed-portrait.png` — a single framed mirror reflecting the player head-on in a torchlit room
- `gallery/mirror-room-reflection.png` — a mirror reflecting a villager-lit wooden interior
- `gallery/mirror-moon-reflection.png` — a mirror reflecting the player out on the moon

## Crafting

Shapeless: `Painting + Black Dye + Glass`.

## Building

```
./gradlew build
```

The mod jar lands in `build/libs/`. Requires JDK 25.

## Publishing to Modrinth

1. Create the project on Modrinth and copy its id/slug into `modrinth_project_id` in `gradle.properties`.
2. Adjust `modrinth_game_versions` (and `neo_version` / `minecraft_version`) to the versions you build against.
3. Export a write-scoped token and publish:

   ```
   export MODRINTH_TOKEN=...
   ./gradlew modrinth
   ```

The `modrinth` task uploads the built jar, tags it for the `neoforge` loader, and syncs the project
description from this README.

## Credits

Extracted from the Ironhold mod.
