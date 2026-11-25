Type: grilling
Status: resolved
Blocked by: 01, 02

## Question

Lock the native iPadOS architecture and adaptive layout (`TARGETED_DEVICE_FAMILY: "1,2"`):

1. **iPad Window & Sheet Presentation**:
   - How the places & location panel is presented on iPad across regular and compact width/height size classes (including Stage Manager / Split View multitasking): e.g., a persistent floating trailing side sheet / inspector card over the full-screen AR Camera and Map when width is regular, collapsing cleanly to the iPhone bottom/side sheet in compact multitasking windows.

2. **iPad AR Camera & Orientation Support**:
   - Supporting all four interface orientations on iPad (`Portrait`, `PortraitUpsideDown`, `LandscapeLeft`, `LandscapeRight`) in `Info.plist`, verifying `ScreenRotation` (`ROTATION_0`, `ROTATION_90`, `ROTATION_180`, `ROTATION_270`), camera `videoRotationAngle`, and AR marker width/row-count scaling on large iPad viewports so cards don't stretch disproportionately or leave excessive dead space.

## Answer

1. **Unified Native System `.sheet` Presentation Across iPhone & iPad**:
   - Use a single native SwiftUI `.sheet(isPresented: $showsPlacesSheet)` across all devices and size classes (matching Apple HIG *Sheets — Platform considerations*: *"Prefer page or form sheet styles in an iPadOS app (`UIModalPresentationStyle`); each uses a default size, centered over a dimmed background"*).
   - In compact width (`horizontalSizeClass == .compact`, including iPhone and narrow iPad Split View / Slide Over windows), the sheet presents as a resizable bottom sheet with `.presentationDetents([.medium, .large], selection: $sheetDetent)`, `.presentationDragIndicator(.visible)`, and `.presentationBackgroundInteraction(.enabled(upThrough: .medium))`.
   - In regular width (`horizontalSizeClass == .regular`), iPadOS automatically presents the sheet as a centered page sheet, and the root view inside the sheet's `NavigationStack` includes a top-trailing **"Done"** toolbar button for explicit one-tap / trackpad dismissal alongside swipe-down and outside-tap dismissal.
   - Set `ARCameraModel.sheetObscuresCamera` to `showsPlacesSheet && (horizontalSizeClass == .regular || sheetDetent == .large)` so the background AR camera feed blurs whenever the centered iPad page sheet or `.large` compact sheet obscures the center of the camera viewport.

2. **Universal Target (`TARGETED_DEVICE_FAMILY: "1,2"`), 4-Orientation `Info.plist`, and Multitasking**:
   - Set `TARGETED_DEVICE_FAMILY: "1,2"` in `iosApp/project.yml`.
   - In `iosApp/Support/Info.plist`, set `UISupportedInterfaceOrientations` to the 3 iPhone orientations (`UIInterfaceOrientationPortrait`, `UIInterfaceOrientationLandscapeLeft`, `UIInterfaceOrientationLandscapeRight`) and add `UISupportedInterfaceOrientations~ipad` with all 4 orientations (`UIInterfaceOrientationPortrait`, `UIInterfaceOrientationPortraitUpsideDown`, `UIInterfaceOrientationLandscapeLeft`, `UIInterfaceOrientationLandscapeRight`), without setting `UIRequiresFullScreen` so iPadOS Split View, Slide Over, and Stage Manager resizable windows work natively via `UILaunchScreen`.
   - Observe the active window scene's `UIInterfaceOrientation` (`UIWindowScene.interfaceOrientation`) and live window size changes to update `ScreenRotation` (`.rotation0`, `.rotation90`, `.rotation180`, `.rotation270`) and `AVCaptureConnection.videoRotationAngle` (`90°`, `0°`, `270°`, `180°`).

3. **AR Marker Card Width, Row Scaling, and Top-Level `TabView` on iPadOS**:
   - Scale marker width across iPhone and iPad viewports via `divisor = Int(usableWidth / 160).clamped(to: 2...6)` and clamp `markerWidth = (usableWidth / CGFloat(divisor)).clamped(to: 150...220)` so cards stay compact and readable on 11-inch / 13-inch iPad displays, while keeping `markerHeight` clamped to `72...88 pt` and `rows` clamped to `1...6` for cross-platform slot and pagination parity.
   - Keep the standard fixed 2-tab `TabView` (`Camera` and `Map`, without sidebar conversion) and the bottom-leading floating **Search Radius** `Menu` + **Places & Location** trigger pill and bottom-trailing **AR Marker Page Picker** across both iPhone and iPad.
