# 26: iOS custom location picker

**Spec:** `.scratch/kmp-ios-migration/spec.md` (feature and value parity), `.scratch/kmp-ios-migration/specs/navigation.md` (shared view-model state includes custom location and search results).

**What to build:** The iOS equivalent of Android's custom-location flow, so a user can search an address and set a custom location instead of using the device fix.
`PreferencesFacade` already exposes `customLocation` and `setCustomLocation`, but there is no search path and no UI, so the preference is unreachable on iOS.

**Blocked by:** 25 (iOS preferences and navigation).

**Status:** wontfix

- [ ] `AddressRepository` search is reachable from the `shared` facade, with the same query/result semantics Android uses (`MainViewModel.customLocationSearchResults`, `CustomLocation`).
- [ ] iOS offers an address search UI, lets the user pick a result, and persists it through the existing `PreferencesFacade.setCustomLocation` path.
- [ ] Places are fetched around the custom location when one is set, and around the device fix otherwise, matching Android's precedence in `MainViewModel`.
- [ ] Choosing "use my location" clears the custom location, matching Android.
- [ ] Android remains unchanged and green, and the iOS smoke suite covers the new path.

## Comments

Raised by the code review of tickets 21-25 (`.scratch/kmp-ios-migration/issues/25-ios-preferences-navigation-smoke.md`).
Deliberately not folded into the review-fix pass because it is a feature gap, not a defect: the parity target is real, but it needs its own UI, facade surface and tests rather than a rider on a cleanup commit.

### Superseded by `ios-parity-and-ipad`

Absorbed into [Spec: iOS Feature, UX, and Orientation Parity + Native iPad Support](../../ios-parity-and-ipad/spec.md) (see also [Shared facade and custom location flow](../../ios-parity-and-ipad/issues/03-shared-facade-and-custom-location-flow.md), which corrects the `"Use my location"` toggle semantics to match Android's actual `MainViewModel.kt` + `UserPreferencesLocalRepository.kt` behavior where toggling `"Use my location"` ON preserves the saved `CustomLocation` in preferences and computes `activeLocation = userLocationEnabled ? userLocation : customLocation`) and tracked in:

- [07: Shared PlacesCoordinator and address facade](../../ios-parity-and-ipad/issues/07-shared-places-coordinator-and-address-facade.md)
- [08: iOS full-screen tabs, HIG places sheet, and custom location UI](../../ios-parity-and-ipad/issues/08-ios-fullscreen-tabs-places-sheet-and-custom-location.md)
