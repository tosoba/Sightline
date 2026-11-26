# 27: Make the Android test gate cover the KMP host suites

**Spec:** `.scratch/kmp-ios-migration/spec.md` ("the Android-correctness gate is mandatory", "Android must compile, its tests stay green").

**What to build:** `./gradlew test` must actually run the KMP modules' Android host tests, so the documented gate cannot pass while those suites are silently skipped.

**Blocked by:** None (can start immediately).

**Status:** resolved

- [x] The root `test` task depends on the KMP host test tasks (`testAndroidHostTest` / `allTests`) for `core:model`, `core:domain`, `core:common`, `core:data`, `core:datastore`, `api:photon` and `core:ar`.
- [x] A deliberately failing assertion in one of those suites makes `./gradlew test` fail, so the wiring is proven and not assumed.
- [x] The documented gate commands in `iosApp/README.md` and the tickets name the task that covers both Android and KMP host tests.

## Comments

Found while merging the final code review fixes: `./gradlew test` reports `:app:testDebugUnitTest NO-SOURCE` and `:feature:places:test NO-SOURCE`, and produces no XML for the KMP modules at all.
The suites pass when run by name (`:core:ar:check`, `:core:model:testAndroidHostTest`, and so on), so no test is broken today, but the gate described in the spec does not currently cover them.

### Resolution

The root build now registers a `test` lifecycle task that depends on the `testAndroidHostTest` task of every KMP module, so `./gradlew test` keeps matching the existing Android `test` tasks and also runs the host suites.

Verification:

- `./gradlew test --dry-run` lists `:test` together with `:core:model:testAndroidHostTest`, `:core:domain:testAndroidHostTest`, `:core:common:testAndroidHostTest`, `:core:data:testAndroidHostTest`, `:core:datastore:testAndroidHostTest`, `:api:photon:testAndroidHostTest`, and `:core:ar:testAndroidHostTest`.
- A temporary failing test in `core:model/src/commonTest` made `./gradlew test` fail at `:core:model:testAndroidHostTest`; after removing it, `./gradlew test` is green.
- The run now writes XML under each module's `build/test-results/testAndroidHostTest`, covering 26 KMP test classes and 167 tests: 3 photon, 90 AR, 11 common, 9 data, 10 datastore, 29 domain, and 15 model.
- `iosApp/README.md` and the spec now name `./gradlew test` as the Android and shared-Kotlin gate, with `./gradlew :shared:iosSimulatorArm64Test` for the iOS shared suite.
