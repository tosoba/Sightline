import Foundation
import Observation
import os
import Shared

/// Thin `@MainActor @Observable` adapter over the shared `PlacesCoordinator`.
///
/// Every piece of places, location, and custom-location-search state lives in the coordinator; this
/// type mirrors the coordinator's consolidated state snapshot into observable properties and
/// forwards user intents.
@MainActor
@Observable
final class NearbyPlacesModel {
    private static let logger = Logger(subsystem: "com.trm.sightline", category: "places")

    private let shared: SightlineShared
    private let coordinator: PlacesCoordinator

    private(set) var activeLocation: GeoLocation?
    private(set) var userLocation: GeoLocation?
    private(set) var allPlaces: [Place] = []
    private(set) var categoryStates: [PlaceCategory: any CategoryPlacesState] = [:]
    private(set) var userLocationAddress: any LocationAddressState = LocationAddressStateLoaded(
        address: ""
    )
    private(set) var customLocationAddress = ""
    private(set) var customLocationSearchResults: any CustomLocationSearchState =
        CustomLocationSearchStateIdle.shared
    private(set) var userLocationEnabled = false
    private(set) var searchRadius: PlaceSearchRadius = .oneKilometer
    private(set) var lastMapPosition: MapCameraPosition?
    private(set) var errorMessage: String?
    private(set) var authorization: LocationAuthorization = .notDetermined
    private(set) var isLocationServicesDisabled = false

    private var locationEnabledTask: Task<Void, Never>?

    var hasLocation: Bool {
        activeLocation != nil
    }

    var isLoading: Bool {
        categoryStates.keys.contains(where: isLoading)
    }

    var activeCategories: [PlaceCategory] {
        PlaceCategory.allCases.filter { categoryStates[$0] != nil }
    }

    var isLocationDenied: Bool {
        authorization == .denied || authorization == .restricted
    }

    init(shared: SightlineShared) {
        self.shared = shared
        coordinator = shared.placesCoordinator
    }

    func isActive(_ category: PlaceCategory) -> Bool {
        categoryStates[category] != nil
    }

    func isLoading(_ category: PlaceCategory) -> Bool {
        guard let state = categoryStates[category] else { return false }
        if case .loading = onEnum(of: state) {
            return true
        }
        return false
    }

    func isLoaded(_ category: PlaceCategory) -> Bool {
        guard let state = categoryStates[category] else { return false }
        if case .loaded = onEnum(of: state) {
            return true
        }
        return false
    }

    func places(in category: PlaceCategory) -> [Place] {
        guard let state = categoryStates[category] else { return [] }
        if case let .loaded(loaded) = onEnum(of: state) {
            return loaded.places
        }
        return []
    }

    // MARK: - Intents

    func onTogglePlaceCategory(_ category: PlaceCategory) {
        coordinator.onTogglePlaceCategory(category: category)
    }

    func setCustomLocationAddress(_ query: String) {
        coordinator.setCustomLocationAddress(query: query)
    }

    func onCustomLocationSearchResultClick(_ location: CustomLocation) {
        coordinator.onCustomLocationSearchResultClick(location: location)
    }

    func cancelCustomLocationSearch() {
        coordinator.cancelCustomLocationSearch()
    }

    func setUserLocationEnabled(_ enabled: Bool) {
        coordinator.setUserLocationEnabled(enabled: enabled)
    }

    func setSearchRadius(_ radius: PlaceSearchRadius) {
        guard radius != searchRadius else { return }
        coordinator.setSearchRadius(radius: radius)
    }

    func saveMapPosition(_ position: MapCameraPosition) {
        coordinator.saveMapPosition(position: position)
    }

    // MARK: - Observation

    func observeAuthorization() async {
        for await state in shared.authorizationState {
            authorization = state
            if state == .denied || state == .restricted, userLocationEnabled {
                coordinator.setUserLocationEnabled(enabled: false)
            }
            Self.logger.info("authorization changed to \(state.name, privacy: .public)")
        }
    }

    func observePreferences() async {
        for await state in coordinator.state {
            apply(state)
        }
    }

    /// `CLLocationManager.locationServicesEnabled()` has no change notification, so the shared flow
    /// re-reads it per collection: restarting this observation is the re-check.
    func refreshLocationServices() {
        coordinator.refreshLocationSettings()
        locationEnabledTask?.cancel()
        locationEnabledTask = Task { [weak self] in
            guard let self else { return }
            for await enabled in self.shared.locationEnabled {
                self.isLocationServicesDisabled = !enabled.boolValue
                Self.logger.info("location services enabled: \(enabled.boolValue, privacy: .public)")
            }
        }
    }

    private func apply(_ state: PlacesUiState) {
        categoryStates = state.places
        allPlaces = state.allPlaces
        activeLocation = state.activeLocation
        userLocation = state.userLocation
        userLocationAddress = state.userLocationAddress
        customLocationAddress = state.customLocationAddress
        customLocationSearchResults = state.customLocationSearchResults
        lastMapPosition = state.lastMapPosition
        errorMessage = state.error.map { message(for: $0) }

        if userLocationEnabled != state.userLocationEnabled {
            userLocationEnabled = state.userLocationEnabled
            Self.logger.info(
                "user location enabled: \(state.userLocationEnabled, privacy: .public)"
            )
        }

        if searchRadius != state.searchRadius {
            searchRadius = state.searchRadius
            Self.logger.info("search radius changed to \(state.searchRadius.meters, privacy: .public) m")
        }
    }

    private func message(for error: any RequestError) -> String {
        switch onEnum(of: error) {
        case .iO: "A connection error occurred."
        case .http: "A server error occurred - try again later."
        case .other: "An unknown error occurred - try again later."
        }
    }
}
