Type: research
Status: resolved

## Question

Can Sightline's AR camera effect be reproduced on iOS, and via what mechanism?

The effect: a live camera feed with a separable, multi-resolution Gaussian blur (7 passes, sigma 3, animated `lod` strength), a constant-color overlay mix, and rounded-rectangle masks that punch unblurred holes around AR markers.
Today it is OpenGL ES 3.0 in 1289 lines of C++ (`core/ar/src/main/cpp/opengl_renderer_jni.cpp`), driven by a Kotlin wrapper (`OpenGLRenderer.kt`) that gets frames from CameraX via `SurfaceTexture`.
AR markers are drawn on an Android Canvas overlay (`ARView.kt`, `ARMarkerRenderer.kt`), not in GL; the GL layer punches stencil holes around marker rects.

Research and report:

- The recommended iOS camera pipeline: AVCaptureSession + video data output feeding Metal via CVMetalTextureCache, or Core Image, or GPUImage.
- Whether to hand-port the GLSL shaders to Metal Shading Language, transpile, or re-express the effect in Core Image / GPUImage filters.
- Whether the C++ transform/kernel math and the Kotlin marker-projection math are reusable or must be ported.
- The marker-overlay equivalent (UIKit / CoreGraphics / SwiftUI Canvas).
- Orientation handling (CMMotionManager vs Android SensorManager).
- A feasibility verdict with risk assessment, under the parity bar of visual equivalence (not pixel-identical).

## Answer

Feasible - visual-equivalence parity is achievable with moderate effort.

- Recommended mechanism: hand-port to Metal - `AVCaptureSession + AVCaptureVideoDataOutput` -> `CVMetalTextureCache` (zero-copy) -> `MTKView`.
  The six GLSL shaders are small (Gaussian + rounded-rect SDF) and port cleanly; the 7-FBO multi-resolution animated-`lod` blur maps to intermediate `MTLTexture` targets with `sample(..., level(lod))`; the stencil/scissor hole-punch maps to `MTLStencilDescriptor` + `setScissorRect`.
- Fallback: a Core Image graph (`CIGaussianBlur` + `CIConstantColorGenerator`/`CISourceOverCompositing` overlay + `CIRoundedRectangleGenerator`/`CIBlendWithMask` holes) works but trades exact control of the animated blur for less code.
  GPUImage3 is out (unmaintained, incomplete).
- Transpiling (SPIRV-Cross) is not worth it - the external-OES sampler and GL state do not translate, and the remaining kernels are trivial to hand-port.
- Math is highly reusable - projection math is pure Kotlin apart from `android.location.Location` and `Surface.ROTATION_*`, so it can go to `commonMain` behind expect/actual or be ported to Swift/simd.
- Marker overlay: SwiftUI `Canvas`/CoreGraphics maps closely to the Android `Canvas`; keep the same "overlay computes rects, renderer punches holes" architecture.
- Orientation: `CMMotionManager` `.xMagneticNorthZVertical` attitude is the direct parity of Android's rotation-vector path; axis/sign remapping is the top parity risk.

Full findings: [research/01-ar-camera-effect-ios.md](research/01-ar-camera-effect-ios.md).
