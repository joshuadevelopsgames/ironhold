#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#moj_import <ironhold:emitter_field.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);

    // Custom block-light tinting: split block-light from sky-light contributions in the
    // baked vanilla lightmap, then rescale ONLY the block-light contribution by our
    // per-vertex emitter tint. This preserves sky/sun lighting and only colors block-lit areas.
    vec4 fullLm = sample_lightmap(Sampler2, UV2);
    vec4 skyLm  = sample_lightmap(Sampler2, ivec2(0, UV2.y));
    vec3 blockContrib = max(fullLm.rgb - skyLm.rgb, vec3(0.0));

    vec3 vanillaTint = ironholdField.vanillaTintAndCount.rgb;
    // Emitter positions are in camera-block-pos-relative space, matching (Position + ChunkPosition - CameraBlockPos).
    // We deliberately drop CameraOffset for the distance compare so a sub-block camera shift
    // doesn't shift the falloff field across the world (would cause flicker as the camera moves).
    vec3 worldOffsetForLighting = Position + (ChunkPosition - CameraBlockPos);
    vec3 ourTint = ironhold_emitter_tint(worldOffsetForLighting, vanillaTint);

    vec3 ratio = vec3(
        vanillaTint.r > 0.001 ? ourTint.r / vanillaTint.r : 1.0,
        vanillaTint.g > 0.001 ? ourTint.g / vanillaTint.g : 1.0,
        vanillaTint.b > 0.001 ? ourTint.b / vanillaTint.b : 1.0
    );
    vec3 retintedBlock = blockContrib * ratio;
    vec3 finalLight = skyLm.rgb + retintedBlock;

    vertexColor = Color * vec4(finalLight, fullLm.a);
    texCoord0 = UV0;
}
