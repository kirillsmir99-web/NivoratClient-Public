#version 150

uniform sampler2D DistTex;

layout(std140) uniform IsoConfig {
    float MaxDist;
    float Thickness;
    float Alpha;
    float Phase;
    vec4 Levels;
};

in vec2 texCoord;
out vec4 fragColor;

const float TAU = 6.2831853;

void main() {
    float d = texture(DistTex, texCoord).r * MaxDist;
    float halfT = Thickness * 0.5;
    float aa = clamp(fwidth(d), 0.6, 2.0);
    float outA = 0.0;
    for (int k = 0; k < 3; k++) {
        float dd = abs(d - Levels[k]);
        float lineK = clamp((halfT - dd) / aa + 0.5, 0.0, 1.0);
        float waveK = 0.5 + 0.5 * sin((Phase - float(k) / 3.0) * TAU);
        outA = max(outA, lineK * waveK);
    }
    float a = outA * Alpha;
    if (a <= 0.002) {
        discard;
    }
    fragColor = vec4(1.0, 1.0, 1.0, a);
}
