#version 330 compatibility

#include "/lib/lightmap.glsl"

uniform sampler2D gtexture;   // Sampler0
uniform sampler2D overlay;    // Sampler1 (unused but pipeline expects it)
uniform sampler2D lightmap;   // Sampler2
uniform int worldTime;
uniform float frameTimeCounter;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec2 overlayCoord;
varying vec4 vcolor;
varying float viewDepth;

void main() {
    vec4 albedo = texture2D(gtexture, texcoord) * vcolor;
    if (albedo.a < 0.01) discard;
    vec4 ov = texture2D(overlay, overlayCoord);
    albedo.rgb = mix(albedo.rgb, ov.rgb, ov.a);

    float day = lanthorn_dayFactor(worldTime);
    vec3 light = lanthorn_lightmap(lmcoord, day);

    // Subtle moving "highlight" on water — a slow sine in screen-aligned UV.
    // Keeps the medieval-painted look without realistic reflections.
    float shimmer = sin(texcoord.x * 32.0 + frameTimeCounter * 0.6)
                  * sin(texcoord.y * 28.0 + frameTimeCounter * 0.4);
    shimmer = max(0.0, shimmer);
    vec3 shimmerColor = mix(vec3(0.15, 0.25, 0.45), vec3(0.85, 0.9, 1.0), day);
    light += shimmerColor * shimmer * 0.08 * lmcoord.y;  // brighter shimmer in open sky

    // Slight deep-water tint with view-distance fade — water in shadow goes darker blue.
    vec3 waterTint = mix(vec3(0.30, 0.55, 0.70), vec3(0.08, 0.18, 0.30), 1.0 - day);
    vec3 tinted = mix(albedo.rgb, albedo.rgb * waterTint, 0.6);

    gl_FragData[0] = vec4(tinted * light, albedo.a);
}
