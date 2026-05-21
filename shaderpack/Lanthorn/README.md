# Lanthorn

An **optional** shader pack for Ironhold. Medieval-cathedral mood without going full photorealism.

## What it does

| Effect | Where |
| --- | --- |
| Warm torchlight + cool moonlight (block-light vs sky-light split, custom palette) | `lib/lightmap.glsl` + every `gbuffers_*.fsh` |
| Volumetric god-rays from sun (day) or moon (night), screen-space radial blur against scene depth | `composite.fsh` |
| Soft light bloom (torch halos, sun shafts) | `composite1.fsh` |
| Painterly post-process (cheap watercolor edge softening) | `final.fsh` |
| Vignette (heavier in rain) | `final.fsh` |
| Biome-aware color grading (palette inferred from vanilla `fogColor`) | `final.fsh` |
| Painted sunrise/sunset glow | `gbuffers_skybasic.fsh` |
| Subtle water shimmer + depth tint | `gbuffers_water.fsh` |
| ACES-ish tone-map + film grain | `final.fsh` |

This is a *shader pack*, not a mod feature. It works without Ironhold installed — feel free to use it with any modpack. It loads through Iris.

## Requirements

- **Iris** (NeoForge or Fabric) — [Modrinth](https://modrinth.com/mod/iris)
- **Sodium** — Iris depends on it
- Minecraft 1.21.x or later. Targeted at 1.26.x but should work back to 1.21.

## Install

1. Download or zip this folder (`Lanthorn`) — the contents of `shaders/` need to be at the root of the zip OR the folder dropped in unzipped is fine
2. Drop the folder/zip into your Minecraft's `shaderpacks/` directory
   - Vanilla launcher: `~/Library/Application Support/minecraft/shaderpacks/` (macOS)
   - Modded launcher: `<instance>/shaderpacks/`
   - Ironhold dev env: `run/shaderpacks/`
3. In-game: **Options → Video Settings → Shader Packs → Lanthorn → Apply**

You can tweak any effect from **Options → Video Settings → Shader Packs → Shader Options** while the pack is active.

## Options

Three presets via the **Profile** picker in the shader-options screen:

- **LOW** — god-rays + bloom OFF; just the warm/cool lightmap + painterly final
- **MEDIUM** — god-rays ON, bloom OFF
- **HIGH** *(recommended)* — everything on

Individual toggles live under the **God Rays**, **Bloom**, **Painterly**, **Color Grading**, and **Light Temperature** sub-screens.

## Iteration

Editing files in this folder while the pack is active:

1. Edit a `.vsh` / `.fsh` / `.glsl` file
2. In-game, press the Iris reload key (default **F3+R**; or open the shader-pack menu and click **Apply**)
3. Compile errors land in chat (red text) and `logs/latest.log`. Search for `Failed to compile` or `ShaderCompileException`

## File layout

```
shaders/
  shaders.properties       — pack metadata + option screen layout
  lang/en_us.lang          — option labels
  lib/lightmap.glsl        — shared warm/cool light remap
  gbuffers_basic.{vsh,fsh} — universal fallback (lines, leashes, debug)
  gbuffers_terrain.{vsh,fsh}
  gbuffers_water.{vsh,fsh}
  gbuffers_skybasic.{vsh,fsh}
  composite.{vsh,fsh}      — god-rays (radial blur from sun/moon)
  composite1.{vsh,fsh}     — bloom extract + blur into colortex1
  final.{vsh,fsh}          — painterly + grading + vignette + tone map
```

There's no shadow pass — god-rays use scene depth instead of a shadow map, so we skip the expense.

## License

Whatever you like; this is part of the Ironhold project.
