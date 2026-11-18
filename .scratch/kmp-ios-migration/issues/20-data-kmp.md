# 20: Convert core:data to KMP

**Spec:** `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** The repositories and mappers (`PlacesNetworkRepository`, `AddressNetworkRepository`, `FeatureMapper`, `UserPreferencesLocalRepository`) build in `commonMain` on top of model/domain/datastore/photon, and the existing data-layer tests run from `commonTest`.

**Blocked by:** 16 (domain), 17 (datastore), 19 (photon)

**Status:** ready-for-agent

- [ ] Repositories and mappers compile in `commonMain`.
- [ ] The existing mapper/tag/radius tests move to `commonTest` and stay green.
- [ ] Android consumes `core:data` unchanged; compiles and tests pass.
