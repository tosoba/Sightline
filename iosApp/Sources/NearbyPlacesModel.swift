import Foundation
import Observation
import Shared
import os

/// Thin `@MainActor @Observable` adapter over the shared `PlacesCoordinator`.
///
/// Every piece of places, location, and custom-location-search state lives in the coordinator; this
/// type mirrors the coordinator's `StateFlow`s into observable properties and forwards user intents.
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
        address: "")
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

    var hasLocation: Bool { activeLocation != nil }

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
        if case .loading = onEnum(of: state) { return true }
        return false
    }

    func isLoaded(_ category: PlaceCategory) -> Bool {
        guard let state = categoryStates[category] else { return false }
        if case .loaded = onEnum(of: state) { return true }
        return false
    }

    func places(in category: PlaceCategory) -> [Place] {
        guard let state = categoryStates[category] else { return [] }
        if case .loaded(let loaded) = onEnum(of: state) { return loaded.places }
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
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.observePlaces() }
            group.addTask { await self.observeAllPlaces() }
            group.addTask { await self.observeActiveLocation() }
            group.addTask { await self.observeUserLocation() }
            group.addTask { await self.observeUserLocationAddress() }
            group.addTask { await self.observeCustomLocationAddress() }
            group.addTask { await self.observeCustomLocationSearch() }
            group.addTask { await self.observeUserLocationEnabled() }
            group.addTask { await self.observeSearchRadius() }
            group.addTask { await self.observeLastMapPosition() }
            group.addTask { await self.observeErrors() }
        }
    }

    /// `CLLocationManager.locationServicesEnabled()` has no change notification, so the shared flow
    /// re-reads it per collection: restarting this observation is the re-check.
    func refreshLocationServices() {
        locationEnabledTask?.cancel()
        locationEnabledTask = Task { [weak self] in
            guard let self else { return }
            for await enabled in self.shared.locationEnabled {
                self.isLocationServicesDisabled = !enabled.boolValue
                Self.logger.info("location services enabled: \(enabled.boolValue, privacy: .public)")
            }
        }
    }

    private func observePlaces() async {
        for await states in coordinator.places {
            categoryStates = states
        }
    }

    private func observeAllPlaces() async {
        for await places in coordinator.allPlaces {
            allPlaces = places
        }
    }

    private func observeActiveLocation() async {
        for await location in coordinator.activeLocation {
            activeLocation = location
        }
    }

    private func observeUserLocation() async {
        for await location in coordinator.userLocation {
            userLocation = location
        }
    }

    private func observeUserLocationAddress() async {
        for await state in coordinator.userLocationAddress {
            userLocationAddress = state
        }
    }

    private func observeCustomLocationAddress() async {
        for await address in coordinator.customLocationAddress {
            customLocationAddress = address
        }
    }

    private func observeCustomLocationSearch() async {
        for await state in coordinator.customLocationSearchResults {
            customLocationSearchResults = state
        }
    }

    private func observeUserLocationEnabled() async {
        for await enabled in coordinator.userLocationEnabled {
            userLocationEnabled = enabled.boolValue
            Self.logger.info("user location enabled: \(enabled.boolValue, privacy: .public)")
        }
    }

    private func observeSearchRadius() async {
        for await radius in coordinator.searchRadius {
            guard radius != searchRadius else { continue }
            searchRadius = radius
            Self.logger.info("search radius changed to \(radius.meters, privacy: .public) m")
        }
    }

    private func observeLastMapPosition() async {
        for await position in coordinator.lastMapPosition {
            lastMapPosition = position
        }
    }

    private func observeErrors() async {
        for await error in coordinator.error {
            errorMessage = error.map { message(for: $0) }
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
