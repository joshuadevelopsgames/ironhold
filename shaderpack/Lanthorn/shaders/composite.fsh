#version 330 compatibility

// God-rays via screen-space radial blur from the sun (or moon) screen position.
// Pixels behind sky (depth == 1.0) contribute light along the ray; pixels behind
// geometry occlude. Classic OptiFine "light shafts" — cheap, no shadowmap needed.

uniform sampler2D colortex0;     // scene
uniform sampler2D depthtex0;     // scene depth
uniform vec3 sunPosition;        // view-space sun direction
uniform vec3 moonPosition;       // view-space moon direction
uniform mat4 gbufferProjection;  // view -> clip
uniform int worldTime;

const int   GODRAY_SAMPLES   = 48;
const float GODRAY_DECAY     = 0.965;
const float GODRAY_WEIGHT    = 0.55;
const float GODRAY_EXPOSURE  = 0.045;

varying vec2 uv;

vec2 projectToScreen(vec3 viewPos) {
    vec4 clip = gbufferProjection * vec4(viewPos, 1.0);
    return clip.xy / clip.w * 0.5 + 0.5;
}

void main() {
    vec3 scene = texture2D(colortex0, uv).rgb;

    // Pick sun by day, moon by night.
    float t = worldTime / 24000.0;
    float day = clamp(cos((t - 0.25) * 6.2831853) * 0.5 + 0.5, 0.0, 1.0);
    bool isDay = day > 0.05;
    vec3 lightPosView = isDay ? sunPosition : moonPosition;

    // Light source behind the camera? skip.
    if (lightPosView.z >= 0.0) {
        gl_FragData[0] = vec4(scene, 1.0);
        return;
    }

    vec2 lightUV = projectToScreen(lightPosView);

    // Ray from current pixel toward light's screen position, sampled in 48 steps.
    vec2 step = (uv - lightUV) / float(GODRAY_SAMPLES) * 0.75;
    vec2 cur = uv;
    float illumination = 1.0;
    float accum = 0.0;

    for (int i = 0; i < GODRAY_SAMPLES; i++) {
        cur -= step;
        if (cur.x < 0.0 || cur.x > 1.0 || cur.y < 0.0 || cur.y > 1.0) break;
        float depth = texture2D(depthtex0, cur).r;
        // Sky (max depth) emits; anything else occludes.
        float emit = depth >= 0.9999 ? 1.0 : 0.0;
        accum += emit * illumination * GODRAY_WEIGHT;
        illumination *= GODRAY_DECAY;
    }
    accum *= GODRAY_EXPOSURE;

    vec3 raysColor = isDay
        ? vec3(1.00, 0.92, 0.75)
        : vec3(0.55, 0.65, 0.95);

    // Slight under-water-overlay attenuation — god-rays underwater feel different;
    // we keep it but tinted. Without isEyeInWater we can't gate on that here, so just
    // let the visible attenuation come from depth occlusion above.

    gl_FragData[0] = vec4(scene + accum * raysColor, 1.0);
}
