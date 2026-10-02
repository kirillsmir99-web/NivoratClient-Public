#version 150

uniform sampler2D Scene;

layout(std140) uniform Ripples {
    vec4 header;
    vec4 header2;
    vec4 header3;
    vec4 header4;
    vec4 header5;
    vec4 header6;
    vec4 data[32];

};

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265;
const float TAU = 6.28318531;

vec3 colorAt(int i) {
    if (i == 0) return header3.xyz;
    if (i == 1) return header4.xyz;
    if (i == 2) return header5.xyz;
    return header6.xyz;
}

vec3 ringColorByAngle(float ang) {
    float t = (ang < 0.0 ? ang + TAU : ang) / TAU * 4.0;
    int seg = int(floor(t)) & 3;
    int segn = (seg + 1) & 3;
    return mix(colorAt(seg), colorAt(segn), fract(t));
}

void main() {
    vec2 uv = texCoord;
    int count = int(header.x + 0.5);
    float aspect = header.y;
    float time = header.z;
    float warpAmp = header.w;
    float satFactor = header2.x;
    float tintStrength = header2.y;

    vec2 offset = vec2(0.0);
    float ringInfluence = 0.0;
    float refractMag = 0.0;
    vec3 emitGlow = vec3(0.0);
    for (int i = 0; i < count; i++) {
        vec4 c = data[i * 2];
        float amp = data[i * 2 + 1].x;
        float env = data[i * 2 + 1].y;
        float footprint = data[i * 2 + 1].z;

        vec2 d = uv - c.xy;
        d.x *= aspect;
        float dist = length(d);
        float ang = atan(d.y, d.x);

        float wob = sin(ang * 4.0 + time * 1.7) * 0.6 + sin(ang * 7.0 - time * 1.1) * 0.4;
        float ringR = c.z * (1.0 + 0.02 * wob);

        float w = (dist - ringR) / max(c.w, 1e-4);

        float ringBand = (1.0 - smoothstep(0.0, 1.5, abs(w))) * env;
        ringInfluence = max(ringInfluence, ringBand);
        if (abs(w) < 1.0) {
            float wave = sin(w * PI) * (1.0 - abs(w));
            vec2 dir = dist > 1e-5 ? d / dist : vec2(0.0);
            dir.x /= aspect;
            offset += dir * wave * amp;
            refractMag = max(refractMag, abs(wave) * env);

            if (tintStrength > 0.001) {
                float progress = clamp(c.z / max(footprint, 1e-4), 0.0, 1.0);
                float life = 1.0 - progress;
                float spread = max(c.w, 1e-4);

                float core = exp(-(w * w) / 0.02);
                float flanks = abs(wave);

                float endPhase = smoothstep(0.5, 1.0, progress);
                float bands = 0.5 + 0.5 * sin(ang * 8.0 - time * 3.0 + w * 6.0);
                float dissolve = mix(1.0, bands, endPhase);
                float sideFade = (w < 0.0) ? pow(life, 0.7) : pow(life, 1.4);
                float fadeIn = smoothstep(0.0, 0.12, progress);

                float intensity = (core * 1.3 * pow(life, 0.95) + flanks * 1.0 * sideFade * dissolve) * fadeIn;
                emitGlow = max(emitGlow, ringColorByAngle(ang) * intensity);
            }
        }

        if (warpAmp > 0.0 && ringBand > 0.001) {
            vec2 turb = vec2(
                sin(d.y * 16.0 + time * 3.0) + 0.5 * sin(d.y * 33.0 - time * 2.0),
                cos(d.x * 16.0 - time * 3.0) + 0.5 * cos(d.x * 33.0 + time * 2.0)
            );
            turb.x /= aspect;
            offset += turb * warpAmp * ringBand;
        }
    }

    float ca = 0.16 * refractMag * tintStrength;
    vec3 col;
    col.r = texture(Scene, uv + offset * (1.0 + ca)).r;
    col.g = texture(Scene, uv + offset).g;
    col.b = texture(Scene, uv + offset * (1.0 - ca)).b;

    if (abs(satFactor - 1.0) > 0.001 && ringInfluence > 0.001) {
        float f = mix(1.0, satFactor, ringInfluence);
        float lum = dot(col, vec3(0.299, 0.587, 0.114));
        col = max(mix(vec3(lum), col, f), vec3(0.0));
    }

    if (tintStrength > 0.001) {
        float lum = dot(col, vec3(0.299, 0.587, 0.114));
        col += emitGlow * tintStrength * (0.55 + 0.7 * lum);
    }

    fragColor = vec4(col, 1.0);
}
