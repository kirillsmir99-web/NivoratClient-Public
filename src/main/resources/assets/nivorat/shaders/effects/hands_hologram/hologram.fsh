#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D BeforeSampler;
uniform sampler2D BackgroundSampler;
uniform sampler2D DepthSampler;
uniform sampler2D NoHandDepthSampler;

layout(std140) uniform HandsHologramData {
    vec4 holoColor;
    vec4 params0;
    vec4 params1;
    vec4 screen;
};

float hash11(float p) {
    return fract(sin(p * 127.1) * 43758.5453123);
}

float hash12(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float handDepthMaskAt(vec2 uv, float depthCompareMode) {
    if (depthCompareMode < 0.5) {
        return 0.0;
    }
    float depth = texture(DepthSampler, uv).r;
    if (depth >= 0.9999) {
        return 0.0;
    }
    float noHandDepth = texture(NoHandDepthSampler, uv).r;
    if (noHandDepth < 0.0001) {
        return 0.0;
    }
    return depth < noHandDepth - 0.00002 ? 1.0 : 0.0;
}

float handColorMaskAt(vec2 uv) {
    vec4 beforeColor = texture(BeforeSampler, uv);
    vec4 afterColor = texture(SceneSampler, uv);
    vec3 delta = abs(afterColor.rgb - beforeColor.rgb);
    float peak = max(max(delta.r, delta.g), delta.b);
    float luma = dot(delta, vec3(0.299, 0.587, 0.114));
    float value = peak * 0.78 + luma * 0.88 + abs(afterColor.a - beforeColor.a);
    return smoothstep(0.004, 0.060, value);
}

float handMaskAt(vec2 uv, float depthCompareMode) {
    return max(handDepthMaskAt(uv, depthCompareMode), handColorMaskAt(uv));
}

float nearHandProbe(vec2 uv, vec2 offset, float depthCompareMode) {
    float mask = handDepthMaskAt(clamp(uv + vec2(offset.x, 0.0), vec2(0.0), vec2(1.0)), depthCompareMode);
    mask = max(mask, handDepthMaskAt(clamp(uv - vec2(offset.x, 0.0), vec2(0.0), vec2(1.0)), depthCompareMode));
    mask = max(mask, handDepthMaskAt(clamp(uv + vec2(0.0, offset.y), vec2(0.0), vec2(1.0)), depthCompareMode));
    mask = max(mask, handDepthMaskAt(clamp(uv - vec2(0.0, offset.y), vec2(0.0), vec2(1.0)), depthCompareMode));
    return mask;
}

float dilatedMask(vec2 uv, vec2 offset, float depthCompareMode) {
    vec2 diagonal = offset * 0.70710678;
    float mask = handMaskAt(clamp(uv + vec2(offset.x, 0.0), vec2(0.0), vec2(1.0)), depthCompareMode);
    mask = max(mask, handMaskAt(clamp(uv - vec2(offset.x, 0.0), vec2(0.0), vec2(1.0)), depthCompareMode));
    mask = max(mask, handMaskAt(clamp(uv + vec2(0.0, offset.y), vec2(0.0), vec2(1.0)), depthCompareMode));
    mask = max(mask, handMaskAt(clamp(uv - vec2(0.0, offset.y), vec2(0.0), vec2(1.0)), depthCompareMode));
    mask = max(mask, handMaskAt(clamp(uv + diagonal, vec2(0.0), vec2(1.0)), depthCompareMode));
    mask = max(mask, handMaskAt(clamp(uv - diagonal, vec2(0.0), vec2(1.0)), depthCompareMode));
    mask = max(mask, handMaskAt(clamp(uv + vec2(diagonal.x, -diagonal.y), vec2(0.0), vec2(1.0)), depthCompareMode));
    mask = max(mask, handMaskAt(clamp(uv + vec2(-diagonal.x, diagonal.y), vec2(0.0), vec2(1.0)), depthCompareMode));
    return mask;
}

void main() {
    float time = params0.x;
    float scanDensity = params0.y;
    float glitch = params0.z;
    float glow = params0.w;
    float depthCompareMode = params1.x;
    float scanSpeed = params1.y;
    float flicker = params1.z;
    float transparency = params1.w;
    float opacity = holoColor.a;
    vec2 px = screen.zw;

    vec3 background = texture(BackgroundSampler, texCoord).rgb;

    float bandHeight = 26.0;
    float band = floor(gl_FragCoord.y / bandHeight);
    float bandSeed = hash11(band + floor(time * 7.0) * 13.0);
    float bandActive = step(1.0 - glitch * 0.34, bandSeed);
    float bandShift = (hash11(band * 3.7 + floor(time * 7.0)) - 0.5) * glitch * 26.0 * px.x * bandActive;

    vec2 handUv = clamp(texCoord + vec2(bandShift, 0.0), vec2(0.0), vec2(1.0));

    vec2 rimOffset = px * (1.6 + glow * 2.2);
    vec2 glowOffset = px * (3.4 + glow * 6.5);

    float mask = handMaskAt(handUv, depthCompareMode);

    if (mask <= 0.004 && nearHandProbe(handUv, glowOffset, depthCompareMode) <= 0.004) {
        fragColor = vec4(background, 1.0);
        return;
    }

    float rimMask = dilatedMask(handUv, rimOffset, depthCompareMode);
    float glowMask = dilatedMask(handUv, glowOffset, depthCompareMode);

    float splitPx = (0.9 + glitch * 3.6 + bandActive * glitch * 5.0);
    vec3 handColor;
    handColor.r = texture(SceneSampler, clamp(handUv + vec2(splitPx * px.x, 0.0), vec2(0.0), vec2(1.0))).r;
    handColor.g = texture(SceneSampler, handUv).g;
    handColor.b = texture(SceneSampler, clamp(handUv - vec2(splitPx * px.x, 0.0), vec2(0.0), vec2(1.0))).b;

    float shade = dot(handColor, vec3(0.299, 0.587, 0.114));
    shade = pow(clamp(shade, 0.0, 1.0), 0.78);

    float scanPhase = (gl_FragCoord.y - time * scanSpeed * 60.0) * scanDensity * 0.5;
    float scan = 0.5 + 0.5 * sin(scanPhase);
    scan = mix(1.0, 0.34 + 0.66 * scan, 0.72 * clamp(scanDensity, 0.0, 1.0));
    float sweep = smoothstep(0.0, 1.0, fract((gl_FragCoord.y * px.y) * 0.9 - time * 0.28));
    float bar = 1.0 + 0.30 * exp(-pow((sweep - 0.5) * 7.0, 2.0));

    float flick = 1.0 - flicker * 0.35 * hash11(floor(time * 24.0));
    float grain = 1.0 - 0.10 * hash12(gl_FragCoord.xy + floor(time * 24.0));

    vec3 tint = holoColor.rgb;
    vec3 body = tint * (0.22 + shade * 1.15) * scan * bar * flick * grain;

    float edge = clamp(rimMask - mask, 0.0, 1.0);
    float outer = clamp(glowMask - rimMask, 0.0, 1.0);
    vec3 rimColor = mix(tint, vec3(1.0), 0.45);
    float edgeStrength = smoothstep(0.05, 0.65, edge) * (0.65 + glow * 0.75) * flick;
    float outerStrength = smoothstep(0.02, 0.55, outer) * glow * 0.55 * flick;

    vec3 behind = texture(BeforeSampler, texCoord).rgb;
    vec3 solid = mix(handColor, body, opacity);
    vec3 ghost = behind + solid * 0.85;
    vec3 color = mix(background, mix(solid, ghost, transparency), clamp(mask, 0.0, 1.0));

    color += rimColor * edgeStrength;
    color += tint * outerStrength;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
