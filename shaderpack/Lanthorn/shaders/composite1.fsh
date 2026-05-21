#version 330 compatibility

// Single-pass bloom — combined bright-extract + separable Gaussian blur folded
// into a 9-tap kernel. Cheap. The result is additively merged with the scene in
// final.fsh. Bright torch-pixels gain a soft warm halo; bright sky pixels gain
// a soft white halo.

uniform sampler2D colortex0;
uniform vec2 viewSize;

const float BLOOM_THRESHOLD = 0.75;
const float BLOOM_SOFTKNEE  = 0.15;

varying vec2 uv;

vec3 sampleBright(vec2 p) {
    vec3 c = texture2D(colortex0, p).rgb;
    float brightness = max(c.r, max(c.g, c.b));
    float soft = clamp((brightness - BLOOM_THRESHOLD + BLOOM_SOFTKNEE) / (2.0 * BLOOM_SOFTKNEE), 0.0, 1.0);
    soft = soft * soft * (3.0 - 2.0 * soft);
    float keep = max(brightness - BLOOM_THRESHOLD, 0.0) + soft;
    return c * keep / max(brightness, 0.0001);
}

void main() {
    vec2 pixel = 1.0 / viewSize;

    // 9-tap two-pass Gaussian collapsed into one pass — not as good as separable
    // but one pass is enough for a stylized halo.
    vec3 sum = vec3(0.0);
    sum += sampleBright(uv + pixel * vec2(-3.0,  0.0)) * 0.03;
    sum += sampleBright(uv + pixel * vec2(-2.0,  0.0)) * 0.08;
    sum += sampleBright(uv + pixel * vec2(-1.0,  0.0)) * 0.12;
    sum += sampleBright(uv                            ) * 0.20;
    sum += sampleBright(uv + pixel * vec2( 1.0,  0.0)) * 0.12;
    sum += sampleBright(uv + pixel * vec2( 2.0,  0.0)) * 0.08;
    sum += sampleBright(uv + pixel * vec2( 3.0,  0.0)) * 0.03;
    sum += sampleBright(uv + pixel * vec2( 0.0, -2.0)) * 0.08;
    sum += sampleBright(uv + pixel * vec2( 0.0,  2.0)) * 0.08;
    sum += sampleBright(uv + pixel * vec2( 0.0, -3.0)) * 0.03;
    sum += sampleBright(uv + pixel * vec2( 0.0,  3.0)) * 0.03;

    // Write only colortex1 — colortex0 (the scene) must remain untouched for final.fsh.
    gl_FragData[0] = vec4(sum, 1.0);
}

/* DRAWBUFFERS:1 */
