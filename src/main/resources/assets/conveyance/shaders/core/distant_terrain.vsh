#version 150

#moj_import <minecraft:light.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform vec3 TileOffset;
uniform sampler2D Sampler2;
out vec2 texCoord0;
out vec4 vertexColor;
out float vertexDistance;
out vec3 localPosition;
flat out int textureIndex;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    int metadata = int(round(Color.a * 255.0));
    vertexColor = vec4(Color.rgb, 1.0) * minecraft_sample_lightmap(Sampler2, ivec2(0, (metadata >> 4) * 16));
    textureIndex = metadata & 15;
    vec3 relativePosition = Position + TileOffset;
    vertexDistance = FogShape == 0 ? length(relativePosition) : max(length(relativePosition.xz), abs(relativePosition.y));
    localPosition = Position;
}
