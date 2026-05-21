#version 330 compatibility

// Universal fallback for any geometry that doesn't match a more specific gbuffer.
// Lines, debug overlays, leashes, beams, and anything Iris can't categorize.

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec2 overlayCoord;
varying vec4 vcolor;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    // Iris's modern compiler doesn't expose gl_TextureMatrix even in compatibility
    // profile; use raw multi-tex-coords and apply the OptiFine lightmap /256 scale.
    texcoord     = gl_MultiTexCoord0.st;
    lmcoord      = gl_MultiTexCoord1.st / 256.0;
    overlayCoord = gl_MultiTexCoord2.st;
    vcolor       = gl_Color;
}
