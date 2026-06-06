# Dragon model — our wings + tail, mimicked from the Mowzie's Naga

Two files here:

| File | What it is |
|---|---|
| **`dragon.geo.json`** | **OURS.** Only the **wings + tail** mimicked from the Naga, anchored on our own placeholder torso (`geometry.ironhold:dragon`). This is the one we build on. |
| `dragon_from_naga.geo.json` | Full converted Naga rig (97 bones) kept as a reference only. |

Pipeline: `tools/naga_to_geo.py` (llibrary Java model → geo) → `tools/build_dragon_geo.py` (carve to
wings + tail, swap in our body anchor). Re-run both to regenerate.

## ⚠️ Licensing
Mowzie's Mobs is a **custom license**. We mimic only the **wing + tail geometry** as a base and build the
rest ourselves — **make our own texture; do not ship the Naga `naga.png` or the full rig.** Don't commit
their decompiled `.java`.

## What `dragon.geo.json` contains (73 bones)
- **Wings** (both sides): `shoulderLJoint/RJoint → shoulder1 → upperArm → lowerArm → hand →
  wingFrame1–4` (finger bones) → `wingClaw`, with **membranes** `wingWebbing1–6`. Each membrane is a flat
  **0-thickness plane** + a mirrored `…Reversed` twin offset `-0.004` (two-sided wing skin — keep it).
- **Tail**: `tail1→…→tail6→tailEnd` tapering chain + flat `tailFin` + tail dorsal fins `backFin2/3` +
  ridge `spike4/5` + the small `backWing_L/R` off the tail base.
- **`body`** = a **placeholder torso** (size 14×13×22) only so the wings/tail have something to attach to.
  **Replace it** with our real dragon body/head/legs. Its pivot is kept so the wing/tail attach points
  don't move when you swap the box.

## Next steps
1. **Open `dragon.geo.json` in Blockbench** (GeckoLib plugin) and check orientation. The Java→Bedrock
   transform handles the Y-flip but **rotation-axis signs are a first cut** — if a wing or tail segment
   bends the wrong way, negate that bone's `rotation` axis and re-check.
2. **Build our body**: replace the placeholder `body` with a real dragon torso, add a head/neck and
   legs, scale the wings/tail to fit.
3. **Retexture**: paint our own 256×256 dragon texture (the wing/tail UVs are a usable starting layout).
4. **Animate** in GeckoLib: flap, hover-idle, swoop/dive, tail-whip. Drive the tail + wing-tips with a
   spring/lag layer (see `docs/research/mowzies_mobs_boss_design_notes.md` §3 — DynamicChain) so they
   trail naturally instead of stiff keyframes.
5. Wire up the Ironhold entity (renderer + entity + GeckoLib controllers) once the shape is locked.
