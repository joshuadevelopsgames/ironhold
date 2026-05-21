#version 330 compatibility

// Sky-gradient mesh — no texture. Vanilla colors via gl_Color encode the
// vertical sky color, which we'll re-grade in the fragment shader.

varying vec4 vcolor;
varying vec3 viewDir;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    vcolor = gl_Color;
    viewDir = normalize((gl_ModelViewMatrix * gl_Vertex).xyz);
}
