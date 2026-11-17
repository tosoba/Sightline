Type: research
Status: resolved

## Question

How should user-preferences persistence migrate to KMP?

Current: AndroidX DataStore with a single Protobuf message (`core/datastore/src/main/proto/user_prefs.proto`, 11 fields: location toggle, custom location, last map camera, search radius), a DataStore serializer, and `DataStoreFactory.create` with `context.filesDir` (`core/data/DataModule.kt`, `UserPreferencesLocalRepository.kt`).
No database.

Research and report:

- AndroidX DataStore KMP status and maturity.
- multiplatform-settings, SQLDelight, and plain kotlinx.serialization (JSON or Protobuf) file-backed storage.
- Which is the simplest robust fit for 11 scalar preferences.

Recommend one, with migration steps from the existing protobuf.

## Answer

Keep AndroidX DataStore (typed); swap the two JVM-only pieces for multiplatform equivalents.

- DataStore is now stable and truly multiplatform - `androidx.datastore:datastore-core:1.3.0` (Oct 2026) publishes `iosArm64` + `iosSimulatorArm64` (plus macOS/linux/JVM/JS/Wasm).
  The non-Android API is `DataStoreFactory.create(storage = Storage<T>)`, with `FileStorage` on Android and `OkioStorage(FileSystem.SYSTEM, ...)` on iOS behind expect/actual.
- The real blocker is not DataStore, it is protobuf: `protobuf-kotlin-lite`/`protobuf-kotlin` bindings are JVM-only, so the generated `UserPreferences` class cannot compile on iOS.
  It must become a `@Serializable` data class regardless of storage choice - the 11 fields map 1:1 (`optional` -> nullable, `bool` -> `Boolean = false`).
- multiplatform-settings (1.3.0) fits key-value but is a downgrade - it loses the typed snapshot + atomic `updateData` the repo already uses, and has not shipped since 2024.
- SQLDelight is overkill (no queries/entities). Reject.
- Plain kotlinx.serialization + okio file reimplements atomic writes, corruption recovery, and Flow invalidation that DataStore gives for free. Needless risk.

Migration path: add `@Serializable UserPreferences` to `core:model` -> rewrite the serializer against JSON (keep `CorruptionException` mapping) -> move `core/datastore` to KMP with expect/actual `Storage` -> repository swaps `.toBuilder()`/`hasX()` for `.copy()`/null checks -> drop the protobuf plugin + `.proto` -> one-time Android-only migration reading the old `.pb` into the new JSON store (iOS is greenfield).

Key upside: `UserPreferencesLocalRepository` and its `Flow`/`updateData` semantics stay almost identical - the lowest-churn, official-library path.

Full findings: [research/05-preferences-persistence-kmp.md](research/05-preferences-persistence-kmp.md).
