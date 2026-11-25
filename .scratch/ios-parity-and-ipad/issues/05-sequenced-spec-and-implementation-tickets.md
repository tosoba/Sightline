Type: task
Status: resolved
Blocked by: 01, 02, 03, 04

## Question

Synthesize the decisions from tickets 01-04 into:

1. `.scratch/ios-parity-and-ipad/spec.md` - the complete, buildable specification covering the AR camera blur & orientation fixes, full-screen layout & HIG multi-category sheet, custom location search, and native iPad support.
2. Ordered implementation tickets (`06-...` onward) in `.scratch/ios-parity-and-ipad/issues/` following the agreed chronology:
   - Phase 1: Issue 24 AR camera fixes (blur only when obscured / pitch out of range + frosted marker cards on sharp feed; landscape orientation, `ScreenRotation`, `videoRotationAngle`, and marker alignment/stacking).
   - Phase 2: Full-screen layout, floating controls, multi-category places sheet, preferences rework (Issue 25), and custom location search (Issue 26) + updated UI smoke tests.
   - Phase 3: Native iPad support (`TARGETED_DEVICE_FAMILY: "1,2"`, adaptive side panel/sheet, 4-orientation AR & viewport scaling).
3. Update `.scratch/kmp-ios-migration/issues/24-ios-ar-camera.md`, `25-ios-preferences-navigation-smoke.md`, and `26-ios-custom-location.md` with cross-references to this effort.

## Answer

1. **Specification**:
   - Authored [Spec: iOS Feature, UX, and Orientation Parity + Native iPad Support](../spec.md) consolidating the decisions from [AR camera blur and landscape orientation](01-ar-camera-blur-and-landscape-orientation.md), [Full-screen layout and HIG places sheet](02-fullscreen-layout-and-hig-places-sheet.md), [Shared facade and custom location flow](03-shared-facade-and-custom-location-flow.md), and [iPad adaptive layout and AR scaling](04-ipad-adaptive-layout-and-ar-scaling.md).
2. **Sequenced Implementation Tickets**:
   - Phase 1 (Issue 24 fixes): [06: iOS AR camera blur, landscape orientation, and slot pagination](06-ios-ar-camera-blur-orientation-pagination.md)
   - Phase 2a (Shared KMP state coordinator & address facade): [07: Shared PlacesCoordinator and address facade](07-shared-places-coordinator-and-address-facade.md)
   - Phase 2b (Issues 25 & 26 UI rework + smoke tests, blocked by 06 and 07): [08: iOS full-screen tabs, HIG places sheet, and custom location UI](08-ios-fullscreen-tabs-places-sheet-and-custom-location.md)
   - Phase 3 (Universal iPad target, adaptive `.sheet`, 4-orientation AR scaling, blocked by 06 and 08): [09: Native iPad adaptive support and 4-orientation AR scaling](09-ipad-adaptive-support-and-ar-scaling.md)
3. **Cross-references in `kmp-ios-migration`**:
   - Linked [24: iOS AR camera (Metal)](../../kmp-ios-migration/issues/24-ios-ar-camera.md), [25: iOS preferences, category navigation, and full smoke gate](../../kmp-ios-migration/issues/25-ios-preferences-navigation-smoke.md), and [26: iOS custom location picker](../../kmp-ios-migration/issues/26-ios-custom-location.md) (marked `wontfix` in favor of the successor tickets) to the new spec and implementation issues.

The map stops here.
