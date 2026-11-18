# 24: iOS AR camera (Metal)

**Spec:** `.scratch/kmp-ios-migration/specs/ar-blur.md`

**What to build:** An iOS user points the camera and sees the blurred live feed with unblurred holes around place markers, matching the Android AR effect to visual-equivalence parity, using the orientation remapping verified by the prototype.

**Blocked by:** 9 (prototype), 21 (ar KMP), 22 (shared + iosApp)

**Orientation remapping (from ticket 9):** `.scratch/kmp-ios-migration/research/09-ar-orientation-prototype.md`. The verified remapping is: `azimuth = atan2(-m.m12, m.m11)`, `pitch = asin(-m.m13)`, `roll = atan2(-m.m23, m.m33)` from `CMAttitude.rotationMatrix` (`.xMagneticNorthZVertical`), fed unchanged into the shared `Math3D.getCamRotation`. Use the matrix form, not ZXY Euler (gimbal lock at upright portrait). Prototype code: branch `prototype/09-ar-orientation`, `prototype/ar-orientation/`.

**Status:** ready-for-agent

- [ ] The Metal pipeline (`AVCaptureSession` -> `CVMetalTextureCache` -> `MTKView`) renders the blurred feed.
- [ ] Rounded-rect holes punch around markers via stencil/scissor.
- [ ] The shared math is consumed through the framework (simd fallback only if the boundary proves awkward).
- [ ] Marker alignment is correct across device orientations, to visual-equivalence parity.
