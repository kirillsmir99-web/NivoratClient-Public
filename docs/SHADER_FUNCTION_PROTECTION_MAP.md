# NivoratClient GLSL Shader Function Protection Map

## Executive Summary

- **Total Shader Files Scanned**: 163 (.fsh, .vsh, .glsl)
- **Total Mapped Functions / Shaders**: 345

### IP Level Breakdown
- CRITICAL_IP: 76 functions
- HIGH_IP: 114 functions
- NORMAL_IP: 155 functions

### Algorithmic Category Breakdown
- COLOR_SPACE_INTERPOLATION: 125 functions
- CONVOLUTION_BLUR_KERNEL: 57 functions
- MSDF_FONT_RASTERIZATION: 9 functions
- OPTICAL_FROST_DISPERSION: 67 functions
- SDF_GEOMETRICAL_MATH: 57 functions
- VECTOR_RENDER_UTILITY: 30 functions

## Protection Pipeline Architecture (Phase 8)

1. **Source Integrity**: Developer originals in src/main/resources/assets/ remain clean and readable for development and debugging.
2. **Safe Stripping & Compression**: protection.shaders.GlslShaderHardener performs:
   - Complete removal of single-line (//) and multi-line (/* ... */) comments.
   - Redundant whitespace collapsing.
   - Strict isolation and preservation of preprocessor directives (#version, #moj_import, #define, #extension) with dedicated newlines.
   - Exact identifier retention for all uniform, sampler2D, and input/output vertex/fragment attributes.
3. **Generation Target**: Generated minified assets are placed in uild/generated/protected-resources/assets/ and packaged into NivoratClient-Protected.jar.

## Complete Shader Function Catalog

| Shader File | Function Name | Signature | Category | IP Level | Uniforms | Samplers | Protection Strategy |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: | :--- |
| assets\nivorat\shaders\core\arc_divider.fsh | parabolaDistance | float parabolaDistance(vec2 p, float k, float halfW) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\arc_divider.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\baritone_path.fsh | main | void main() | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\baritone_path.vsh | main | void main() | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\batched_blur.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\batched_blur.vsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\checkbox.fsh | segmentDistance | float segmentDistance(vec2 p, vec2 a, vec2 b) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\checkbox.vsh | main | void main() | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\circle.fsh | main | void main() | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\circle.vsh | main | void main() | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\effect_icon.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\effect_icon.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\glass.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\glass.vsh | main | void main() | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\image.fsh | imageColor | vec4 imageColor(vec2 coord, vec4 topLeft, vec4 topRight, vec4 bottomRight, vec4 bottomLeft) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\image.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\item.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\item.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\kawase_down.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\kawase_down.vsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\kawase_up.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\line.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\line.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\msdf_shimmer.fsh | median | float median(float r, float g, float b) | MSDF_FONT_RASTERIZATION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\msdf_shimmer.fsh | screenPxRange | float screenPxRange() | MSDF_FONT_RASTERIZATION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\msdf_shimmer.vsh | main | void main() | MSDF_FONT_RASTERIZATION | CRITICAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\msdf_text.fsh | median | float median(float r, float g, float b) | MSDF_FONT_RASTERIZATION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\msdf_text.fsh | screenPxRange | float screenPxRange() | MSDF_FONT_RASTERIZATION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\msdf_text.vsh | main | void main() | MSDF_FONT_RASTERIZATION | CRITICAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\msdf_wave.fsh | median | float median(float r, float g, float b) | MSDF_FONT_RASTERIZATION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\msdf_wave.fsh | screenPxRange | float screenPxRange() | MSDF_FONT_RASTERIZATION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\msdf_wave.vsh | main | void main() | MSDF_FONT_RASTERIZATION | CRITICAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_360.fsh | outlineAlpha | float outlineAlpha(vec2 coord, vec2 size, vec4 radius, float thickness, float smoothness) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_360.fsh | contourAngleDegrees | float contourAngleDegrees(vec2 coord, vec2 size, vec4 radius, float thickness, float offsetDegrees) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_360.fsh | rangeFactor | float rangeFactor(float angle, vec4 range, float blendDegrees, out float spanT) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_360.fsh | color360 | vec4 color360(float angle, vec4 baseColor, int rangeOffset, int rangeCount, float blendDegrees) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_360.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_default.fsh | outlineColor | vec4 outlineColor(vec2 coord, vec4 topLeft, vec4 topRight, vec4 bottomRight, vec4 bottomLeft) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_default.fsh | outlineAlpha | float outlineAlpha(vec2 coord, vec2 size, vec4 radius, float thickness, float smoothness) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_default.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_glass.fsh | outlineAlpha | float outlineAlpha(vec2 coord, vec2 size, vec4 radius, float thickness, float smoothness) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\outline_glass.vsh | main | void main() | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\picker.fsh | hueRamp | vec3 hueRamp(float h) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\picker.fsh | roundedAlpha | float roundedAlpha(vec2 coord, vec2 size, float radius, float smoothness) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\picker.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_default.fsh | themeLayerCoverage | float themeLayerCoverage(vec4 wave, vec2 fragXY) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_default.fsh | themeWaveMix | void themeWaveMix(vec2 fragXY) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_default.fsh | themeBlend | vec3 themeBlend(vec3 base, vec3 a, vec3 b, vec3 c, vec3 d, vec3 e, vec3 f) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_default.fsh | paletteColorAt | vec3 paletteColorAt(int idx) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_default.fsh | paletteRamp | vec3 paletteRamp(float t) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_default.fsh | paletteLoop | vec3 paletteLoop(float t) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_default.fsh | roundedAlpha | float roundedAlpha(vec2 coord, vec2 size, vec4 radius, float smoothness) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_default.fsh | rectangleColor | vec4 rectangleColor(vec2 coord, vec4 topLeft, vec4 topRight, vec4 bottomRight, vec4 bottomLeft) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_default.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_half_icon.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_half_icon.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_halftone.fsh | rectangleColor | vec4 rectangleColor(vec2 coord, vec4 topLeft, vec4 topRight, vec4 bottomRight, vec4 bottomLeft) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_halftone.fsh | hash21 | float hash21(vec2 p) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_halftone.fsh | squareMosaic | vec3 squareMosaic(vec2 cell, vec3 baseRgb) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\rect_halftone.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\ripple.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\ripple.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\shimmer.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\shimmer.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\terrain_wave.vsh | minecraft_sample_lightmap | vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) | SDF_GEOMETRICAL_MATH | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\terrain_wave.vsh | nv_raw_wave | vec3 nv_raw_wave(vec3 p, float wind) | SDF_GEOMETRICAL_MATH | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\text_atlas.fsh | sampledCoverage | float sampledCoverage(vec2 uv) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\text_atlas.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\text_atlas_fade.fsh | sampledCoverage | float sampledCoverage(vec2 uv) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\text_atlas_fade.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\underhand_rect.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\underhand_rect.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\world_border_fade.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\world_border_fade.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\zippy.fsh | stanh | vec2 stanh(vec2 value) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\core\zippy.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\frag_effect_scan\frag_effect_scan.fsh | scanlines | float scanlines() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\frag_effect_scan\frag_effect_scan.fsh | hash12 | float hash12(vec2 value) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\frag_effect_scan\frag_effect_scan.fsh | surfaceProjection | vec2 surfaceProjection(vec3 worldPosition) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\frag_effect_scan\frag_effect_scan.fsh | reconstructWorldPos | vec3 reconstructWorldPos(float depth) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\frag_effect_scan\frag_effect_scan.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\composite.fsh | quickTrailAlpha | float quickTrailAlpha(vec2 uv, vec2 blurPx) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 6 | 6 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\composite.fsh | blurredTrailColor | vec4 blurredTrailColor(vec2 uv, vec2 blurPx) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 6 | 6 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\composite.fsh | handDepthMaskAt | float handDepthMaskAt(vec2 uv, float irisDepthMode) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 6 | 6 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\fullscreen.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\restore.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | handGradUV | vec2 handGradUV(vec2 p) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | gradientColor | vec4 gradientColor(vec2 pos) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | hash12 | float hash12(vec2 p) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | noise | float noise(vec2 p) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | fbm3 | float fbm3(vec2 p) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | fbm2 | float fbm2(vec2 p) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | handDepthMaskAt | float handDepthMaskAt(vec2 uv, float irisDepthMode) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | rawHandMaskColor | float rawHandMaskColor(vec2 uv, float itemOnly, float irisDepthMode, out vec3 outColor) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | nearbyDepthMaskFast | float nearbyDepthMaskFast(vec2 uv, vec2 px, float radiusPx, float irisDepthMode) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | frameDt | float frameDt() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | hintFade | float hintFade() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | previousTrailHint | float previousTrailHint(vec2 uv, vec2 histUv, vec2 px, float wobble) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | previousTrailColor | vec4 previousTrailColor(vec2 histUv, vec2 softPx) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_flame\trail.fsh | computeFlameField | void computeFlameField(vec2 uv, vec2 px, float radiusPx, float prevAlpha, vec3 prevColor, float itemOnly, float irisDepthMode,
                       float centerMask, vec3 centerColor,
                       out float envelope, out float currentMask, out vec3 itemColor) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 7 | 7 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_hologram\fullscreen.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_hologram\hologram.fsh | hash11 | float hash11(float p) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_hologram\hologram.fsh | hash12 | float hash12(vec2 p) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_hologram\hologram.fsh | handDepthMaskAt | float handDepthMaskAt(vec2 uv, float depthCompareMode) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_hologram\hologram.fsh | handColorMaskAt | float handColorMaskAt(vec2 uv) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_hologram\hologram.fsh | handMaskAt | float handMaskAt(vec2 uv, float depthCompareMode) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_hologram\hologram.fsh | nearHandProbe | float nearHandProbe(vec2 uv, vec2 offset, float depthCompareMode) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\effects\hands_hologram\hologram.fsh | dilatedMask | float dilatedMask(vec2 uv, vec2 offset, float depthCompareMode) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\frag_effect_scan_fragment.fsh | scanlines | float scanlines() | SDF_GEOMETRICAL_MATH | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\frag_effect_scan_fragment.fsh | reconstructWorldPos | vec3 reconstructWorldPos(float depth) | SDF_GEOMETRICAL_MATH | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\frag_effect_scan_vertex.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\common.glsl | rdist | float rdist(vec2 pos, vec2 halfSize, vec4 radius) | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\common.glsl | ralpha | float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\common.glsl | rvertexcoord | vec2 rvertexcoord(int id) | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\releon_common.glsl | rdist | float rdist(vec2 pos, vec2 size, vec4 radius) | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\releon_common.glsl | ralpha | float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\releon_common.glsl | rsmin | float rsmin(float a, float b, float k) | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\releon_common.glsl | rvertexcoord | vec2 rvertexcoord(int id) | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvScreenSize | vec2 nvScreenSize() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvGuiScale | float nvGuiScale() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientStopCount | int nvClientStopCount() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientPhase | float nvClientPhase() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientStyleId | float nvClientStyleId() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientSweep | float nvClientSweep() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientPrevStyleId | float nvClientPrevStyleId() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientClosed | float nvClientClosed() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientScrollPhase | float nvClientScrollPhase() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvWaveActive | bool nvWaveActive() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvFragXYFromUV | vec2 nvFragXYFromUV(vec2 uv) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvFragXYMapped | vec2 nvFragXYMapped(vec2 local, vec4 map) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvFragXYMappedFlipY | vec2 nvFragXYMappedFlipY(vec2 local, vec4 map) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvThemeLayerCoverage | float nvThemeLayerCoverage(int layer, vec2 fragXY) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvThemeCoverageCache | void nvThemeCoverageCache(vec2 fragXY) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvThemeSlot | vec3 nvThemeSlot(int slot, vec2 fragXY) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientPrimary | vec3 nvClientPrimary(vec2 fragXY) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientSecondary | vec3 nvClientSecondary(vec2 fragXY) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientStop | vec3 nvClientStop(int index, vec2 fragXY) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientPaletteColor | vec3 nvClientPaletteColor(float t, vec2 fragXY) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\include\theme_wave.glsl | nvClientPaletteLoop | vec3 nvClientPaletteLoop(float t, vec2 fragXY) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\aurora.fsh | hsv2rgb | vec3 hsv2rgb(vec3 c) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\aurora.fsh | nmzHash33 | vec3 nmzHash33(vec3 q) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\aurora.fsh | hash21 | float hash21(vec2 p) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\aurora.fsh | vnoise | float vnoise(vec2 p) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\aurora.fsh | fbm | float fbm(vec2 p) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\aurora.fsh | starField | vec3 starField(vec3 dir, float time, float density, float twinkle) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\aurora.fsh | skyTint | vec3 skyTint(float axis, float t, float time) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | hsv2rgb | vec3 hsv2rgb(vec3 c) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | nmzHash33 | vec3 nmzHash33(vec3 q) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | hash21 | float hash21(vec2 p) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | noise | float noise(vec3 x) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | discCoord | vec3 discCoord(float ang, float rad, float freq) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | octave | float octave(float ang, float rad, float freq, float aa) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | pcurve | float pcurve(float x, float a, float b) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | sdTorus | float sdTorus(vec3 p, vec2 t) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | encodeHDR | vec3 encodeHDR(vec3 c) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | starField | vec3 starField(vec3 dir, float time, float pixAngle) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | skyTint | vec3 skyTint(float axis, float t, float time) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | Haze | void Haze(inout vec3 color, vec3 pos, float alpha, vec3 mainColor) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | GasDisc | void GasDisc(inout vec3 color, inout float alpha, vec3 pos, float time, vec3 mainColor, float aa, vec3 eyevec) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | WarpSpace | void WarpSpace(inout vec3 eyevec, vec3 raypos) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | rayDir | vec3 rayDir(vec2 uv) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | toLocal | vec3 toLocal(vec3 dir, vec3 eX, vec3 eY, vec3 eZ) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\blackhole.fsh | sampleHistory | vec3 sampleHistory(vec2 uv, vec2 res) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\bloom_down.fsh | decodeHDR | vec3 decodeHDR(vec3 c) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\bloom_down.fsh | encodeHDR | vec3 encodeHDR(vec3 c) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\bloom_down.fsh | fetch | vec3 fetch(vec2 uv, float hdr) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\composite.fsh | nmzHash33 | vec3 nmzHash33(vec3 q) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 9 | 9 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\composite.fsh | bhStars | vec3 bhStars(vec3 dir, float time) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 9 | 9 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\composite.fsh | decodeHDR | vec3 decodeHDR(vec3 c) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 9 | 9 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\composite.fsh | cubic | vec4 cubic(float x) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 9 | 9 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\composite.fsh | bicubic | vec3 bicubic(sampler2D tex, vec2 uv) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 9 | 9 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\composite.fsh | blackHole | vec3 blackHole() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 9 | 9 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\customsky.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\starfall.fsh | hsv2rgb | vec3 hsv2rgb(vec3 c) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\starfall.fsh | nmzHash33 | vec3 nmzHash33(vec3 q) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\starfall.fsh | hash21 | float hash21(vec2 p) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\starfall.fsh | vnoise | float vnoise(vec2 p) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\starfall.fsh | fbm | float fbm(vec2 p) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\starfall.fsh | skyTint | vec3 skyTint(float axis, float t, float time) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\starfall.fsh | starField | vec3 starField(vec3 dir, float time) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\customsky\starfall.fsh | meteors | vec3 meteors(vec3 rd, float time, vec3 themeTint) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\fogblur\blur.fsh | sampleScene | vec3 sampleScene(vec2 uv, vec2 axisStep) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\fogblur\composite.fsh | linearizeDepth | float linearizeDepth(float depth, float nearPlane, float farPlane) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 3 | 3 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\fogblur\composite.fsh | fogFor | float fogFor(float depth) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 3 | 3 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\fogblur\fogblur.vsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\fogblur\kawase.vsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glassvapor\glassvapor.fsh | linearizeDepth | float linearizeDepth(float d, float near, float far) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glassvapor\glassvapor.fsh | themePaletteFade | vec3 themePaletteFade(float t) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glassvapor\glassvapor.fsh | sn_permute | vec4 sn_permute(vec4 x) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glassvapor\glassvapor.fsh | sn_taylorInvSqrt | vec4 sn_taylorInvSqrt(vec4 r) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glassvapor\glassvapor.fsh | snoise | float snoise(vec3 v) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glowesp\composite.fsh | gradientOf | vec4 gradientOf(vec2 p, float blend, vec4 c1, vec4 c2, vec4 c3, vec4 c4) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glowesp\composite.fsh | themeOutlineGradient | vec4 themeOutlineGradient(vec2 p, float blend) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glowesp\composite.fsh | linearize | float linearize(float d, float near, float far) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glowesp\composite.fsh | sampleOccluded | float sampleOccluded(vec2 p, float near, float far, float bias, float slope, vec2 texel, float searchRadius) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glowesp\composite.fsh | computeOutline | float computeOutline(vec2 texel, float outlineWidth, bool renderOutline) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glowesp\gauss.fsh | getGradientColor | vec4 getGradientColor(vec2 p, float blend) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glowesp\glowesp.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glowesp\occlude.fsh | linearize | float linearize(float d, float near, float far) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\glowesp\solidify.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\composite.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\composite_world_occluded.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 3 | 3 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\fullscreen.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\gaussian.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\shard.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\shard.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\shard_occluded.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 3 | 3 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\slot_blit.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\world_backdrop.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\world_backdrop_slots.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guilayerblur\world_quad.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guimotionblur\composite.fsh | roundedRectMask | float roundedRectMask(vec2 point, vec4 rect, float radius, float feather) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guimotionblur\fullscreen.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guimotionblur\gaussian.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guimotionblur\mask.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\guimotionblur\mask.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\hitbubbles\hitbubbles.fsh | colorAt | vec3 colorAt(int i) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\hitbubbles\hitbubbles.fsh | ringColorByAngle | vec3 ringColorByAngle(float ang) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\hitbubbles\hitbubbles.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\hpfocus\copy.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\hpfocus\copy.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\itemoutline\blur.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\itemoutline\composite.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\itemoutline\dt_h.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\itemoutline\dt_v.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\itemoutline\isoline.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\itemoutline\itemoutline.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\itemoutline\sobel.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\jumpdistort\jumpdistort.fsh | worldFromDepth | vec3 worldFromDepth(vec2 uv, float depth) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\jumpdistort\jumpdistort.fsh | ringColorByAngle | vec3 ringColorByAngle(int i, float ang) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\jumpdistort\jumpdistort.fsh | ringColor | vec3 ringColor(int i, vec2 dir) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\jumpdistort\jumpdistort.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\killdistortion\killdistortion.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\killdistortion\killdistortion.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\liquidpanel\liquidpanel.fsh | sn_permute | vec4 sn_permute(vec4 x) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\liquidpanel\liquidpanel.fsh | sn_taylorInvSqrt | vec4 sn_taylorInvSqrt(vec4 r) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\liquidpanel\liquidpanel.fsh | snoise | float snoise(vec3 v) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\saturation\saturation.fsh | adjustSaturation | vec3 adjustSaturation(vec3 color, float saturationValue) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\saturation\saturation.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\scarglass\scarglass.fsh | linearizeDepth | float linearizeDepth(float d, float near, float far) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\scarglass\scarglass.fsh | segDist | float segDist(vec2 p, vec2 a, vec2 b, float aspect, out float tOut, out vec2 perpDir) | SDF_GEOMETRICAL_MATH | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\bounds_init.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\bounds_reduce.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\bounds_write.fsh | main | void main() | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | handGradUV | vec2 handGradUV(vec2 p) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | themeLayerCoverage | float themeLayerCoverage(vec4 wave, vec2 fragXY) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | themeWaveMix | void themeWaveMix(vec2 fragXY) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | themeBlend | vec3 themeBlend(vec3 base, vec3 a, vec3 b, vec3 c, vec3 d, vec3 e, vec3 f) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | paletteColorAt | vec3 paletteColorAt(int idx) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | paletteRamp | vec3 paletteRamp(float t) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | paletteLoop | vec3 paletteLoop(float t) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | boxGradient | vec3 boxGradient(vec2 uv, float phase) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | meshGradient | vec3 meshGradient(vec2 uv, float phase, float aspect) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | selectStyle | vec3 selectStyle(float id, vec3 ramp, vec3 mesh, vec3 box) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | sn_permute | vec4 sn_permute(vec4 x) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | sn_taylorInvSqrt | vec4 sn_taylorInvSqrt(vec4 r) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glass.fsh | snoise | float snoise(vec3 v) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 4 | 4 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glow_dilate.fsh | handGradUV | vec2 handGradUV(vec2 p) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 3 | 3 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glow_dilate.fsh | gradientColor | vec4 gradientColor(vec2 pos) | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 3 | 3 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glow_gauss.fsh | handGradUV | vec2 handGradUV(vec2 p) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 3 | 3 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glow_gauss.fsh | themeCorner | vec4 themeCorner(float t, vec2 fragXY) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 3 | 3 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\glow_gauss.fsh | gradientColor | vec4 gradientColor(vec2 pos) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 3 | 3 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\handmask.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\light_kawase_down.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\light_kawase_up.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\outline.fsh | handGradUV | vec2 handGradUV(vec2 p) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\outline.fsh | themeCorner | vec4 themeCorner(float t, vec2 fragXY) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\outline.fsh | gradientColor | vec4 gradientColor(vec2 pos) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\outline.fsh | innerEdge | float innerEdge(float centerMask) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\outline.fsh | outlineBand | float outlineBand() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 5 | 5 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\passthrough.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\shaderhands\shaderhands.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\targetcircle\composite.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\targetcircle\fullscreen.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\targetcircle\threshold.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\themeshock\themeshock.fsh | easeInOutSine | float easeInOutSine(float t) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\themeshock\themeshock.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\trailglass\trailglass.fsh | main | void main() | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\post\trailglass\trailglass.vsh | main | void main() | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\batched_blur\batched_blur.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\batched_blur\batched_blur.vsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | hash4 | vec4 hash4(vec2 p) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | computeShards | ShardInfo computeShards(vec2 p, vec2 scale, float animTime, float mosaicMorph) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | themeLayerCoverage | float themeLayerCoverage(int base, vec4 wave, vec2 fragXY) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | themeWaveMix | void themeWaveMix(int base, vec2 fragXY) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | paletteColorAt | vec3 paletteColorAt(int base, int idx) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | paletteRamp | vec3 paletteRamp(int base, float t) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | paletteLoop | vec3 paletteLoop(int base, float t) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | boxGradient | vec3 boxGradient(int base, vec2 uv, float phase) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | selectStyle | vec3 selectStyle(float id, vec3 ramp, vec3 mesh, vec3 box) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | meshGradient | vec3 meshGradient(int base, vec2 uv, float phase, float aspect) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | gradientBeam | float gradientBeam(vec2 uv, float sweep) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.fsh | hue2rgb | vec3 hue2rgb(float h) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glass\glass.vsh | main | void main() | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_blur.vsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_blur_down.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_blur_up.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_composite.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_composite.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_cutout.fsh | main | void main() | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 2 | 2 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | glowPaletteColorAt | vec3 glowPaletteColorAt(int idx) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | glowPaletteRamp | vec3 glowPaletteRamp(float t) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | glowPaletteLoop | vec3 glowPaletteLoop(float t) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | glowBox | vec3 glowBox(vec2 uv, float phase) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | glowSelect | vec3 glowSelect(float id, vec3 ramp, vec3 mesh, vec3 box) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | glowMesh | vec3 glowMesh(vec2 uv, float phase, float aspect) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | getSegment_glow | vec4 getSegment_glow(int i) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | roundedBoxSDF_glow | float roundedBoxSDF_glow(vec2 center, vec2 halfSize, vec4 r) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | resolveSegmentRadius_glow | vec4 resolveSegmentRadius_glow(int index, vec4 seg, int count, float r,
                               float leftAligned, float bottomAnchored) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | boxesSDF | float boxesSDF(vec2 p, float spanCountF, float innerRadius, out vec2 boxUV) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.fsh | staircaseSDF | float staircaseSDF(vec2 p, float spanCountF, float innerRadius,
                   float leftAligned, float bottomAnchored) | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_shape.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\glow\glow_source.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\kawase\down.fsh | sampleSource | vec4 sampleSource(vec2 coord) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\kawase\down.vsh | main | void main() | VECTOR_RENDER_UTILITY | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\kawase\up.fsh | sampleSource | vec4 sampleSource(vec2 coord) | CONVOLUTION_BLUR_KERNEL | HIGH_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.fsh | themeLayerCoverage | float themeLayerCoverage(vec4 wave, vec2 fragXY) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.fsh | themeWaveMix | void themeWaveMix(vec2 fragXY) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.fsh | themeBlend | vec3 themeBlend(vec3 base, vec3 a, vec3 b, vec3 c, vec3 d, vec3 e, vec3 f) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.fsh | paletteColorAt | vec3 paletteColorAt(int idx) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.fsh | paletteRamp | vec3 paletteRamp(float t) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.fsh | paletteLoop | vec3 paletteLoop(float t) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.fsh | boxGradient | vec3 boxGradient(vec2 uv, float phase) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.fsh | meshGradient | vec3 meshGradient(vec2 uv, float phase, float aspect) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.fsh | selectStyle | vec3 selectStyle(float id, vec3 ramp, vec3 mesh, vec3 box) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\radialglass\radialglass.vsh | main | void main() | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\sectormask\sectormask.fsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\sectormask\sectormask.vsh | main | void main() | COLOR_SPACE_INTERPOLATION | NORMAL_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | themeLayerCoverage | float themeLayerCoverage(vec4 wave, vec2 fragXY) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | themeWaveMix | void themeWaveMix(vec2 fragXY) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | themeBlend | vec3 themeBlend(vec3 base, vec3 a, vec3 b, vec3 c, vec3 d, vec3 e, vec3 f) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | paletteColorAt | vec3 paletteColorAt(int idx) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | paletteRamp | vec3 paletteRamp(float t) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | paletteLoop | vec3 paletteLoop(float t) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | boxGradient | vec3 boxGradient(vec2 uv, float phase) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | selectStyle | vec3 selectStyle(float id, vec3 ramp, vec3 mesh, vec3 box) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | meshGradient | vec3 meshGradient(vec2 uv, float phase, float aspect) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | gradientBeam | float gradientBeam(vec2 uv, float sweep) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | hue2rgb | vec3 hue2rgb(float h) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | rainbowColor | vec3 rainbowColor(float along, float phase, float spread, float sat) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | getSegment | vec4 getSegment(int base, int i) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | roundedBoxSDF | float roundedBoxSDF(vec2 center, vec2 halfSize, vec4 r) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | resolveSegmentRadius | vec4 resolveSegmentRadius(int index, vec4 seg, int count, float r,
                          float leftAligned, float bottomAnchored, int base) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.fsh | shapeSDF | float shapeSDF(vec2 p, int base, float spanCount, float innerRadius,
               float leftAligned, float bottomAnchored) | OPTICAL_FROST_DISPERSION | CRITICAL_IP | 1 | 1 | Safe Minify + Strip Comments + Preserve Directives |
| assets\nivorat\shaders\ui\shape\shape.vsh | main | void main() | SDF_GEOMETRICAL_MATH | HIGH_IP | 0 | 0 | Safe Minify + Strip Comments + Preserve Directives |
