#version 330 compatibility

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec2 overlayCoord;
varying vec4 vcolor;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    texcoord     = gl_MultiTexCoord0.st;
    lmcoord      = gl_MultiTexCoord1.st / 256.0;
    overlayCoord = gl_MultiTexCoord2.st;
    vcolor       = gl_Color;
}
