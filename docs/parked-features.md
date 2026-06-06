# Parked features (built, intentionally not active)

These subsystems are fully written but **deliberately not wired in**, so a "find
unused code" pass will flag them as dead. They are not dead — leave them in place.

## GUI reskin (container screens)

Mixin classes in `kingdom.smp.mixin` that reskin vanilla container screens and
reposition their slots, e.g. `AnvilScreenMixin`, `ChestScreenMixin`,
`FurnaceScreenMixin`, `CraftingMenuLayoutMixin`, `AbstractContainerScreenSlotIconMixin`,
etc. — plus their in-game tuning overlays in `kingdom.smp.client` (`*SlotDebug`,
`*SlotDebugHandler`). Several are marked "HAND-EDITED (do not regenerate)".

They are **not listed in `ironhold.mixins.json`**, so they do not load and cost
nothing at runtime. To activate the reskin, add the desired class names to the
`mixins` / `client` arrays in `ironhold.mixins.json`.

## RTF terrain integration

Mixin classes `ChunkMapRTFMixin`, `NoiseBasedChunkGeneratorRTFMixin`,
`NoiseChunkRTFMixin`, `RandomStateRTFMixin`, `SurfaceSystemRTFMixin` (+
`RandomStateMixin`, `SurfaceSystemAccessor`) are the hooks that wire the vendored
`kingdom.smp.rtf` terrain engine (~22K LOC) into vanilla worldgen. They are not in
the mixin config and no world preset selects the RTF generator, so RTF terrain is
currently inactive. Deleting these orphans the whole `rtf/` package.
