# 13: Migrate Hilt to Metro

**Spec:** `.scratch/kmp-ios-migration/specs/di.md`

**What to build:** The app assembles its dependency graph through a Metro `AppGraph` instead of Hilt, so the shared modules no longer depend on an Android-only DI framework. The application, activity, and view models re-wire manually (no Hilt bytecode transform). Android DI behaves identically.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] A Metro `AppGraph` assembles the full graph; the current provides/binds/inject sites map to Metro annotations.
- [ ] `SightlineApp`, `MainActivity`, and `MainViewModel` re-wire manually with no Hilt annotation processor.
- [ ] Hilt is removed from `core:common`, `api:photon`, `core:data`, and `app`.
- [ ] Android compiles, tests pass, and injected behavior is identical.
