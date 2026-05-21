#version 330 compatibility

#include "/lib/lightmap.glsl"

uniform sampler2D gtexture;   // Sampler0 — atlas / GUI texture
uniform sampler2D overlay;    // Sampler1 — declared so Iris keeps the binding stable, unused here
uniform sampler2D lightmap;   // Sampler2 — declared but we re-derive lighting from lmcoord
uniform int worldTime;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec2 overlayCoord;
varying vec4 vcolor;

void main() {
    vec4 albedo = texture2D(gtexture, texcoord) * vcolor;
    // Reference overlay so the GLSL optimizer keeps Sampler1 bound — required by
    // any Iris pipeline that falls through to this shader for entity overlays.
    vec4 ov = texture2D(overlay, overlayCoord);
    albedo.rgb = mix(albedo.rgb, ov.rgb, ov.a);
    vec3 light = lanthorn_lightmap(lmcoord, lanthorn_dayFactor(worldTime));
    gl_FragData[0] = vec4(albedo.rgb * light, albedo.a);
}
