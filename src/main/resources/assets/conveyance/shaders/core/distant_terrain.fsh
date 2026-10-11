#version 150

#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec4 ShadeFactors;
uniform vec3 WaterTint;
uniform vec3 TileOffset;
uniform vec2 CoverageOrigin;
uniform float CoverageSize;
uniform float SpriteU0[13];
uniform float SpriteV0[13];
uniform float SpriteU1[13];
uniform float SpriteV1[13];
in vec2 texCoord0;
in vec4 vertexColor;
in float vertexDistance;
in vec3 localPosition;
flat in int textureIndex;
out vec4 fragColor;

void main() {
    vec2 textureDx = dFdx(texCoord0), textureDy = dFdy(texCoord0);
    // Sample just inside the owning solid, including faces exactly on chunk boundaries.
    vec3 normal = normalize(cross(dFdx(localPosition), dFdy(localPosition)));
    if (!gl_FrontFacing) normal = -normal;
    vec3 owner = localPosition - normal * 0.002;
    vec2 chunk = floor((owner.xz + TileOffset.xz - CoverageOrigin) / 16.0);
    int section = clamp(int(floor((owner.y - 0.0001) / 16.0)) + 2, 0, 3);
    if (all(greaterThanEqual(chunk, vec2(0.0))) && all(lessThan(chunk, vec2(CoverageSize)))) {
        vec4 ready = texture(Sampler1, (chunk + 0.5) / CoverageSize);
        if (ready[section] > 0.5) discard;
    }
    int material = clamp(textureIndex, 0, 12);
    vec2 low = vec2(SpriteU0[material], SpriteV0[material]);
    vec2 span = vec2(SpriteU1[material], SpriteV1[material]) - low;
    vec2 wrapped = clamp(fract(texCoord0), vec2(0.001), vec2(0.999));
    // Derivatives of unwrapped coordinates preserve mipmaps across every repeat.
    vec4 color = textureGrad(Sampler0, low + wrapped * span, textureDx * span, textureDy * span) * vertexColor * ColorModulator;
    if (color.a < 0.1) discard;
    float shade = abs(normal.y) > 0.5 ? (normal.y > 0.0 ? ShadeFactors.z : ShadeFactors.w)
                                     : (abs(normal.x) > 0.5 ? ShadeFactors.x : ShadeFactors.y);
    color.rgb *= shade;
    if (material == 5) color.rgb *= WaterTint;
    else color.a = 1.0;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
