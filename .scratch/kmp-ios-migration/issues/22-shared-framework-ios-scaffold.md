# 22: Add shared framework + SwiftUI iosApp + first end-to-end slice

**Spec:** `.scratch/kmp-ios-migration/specs/module-architecture.md`, `.scratch/kmp-ios-migration/specs/navigation.md`

**What to build:** A `shared` XCFramework exports the shared pipeline through SKIE, and a SwiftUI `iosApp` fetches nearby places for a chosen category via the shared location -> photon -> data -> model pipeline and shows them in a list. This proves the full shared stack works on iOS.

**Blocked by:** 15, 16, 17, 18, 19, 20

**Status:** claimed

- [ ] A `shared` XCFramework exports the shared modules with SKIE interop (Flow to AsyncSequence, suspend to async).
- [ ] The `iosApp` builds and fetches real nearby places end to end on a simulator or device.
- [ ] Android remains unchanged and green.
