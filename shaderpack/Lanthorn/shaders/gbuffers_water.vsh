#version 330 compatibility

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec2 overlayCoord;
varying vec4 vcolor;
varying float viewDepth;

void main() {
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    gl_Position  = gl_ProjectionMatrix * viewPos;
    viewDepth    = -viewPos.z;
    texcoord     = gl_MultiTexCoord0.st;
    lmcoord      = gl_MultiTexCoord1.st / 256.0;
    overlayCoord = gl_MultiTexCoord2.st;
    vcolor       = gl_Color;
}
