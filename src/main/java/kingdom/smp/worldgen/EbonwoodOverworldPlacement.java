package kingdom.smp.worldgen;

/**
 * EbonwoodOverworldPlacement — documents the placement strategy for Ebonwood Hollow
 * in the overworld and the rationale for the dual-path setup.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * CURRENT STATUS: DUAL-PATH (lithostitched + vanilla mixin)
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Ebonwood Hollow is placed via TWO parallel mechanisms. Exactly one is active
 * at runtime depending on which mods the player has loaded.
 *
 * ────────────────────────────────────────────────────────────────────────────
 * PATH A — lithostitched biome injector (preferred; Terralith/Tectonic compatible)
 * ────────────────────────────────────────────────────────────────────────────
 * File: data/ironhold/lithostitched/biome_injector/ebonwood_hollow.json
 *
 * When the optional dependency `lithostitched` is loaded, lithostitched takes
 * over the climate→biome router for the overworld. It reads our biome_injector
 * file and merges the ebonwood_hollow ParameterPoint into the parameter list.
 *
 * Tectonic ≥ 2.4.3 declares lithostitched as a HARD dependency, so any player
 * running Tectonic automatically has lithostitched and Path A activates.
 *
 * Terralith disables its own dimension JSON when lithostitched is present
 * (via `neoforge:conditions` → `not mod_loaded("lithostitched")`) and instead
 * ships its biome library through a vanilla parameter_list override with
 * `lithostitched:biomes` extension keys. So under Terralith + lithostitched,
 * Terralith's ~95 biomes coexist with our ebonwood_hollow via lithostitched's
 * merge pipeline.
 *
 * Common Biome Tags applied (data/c/tags/worldgen/biome/):
 *   is_overworld, is_forest, is_cold/, is_dense_vegetation/
 *
 * Vanilla tags applied (data/minecraft/tags/worldgen/biome/):
 *   is_overworld, is_forest, is_taiga
 *
 * ────────────────────────────────────────────────────────────────────────────
 * PATH B — vanilla OverworldBiomeBuilder mixin (fallback for vanilla worlds)
 * ────────────────────────────────────────────────────────────────────────────
 * File: kingdom.smp.mixin.OverworldBiomeBuilderMixin
 *
 * Injects the same Climate.ParameterPoint into vanilla's OverworldBiomeBuilder
 * at RETURN of addBiomes(). This is the only path that works when lithostitched
 * is NOT loaded.
 *
 * Caveat: the mixin still runs even when lithostitched IS loaded, because it
 * patches the vanilla bootstrap. Whether this causes a duplicate parameter
 * point depends on lithostitched's implementation — at worst, ebonwood appears
 * at its expected niche from two contributing sources, which is benign (the
 * climate parameter space cannot be double-occupied by the same biome at the
 * same point — duplicates collapse).
 *
 * ────────────────────────────────────────────────────────────────────────────
 * PATH C — testing without overworld placement
 * ────────────────────────────────────────────────────────────────────────────
 * In a creative world use the single-biome preset:
 *   /gamemode creative
 *   Create a new world → More options → World Type: Single Biome
 *   → Choose "Ebonwood Hollow" (ironhold:ebonwood_hollow)
 *
 * Or place the biome manually in any world with:
 *   /place biome ironhold:ebonwood_hollow
 *   /locate biome ironhold:ebonwood_hollow
 *
 * ════════════════════════════════════════════════════════════════════════════
 * COMPATIBILITY MATRIX
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   Vanilla only                       → Path B (mixin)             — works
 *   Vanilla + Tectonic                 → Path A (Tectonic pulls LS) — works
 *   Vanilla + Terralith (no LS)        → neither path engages       — DOES NOT GENERATE
 *   Vanilla + Terralith + lithostitched→ Path A                     — works
 *   Vanilla + Tectonic + Terralith     → Path A                     — works
 *
 * The "Terralith without lithostitched" gap is eliminated by declaring
 * lithostitched as a REQUIRED dependency in neoforge.mods.toml. The server
 * (or client) will refuse to load ironhold without lithostitched, surfacing
 * the missing dep as a clear loader error instead of a silent placement
 * failure.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * THE EBONWOOD HOLLOW CLIMATE NICHE
 * ════════════════════════════════════════════════════════════════════════════
 *   temperature  : [-0.45, -0.15]   — cool (taiga band)
 *   humidity     : [-0.10,  0.30]   — moderate
 *   continentalness: [0.30,  1.00]  — inland only
 *   erosion      : [-0.78,  0.05]   — hilly to flat
 *   weirdness    : [ 0.05,  0.40]   — mid-weirdness band
 *   depth        : [0.0,    0.0]    — surface layer only
 *   offset       : 0.0              — same priority as vanilla biomes
 *
 * These values are mirrored in both Path A (the biome_injector JSON) and
 * Path B (the OverworldBiomeBuilderMixin Java). Keep them in sync when tuning.
 */
public final class EbonwoodOverworldPlacement {
    private EbonwoodOverworldPlacement() {}
}
