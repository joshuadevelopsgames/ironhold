# TRMT — The Roads More Travelled (third-party port)

Ironhold integrates a NeoForge port of **TRMT — The Roads More Travelled**
by **milkucha** ([github.com/milkucha/trmt](https://github.com/milkucha/trmt),
[modrinth.com/mod/the-roads-more-travelled](https://modrinth.com/mod/the-roads-more-travelled)).

TRMT is a Fabric-only mod; we ported it to NeoForge in-tree because Ironhold
needs the same gradual-path-erosion effect (grass / dirt / sand / vegetation
that wear down where the player walks repeatedly) and there is no upstream
NeoForge build.

## Upstream source

- Repo: <https://github.com/milkucha/trmt>
- Branch ported: `26.1+26.1.1+26.1.2`
- Commit at time of port: `474d9ccdb34eb9a00838194dae43d5b7d79f6ac3`
- Upstream version: `0.5-26.1+26.1.1+26.1.2`

## License

TRMT is licensed under **CC BY-NC 4.0** (Creative Commons
Attribution-NonCommercial 4.0 International).
<https://creativecommons.org/licenses/by-nc/4.0/>

What that means for Ironhold:

- **Attribution** — milkucha must be credited as the original author of the
  TRMT subsystem. This file is the canonical attribution record; the
  in-game `trmt` mod entry in `neoforge.mods.toml` also names milkucha as
  author.
- **NonCommercial** — Ironhold builds that include the TRMT subsystem may
  **not** be sold, monetised, or distributed as part of a paid product.
  Personal / private / non-commercial public distribution is fine.
- **Same-license fragments** — porting changes to TRMT-derived files
  (anything under `milkucha.trmt.*`, `assets/trmt/*`, `data/trmt/*`) remain
  under CC BY-NC 4.0. Original Ironhold code (`kingdom.smp.*`) is
  unaffected.

If Ironhold ever ships commercially, the TRMT subsystem must either be
removed or relicensed by agreement with milkucha first.

## Port layout

The ported code keeps the upstream `milkucha.trmt` package and `trmt`
namespace so future upstream changes diff cleanly:

```
src/main/java/milkucha/trmt/          # ported Java (was src/main/java/...)
src/main/java/milkucha/trmt/client/   # ported client code (was src/client/...)
src/main/java/milkucha/trmt/mixin/    # ported mixins
src/main/resources/assets/trmt/       # ported assets
src/main/resources/data/trmt/         # ported data (loot, recipes, tags)
src/main/resources/trmt.mixins.json   # ported mixin config
```

TRMT is declared as a **second `[[mods]]` entry** inside Ironhold's
`META-INF/neoforge.mods.toml`. One jar, two mod ids (`ironhold` + `trmt`).
This was chosen over a separate Gradle submodule because Ironhold is a
single-module ModDevGradle build and bringing in multi-module overhead
would buy nothing.

## What changed from upstream

The mod's *behaviour* and assets are identical. Only the loader-glue had to
be rewritten:

| Upstream (Fabric) | Port (NeoForge) |
| --- | --- |
| `ModInitializer.onInitialize()` | `@Mod("trmt")` class + `IEventBus` constructor |
| `Registry.register(...)` direct calls | `DeferredRegister` |
| `PayloadTypeRegistry` | `RegisterPayloadHandlersEvent` + `PayloadRegistrar` |
| `ServerLifecycleEvents` | `ServerStartedEvent` / `ServerStoppedEvent` |
| `ServerPlayConnectionEvents.JOIN` | `PlayerEvent.PlayerLoggedInEvent` |
| `PlayerBlockBreakEvents.AFTER` | `BlockEvent.BreakEvent` |
| `UseBlockCallback` | `PlayerInteractEvent.RightClickBlock` |
| `CommandRegistrationCallback` | `RegisterCommandsEvent` |
| `Permissions.COMMANDS_GAMEMASTER` | `src.hasPermission(2)` |
| `BlockColorRegistry` (Fabric Rendering API) | `RegisterColorHandlersEvent.Block` |
| `WrapperBlockStateModel` + `QuadEmitter` (Fabric Rendering API) | Custom `BakedModel` wrapper |
| `fabric.mod.json` | `META-INF/neoforge.mods.toml` (second `[[mods]]`) |
| `fabric-datagen` entrypoint | `GatherDataEvent` |

All `@Mixin`-annotated classes targeting vanilla types (`ServerPlayer`,
`BoneMealItem`, `BrushItem`, `GrassBlock`, `HoeItem`, `Mob`, `SandBlock`,
`ShovelItem`, `SugarCaneBlock`) drop in unchanged — MixinExtras is bundled
with NeoForge and the mappings already match.

## Syncing upstream

When milkucha publishes a new TRMT release on a matching MC version:

1. Diff the upstream tree against the commit recorded above.
2. Apply non-glue changes (erosion logic, mixin bodies, assets) directly.
3. Re-port any new `fabric-api` calls using the mapping table above.
4. Bump the commit reference at the top of this file.
