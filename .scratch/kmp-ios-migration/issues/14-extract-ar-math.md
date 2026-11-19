# 14: Extract core:ar math into its own package

**Spec:** `.scratch/kmp-ios-migration/specs/ar-blur.md`, `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** The pure math/geometry (vectors, trig, matrix/3D, rounded-rect, marker projection) is extracted into a self-contained package free of Android types, so it can move to `commonMain` without the renderer. Android rendering is unchanged.

**Blocked by:** 11 (GeoLocation)

**Status:** resolved

- [x] The math/geometry lives in a dedicated package with no Android type dependencies.
- [x] The OpenGL/CameraX/Canvas renderer consumes the extracted math and renders identically.
- [x] Android compiles, tests pass, and AR rendering behavior is unchanged.

## Comments

`com.trm.sightline.core.ar.math` is now the pure Kotlin surface intended for `commonMain`.
It imports only `kotlin.math`, `core:model.GeoLocation`, and the pure `core.ar.orientation.Orientation` data class - no `android.*` or `java.*` anywhere.
On top of the existing `Vector1`/`Vector2`/`Vector3`, `Trig1`/`Trig3`, and `Math3D`, the package gained four things:

- `ScreenRotation`, an enum mirroring `Surface.ROTATION_0/90/180/270`. `Math3D.getCamRotation` now takes it instead of the raw `Int`.
  The `Surface.ROTATION_*` to neutral mapping lives in `Context.screenRotation` (`ARContextExtensions.kt`), and `Context.phoneRotation: Int` stays for CameraX `setPreviewRotation`, so the Android caller still owns the platform constants.
- `RoundedRectF(left, top, right, bottom, cornerRadius)` with `width`/`height`, replacing the old `RectF` wrapper. The package changed from `core.ar.model` to `core.ar.math` and every consumer (`ARMarkerRenderer`, `OpenGLRenderer`, `MainScreen`, `MainPager`, `feature:camera`) was updated mechanically; the `OpenGLRenderer` GPU buffer still receives the same five floats per rect.
- `MarkerProjection` (`updateViewport`, `updateCamera`, `project`) lifted out of `ARView.preDraw`/`calculateMarkerScreenPosition`. It runs the same `convertLocationToPosition` -> `getRelativeTranslationInMeters` -> `getRelativeRotation` -> `convert3dTo2d` chain and exposes `ProjectedMarker(screenX, screenY, isDrawn, isInFront)`.
- `MarkerSlotAssignment.assign(...)` plus `MarkerSlotTarget`/`MarkerSlotGeometry`/`MarkerSlot`, lifted out of `ARMarkerRenderer.reassignSlots`. The bearing normalisation, angular-distance conflict test, row stacking, and page rollover are now a pure function of `GeoLocation` bearings plus pixel geometry. The geometry inputs (`markerWidthPx`, `numberOfRows`, `baseY` from insets and dp) are still computed from `Context` by the renderer and passed in.

Deliberately left Android-side, because lifting them would need a rewrite rather than a move: all of `ARMarkerRenderer`'s Canvas work (paints, `drawRoundRect`, text layout via `StaticLayout`/`TextUtils`, `ImageVector` path baking including `svgArcTo`/`svgAngle`, the icon cache, `Bundle` save/restore, and the paging `StateFlow`s); `ARView`'s `View` lifecycle, touch hit-testing, `Parcelable` state, and distance/range bookkeeping; `OrientationManager` (sensors); and the `Context` extensions for rotation, insets, and `sideSheetRectF`.
The renderer also kept its private `PagedMarker`/`PagedYPosition` because they are its own mutable draw state; it now copies the pure `MarkerSlot` results into them.
`ARMarker` lost its `java.util.Objects.hash` dependency (`place.hashCode()`) but stays in `core.ar.model`: it is mutable per-frame draw state, not geometry, and its identity is only used for lookup within `core:ar`.

`Math.toDegrees`/`Math.PI` in the moved code became `kotlin.math` equivalents, which are the same doubles (`x * 180.0 / PI`), so the numbers are bit-identical.
`ARMarkerRenderer.markerAngularWidthDeg` was dead state (written, never read) and is now a local inside the slot assigner.

Tests: 31 characterization tests in `core/ar/src/test/java/com/trm/sightline/core/ar/math/`, on `kotlin.test` via `testImplementation(kotlin("test-junit"))` (`kotlin("test")` alone resolves to the base artifact without the JUnit annotation for an Android library module).
`Math3DTest` pins all four `ScreenRotation` mappings, `getRelativeTranslationInMeters`, `getRelativeRotation`, and `convert3dTo2d` including the out-of-bounds and `z <= 0` false paths.
`MarkerProjectionTest` pins the whole projection chain (centre, pitched, behind, off-screen, rotated), `MarkerSlotAssignmentTest` pins row stacking and page rollover, and `VectorTrigTest`/`RoundedRectFTest` cover the small value types.
All expected values were produced by the pre-extraction implementation (recomputed independently), so they are the regression net for ticket 21.

Verified with `./gradlew :app:assembleDebug test --console=plain` and `./gradlew :core:ar:test --console=plain`, both green.
