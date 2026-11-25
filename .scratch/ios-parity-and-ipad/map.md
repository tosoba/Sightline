## Destination

Produce a sequenced implementation spec (`spec.md`) and ordered implementation tickets under `.scratch/ios-parity-and-ipad/` that bring `iosApp` to full feature, UX, and orientation parity with Android while adhering to Apple HIG and supporting iPad natively.
The final implementation plan is ordered chronologically:
1. Issue 24 AR camera fixes (camera blur only when obscured or pitch out of range; landscape/horizontal orientation, marker alignment, and slot pagination).
2. Full-screen layout, HIG-aligned multi-category places & location sheet (reworking Issue 25 and integrating Issue 26 custom location).
3. Native iPad adaptive support.

## Notes

- Supersedes and absorbs the remaining/reworked iOS items from `.scratch/kmp-ios-migration/` (specifically the physical-device/orientation and blur gaps in [24: iOS AR camera (Metal)](../kmp-ios-migration/issues/24-ios-ar-camera.md), the multi-category sheet and screen-space rework of [25: iOS preferences, category navigation, and full smoke gate](../kmp-ios-migration/issues/25-ios-preferences-navigation-smoke.md), and [26: iOS custom location picker](../kmp-ios-migration/issues/26-ios-custom-location.md)).
- Follow Android's functional model (no auto-fetch on launch, multiple active place categories with independent loading/loaded counts, toggle vs. open category list, custom location search when "Use my location" is off, full-screen camera and map with floating page/radius controls), adapted to Apple HIG where Android's Material 3 patterns do not fit iOS/iPadOS idioms.
- Skills every session should consult: `grilling`, `domain-modeling`, `apple-hig`, `swiftui-expert-skill`, `kmp-ios-integration`.

## Decisions so far

<!-- the index: one line per closed ticket, enough to judge relevance, then zoom the link for the detail the ticket holds -->

- [AR camera blur and landscape orientation](issues/01-ar-camera-blur-and-landscape-orientation.md): Keep the live AR feed sharp with frosted max-LOD stencil blur inside marker cards (full-frame blur only when pitch exceeds ±45°, an expanded sheet obscures the camera, or forced by `-arForceBlur`/`-arBlur`), unlock landscape orientations mapped to `ScreenRotation` and `AVCaptureConnection.videoRotationAngle`, and add adaptive marker geometry, slot pagination, and tap-to-toggle overlay visibility.
- [Full-screen layout and HIG places sheet](issues/02-fullscreen-layout-and-hig-places-sheet.md): Use a full-screen `TabView` (`Camera` and `Map` tabs) retiring the top header and `PreferencesView`, place floating Search Radius `Menu` and Places trigger controls at bottom-leading (opposite the bottom-trailing AR Marker Page Picker), present the multi-category Places & Location UI in a native system `.sheet` (`[.medium, .large]` detents) with its own `NavigationStack` pushing an interactive map+list `CategoryPlacesScreen` (with Apple Maps handoff), and start with zero selected categories on launch.
- [Shared facade and custom location flow](issues/03-shared-facade-and-custom-location-flow.md): Put the reactive multi-category, reverse-geocoding, and custom-location search state machine (`PlacesCoordinator` with non-generic SKIE-bridged state enums, 1 s debounces, 35 s place timeout, and 4 s `RequestError` auto-clear) in `shared/commonMain` consumed by a thin `@MainActor @Observable NearbyPlacesModel` (using `activeLocation = userLocationEnabled ? userLocation : customLocation` without clearing saved `CustomLocation` across toggle switches), and render an inline Custom Location search mode inside the Places `.sheet`.
- [iPad adaptive layout and AR scaling](issues/04-ipad-adaptive-layout-and-ar-scaling.md): Enable universal iPhone/iPad support (`TARGETED_DEVICE_FAMILY: "1,2"`) with all 4 iPad orientations (`UISupportedInterfaceOrientations~ipad`) and full multitasking (no `UIRequiresFullScreen`), reuse the unified system `.sheet` across size classes (resizable `[.medium, .large]` bottom sheet in `.compact` width and centered iPadOS page sheet with a "Done" toolbar button and camera blur in `.regular` width), keep the fixed 2-tab `TabView` and floating bottom controls, and clamp AR marker width to `150...220 pt` (`divisor` in `2...6`) with `1...6` rows.
- [Sequenced spec and implementation tickets](issues/05-sequenced-spec-and-implementation-tickets.md): Consolidated the four design decisions into [Spec: iOS Feature, UX, and Orientation Parity + Native iPad Support](spec.md), created four ordered implementation tickets ([06: iOS AR camera blur, landscape orientation, and slot pagination](issues/06-ios-ar-camera-blur-orientation-pagination.md), [07: Shared PlacesCoordinator and address facade](issues/07-shared-places-coordinator-and-address-facade.md), [08: iOS full-screen tabs, HIG places sheet, and custom location UI](issues/08-ios-fullscreen-tabs-places-sheet-and-custom-location.md), and [09: Native iPad adaptive support and 4-orientation AR scaling](issues/09-ipad-adaptive-support-and-ar-scaling.md)), and cross-referenced the superseded/reworked tickets in `.scratch/kmp-ios-migration/`.

## Not yet specified

<!-- None: the map has reached its destination. -->

## Out of scope

- Changes to Android behavior or UI (Android remains the reference baseline and must stay green).
- Compose Multiplatform for shared UI (iOS UI remains 100% native SwiftUI / UIKit / Metal).
- Executing the implementation code inside the wayfinding tickets themselves (the terminal ticket of this map writes `spec.md` and the sequenced implementation tickets).
