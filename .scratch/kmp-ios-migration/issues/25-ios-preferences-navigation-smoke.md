# 25: iOS preferences, category navigation, and full smoke gate

**Spec:** `.scratch/kmp-ios-migration/specs/persistence.md`, `.scratch/kmp-ios-migration/specs/navigation.md`

**What to build:** An iOS user's preferences persist across launches, the category screen pushes/pops through a SwiftUI `NavigationStack`, and a build-and-run smoke pass confirms camera, map, category, location, and preferences all work on a simulator or device.

**Blocked by:** 22, 23, 24

**Status:** ready-for-agent

- [ ] Preferences persist across launches via the shared `UserPreferencesLocalRepository` and `OkioStorage`.
- [ ] The category screen pushes and pops with standard back navigation carrying `category` and `places`.
- [ ] The iOS app passes a full build-and-run smoke pass covering camera, map, category, location, and preferences.
