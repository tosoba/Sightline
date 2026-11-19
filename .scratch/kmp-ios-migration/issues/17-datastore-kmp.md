# 17: Convert core:datastore to KMP (JSON UserPreferences + expect/actual Storage)

**Spec:** `.scratch/kmp-ios-migration/specs/persistence.md`

**What to build:** User preferences persist through a `@Serializable` JSON `UserPreferences` in `commonMain` with expect/actual `Storage` (`FileStorage` on Android), replacing the protobuf serializer. The protobuf plugin and `.proto` are dropped, and a one-time migration reads the old `.pb` into the JSON store on first Android launch after upgrade.

**Blocked by:** 15 (model), 16 (domain)

**Status:** resolved

- [x] `@Serializable UserPreferences` in `core:model` with all 11 fields mapped (`optional` to nullable, `bool` to `Boolean = false`).
- [x] The serializer is rewritten against JSON with `CorruptionException` mapping preserved; expect/actual `Storage` compiles for Android and iOS.
- [x] `UserPreferencesLocalRepository` uses `copy()`/null checks; its `Flow`/`updateData` semantics are unchanged.
- [x] One-time `.pb` to JSON migration runs on first launch; Android preferences persist unchanged and tests pass.

## Comments

`UserPreferences` now lives in `core:model` as `@Serializable` JSON with the 11 protobuf fields mapped (`optional` to nullable, `bool` to `Boolean = false`).
`core:datastore` is a KMP module: `commonMain` holds the JSON codec (mapping `SerializationException` to `CorruptionException`) plus `createUserPreferencesDataStore`, `androidMain` implements the `Storage` with `FileStorage`, and `iosMain` with `OkioStorage`.
The protobuf plugin, the `.proto`, and the protobuf dependencies are gone, and the trampoline `jvm()` targets on `core:model` and `core:domain` were removed.

The one-time migration is a DataStore `DataMigration` (`LegacyProtobufPreferencesMigration`) wired in the Android `DataModule`: it runs only when the legacy `datastore/user_preferences.pb` exists and the JSON store is still empty, and deletes the `.pb` after the write.
Because the protobuf runtime was dropped, the legacy message is decoded by a small hand-written proto3 wire reader (`LegacyUserPreferencesReader`) that skips unknown fields and treats truncated data as "nothing to migrate".

`UserPreferencesLocalRepository` keeps its `Flow`/`updateData` semantics with `copy()`/null checks; the custom location timestamp is still stamped at write time.

Tests: JSON round trip, defaults, unknown-key tolerance, and corruption mapping in `commonTest`; legacy wire decoding in `androidHostTest`.
Verified `:app:assembleDebug`, `test`, `:core:datastore:testAndroidHostTest`, and `:core:datastore:compileKotlinIosSimulatorArm64` green.
