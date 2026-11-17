# Navigation

Groundwork spec for per-platform navigation.

Source: issue 07-target-shared-architecture.md (UI stays native).

## Decision

Navigation stays platform-native; there is no shared navigation abstraction.
Android keeps Navigation 3; iOS uses SwiftUI `NavigationStack`.

## Current (Android)

Navigation 3 with two routes (`app/MainActivity.kt`, `app/MainRoute.kt`, `feature/place-category/PlaceCategoryRoute.kt`):
- `MainRoute` (data object) -> `MainScreen` (the camera/map pager).
- `PlaceCategoryRoute(category, places)` (data class) -> `PlaceCategoryScreen`, pushed on category click, popped on back.
`rememberNavBackStack` + `NavDisplay` + `entryProvider` drive it.

## Target design (iOS)

Map the two routes to a SwiftUI `NavigationStack` with a path array:
- Root: the camera/map screen (SwiftUI equivalent of `MainScreen`), with the two-page pager (Camera / Map).
- Push destination: the place-category screen (`PlaceCategoryScreen` equivalent), carrying `category` and `places`.
- Back is the standard navigation-bar back / swipe gesture.

## Shared state

The view-model state behind navigation (`MainViewModel`'s user location, places, custom location, search results, map position, radius) becomes shared Kotlin code exposed to Swift via SKIE (Flow -> AsyncSequence, suspend -> async).
The SwiftUI screens consume that state; navigation itself is a pure-UI concern and stays in SwiftUI.

## Out of scope

- A KMP navigation library (Voyager, Decompose, etc.).
- Sharing the back-stack as state across platforms.
