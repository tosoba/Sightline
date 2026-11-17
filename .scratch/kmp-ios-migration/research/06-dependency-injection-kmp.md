# DI framework for the KMP migration

Research ticket 06: which DI framework replaces Hilt.

## Recommendation

Adopt **Metro** (`dev.zacsweers.metro`, Zac Sweers).

It is the only option that is compile-time, first-class KMP, and designed explicitly as the Hilt/Dagger migration path, and Sightline's graph (3 modules, 6 `@Provides`, 2 `@Binds`, 3 `@Inject` constructors) maps almost one-to-one onto its annotations.

Caveats: Metro is a Kotlin compiler plugin (not KSP), so it carries a small version-coupling risk with the Kotlin compiler; it has no `@HiltAndroidApp`/`@AndroidEntryPoint` bytecode transform, so the two Android entry points (`SightlineApp`, `MainActivity`) become a few lines of manual glue; and the `Context`/`File`/`DataStore`-dependent `@Provides` methods still need the platform split that tickets 03/04/05 already require.

Versions as of 2026-11-17: Metro **1.5.2** (stable), Koin **4.2.2**, Kotlin-Inject **0.9.0** (pre-1.0).

---

## 1. Metro

### Status and version

- Stable, 1.x line. `1.0.0` shipped 2026-04-27; latest is `1.5.2` (2026-11-03), with a release roughly every one to two weeks through late 2026.
- Apache 2.0, `dev.zacsweers.metro` on Maven Central.
- Solo primary maintainer (Zac Sweers), but heavily co-developed with production users: Cash App, Square/Block, Vinted, BandLab, and Freeletics all migrated large Android codebases from Dagger/Anvil to Metro in 2024-2026 and now contribute. Square/Block deprecating Anvil in Metro's favor is the strongest signal of ecosystem momentum.

### Codegen model: a compiler plugin, not KSP

This corrects a premise in the ticket. Metro does not use KSP or KAPT. It is implemented as a Kotlin compiler plugin that generates code directly into FIR/IR, which gives it two advantages over annotation processing:

- No extra frontend pass to analyze sources and emit stub sources, so it is faster than Dagger+KAPT and needs no KSP per-target wiring.
- It can do things source generation cannot, such as private `@Provides` declarations and private member injection.

It validates the graph at compile time and emits Dagger-style code (factories, `DoubleCheck`) with a Dagger-like runtime. It does not support Java (irrelevant for Sightline's all-Kotlin codebase).

### Targets

JVM, Android, iOS (`x64`, `arm64`, `simulatorArm64`), macOS, JS, Wasm, Linux, Windows, and Android Native. Runtime and `runtime-coroutines` are available on all targets. Multi-module aggregation needs Kotlin 2.3.0+ (JVM) / 2.3.20+ (Android and Apple). Sightline is on Kotlin 2.4.21, which is within Metro's supported window (the compatibility table lists 2.4.20 with open-ended forward support, plus 2.5.x dev builds).

### Annotation mapping from Dagger/Hilt

Metro deliberately reuses Dagger's vocabulary, so the migration is largely a find-and-replace of imports and component wiring.

| Dagger/Hilt | Metro equivalent | Notes |
| --- | --- | --- |
| `@Inject constructor` | `@Inject` on the class (single constructor) or `@Inject constructor` | Same name. |
| `@Module @InstallIn(SingletonComponent::class)` | `@ContributesTo(Singleton::class) @BindingContainer` object/interface | `@InstallIn`'s component maps to a scope used as the graph aggregation key. |
| `@Provides` | `@Provides` | Same name; may be private. |
| `@Binds` | `@Binds` (extension-receiver syntax) or `@ContributesBinding(Scope::class)` on the impl class | `@ContributesBinding` is the idiomatic multi-module form and auto-binds the impl to its single supertype. |
| `@Singleton` | `@Singleton` (Metro maps Hilt `SingletonComponent` to `@Singleton`) or `@SingleIn(AppScope::class)` | |
| `@BindsInstance` | `@Provides` on a `@DependencyGraph.Factory` creator parameter | Consolidated to one annotation. |
| `@IntoSet` / `@IntoMap` | `@ContributesIntoSet` / `@ContributesIntoMap` | Map keys use `@MapKey` annotations, not function-style pairs. |
| `Provider<T>` / `dagger.Lazy<T>` | `() -> T` / stdlib `Lazy<T>` | Metro defaults to function-syntax providers. |
| `@ApplicationContext Context` | plain `Context` / `Application` parameter | No qualifier needed. |
| `@HiltAndroidApp` | plain `Application` subclass holding `createGraph<AppGraph>()` | See below. |
| `@AndroidEntryPoint` | constructor injection via `AppComponentFactory` (metrox-android) or manual `graph.inject(this)` member injection | See below. |
| `@HiltViewModel` | `@ViewModelKey @ContributesIntoMap(Scope::class)` | See below. |
| `hiltViewModel()` | `metroViewModel()` (metrox-viewmodel-compose) | |

Multibindings and scopes are first-class, so the "3-module / 6-provide / 2-bind" shape of Sightline's graph translates directly. There is no `@Reusable` (Metro intentionally omits it) and no dagger-android support (long-deprecated), neither of which Sightline uses.

### Android integration

Metro does not replicate Hilt's AGP bytecode transform, so there is no literal `@AndroidEntryPoint` that rewrites an Activity's superclass. The Android surface is the **MetroX** artifact family:

- **metrox-android** - `AppComponentFactory`-based constructor injection of Activity/Service/Receiver/ContentProvider. This is the closest thing to `@AndroidEntryPoint`, but it requires **minSdk 28** (Sightline is minSdk 24).
- **metrox-viewmodel** - `MetroViewModelFactory`, a `ViewModelProvider.Factory` built from map multibindings of `@ViewModelKey`/`@ContributesIntoMap`. This is the direct replacement for `@HiltViewModel` and has no minSdk 28 requirement.
- **metrox-viewmodel-compose** - `metroViewModel()`, `LocalMetroViewModelFactory`, `CompositionLocalProvider`. The replacement for `hiltViewModel()`.

The practical path for Sightline, which does not constructor-inject any Activity (its `MainActivity` has no `@Inject` fields and only calls `hiltViewModel()`), avoids the minSdk 28 constraint entirely:

1. `SightlineApp` becomes a plain `Application` that does `appGraph = createGraph<AppGraph>()` in `onCreate` (no annotation).
2. `MainViewModel` drops `@HiltViewModel`, gains `@ViewModelKey @ContributesIntoMap(AppScope::class)`.
3. `MainActivity` drops `@AndroidEntryPoint`, wraps `setContent` in `CompositionLocalProvider(LocalMetroViewModelFactory provides graph.metroViewModelFactory)`, and switches `hiltViewModel<MainViewModel>()` to `metroViewModel<MainViewModel>()`.

`metrox-android` (and thus minSdk 28) is only needed if Sightline later wants constructor-injected Activities/Services. It is not required for the current graph.

### iOS/Swift interop

Metro generates ordinary Kotlin classes, so the iOS side consumes the graph the same way any KMP object graph is exposed to Swift:

- The shared framework exports the graph interface and its accessors. The canonical `AppGraph` lives in `commonMain`; the final `@DependencyGraph` type (e.g. `IosAppGraph`) is declared in `iosMain` so it can see platform contributions, per the Metro multiplatform docs.
- A small Kotlin facade (`initKoin`-style) creates the graph once at app launch (`iOSApp.init`) and exposes typed accessors (e.g. `repositories`, `useCases`) as `val`/`fun` on the graph interface. Swift calls those accessors directly.
- No reflection, no service-locator facade needed beyond "hold the graph and expose accessors" - simpler than Koin's `KoinComponent` bridge, because graph members are plain typed properties.

### Maturity and maintenance in 2026

Stable and actively maintained, with weekly releases, a documented compatibility matrix, a JetBrains IDE plugin, and a sponsorship program. The main structural risks are the ones its own FAQ acknowledges: Kotlin compiler plugins are not a stable API (Metro mitigates with a wide `N+.2` forward-compat window), and it is ultimately one maintainer's project (though now with significant corporate backing from Cash App and Block). Kotlin-version coupling is the one recurring cost: compiler-plugin DI means a Kotlin upgrade can require waiting for or pinning a Metro release.

---

## 2. Koin

- Current version **4.2.2**, with the **Koin Compiler Plugin 1.2.1** (requires Kotlin 2.3.20+). Apache 2.0, maintained by Kotzilla plus a large community.
- Fully KMP: `koin-core` runs on every target; `koin-android`/`koin-compose`/`koin-compose-viewmodel` add platform APIs.
- Model is a **runtime service-locator**: modules are DSL blocks of lambdas (`single { }`, `factory { }`, `viewModel { }`); objects are resolved at runtime with no reflection. Historically, a missing binding surfaced as a runtime crash. The new Compiler Plugin adds optional compile-time graph verification, and `koin-annotations` (`@Singleton`, `@Factory`, `@KoinViewModel`, `@Module`, `@ComponentScan`) is JSR-330-compatible, so it can now look Hilt-like and be compile-checked.
- Android: `androidContext`/`startKoin` from `Application.onCreate`, `by inject()`/`koinViewModel()` in Activities and Compose. iOS: a Kotlin `KoinComponent` facade must expose dependencies to Swift, because Swift cannot call Koin's reified `get<T>()` directly.
- Migration from Sightline's Hilt modules would be a **rewrite, not a translation**: the 3 `@Module` classes become DSL `module { }` blocks, the 2 `@Binds` become `single<PlacesRepository> { PlacesNetworkRepository(get()) }`, the 6 `@Provides` become lambda bodies. The annotation sites do not map across; the wiring logic is restated in a different idiom. Roughly the same amount of code, but none of it is mechanical find-and-replace.

Koin is the widest-adopted and best-documented KMP DI option, and it is the safe default for teams that do not need compile-time guarantees. Its trade-off is that resolution is fundamentally runtime, with compile safety bolted on as an optional second style, and the iOS side always needs a Kotlin facade.

---

## 3. Kotlin-Inject

- Current version **0.9.0** (2026-01-07), still **pre-1.0**. Apache 2.0, `me.tatarka.inject`, by Evan Tatarka.
- KMP compile-time DI modeled on Dagger, using **KSP** (one `ksp` configuration per target: `kspCommonMainMetadata`, `kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`, etc.).
- API is components as abstract classes/annotations (`@Component`, `@Provides`, `@Inject`, `@Scope`, `@Qualifier`), generated implementations plus per-target `create()` extension functions.
- **No Hilt-style Android entry points.** Like Dagger (not Hilt), it is constructor-injection-only; Android framework classes are handled manually via a hand-built `ApplicationComponent` and subcomponent/helper glue. There is no `@HiltViewModel`/`@AndroidEntryPoint` equivalent and no first-party ViewModel factory.
- Ecosystem: `kotlin-inject-anvil` (originally Amazon's) added Anvil-style `@ContributesTo`/`@ContributesBinding` aggregation, but it is now in **maintenance mode** and its README directs new users to Metro ("We moved our projects from kotlin-inject-(anvil) to Metro").

Kotlin-Inject is the closest conceptual ancestor of Metro (Metro's user-facing API is "kotlin-inject-esque"). For a fresh KMP migration there is little reason to choose it over Metro in 2026: it is pre-1.0, needs per-target KSP wiring, has no Android entry-point story, and its ecosystem's anvil layer has already migrated to Metro. Its main appeal is to teams already on it.

---

## 4. Manual constructor injection + a small platform composition root

Given only ~20 annotation sites, hand-writing the graph is genuinely feasible and has zero framework risk:

- Define a `commonMain` interface exposing `PlacesRepository`, `AddressRepository`, `UserPreferencesRepository`, `PhotonApi`, and the `OkHttpClient`.
- One `object`/function per platform constructs the singletons in dependency order (roughly 8-10 lines of construction each for Android and iOS).
- The 3 `@Inject` constructors become plain classes; the 2 `@Binds` become trivial `= PlacesNetworkRepository(photonApi)` assignments; the 6 `@Provides` become plain functions.
- Android wiring: `SightlineApp` holds the singletons; `MainViewModel` is built in a factory or `viewModel { }` lambda. iOS wiring: one `init` function that constructs and stashes the singletons.

This is the lowest-dependency option and the right answer for libraries or tiny apps. Its cost is that the "3-module" structure dissolves into a hand-maintained construction file, and there is no compile-time validation of the wiring (a wrong constructor order is a runtime `NullPointerException` rather than a build error). At ~20 sites the cost is small, but it grows linearly and loses the "add an `@Inject` constructor and you are done" ergonomics as shared code expands.

---

## 5. Migration-effort assessment for the existing graph

The current Hilt graph is:

- `PhotonApiModule` (1 `@Provides`), `NetworkModule` (3 `@Provides`), `DataModule` (2 `@Binds` + 2 `@Provides`).
- 3 `@Inject` constructors (`AddressNetworkRepository`, `PlacesNetworkRepository`, `MainViewModel`).
- 3 Android surface sites (`@HiltAndroidApp` on `SightlineApp`, `@AndroidEntryPoint` on `MainActivity`, `@HiltViewModel` on `MainViewModel`).

The Metro migration is a near 1:1 translation:

| Site | Change |
| --- | --- |
| 3 `@Inject` constructors | Change `javax.inject.Inject` import to `dev.zacsweers.metro.Inject`. Mechanical. |
| `PhotonApiModule`, `NetworkModule`, `DataModule` | Replace `@Module @InstallIn(SingletonComponent::class)` with `@ContributesTo(Singleton::class) @BindingContainer`; keep the `@Provides` bodies. Mechanical. |
| 2 `@Binds` | Move to `@ContributesBinding(Singleton::class)` on `PlacesNetworkRepository` and `AddressNetworkRepository` (recommended) or keep as `@Binds` in a binding container. Trivial. |
| `@HiltAndroidApp` | Delete; make `SightlineApp` a plain `Application` that calls `createGraph<AppGraph>()`. A few lines. |
| `@AndroidEntryPoint` | Delete; wrap `setContent` in `CompositionLocalProvider(LocalMetroViewModelFactory provides graph.metroViewModelFactory)`. A few lines. |
| `@HiltViewModel` | Replace with `@ViewModelKey @ContributesIntoMap(AppScope::class)`; swap `hiltViewModel()` for `metroViewModel()`. A few lines. |
| New | One `@DependencyGraph(AppScope::class) interface AppGraph` declaring the accessors and a `@Provides` `Application`/`Context` binding (the Hilt `@ApplicationContext` default). |
| Build | Drop the `com.google.dagger.hilt.android` Gradle plugin and `hilt-*`/`androidx.hilt` deps; apply `dev.zacsweers.metro`. No KSP needed. |

The DI-specific work is roughly half a day to a day of mechanical edits.

The genuinely non-mechanical part is not DI at all but the platform split that tickets 03/04/05 already mandate:

- `NetworkModule.provideCache` uses `android.content.Context` + `java.io.File`, and `PhotonApiModule` uses Android `BuildConfig.DEBUG`. Both must move to platform source sets (Android uses the existing impl; iOS provides an NSFileManager/NSUserDefaults-backed equivalent).
- `DataModule.provideUserPreferencesDataStore` uses AndroidX DataStore, which ticket 05 is replacing with a KMP storage option anyway.

Because Metro's graph model already assumes this shape - a common graph interface plus `@Provides` contributed from `androidMain`/`iosMain` - the DI layer is ready to absorb the platform split those tickets produce. Koin would absorb it too, but via a runtime module list; Kotlin-Inject via KSP per-target components.

---

## 6. Why Metro over the alternatives

- Over Koin: compile-time validation and Dagger-familiar annotations, which preserve the existing module structure and 1:1 map from Hilt. Koin is fine and more popular, but it is a runtime service-locator at heart and forces a rewrite of the module DSL rather than a translation.
- Over Kotlin-Inject: Metro is the direct successor (kotlin-inject's anvil layer has already moved to Metro), is stable 1.x vs pre-1.0, and needs no per-target KSP configuration.
- Over manual injection: Metro keeps the "annotate a constructor, done" ergonomics and compile-time graph checking as the shared core grows, at near-zero additional cost for a graph this size.

Net recommendation: migrate Hilt to Metro, using the interop mode only if a transitional dual-build (Hilt and Metro both compiling, gated by a Gradle property, as Cash App and Square did) is wanted; otherwise do the full mechanical translation in one pass.
