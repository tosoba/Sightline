# AR camera effect (blur)

Groundwork spec for porting the OpenGL ES AR camera effect to iOS Metal.

Source: issue 01-ar-camera-effect-ios.md, issue 07-target-shared-architecture.md.

## Current

Android renders a live camera feed with a separable multi-resolution Gaussian blur (7 passes, sigma 3, animated lod strength), a constant-color overlay mix, and rounded-rectangle masks punching unblurred holes around AR markers.
Implementation is OpenGL ES 3.0 in 1289 lines of C++ (`core/ar/src/main/cpp/opengl_renderer_jni.cpp`, `jni_hooks.cpp`), driven by a Kotlin wrapper (`OpenGLRenderer.kt`) fed CameraX frames through `SurfaceTexture`.
Markers are drawn on an Android Canvas overlay (`ARView.kt`, `ARMarkerRenderer.kt`); the GL layer punches stencil holes around marker rects.

## Target (iOS)

Hand-port the effect to Metal.

- Camera pipeline: `AVCaptureSession + AVCaptureVideoDataOutput` -> `CVMetalTextureCache` (zero-copy) -> `MTKView`.
- The six GLSL shaders (Gaussian + rounded-rect SDF) port cleanly to Metal Shading Language.
- The 7-FBO multi-resolution animated-lod blur maps to intermediate `MTLTexture` targets with `sample(..., level(lod))`.
- The stencil/scissor hole-punch maps to `MTLStencilDescriptor` + `setScissorRect`.
- Marker overlay: SwiftUI `Canvas` / CoreGraphics, keeping the same "overlay computes rects, renderer punches holes" split.

## Shared math

The transform/kernel and marker-projection math is pure Kotlin except for `android.location.Location` and `Surface.ROTATION_*`.
Extract it into `core:ar` commonMain as the single source of truth, called from the Metal renderer through the shared framework.
Porting to Swift/simd is the fallback if the Kotlin/Native boundary proves awkward.

## Orientation

`CMMotionManager` `.xMagneticNorthZVertical` attitude is the parity of Android's rotation-vector path.
Axis/sign remapping is the top parity risk and must be verified with a prototype before committing to the full port.

## Fallback

A Core Image graph (`CIGaussianBlur` + `CIConstantColorGenerator`/`CISourceOverCompositing` + `CIRoundedRectangleGenerator`/`CIBlendWithMask`) works but trades exact control of the animated blur for less code.
GPUImage3 is ruled out (unmaintained).

## Risks

- Orientation axis/sign remapping (highest).
- The animated-lod blur is visual-equivalence, not pixel-identical; the parity bar accepts this.
- Metal boilerplate (device, queue, pipeline) has no GL equivalent and is new code.

## Out of scope

- Pixel-identical parity (the bar is visual equivalence).
