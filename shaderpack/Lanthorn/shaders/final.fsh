#version 330 compatibility

// Final pass — combine scene + bloom, then apply painterly post + vignette +
// biome-aware color grading. This is the last pass; output goes to the screen.

uniform sampler2D colortex0;      // scene (with god-rays already added)
uniform sampler2D colortex1;      // bloom halo from composite1
uniform vec3      fogColor;       // proxy for biome (vanilla varies it per biome)
uniform vec3      skyColor;       // proxy for biome
uniform float     rainStrength;
uniform int       worldTime;
uniform vec2      viewSize;

varying vec2 uv;

// ---------- biome-aware grading ----------
//
// Without mod hooks, the cleanest signal a shader pack has for "what biome am I
// in" is vanilla's per-biome fog and sky colors. We push the scene toward a
// biome palette derived from those — desaturate cold biomes, warm up forests,
// etc. — without ever knowing the biome ID.
vec3 biomeGrading(vec3 c) {
    // Luma-preserving hue shift toward the fog color.
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    vec3 paletted = mix(vec3(luma), fogColor * 1.4, 0.6);
    return mix(c, paletted, 0.18);
}

// ---------- painterly post ----------
//
// Cheap "watercolor edge" — sample the four diagonal neighbors and pull the
// fragment slightly toward the local mean. Smooths flat regions and softens
// blocky pixel-art edges without losing silhouette.
vec3 painterly(vec3 c) {
    vec2 px = 1.0 / viewSize;
    vec3 a = texture2D(colortex0, uv + px * vec2( 1.0,  1.0)).rgb;
    vec3 b = texture2D(colortex0, uv + px * vec2(-1.0,  1.0)).rgb;
    vec3 d = texture2D(colortex0, uv + px * vec2( 1.0, -1.0)).rgb;
    vec3 e = texture2D(colortex0, uv + px * vec2(-1.0, -1.0)).rgb;
    vec3 mean = (a + b + d + e) * 0.25;
    return mix(c, mean, 0.25);
}

// ---------- vignette ----------
//
// Soft circular darkening from the corners. Intensifies in rain for that
// "stormy castle painting" mood.
float vignette() {
    vec2 centered = uv - 0.5;
    float r = dot(centered, centered);              // 0 at center, ~0.5 at corner
    float base = smoothstep(0.55, 0.18, r);
    return mix(base, base * 0.7, rainStrength);
}

// ---------- tone map ----------
//
// Hand-rolled ACES-ish curve. Pulls highlights down so bloomed torches don't
// blow out, lifts shadows slightly so castle interiors stay readable.
vec3 toneMap(vec3 c) {
    c = max(c, 0.0);
    const float A = 2.51;
    const float B = 0.03;
    const float C = 2.43;
    const float D = 0.59;
    const float E = 0.14;
    return clamp((c * (A * c + B)) / (c * (C * c + D) + E), 0.0, 1.0);
}

void main() {
    vec3 scene = painterly(texture2D(colortex0, uv).rgb);
    vec3 bloom = texture2D(colortex1, uv).rgb;

    vec3 c = scene + bloom * 0.55;

    c = biomeGrading(c);
    c *= vignette();
    c = toneMap(c);

    // Subtle film grain — pseudo-random noise tied to screen position + time.
    float grain = fract(sin(dot(uv * viewSize + worldTime, vec2(12.9898, 78.233))) * 43758.5453);
    c += (grain - 0.5) * 0.012;

    gl_FragColor = vec4(c, 1.0);
}
