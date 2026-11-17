Type: research
Status: resolved

## Question

Which DI framework replaces Hilt?

Current: Hilt 2.60.1, about 20 annotation sites across 6 files (3 @Module, 6 @Provides, 2 @Binds, @HiltAndroidApp, @AndroidEntryPoint, @HiltViewModel, 3 @Inject constructors).
Hilt is Android-only.

Leading candidate: Metro (compile-time KMP DI with a Hilt/Dagger migration path).
Evaluate Metro vs Koin vs Kotlin-Inject vs manual constructor injection on: KMP support (Android + iOS), compile-time vs runtime, migration effort from the existing Hilt modules, Android integration (view models, Application), iOS/Swift interop, and maturity/maintenance.

Recommend one.

## Answer

Recommend **Metro** (`dev.zacsweers.metro`, stable 1.5.2, Nov 2026).
It is the only compile-time, first-class-KMP option and is built explicitly as the Hilt/Dagger migration path; Sightline's 3-module / 6-@Provides / 2-@Binds / 3-@Inject graph maps almost 1:1 onto its annotations.

Caveats:

- Metro is a Kotlin compiler plugin (FIR/IR), not KSP - faster and no per-target KSP wiring, but a small Kotlin-version coupling risk (Metro supports Kotlin 2.4.21).
- No Hilt bytecode transform: `@HiltAndroidApp` / `@AndroidEntryPoint` have no equivalent.
  `SightlineApp` becomes a plain `Application` holding `createGraph<AppGraph>()`; `MainActivity` drops `@AndroidEntryPoint` and provides a view-model factory; `@HiltViewModel` becomes `@ViewModelKey @ContributesIntoMap` via `metrox-viewmodel-compose`.
- The real work is not DI: the `Context` / `File` / `DataStore`-dependent `@Provides` methods still need the platform split tickets 03/04/05 mandate; Metro's graph model absorbs that split cleanly.
- Alternatives: Koin 4.2.2 is runtime service-locator forcing a module-DSL rewrite; Kotlin-Inject 0.9.0 is pre-1.0, KSP-per-target, no Android entry points.

Full findings: [research/06-dependency-injection-kmp.md](research/06-dependency-injection-kmp.md).
