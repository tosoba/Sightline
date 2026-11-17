# Preferences persistence

Groundwork spec for migrating user-preferences persistence to KMP.

Source: issue 05-preferences-persistence-kmp.md.

## Decision

Keep AndroidX DataStore (now multiplatform) and swap the two JVM-only pieces for multiplatform equivalents: the protobuf serializer becomes `@Serializable` JSON, and storage becomes expect/actual (`FileStorage` on Android, `OkioStorage` on iOS).

## Current

AndroidX DataStore with one Protobuf message (`core/datastore/src/main/proto/user_prefs.proto`, 11 fields), a `UserPreferencesSerializer`, and `DataStoreFactory.create` with `context.filesDir` in `core/data/DataModule.kt`.
No database.

## Why

- DataStore `androidx.datastore:datastore-core:1.3.0` publishes `iosArm64` + `iosSimulatorArm64`; the non-Android API is `DataStoreFactory.create(storage = Storage<T>)`.
- The blocker is protobuf, not DataStore: `protobuf-kotlin-lite`/`protobuf-kotlin` bindings are JVM-only, so `UserPreferences` cannot compile on iOS.
- multiplatform-settings loses the typed snapshot + atomic `updateData`; SQLDelight is overkill; plain kotlinx.serialization reimplements atomic writes and Flow invalidation that DataStore gives for free.

## Target design

- Add `@Serializable UserPreferences` to `core:model`; the 11 fields map 1:1 (`optional` -> nullable, `bool` -> `Boolean = false`).
- Rewrite the serializer against JSON, keeping the `CorruptionException` mapping.
- Move `core:datastore` to KMP with expect/actual `Storage` (androidMain `FileStorage`, iosMain `OkioStorage(FileSystem.SYSTEM, ...)`).
- `UserPreferencesLocalRepository` swaps `.toBuilder()`/`hasX()` for `.copy()`/null checks.
- Drop the protobuf plugin and `.proto`.

## Migration

One-time Android-only migration reads the old `.pb` into the new JSON store on first launch after upgrade; iOS is greenfield.

## Upside

`UserPreferencesLocalRepository` and its `Flow`/`updateData` semantics stay almost identical - the lowest-churn, official-library path.
