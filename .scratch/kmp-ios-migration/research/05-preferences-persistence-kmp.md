# Research: user-preferences persistence to KMP

Ticket: `.scratch/kmp-ios-migration/issues/05-preferences-persistence-kmp.md`

## TL;DR

Keep AndroidX DataStore (the typed flavor), but swap the two JVM-only pieces for multiplatform equivalents.

1. Replace the protobuf-generated `UserPreferences` binding with a `@Serializable` data class serialized by kotlinx.serialization JSON.
2. Replace `DataStoreFactory.create(produceFile = File(...))` with a platform `Storage` (`FileStorage` on Android, `OkioStorage` on iOS) behind an expect/actual.

Everything else (the `Flow` API, `updateData`, corruption handling, the repository) stays essentially unchanged.

The blocker is not DataStore itself, it is the protobuf bindings and the `context.filesDir` file path.

## Findings

### 1. AndroidX DataStore KMP status (2026): stable and multiplatform

DataStore is now a genuine multiplatform library, stable at `1.3.0` (released October 21, 2026).

- KMP support landed in `1.1.0` (April 2024), with non-Android targets marked experimental at the time.
- By `1.3.0` the library is stable and publishes native targets.

Confirmed by reading the published Gradle module metadata for `androidx.datastore:datastore-core:1.3.0`:

- common metadata, Android (androidJvm), JVM, JS, WasmJS.
- Native: `iosArm64`, `iosSimulatorArm64`, `macosArm64`, `linuxArm64`, `linuxX64`, `tvosArm64`, `tvosSimulatorArm64`, `watchosArm64`, `watchosDeviceArm64`, `watchosSimulatorArm64`, `watchosArm32`.
- The typed artifact `androidx.datastore:datastore:1.3.0` publishes the same target set.

Two artifact forms (both `1.3.0`):

- `androidx.datastore:datastore` and `datastore-preferences` include an Android dependency (`Context` delegates, `dataStoreFile`).
- `androidx.datastore:datastore-core` and `datastore-preferences-core` are Android-free and are what common code should depend on.

Caveat: no `iosX64` target is published.

Only `iosArm64` (device) and `iosSimulatorArm64` (Apple Silicon simulator) are available.
Intel Mac iOS simulators are unsupported.
In practice this is a non-issue on current hardware and matches Kotlin's own deprecation of `iosX64`.

Non-Android API (no `context.filesDir`).

The official KMP guide (`developer.android.com/kotlin/multiplatform/datastore`) shows the pattern:

- Common: `DataStoreFactory.create(storage = storage)` where `storage: Storage<T>`.
- Android: `FileStorage(serializer = ..., produceFile = { context.filesDir.resolve(fileName) })`.
- iOS: `OkioStorage(fileSystem = FileSystem.SYSTEM, serializer = ..., producePath = { ...NSDocumentDirectory path... })`.

The `Storage<T>` and `Serializer<T>` interfaces are generic, so this works for a typed DataStore, not only for the Preferences flavor.

Note on the official wording: the KMP guide page currently says "Only DataStore Preferences is supported in KMP projects".
That statement describes the flavor Google documents end-to-end, not a hard technical limit.
The typed `DataStore<T>` with a custom `Serializer<T>` shares the same `datastore-core` primitives (`Serializer`, `Storage`, `FileStorage`, `OkioStorage`, `DataStoreFactory`) and works on iOS.
The real constraint is that the serialized payload type must be multiplatform, which the protobuf bindings are not (see section 5).

Version `1.3.0` also added `DataStore.Builder<T>` as the recommended replacement for `DataStoreFactory`.
It takes a `CoroutineContext` instead of a `CoroutineScope`.
`DataStoreFactory.create(storage = ...)` still works and is what the KMP guide shows.

### 2. multiplatform-settings (com.russhwolf:multiplatform-settings)

Current version `1.3.0` (published November 2024, still the latest on Maven Central).

- iOS/macOS/tvOS/watchOS: `NSUserDefaultsSettings` (wraps `NSUserDefaults`).
- Android: `SharedPreferencesSettings` (wraps `SharedPreferences`).
- JVM, JS/Wasm, Windows, and an in-memory `MapSettings` for tests.
- A coroutines module adds `Flow`/`StateFlow` extensions and `FlowSettings`.
- A serialization module stores a `@Serializable` object under one key.

It fits 11 scalar prefs fine on a key-value basis, and it is the right tool for key-value preferences.

For this repo it is a downgrade, not a simplification:

- The current code already uses a typed DataStore with `Flow` and atomic `updateData`.
- Flattening `UserPreferences` into 11 individual keys loses the single typed snapshot and the atomic read-modify-write.
- It adds a dependency that has not released since November 2024 (stable, but low churn).

Keep it as the fallback if DataStore is ever rejected, but it is not the best fit here.

### 3. SQLDelight

Current version `2.4.1` (October 2026).

Overkill for 11 scalar preferences.

- It brings SQL codegen, a schema `.sq` file, and a per-platform native driver (Android driver vs `NativeSqliteDriver` on iOS).
- The repo has no relational queries, no entities, no joins.
- The payoff (typed queries, migrations) is irrelevant for a single flat config object.

Reject.

### 4. Plain kotlinx.serialization file-backed storage (okio / expect-actual filesystem)

Simplest in dependency count (kotlinx.serialization JSON is already in the project via `core:model`, and okio `3.18.2` is multiplatform).

But it reimplements, by hand, everything DataStore already gives you:

- Atomic writes (write-temp-then-rename).
- Corruption detection and a recovery path.
- A `Flow` that emits on every write and on first read.
- Serialized, transactionally consistent read-modify-write.

Each of these is a real correctness trap (torn writes on crash, partial reads, missing invalidation).
Because DataStore is now multiplatform and stable, hand-rolling a file store is needless risk with no benefit for this use case.

Reject as the primary path.
It is the correct answer only if the team decides to drop DataStore entirely.

### 5. The protobuf bindings are the actual JVM-only blocker

The current `UserPreferences` type is generated by the protobuf Gradle plugin from `core/datastore/src/main/proto/user_prefs.proto`, using the `kotlin` builtin with `option("lite")`.

Google's protobuf runtimes are JVM-only.

Confirmed by Maven Central: `com.google.protobuf:protobuf-kotlin-lite:4.36.2` publishes a single `protobuf-kotlin-lite-4.36.2.jar` plus sources/javadoc.
There is no Gradle KMP metadata (`*.module`) and no `iosArm64`/`iosSimulatorArm64` variant.
The same holds for `protobuf-kotlin` (full) and the underlying Java runtimes.

So the generated `UserPreferences` class, and the `.proto` + protobuf plugin setup, cannot compile for Kotlin/Native (iOS).
This is independent of DataStore.

Consequence: the `UserPreferences` type must become a `@Serializable` data class, no matter which storage option is chosen.

The existing schema maps trivially:

- `bool user_location_enabled = 1` to `val userLocationEnabled: Boolean = false`.
- each `optional double`/`optional string`/`optional int32` to a nullable property with a `null` default (`Double?`, `String?`, `Int?`), preserving the proto "absent vs zero" distinction that the repository currently relies on (`hasCustomLocationLat()` etc.).

## Recommendation

Keep AndroidX DataStore (typed, `datastore-core`), backed by a kotlinx.serialization JSON serializer, with the file location behind an expect/actual.

Rationale:

- Least churn: the repository already speaks DataStore (`Flow`, `updateData`, `Serializer`).
- Keeps atomic writes, corruption handling, and `Flow` observation for free.
- Uses an official, stable, actively maintained library (`1.3.0`, October 2026).
- Removes the only two JVM-only pieces (protobuf bindings, `context.filesDir`).

JSON over protobuf-format: 11 scalar fields, human-readable for debugging, and `kotlinx-serialization-json` is already a dependency.
`kotlinx-serialization-protobuf` (`1.13.0`) exists as a binary alternative if size ever matters, but it is not worth the extra dependency here.

## Migration steps

1. Add a `@Serializable` data class `UserPreferences` in `core:model` (already pure Kotlin, already on kotlinx.serialization).

   - `val userLocationEnabled: Boolean = false`.
   - `val customLocationLat: Double? = null`, `customLocationLng: Double? = null`, `customLocationAddress: String? = null`, `customLocationTimestamp: Long? = null`.
   - `val lastMapLat: Double? = null`, `lastMapLng: Double? = null`, `lastMapZoom: Double? = null`, `lastMapBearing: Double? = null`, `lastMapTilt: Double? = null`.
   - `val searchRadius: Int? = null`.

2. Rewrite the serializer against `androidx.datastore.core.Serializer<UserPreferences>` using `Json`.

   - `readFrom`: `Json.decodeFromString(input.readBytes().decodeToString())`, catch `SerializationException` and rethrow as `CorruptionException`.
   - `writeTo`: write `Json.encodeToString(...).encodeToByteArray()`.
   - `defaultValue = UserPreferences()`.

   This mirrors the DataStore guide's documented "JSON DataStore" serializer.

3. Move `core/datastore` to a KMP module (or fold it into the shared `core:data` module the architecture ticket defines).

   - `commonMain`: the `Serializer` plus `fun createDataStore(storage: Storage<UserPreferences>): DataStore<UserPreferences> = DataStoreFactory.create(storage)`.
   - `androidMain`: `FileStorage(serializer, produceFile = { context.filesDir.resolve("datastore/user_preferences.json") })`.
   - `iosMain`: `OkioStorage(fileSystem = FileSystem.SYSTEM, serializer, producePath = { NSDocumentDirectory path + "/user_preferences.json" })`.

   Dependencies become `androidx.datastore:datastore-core:1.3.0` in `commonMain`, and the Android-only `datastore`/`datastore-preferences` artifacts can be dropped.

4. Update `UserPreferencesLocalRepository`.

   - `preferences.toBuilder().setX(...).build()` becomes `preferences.copy(x = ...)`.
   - `hasX()` checks become null checks (`customLocationLat != null` etc.), which is exactly what `hasX()` encoded for proto `optional` fields.

   No other logic changes.

5. Remove the protobuf Gradle plugin, `protobuf-kotlin-lite`, and the `.proto` file from `core/datastore`.
   Delete the generated `UserPreferences` binding.

6. Migrate existing Android installs once.

   - iOS is greenfield (no existing users), so migration is Android-only.
   - On first launch after upgrade, read the old `user_preferences.pb` through the old protobuf serializer, map it to the `@Serializable` `UserPreferences`, write it through the new JSON serializer, then delete the old file.
   - Given 11 low-stakes scalar fields, an acceptable simpler alternative is to accept a one-time reset to defaults, but the read-once migration is cheap and preserves the user's custom location and map state.

## Version reference (as of November 2026)

| Library | Version | Note |
| --- | --- | --- |
| androidx.datastore:datastore-core | 1.3.0 | stable, multiplatform, iOS targets published |
| com.russhwolf:multiplatform-settings | 1.3.0 | latest (Nov 2024), key-value |
| app.cash.sqldelight:runtime | 2.4.1 | latest, overkill here |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.13.0 | repo currently on 1.12.0 |
| org.jetbrains.kotlinx:kotlinx-serialization-protobuf | 1.13.0 | optional binary alternative |
| com.squareup.okio:okio | 3.18.2 | used by `OkioStorage` |
| com.google.protobuf:protobuf-kotlin-lite | 4.36.2 | JVM-only, blocker |

## Sources

- AndroidX DataStore release notes: `developer.android.com/jetpack/androidx/releases/datastore`
- DataStore KMP guide: `developer.android.com/kotlin/multiplatform/datastore`
- DataStore user guide: `developer.android.com/topic/libraries/architecture/datastore`
- Published Gradle module metadata: `androidx/datastore/datastore-core/1.3.0/datastore-core-1.3.0.module` and `.../datastore/1.3.0/datastore-1.3.0.module` (Google Maven)
- multiplatform-settings README and Maven Central metadata: `github.com/russhwolf/multiplatform-settings`
- Maven Central metadata for SQLDelight, kotlinx-serialization, okio, and `com.google.protobuf:protobuf-kotlin-lite`
