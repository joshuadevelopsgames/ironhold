#version 330 compatibility

// Full-screen pass — no vertex transform needed; vanilla provides a screen-aligned quad.

varying vec2 uv;

void main() {
    gl_Position = ftransform();
    uv = gl_MultiTexCoord0.st;
}
