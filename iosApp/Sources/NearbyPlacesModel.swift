import Foundation
import Observation
import Shared
import os

@MainActor
@Observable
final class NearbyPlacesModel {
    private static let logger = Logger(subsystem: "com.trm.sightline", category: "places")

    private let shared: SightlineShared
    private(set) var currentLocation: GeoLocation?

    var selectedCategory: PlaceCategory = .sightsLandmarks
    private(set) var places: [Place] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var authorization: LocationAuthorization = .notDetermined
    private(set) var hasLocation = false

    private(set) var searchRadius: PlaceSearchRadius = .oneKilometer
    private(set) var userLocationEnabled = false
    private(set) var lastMapPosition: MapCameraPosition?

    private var locationTask: Task<Void, Never>?
    private var mapPositionTask: Task<Void, Never>?

    var isLocationDenied: Bool {
        authorization == .denied || authorization == .restricted
    }

    init(shared: SightlineShared) {
        self.shared = shared
    }

    func observeAuthorization() async {
        for await state in shared.authorizationState {
            authorization = state
            Self.logger.info("authorization changed to \(state.name, privacy: .public)")
        }
    }

    func observePreferences() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.observeUserLocationEnabled() }
            group.addTask { await self.observeSearchRadius() }
            group.addTask { await self.observeLastMapPosition() }
        }
    }

    func select(_ category: PlaceCategory) async {
        selectedCategory = category
        await reload()
    }

    func setSearchRadius(_ radius: PlaceSearchRadius) {
        guard radius != searchRadius else { return }
        searchRadius = radius
        Task {
            do {
                try await shared.preferences.setSearchRadius(radius: radius)
            } catch {
                Self.logger.error("saving the search radius failed: \(error.localizedDescription, privacy: .public)")
            }
        }
        if hasLocation {
            Task { await reload() }
        }
    }

    func setUserLocationEnabled(_ enabled: Bool) {
        guard enabled != userLocationEnabled else { return }
        userLocationEnabled = enabled
        restartLocationUpdates()
        Task {
            do {
                try await shared.preferences.setUserLocationEnabled(enabled: enabled)
            } catch {
                Self.logger.error("saving the user-location toggle failed: \(error.localizedDescription, privacy: .public)")
            }
        }
    }

    func saveMapPosition(_ position: MapCameraPosition) {
        mapPositionTask?.cancel()
        mapPositionTask = Task { [weak self] in
            do {
                try await Task.sleep(for: .seconds(1))
            } catch {
                return
            }
            guard let self, !Task.isCancelled else { return }
            do {
                try await self.shared.preferences.setLastMapPosition(position: position)
                Self.logger.info(
                    "saved map camera \(position.latitude, privacy: .public), \(position.longitude, privacy: .public), zoom \(position.zoom, privacy: .public)"
                )
            } catch {
                Self.logger.error("saving the map camera failed: \(error.localizedDescription, privacy: .public)")
            }
        }
    }

    func reload() async {
        guard let currentLocation else {
            places = []
            hasLocation = false
            errorMessage = userLocationEnabled ? "Waiting for a location fix." : nil
            return
        }

        isLoading = true
        errorMessage = nil

        do {
            let fetched = try await shared.nearbyPlaces(
                category: selectedCategory,
                location: currentLocation,
                radiusMeters: Float(searchRadius.meters)
            )
            places = fetched
            Self.logger.info(
                "fetched \(fetched.count, privacy: .public) places for \(self.selectedCategory.name, privacy: .public) within \(self.searchRadius.meters, privacy: .public) m"
            )
            print("SIGHTLINE fetched \(fetched.count) places for \(selectedCategory.name)")
        } catch {
            places = []
            errorMessage = error.localizedDescription
            Self.logger.error("places request failed: \(error.localizedDescription, privacy: .public)")
            print("SIGHTLINE places request failed: \(error)")
        }

        isLoading = false
    }

    private func observeUserLocationEnabled() async {
        for await enabledValue in shared.preferences.userLocationEnabled {
            let enabled = enabledValue.boolValue
            guard enabled != userLocationEnabled || locationTask == nil else { continue }
            userLocationEnabled = enabled
            Self.logger.info("user location enabled: \(enabled, privacy: .public)")
            restartLocationUpdates()
        }
    }

    private func observeSearchRadius() async {
        for await radius in shared.preferences.searchRadius {
            guard radius != searchRadius else { continue }
            searchRadius = radius
            Self.logger.info("search radius changed to \(radius.meters, privacy: .public) m")
            if hasLocation {
                await reload()
            }
        }
    }

    private func observeLastMapPosition() async {
        for await position in shared.preferences.lastMapPosition {
            if position != lastMapPosition {
                lastMapPosition = position
            }
        }
    }

    private func restartLocationUpdates() {
        locationTask?.cancel()
        locationTask = nil

        guard userLocationEnabled else {
            currentLocation = nil
            hasLocation = false
            places = []
            errorMessage = nil
            return
        }

        locationTask = Task { [weak self] in
            guard let self else { return }
            await self.consumeLocationUpdates()
        }
    }

    private func consumeLocationUpdates() async {
        for await location in shared.locationUpdates {
            guard !Task.isCancelled else { return }
            currentLocation = location
            hasLocation = true
            Self.logger.info(
                "location fix \(location.latitude, privacy: .public), \(location.longitude, privacy: .public)"
            )
            await reload()
        }
    }
}
