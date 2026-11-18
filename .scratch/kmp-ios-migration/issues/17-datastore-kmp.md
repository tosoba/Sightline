# 17: Convert core:datastore to KMP (JSON UserPreferences + expect/actual Storage)

**Spec:** `.scratch/kmp-ios-migration/specs/persistence.md`

**What to build:** User preferences persist through a `@Serializable` JSON `UserPreferences` in `commonMain` with expect/actual `Storage` (`FileStorage` on Android), replacing the protobuf serializer. The protobuf plugin and `.proto` are dropped, and a one-time migration reads the old `.pb` into the JSON store on first Android launch after upgrade.

**Blocked by:** 15 (model), 16 (domain)

**Status:** ready-for-agent

- [ ] `@Serializable UserPreferences` in `core:model` with all 11 fields mapped (`optional` to nullable, `bool` to `Boolean = false`).
- [ ] The serializer is rewritten against JSON with `CorruptionException` mapping preserved; expect/actual `Storage` compiles for Android and iOS.
- [ ] `UserPreferencesLocalRepository` uses `copy()`/null checks; its `Flow`/`updateData` semantics are unchanged.
- [ ] One-time `.pb` to JSON migration runs on first launch; Android preferences persist unchanged and tests pass.
