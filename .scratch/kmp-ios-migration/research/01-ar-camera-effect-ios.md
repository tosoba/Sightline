# Research: Reproducing Sightline's AR camera effect on iOS

Status: complete (Nov 2026)

## TL;DR verdict

Yes, the effect can be reproduced on iOS to visual-equivalence parity.
The recommended mechanism is a hand-ported Metal pipeline:
`AVCaptureSession + AVCaptureVideoDataOutput` feeding `CVMetalTextureCache`, with the six GLSL shaders rewritten in Metal Shading Language (MSL) and rendered through an `MTKView`.
The shaders are small (Gaussian + rounded-rect SDF) and port cleanly.
Core Image is a viable faster fallback that trades exact control of the animated multi-resolution blur for less code.
GPUImage3 is not recommended (effectively unmaintained, incomplete port).
The projection/marker math is almost entirely plain Kotlin trig and is directly reusable across platforms.
The main risks are orientation axis/sign remapping and matching the blur falloff, both well within the visual-equivalence bar.

## 1. What the effect actually is (from code)

The renderer is `core/ar/src/main/cpp/opengl_renderer_jni.cpp` (1289 lines) driven by `core/ar/src/main/java/.../camera/OpenGLRenderer.kt`.

The pipeline per frame:

1. CameraX writes into a `SurfaceTexture` (external OES texture).
2. GL renders with a `samplerExternalOES` input, applies the surface `texTransform` (from `SurfaceTexture.getTransformMatrix`) and a center-crop vertex transform.
3. Either a full no-blur pass, or an eight-draw separable Gaussian blur across seven intermediate FBOs:
   vertical at half resolution, horizontal at half, vertical at quarter, horizontal at quarter, vertical at half, horizontal at half, vertical at full, horizontal at full.
4. A constant-color overlay mix is applied during the blur passes (`mix(blurred.rgb, contrastingColor, mix)`), only when the contrasting color is set.
5. For each marker rect, GL punches an unblurred hole: stencil test (`GL_EQUAL`, ref 1), `glScissor` to the rect, then a sharp no-blur draw whose fragment shader computes a rounded-rectangle SDF (`udRoundBox`) and discards pixels outside the rounded rect (with a `smoothstep` edge fade to black).

Key parameters from the shaders:

- Gaussian `sigma = 3`, loop radius `r = sigma * 2 = 6` (taps `i = 1..5`), weight `exp(-i*i/(2*sigma*sigma))`, normalized by alpha.
- `lod` ranges `MIN_LOD = -2` to `MAX_LOD = 2`, animated over 18 frames.
  In the shader, `lod` both scales the sample spacing (`exp2(lod) / dimension`) and is passed as the LOD bias to `texture()`.
- Contrasting color mix animates `0 -> 0.025 -> 0`; color itself animates over 60 frames.

This "multi-resolution separable Gaussian with an animated lod strength, plus stencil/scissor rounded-rect holes" is the distinctive effect to preserve.

## 2. Recommended iOS camera pipeline

### Option A: Metal hand-port (recommended)

`AVCaptureSession` + `AVCaptureVideoDataOutput` + `CVMetalTextureCache` + `MTKView`.

This is the standard, Apple-documented, zero-copy path for real-time camera processing:

- Configure `AVCaptureSession` with `AVCaptureDeviceInput` (wide-angle rear) and `AVCaptureVideoDataOutput`.
- Request BGRA (`kCVPixelFormatType_32BGRA`) on the output and set `alwaysDiscardsLateVideoFrames = true` (drop-over-buffer, matching the Android frame listener).
- In `captureOutput(_:didOutput:from:)`, unwrap `CMSampleBufferGetImageBuffer` to a `CVPixelBuffer`.
- Wrap the `CVPixelBuffer` as an `MTLTexture` with `CVMetalTextureCacheCreateTextureFromImage` + `CVMetalTextureGetTexture`.
  This is zero-copy (the texture aliases the camera buffer memory).
- Hold a reference to the `CVMetalTexture` until the GPU finishes reading it (avoid flicker/recycle).
- Render in an `MTKView` (or a `CAMetalLayer` wrapped in a `UIViewRepresentable`), drawing a full-screen triangle/quad textured with the camera frame.

SwiftUI integration:
`MTKView` is wrapped with `UIViewRepresentable` (`makeUIView` returns an `MTKView` with a delegate).
Apple's own sample code uses this pattern; the `MTKViewDelegate.draw(in:)` method drives the render pass.

### Option B: Core Image pipeline (faster fallback)

`CIImage(cvPixelBuffer:)` -> Core Image filter chain -> `CIContext.render(_, to: MTKView.currentDrawable.texture, ...)`.
Also a standard path, documented by Apple ("Processing Images" / "Selectively Focusing on an Image").

Pros: less GPU code, filters are prebuilt and hardware-optimized.
Cons: less control over the exact multi-resolution + animated-lod blur, and the overlay/hole-punch is expressed as a filter graph rather than direct math.

### Option C: GPUImage3 (not recommended)

`BradLarson/GPUImage3` is BSD-licensed and Metal-based, but it is effectively unmaintained:
93 commits, no tagged releases, last repository activity about 2 years ago, a "DEAD PROJECT" issue (#132), and many operations still listed as "Functionality not completed".
GPUImage2 remains somewhat active but targets OpenGL, which Apple has deprecated on iOS.
Do not adopt it as a dependency for a production migration.

### Option D: Metal Performance Shaders (partial)

`MPSImageGaussianBlur(device:sigma:)` provides a fast single-pass separable Gaussian at `sigma = 3`.
It can serve as the blur kernel, but `sigma` is fixed at init and it does not reproduce the multi-resolution lod-stepped animation, so it would still need custom MSL for the overlay and hole-punch.
Useful only as an optimization inside Option A, not as a standalone approach.

## 3. The blur + overlay + mask effect specifically

### Reproducing it in Metal Shading Language

All six shaders port cleanly to MSL.
The mappings:

- `samplerExternalOES sampler` -> `texture2d<float>` (the CVMetalTextureCache texture).
- `texture(tex, uv, l)` with LOD bias -> `tex.sample(sampler, uv, level(l))`.
- `discard` -> `discard_fragment()`.
- `gl_FragCoord` -> a fragment input declared with the `[[position]]` attribute.
- `udRoundBox` SDF -> identical `length(max(abs(p) - b + r, 0.0)) - r`.
- The seven intermediate FBOs -> seven `MTLTexture` render targets (RGBA8, `.private` storage, usage `.renderTarget | .shaderRead`).
- Stencil hole-punch -> `MTLStencilDescriptor` with `stencilCompareFunction = .equal`, reference value 1, and a `renderEncoder.setScissorRect` per rect (Metal scissor is per-encoder, same semantics as `glScissor`).
- The center-crop/rotation vertex transform -> the same affine math re-expressed with `simd_float4x4`.

The animated `lod` maps directly: pass `lod` as a fragment buffer/uniform, and use `sample(sampler, uv, level(lod))` plus `exp2(lod)/dimension` for the sample spacing.
The 18-frame `lod` ramp and the 60-frame color ramp are pure CPU-side animation and port verbatim.

Verdict on hand-porting:
Yes, hand-porting is reasonable and is the recommended approach.
Each kernel is roughly 30 lines and is already self-contained; the Metal-specific differences (texture/sampler binding, `[[position]]`, `discard_fragment`) are mechanical.
This preserves the exact multi-resolution animated-lod behavior, which is the hard-to-match part.

### Transpiling (GLSL -> MSL)

`SPIRV-Cross` (KhronosGroup) is actively maintained and does convert SPIR-V to MSL (via GLSL -> glslang -> SPIR-V -> MSL).
It supports vertex/fragment/compute shaders and targets iOS Metal (`--msl --msl-ios`).

It is not worth it here because:

- The input shader uses `samplerExternalOES`, an Android-only extension that does not exist in SPIR-V; it would have to be rewritten anyway.
- The stencil/scissor and texture-transform logic lives in the C++/GL state, not in the shaders, so it must be re-expressed on the Metal side regardless.
- The remaining math kernels (V-2D, H, rounded-rect SDF) are trivial to hand-port and need Metal coordinate/sampler treatment anyway.

Transpiling would only help for the pure `sampler2D` Gaussian kernels, and the effort to set up glslang + SPIRV-Cross and fix the emitted code exceeds a manual rewrite of ~100 lines of MSL.

### Reproducing it in Core Image

The equivalent Core Image graph:

1. Blur: `CIGaussianBlur` (radius parameter).
2. Overlay: `CIConstantColorGenerator` (solid color) composited over the blurred image with `CISourceOverCompositing`, using alpha = mix factor.
   This reproduces the `mix(blurred.rgb, color, mix)` lerp exactly (premultiplied source-over).
3. Holes: build a grayscale mask with `CIRoundedRectangleGenerator` (one per marker, radius = corner radius, color = white), union them with `CIMaximumCompositing`, then `CIBlendWithMask`:
   `inputImage` = sharp original, `inputBackgroundImage` = blurred + overlay, `inputMaskImage` = rounded-rect mask.
   White mask => sharp original (unblurred hole), black => blurred.

This is exactly Apple's documented "Selectively Focusing on an Image" recipe.
Alternative: `CIMaskedVariableBlur` (blur everywhere except an inverted mask), but that applies the overlay differently and is a poorer fit for the global-overlay + hole-punch semantics.

Caveats vs the Android look:

- `CIGaussianBlur` uses its own Gaussian kernel and a single `radius`; it does not do the 7-step multi-resolution downsampling, so the falloff differs slightly.
- The animated `lod` strength becomes an animated `radius` (18-frame ramp), which is visually equivalent but not identical.

The parity bar is visual equivalence, so Core Image is acceptable if the team prefers less GPU code and accepts a slightly different blur falloff.

## 4. Reusability of the C++/Kotlin math

### Kotlin marker-projection math

`core/ar/src/main/java/com/trm/sightline/core/ar/math/Math3D.kt` plus `Vector2`, `Vector3`, `Trig1`, `Trig3` and the `Orientation` data class are the projection core.

- `Vector2`, `Vector3`, `Trig1`, `Trig3`, `Orientation` are pure Kotlin (no Android imports).
- `Math3D.kt` has exactly two Android dependencies:
  - `android.location.Location` (latitude, longitude, altitude, `distanceTo`, `bearingTo`).
  - `android.view.Surface` (the four `ROTATION_0/90/180/270` integer constants).

Conclusion:
The projection math is ~95% portable as-is.
For a KMP migration, move it to `commonMain` and define an `expect`/`actual` pair for (a) a minimal `GeoPoint` (lat/lon/alt + distance + bearing) and (b) a screen-rotation enum.
On iOS the actual uses `CLLocation` for coordinates/altitude and a small forward-azimuth (bearing) helper, because `CLLocation` has no built-in `bearingTo`.
The math then compiles to iOS via Kotlin/Native and is callable from Swift unchanged.

Alternatively, porting to Swift/`simd` (`simd_double3`, `simd_double2`, `sin`/`cos`) is a small, mechanical job.
Either path is low effort; keeping it in shared KMP is preferable for single-source-of-truth.

### C++ shader/transform math

- The shader kernels (Gaussian weights, `udRoundBox` SDF, `mix`) are pure math and portable verbatim into MSL.
- The vertex transform (center-crop + surface rotation) is currently computed with `android.opengl.Matrix` in `OpenGLRenderer.kt` (`getTransformMatrix` from `SurfaceTexture`, then `setRotateM`/`scaleM`).
  iOS has no `SurfaceTexture`; `AVCaptureVideoDataOutput` instead yields already-oriented buffers and you set `connection.videoRotationAngle`/`isVideoMirrored` (or `videoOrientation`).
  The center-crop/rotation affine math must be re-derived in Swift with `simd` (or in Metal), but it is the same math, not new logic.

Net: no Android-specific math needs to be reinvented; only the Android sensor/location/surface adapters are swapped.

## 5. Marker overlay equivalent

Android draws markers on an `android.graphics.Canvas` overlay (`ARView.kt`, `ARMarkerRenderer.kt`):
`drawRoundRect`, `drawText` (with `TextPaint` + ellipsize), `drawPath` for icons, `clipRect`, text shadows.

iOS equivalents:

- CoreGraphics/UIKit: override `UIView.draw(_:)` and use a `CGContext` (`addRoundedRect`, `addPath`, `drawText`, `saveGState`/`clip`).
  This is the closest literal equivalent to the Android `Canvas` code.
- SwiftUI `Canvas` with `GraphicsContext`: `context.fill(Path(roundedRect:cornerRadius:))`, `context.draw(Text)`, `context.drawLayer { $0.clip(to: path) }`, `context.addFilter(.shadow(...))`.
  This is the cleanest fit for a SwiftUI app and matches the "overlay on top of the camera layer" architecture.

Icons:
Android bakes Compose `ImageVector` -> `android.graphics.Path`.
On iOS the equivalent is SF Symbols (`Image(systemName:)`) or a pre-baked `CGPath`; no `ImageVector` port is required.

Keeping rects in sync (the important part):
Android passes marker rounded-rects from the Canvas layer back into GL (`drawnMarkerRectFs` -> `rectFsCoordinates` -> JNI) to punch stencil/scissor holes.
iOS should use the same architecture: the shared KMP marker-projection code computes screen-space rounded-rect frames, which both the overlay layer (for drawing text/icons) and the renderer (for Metal scissor/rounded-rect masks, or Core Image `CIRoundedRectangleGenerator` masks) consume.
Because the marker math and the renderer rects share one screen-point coordinate system, this is a direct structural port.

## 6. Orientation parity

Android (`OrientationManager.kt`):

- Primary: `Sensor.TYPE_ROTATION_VECTOR` -> `getRotationMatrixFromVector` -> `remapCoordinateSystem(AXIS_Y, AXIS_MINUS_X)` -> `getOrientation` -> `pitch/azimuth/roll`, low-pass smoothed (`SMOOTH_FACTOR = 0.12`, ~18 degree threshold).
- Fallback: accelerometer + magnetometer.

iOS (`CMMotionManager`):

- `startDeviceMotionUpdates(using: .xMagneticNorthZVertical)` -> `CMDeviceMotion.attitude` gives `pitch`, `roll`, `yaw` (radians, -pi..pi).
- Core Motion already performs gyro + accelerometer + magnetometer sensor fusion and outputs a drift-free attitude, so the Android fallback path is unnecessary; the fused attitude is the direct parity of the Android rotation-vector path.
- Reference frame `.xMagneticNorthZVertical` corresponds to Android's compass (magnetometer-corrected) mode; `.xArbitraryZVertical` corresponds to a relative/gyro-only mode (analogous to Android `TYPE_GAME_ROTATION_VECTOR`).

Parity caveats (the #1 risk):

- Axis and sign conventions differ.
  Android `getOrientation` azimuth is 0 = north, positive east; iOS `yaw` in `.xMagneticNorthZVertical` is also 0 = north, positive east, but the Android code also remaps to a specific body frame (`AXIS_Y`/`AXIS_MINUS_X`) and negates pitch (`outCamRot.x = -pitch`).
  iOS pitch/roll/yaw follow the right-hand rule on a different body-axis definition, so a naive value copy produces mirrored or tilted markers.
- The mapping must be derived once and verified on device, and the low-pass smoothing constants should be reproduced.

This is a well-understood, bounded task, but it is where most visual parity bugs will surface.

## 7. Feasibility verdict and risk assessment

Verdict:
Feasible.
Visual-equivalence parity is achievable with moderate effort.

Recommendation:
Hand-port to Metal (Option A) as the primary mechanism.
It preserves the two distinctive behaviors (the multi-resolution animated-lod Gaussian blur and the stencil/scissor rounded-rect holes) with full control, and the shader surface is small.

Risk assessment:

- Low risk: camera capture (AVFoundation + CVMetalTextureCache is a mature, documented path).
- Low risk: the Gaussian and SDF math (pure, portable).
- Low-medium risk: the multi-resolution animated-lod blur "look".
  Hand-porting the exact 7-intermediate-FBO structure with the same `lod` semantics removes this risk; the Core Image fallback reintroduces a slight falloff difference.
- Medium risk: orientation axis/sign remapping (mirrored/tilted markers if signs are wrong).
  Bounded and testable on device.
- Low risk: marker overlay (Canvas -> SwiftUI Canvas/CoreGraphics is a close literal mapping).
- Non-issue: GPUImage3 (unmaintained, not recommended).

Parity note:
The bar is visual equivalence, not pixel-identical.
The metal/Core Image blur kernels differ slightly from the custom GLSL Gaussian, and the orientation smoothing may feel subtly different, but neither changes the user-visible effect.

## 8. References

- Apple: `CVMetalTextureCache` / `CVMetalTextureCacheCreateTextureFromImage` (Core Video).
- Apple: `AVCaptureVideoDataOutput` and `alwaysDiscardsLateVideoFrames`.
- Apple: Core Image Filter Reference - `CIGaussianBlur`, `CIConstantColorGenerator`, `CISourceOverCompositing`, `CIRoundedRectangleGenerator`, `CIBlendWithMask`, `CIMaskedVariableBlur`, `CIColorMatrix`.
- Apple: "Selectively Focusing on an Image" (Core Image mask recipe).
- Apple: Metal Performance Shaders - `MPSImageGaussianBlur(device:sigma:)`.
- Apple: `CMMotionManager` / `CMAttitude` / `CMAttitudeReferenceFrame` (`.xMagneticNorthZVertical`, `.xArbitraryZVertical`).
- KhronosGroup: `SPIRV-Cross` (SPIR-V to MSL backend).
- BradLarson: `GPUImage3` (Metal; BSD-3; unmaintained).
- Android: `SensorManager` rotation-vector sensor documentation.
