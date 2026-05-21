#version 330 compatibility

#include "/lib/lightmap.glsl"

uniform sampler2D gtexture;   // Sampler0 — entity skin / atlas
uniform sampler2D overlay;    // Sampler1 — hurt-flash + mining-crack overlay
uniform sampler2D lightmap;   // Sampler2 — declared, we re-derive lighting from lmcoord
uniform int worldTime;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec2 overlayCoord;
varying vec4 vcolor;

void main() {
    vec4 albedo = texture2D(gtexture, texcoord) * vcolor;
    if (albedo.a < 0.01) discard;

    // Entity damage / hurt-flash / breaking overlay.
    vec4 overlayColor = texture2D(overlay, overlayCoord);
    albedo.rgb = mix(albedo.rgb, overlayColor.rgb, overlayColor.a);

    vec3 light = lanthorn_lightmap(lmcoord, lanthorn_dayFactor(worldTime));
    gl_FragData[0] = vec4(albedo.rgb * light, albedo.a);
}
