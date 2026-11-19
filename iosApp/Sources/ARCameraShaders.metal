#include <metal_stdlib>
using namespace metal;

// Port of core/ar-native/src/main/cpp/opengl_renderer_jni.cpp.
// GLSL `texture(tex, uv)` -> sample(smp, uv); no mip maps, exactly like the GL textures.
// `gl_FragCoord` -> [[position]] (pixel centre, top-left origin, same convention as Canvas rects).

struct ARShaderUniforms {
    float4x4 texTransform;
    float width;
    float height;
    int x;
    int y;
    float cornerRadius;
    float lod;
    float minLod;
    float4 contrastingColor;
    float contrastingColorMix;
};

struct ARVertexOut {
    float4 position [[position]];
    float2 texCoord;
};

constant float kSigma = 3.0f;
constant float kRadius = kSigma * 2.0f;
constant float kInvTwoSigmaSqr = 1.0f / (2.0f * kSigma * kSigma);

// Full-screen triangle with UVs authored for a top-left origin (Metal texture convention).
vertex ARVertexOut arVertex(uint vid [[vertex_id]]) {
    const float2 positions[3] = { float2(-1.0, -1.0), float2(3.0, -1.0), float2(-1.0, 3.0) };
    ARVertexOut out;
    float2 p = positions[vid];
    out.position = float4(p, 0.0, 1.0);
    out.texCoord = float2((p.x + 1.0) * 0.5, (1.0 - p.y) * 0.5);
    return out;
}

static float2 transformedTexCoord(ARVertexOut in, constant ARShaderUniforms &u) {
    return (u.texTransform * float4(in.texCoord, 0.0, 1.0)).xy;
}

static float4 gaussBlur(texture2d<float> tex, sampler smp, float2 uv, float2 step) {
    float4 c = tex.sample(smp, uv);
    for (float i = 1.0; i < kRadius; i += 1.0) {
        c += (tex.sample(smp, uv + step * i) + tex.sample(smp, uv - step * i)) *
             exp(-i * i * kInvTwoSigmaSqr);
    }
    return c / c.a;
}

static float4 applyContrast(float3 rgb, float alpha, constant ARShaderUniforms &u) {
    // Android's sentinel: contrastingColor == (-1, -1, -1) disables the overlay mix.
    if (any(u.contrastingColor.rgb != float3(-1.0))) {
        return float4(mix(rgb, u.contrastingColor.rgb, u.contrastingColorMix), alpha);
    }
    return float4(rgb, alpha);
}

// FRAGMENT_SHADER_SRC_NO_BLUR: sharp camera, rounded-rect SDF mask, discard outside.
fragment float4 arNoBlurFragment(ARVertexOut in [[stage_in]],
                                 constant ARShaderUniforms &u [[buffer(0)]],
                                 texture2d<float> tex [[texture(0)]],
                                 sampler smp [[sampler(0)]]) {
    float4 texColor = tex.sample(smp, transformedTexCoord(in, u));
    if (u.cornerRadius > 0.0) {
        float2 halfRes = float2(u.width, u.height) * 0.5;
        float2 coord = in.position.xy - float2(u.x, u.y) - halfRes;
        float2 b = max(abs(coord) - halfRes + u.cornerRadius, 0.0);
        float box = length(b) - u.cornerRadius;
        if (box > 1.0) {
            discard_fragment();
        }
        // GL computed this edge mix but returned the unmodified colour; applying it here is the
        // intended corner softness and only affects the outermost pixel.
        float edge = smoothstep(0.0, 1.0, box);
        return float4(mix(texColor.rgb, float3(0.0), edge), texColor.a);
    }
    return texColor;
}

// FRAGMENT_SHADER_SRC_V_OES: first pass, camera texture with the aspect-fill UV transform.
fragment float4 arBlurVerticalCameraFragment(ARVertexOut in [[stage_in]],
                                             constant ARShaderUniforms &u [[buffer(0)]],
                                             texture2d<float> tex [[texture(0)]],
                                             sampler smp [[sampler(0)]]) {
    float2 uv = transformedTexCoord(in, u);
    if (u.lod > u.minLod) {
        float4 blurred = gaussBlur(tex, smp, uv, float2(0.0, exp2(u.lod) / u.height));
        return applyContrast(blurred.rgb, blurred.a, u);
    }
    return tex.sample(smp, uv);
}

// FRAGMENT_SHADER_SRC_V_2D
fragment float4 arBlurVerticalFragment(ARVertexOut in [[stage_in]],
                                       constant ARShaderUniforms &u [[buffer(0)]],
                                       texture2d<float> tex [[texture(0)]],
                                       sampler smp [[sampler(0)]]) {
    if (u.lod > u.minLod) {
        float4 blurred = gaussBlur(tex, smp, in.texCoord, float2(0.0, exp2(u.lod) / u.height));
        return applyContrast(blurred.rgb, blurred.a, u);
    }
    return tex.sample(smp, in.texCoord);
}

// FRAGMENT_SHADER_SRC_H
fragment float4 arBlurHorizontalFragment(ARVertexOut in [[stage_in]],
                                         constant ARShaderUniforms &u [[buffer(0)]],
                                         texture2d<float> tex [[texture(0)]],
                                         sampler smp [[sampler(0)]]) {
    if (u.lod > u.minLod) {
        float4 blurred = gaussBlur(tex, smp, in.texCoord, float2(exp2(u.lod) / u.width, 0.0));
        return applyContrast(blurred.rgb, blurred.a, u);
    }
    return tex.sample(smp, in.texCoord);
}
