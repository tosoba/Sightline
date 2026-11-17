# Dependency injection

Groundwork spec for replacing Hilt with Metro.

Source: issue 06-dependency-injection-kmp.md.

## Decision

Use Metro (`dev.zacsweers.metro`, stable 1.5.2).
It is the only compile-time, first-class-KMP option and is built as the Hilt/Dagger migration path.

## Current

Hilt 2.60.1, ~20 annotation sites across 6 files: `SightlineApp` (@HiltAndroidApp), `MainActivity` (@AndroidEntryPoint), `MainViewModel` (@HiltViewModel), `DataModule` (2 @Binds + 2 @Provides), `PhotonApiModule` (1 @Provides), `NetworkModule` in `core:common`.

## Target design

- A Metro `AppGraph` assembles the full graph; Sightline's 3-module / 6-@Provides / 2-@Binds / 3-@Inject graph maps almost 1:1 onto Metro's annotations.
- Android entry points re-wire manually, since Metro has no Hilt bytecode transform:
  - `SightlineApp` becomes a plain `Application` holding `createGraph<AppGraph>()`.
  - `MainActivity` drops `@AndroidEntryPoint` and provides a view-model factory.
  - `@HiltViewModel` becomes `@ViewModelKey @ContributesIntoMap` via `metrox-viewmodel-compose`.
- The `Context`/`File`/`DataStore`-dependent providers stay platform-split (see specs/location.md, specs/persistence.md, specs/networking.md); Metro's graph model absorbs that split.

## Caveats

- Metro is a Kotlin compiler plugin (FIR/IR), not KSP: faster, no per-target KSP wiring, but a Kotlin-version coupling risk (Metro supports Kotlin 2.4.21).
- Koin 4.2.2 is runtime service-locator requiring a module-DSL rewrite; Kotlin-Inject 0.9.0 is pre-1.0, KSP-per-target, no Android entry points. Both rejected.
