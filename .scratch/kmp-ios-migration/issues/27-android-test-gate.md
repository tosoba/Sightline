# 27: Make the Android test gate cover the KMP host suites

**Spec:** `.scratch/kmp-ios-migration/spec.md` ("the Android-correctness gate is mandatory", "Android must compile, its tests stay green").

**What to build:** `./gradlew test` must actually run the KMP modules' Android host tests, so the documented gate cannot pass while those suites are silently skipped.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] The root `test` task depends on the KMP host test tasks (`testAndroidHostTest` / `allTests`) for `core:model`, `core:domain`, `core:common`, `core:data`, `core:datastore`, `api:photon` and `core:ar`.
- [ ] A deliberately failing assertion in one of those suites makes `./gradlew test` fail, so the wiring is proven and not assumed.
- [ ] The documented gate commands in `iosApp/README.md` and the tickets name the task that covers both Android and KMP host tests.

## Comments

Found while merging the final code review fixes: `./gradlew test` reports `:app:testDebugUnitTest NO-SOURCE` and `:feature:places:test NO-SOURCE`, and produces no XML for the KMP modules at all.
The suites pass when run by name (`:core:ar:check`, `:core:model:testAndroidHostTest`, and so on), so no test is broken today, but the gate described in the spec does not currently cover them.
