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

    init(shared: SightlineShared) {
        self.shared = shared
    }

    func observeAuthorization() async {
        for await state in shared.authorizationState {
            authorization = state
            Self.logger.info("authorization changed to \(state.name, privacy: .public)")
        }
    }

    func observeLocation() async {
        for await location in shared.locationUpdates {
            currentLocation = location
            hasLocation = true
            Self.logger.info(
                "location fix \(location.latitude, privacy: .public), \(location.longitude, privacy: .public)"
            )
            await reload()
        }
    }

    func select(_ category: PlaceCategory) {
        selectedCategory = category
        Task { await reload() }
    }

    func reload() async {
        guard let currentLocation else {
            errorMessage = "Waiting for a location fix."
            return
        }

        isLoading = true
        errorMessage = nil

        do {
            let fetched = try await shared.nearbyPlaces(
                category: selectedCategory,
                location: currentLocation,
                radiusMeters: 1_000
            )
            places = fetched
            Self.logger.info(
                "fetched \(fetched.count, privacy: .public) places for \(self.selectedCategory.name, privacy: .public)"
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
}
