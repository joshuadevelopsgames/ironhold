#version 330 compatibility

#include "/lib/lightmap.glsl"

uniform sampler2D gtexture;   // Sampler0
uniform sampler2D overlay;    // Sampler1 — placeholder so the pipeline binding stays in sync
uniform sampler2D lightmap;   // Sampler2
uniform int worldTime;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec2 overlayCoord;
varying vec4 vcolor;

void main() {
    vec4 albedo = texture2D(gtexture, texcoord) * vcolor;
    if (albedo.a < 0.1) discard;
    vec4 ov = texture2D(overlay, overlayCoord);
    albedo.rgb = mix(albedo.rgb, ov.rgb, ov.a);  // mining-crack overlay on terrain
    vec3 light = lanthorn_lightmap(lmcoord, lanthorn_dayFactor(worldTime));
    gl_FragData[0] = vec4(albedo.rgb * light, albedo.a);
}
