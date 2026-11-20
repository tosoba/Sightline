# 26: iOS custom location picker

**Spec:** `.scratch/kmp-ios-migration/spec.md` (feature and value parity), `.scratch/kmp-ios-migration/specs/navigation.md` (shared view-model state includes custom location and search results).

**What to build:** The iOS equivalent of Android's custom-location flow, so a user can search an address and set a custom location instead of using the device fix.
`PreferencesFacade` already exposes `customLocation` and `setCustomLocation`, but there is no search path and no UI, so the preference is unreachable on iOS.

**Blocked by:** 25 (iOS preferences and navigation).

**Status:** ready-for-agent

- [ ] `AddressRepository` search is reachable from the `shared` facade, with the same query/result semantics Android uses (`MainViewModel.customLocationSearchResults`, `CustomLocation`).
- [ ] iOS offers an address search UI, lets the user pick a result, and persists it through the existing `PreferencesFacade.setCustomLocation` path.
- [ ] Places are fetched around the custom location when one is set, and around the device fix otherwise, matching Android's precedence in `MainViewModel`.
- [ ] Choosing "use my location" clears the custom location, matching Android.
- [ ] Android remains unchanged and green, and the iOS smoke suite covers the new path.

## Comments

Raised by the code review of tickets 21-25 (`.scratch/kmp-ios-migration/issues/25-ios-preferences-navigation-smoke.md`).
Deliberately not folded into the review-fix pass because it is a feature gap, not a defect: the parity target is real, but it needs its own UI, facade surface and tests rather than a rider on a cleanup commit.
