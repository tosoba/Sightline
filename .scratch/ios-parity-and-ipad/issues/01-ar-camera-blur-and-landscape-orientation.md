Type: grilling
Status: resolved

## Question

Lock the exact design for the Issue 24 AR camera fixes on iOS:

1. **Blur trigger & frosted marker cards**:
   - On Android (`MainScreen.kt` / `CameraPreview.kt`), full-screen camera blur (`previewBlurred`) is `false` during normal AR viewing and only becomes `true` when the camera is obscured by an expanded sheet (`expandedProgress > 0`) or when `!pitchWithinLimit` (device tilted past ±45°).
   - While unblurred, marker cards (and any translucent sheet region over the camera) receive frosted max-LOD blur + contrasting tint inside their stencil rects (`holeFillStyle = .frosted`).
   - Decide how `ARCameraRenderer` and `ARCameraModel` should coordinate full-frame blur vs. per-rect frosted stencil blur when `blurEnabled == false`, and what sheet/modal states trigger `blurEnabled = true`.

2. **Horizontal (landscape) device orientation & marker alignment**:
   - Currently `iosApp` locks `UISupportedInterfaceOrientations` to portrait and hardcodes `screenRotation: .rotation0`. When the device is held horizontally (`roll ≈ ±90°`), `Math3D.convert3dTo2d` rotates `(viewPortPos.x, viewPortPos.y)` by `-roll`, mapping horizontal bearing into `screenY` (which `ARCameraModel` discards in favor of portrait `slot.y`) and altitude into `screenX` (causing markers to bunch up, overlap, and pan wrongly).
   - Decide how interface orientation, `ScreenRotation` (`ROTATION_0`, `ROTATION_90`, `ROTATION_270`, `ROTATION_180`), `AVCaptureConnection.videoRotationAngle`, and `ArOrientationTracker` / `MarkerSlotAssignment` work together across portrait and landscape so horizontal marker placement, slot stacking, and camera preview alignment match vertical mode.

3. **Marker pagination (`page > 0`) & tap-to-toggle overlay**:
   - Currently `ARCameraModel` drops any marker assigned to `slot.page > 0`. On Android, `CameraPreviewPagePicker` lets the user page through overflowing marker slots, and tapping empty camera space toggles toolbar/overlay visibility. Decide whether iOS adopts the marker page picker and tap-to-toggle behavior.

## Answer

1. **Full-frame blur trigger & frosted AR Marker cards**:
   - Normal AR viewing starts unblurred (`blurEnabled = false` by default). The base camera pass renders sharp (`arNoBlurFragment`), and each visible AR Marker card punches a stencil hole (`holeRects`) filled with max-LOD frosted blur (`lod = 2.0`) plus contrasting tint (`holeFillStyle = .frosted`).
   - Full-frame blur (`blurEnabled = true`, animated over 18 frames via `ArBlurAnimator`) triggers only when `pitchOutOfRange` (pitch outside ±45°), when an expanded sheet obscures the camera (`sheetObscuresCamera`), or when forced by debug launch flags (`-arForceBlur` or `-arBlur <0..1>`).
   - Whenever `pitchOutOfRange || sheetObscuresCamera`, `ARCameraModel` clears `placements` and `holeRects` so markers are hidden and `ARCameraRenderer` skips the second (hole-fill) blur pass. During the 18-frame unblur transition (`!blurEnabled && frame.isAnimatingLod`), visible marker holes continue to render frosted.
   - Sheet and floating UI backdrops over the camera use native SwiftUI materials (`.ultraThinMaterial` / `.regularMaterial`), so `ARCameraRenderer` only stencil-blurs `holeRects` for AR Marker cards.
   - `contrastingColor` is dynamically derived from `UIColor.secondarySystemBackground` resolved for the active SwiftUI `ColorScheme`, easing over 60 frames via `ArBlurAnimator` when appearance changes.

2. **Horizontal (landscape) device orientation & marker alignment**:
   - Unlock `UIInterfaceOrientationPortrait`, `UIInterfaceOrientationLandscapeLeft`, and `UIInterfaceOrientationLandscapeRight` on iPhone in `Info.plist` (and all 4 orientations on iPad in Ticket 04).
   - Observe the active window scene's `UIInterfaceOrientation` and map it to both `ScreenRotation` (for `ArProjection.updateCamera`) and `AVCaptureConnection.videoRotationAngle`:
     - `.portrait` -> `ScreenRotation.rotation0`, `videoRotationAngle = 90`
     - `.landscapeRight` (device rotated 90° CCW, top on left) -> `ScreenRotation.rotation90`, `videoRotationAngle = 0`
     - `.landscapeLeft` (device rotated 90° CW, top on right) -> `ScreenRotation.rotation270`, `videoRotationAngle = 180`
     - `.portraitUpsideDown` -> `ScreenRotation.rotation180`, `videoRotationAngle = 270`
   - Include `screenRotation` and usable viewport geometry in `cachedSlots` invalidation.
   - Offset scripted simulator pose roll in `ARCameraMotion` by the active `ScreenRotation` (`0` for `.rotation0`, `-π/2` for `.rotation90`, `+π/2` for `.rotation270`, `π` for `.rotation180`) so simulator landscape verification exercises the exact same projection math as physical hardware.
   - In `ARCameraRenderer.updateTexTransform`, apply the 90° UV rotation fallback only when source buffer and drawable aspect orientations disagree.

3. **Adaptive marker card sizing, slot pagination (`page > 0`), and tap-to-toggle overlay**:
   - Compute `MarkerSlotGeometry` (`baseY`, `rows`, `markerWidth`, `markerHeight`) from the usable unobscured camera region (excluding top safe area and collapsed bottom/side sheet insets), clamping card height to `72...88` pt (`rows` in `1...6`) and card width to `usableWidth / divisor` where `divisor = Int(usableWidth / 160).clamped(to: 2...5)`.
   - Track `currentPage` and `totalPages` (`maxPage + 1`) in `ARCameraModel` (preserving the page of previously drawn markers across rotations and clamping `currentPage` to `0...maxPage`), drawing markers where `slot.page == currentPage`.
   - Show a floating marker page picker when `totalPages > 1` (displaying up to 3 page numbers at a time in 1-based numbering) and toggle floating controls/picker visibility (`toolbarsVisible`) when the user taps empty space on the AR camera preview.
