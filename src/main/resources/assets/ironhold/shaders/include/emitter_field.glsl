#version 330

// std140 UBO uploaded by ColorEmitterUploader once per render frame.
// Emitter positions are camera-block-pos-relative (= worldPos - CameraBlockPos) so they match
// the vertex world offset computed in terrain_colored.vsh without precision loss in far chunks.
// .a of emitterColors carries normalized intensity (emission / 15).
// vanillaBlockLightTint is whatever the camera's EnvironmentAttributes.BLOCK_LIGHT_TINT resolved
// to this frame — used as the no-emitter fallback so Nether/End dimension baselines survive.
layout(std140) uniform IronholdEmitterField {
    vec4 vanillaTintAndCount;   // .rgb = vanilla BlockLightTint, .a = active emitter count
    vec4 emitters[32];          // .xyz = relative pos, .w = radius (blocks)
    vec4 emitterColors[32];     // .rgb = target color (linear), .a = intensity (emission/15)
} ironholdField;

vec3 ironhold_emitter_tint(vec3 worldOffset, vec3 fallback) {
    // DIAGNOSTIC v1.28.0 take 2 — unconditional PURPLE. Bypasses UBO read entirely.
    // If terrain looks purple, the new shader IS being loaded; the UBO is the bug.
    // If terrain looks normal warm, the shader file is being cached and we need to
    // force a rebuild (Gradle --rerun-tasks or process-resources).
    return vec3(0.7, 0.2, 1.0);
}
