# 13: Migrate Hilt to Metro

**Spec:** `.scratch/kmp-ios-migration/specs/di.md`

**What to build:** The app assembles its dependency graph through a Metro `AppGraph` instead of Hilt, so the shared modules no longer depend on an Android-only DI framework. The application, activity, and view models re-wire manually (no Hilt bytecode transform). Android DI behaves identically.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] A Metro `AppGraph` assembles the full graph; the current provides/binds/inject sites map to Metro annotations.
- [x] `SightlineApp`, `MainActivity`, and `MainViewModel` re-wire manually with no Hilt annotation processor.
- [x] Hilt is removed from `core:common`, `api:photon`, `core:data`, and `app`.
- [x] Android compiles, tests pass, and injected behavior is identical.

## Comments

Migrated Hilt to Metro 1.5.2. Added `AppGraph` (`@DependencyGraph(AppScope::class) interface AppGraph : ViewModelGraph`) and `SightlineViewModelFactory` (`@ContributesBinding` `MetroViewModelFactory` subclass). `NetworkModule`/`PhotonApiModule`/`DataModule` became `@ContributesTo(AppScope::class) @BindingContainer` objects; the 2 `@Binds` became `@ContributesBinding` on `PlacesNetworkRepository`/`AddressNetworkRepository`. `SightlineApp` is a plain `Application` holding `createGraphFactory<AppGraph.Factory>().create(this)`; `MainActivity` drops `@AndroidEntryPoint` and wraps `setContent` in `CompositionLocalProvider(LocalMetroViewModelFactory provides ...)`; `MainViewModel` uses `@ViewModelKey @ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())`.

Removed Hilt and (now-dead) KSP plugin/deps. To satisfy Metro's compile-time graph resolution, changed several `implementation` -> `api` so leaf types reach the graph module. Verified `./gradlew :app:assembleDebug test` green; no `dagger`/`hilt`/`javax.inject` references remain.

Commit `7842121`, merged as `90887ae`-follow-up merge commit on the integration branch.
