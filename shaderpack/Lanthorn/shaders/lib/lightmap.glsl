// Lanthorn — shared lightmap remap.
//
// Vanilla samples the 16x16 lightmap LUT to get a single color per fragment.
// We bypass it and reconstruct the lightmap from scratch so we can:
//   1. tint block-light (torches, lava, etc.) WARM
//   2. tint sky-light COOL at night, neutral by day
// The lmcoord input is (blockLight, skyLight) each in [0..1] after vanilla's
// pre-scaling. Output is straight RGB to multiply into the fragment albedo.

const vec3 LANTHORN_TORCH_COLOR  = vec3(1.00, 0.55, 0.22);  // ~1900 K candle
const vec3 LANTHORN_DAY_COLOR    = vec3(1.00, 0.98, 0.95);  // bright noon
const vec3 LANTHORN_NIGHT_COLOR  = vec3(0.22, 0.32, 0.55);  // cool moonlight
const vec3 LANTHORN_AMBIENT      = vec3(0.04, 0.04, 0.05);  // pitch-dark floor

vec3 lanthorn_lightmap(vec2 lmcoord, float dayFactor) {
    float blockAmt = clamp(lmcoord.x, 0.0, 1.0);
    float skyAmt   = clamp(lmcoord.y, 0.0, 1.0);

    // Block-light contribution. Bias the curve so very-low torchlight stays
    // dim instead of giving every dark cave a flat orange wash.
    float blockCurve = blockAmt * blockAmt * (3.0 - 2.0 * blockAmt);
    vec3 blockLit = LANTHORN_TORCH_COLOR * blockCurve;

    // Sky-light contribution. Mix between night moonlight and day sunlight.
    vec3 skyTint = mix(LANTHORN_NIGHT_COLOR, LANTHORN_DAY_COLOR, dayFactor);
    vec3 skyLit  = skyTint * skyAmt;

    return LANTHORN_AMBIENT + blockLit + skyLit;
}

// dayFactor: 1.0 = solar noon, 0.0 = midnight, smooth dawn/dusk.
// worldTime is 0..24000; 6000=noon, 18000=midnight.
float lanthorn_dayFactor(float worldTime) {
    float t = worldTime / 24000.0;
    // Center on noon (0.25 of the day) and let it fall off on either side.
    float d = cos((t - 0.25) * 6.2831853);
    return clamp(d * 0.5 + 0.5, 0.0, 1.0);
}
