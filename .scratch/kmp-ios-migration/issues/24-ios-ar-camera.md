# 24: iOS AR camera (Metal)

**Spec:** `.scratch/kmp-ios-migration/specs/ar-blur.md`

**What to build:** An iOS user points the camera and sees the blurred live feed with unblurred holes around place markers, matching the Android AR effect to visual-equivalence parity, using the orientation remapping verified by the prototype.

**Blocked by:** 9 (prototype), 21 (ar KMP), 22 (shared + iosApp)

**Status:** ready-for-agent

- [ ] The Metal pipeline (`AVCaptureSession` -> `CVMetalTextureCache` -> `MTKView`) renders the blurred feed.
- [ ] Rounded-rect holes punch around markers via stencil/scissor.
- [ ] The shared math is consumed through the framework (simd fallback only if the boundary proves awkward).
- [ ] Marker alignment is correct across device orientations, to visual-equivalence parity.
