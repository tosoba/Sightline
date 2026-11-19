import Shared
import SwiftUI

extension PlaceCategory {
    var title: String {
        switch self {
        case .sightsLandmarks: "Sights"
        case .artsCulture: "Arts & culture"
        case .foodDrink: "Food & drink"
        case .nightlife: "Nightlife"
        case .shopping: "Shopping"
        case .natureOutdoors: "Nature & outdoors"
        case .library: "Library"
        case .accommodation: "Accommodation"
        case .gettingAround: "Getting around"
        case .roadTrip: "Road trip"
        case .moneyBanking: "Money & banking"
        case .health: "Health"
        case .practical: "Practical"
        }
    }
}

struct NearbyPlacesView: View {
    @State private var model: NearbyPlacesModel

    init(shared: SightlineShared) {
        _model = State(initialValue: NearbyPlacesModel(shared: shared))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                categoryPicker
                Divider()
                content
            }
            .navigationTitle("Nearby places")
        }
        .task {
            await withTaskGroup(of: Void.self) { group in
                group.addTask { await model.observeAuthorization() }
                group.addTask { await model.observeLocation() }
            }
        }
    }

    private var categoryPicker: some View {
        Picker(
            "Category",
            selection: Binding(
                get: { model.selectedCategory },
                set: { model.select($0) }
            )
        ) {
            ForEach(PlaceCategory.allCases, id: \.self) { category in
                Text(category.title).tag(category)
            }
        }
        .pickerStyle(.menu)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
    }

    @ViewBuilder
    private var content: some View {
        if let errorMessage = model.errorMessage {
            message(errorMessage, systemImage: "exclamationmark.triangle")
        } else if !model.hasLocation {
            VStack(spacing: 12) {
                ProgressView()
                Text(authorizationMessage)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding()
        } else if model.isLoading {
            VStack(spacing: 12) {
                ProgressView()
                Text("Fetching places...")
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if model.places.isEmpty {
            message("No places found nearby.", systemImage: "mappin.slash")
        } else {
            List(model.places, id: \.id) { place in
                VStack(alignment: .leading, spacing: 4) {
                    Text(place.name)
                        .font(.headline)
                    Text(place.category.title)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            .listStyle(.plain)
        }
    }

    private var authorizationMessage: String {
        switch model.authorization {
        case .notDetermined:
            "Waiting for location permission..."
        case .denied, .restricted:
            "Location access is unavailable. Enable it in Settings to see nearby places."
        case .granted:
            "Waiting for a location fix..."
        }
    }

    private func message(_ text: String, systemImage: String) -> some View {
        ContentUnavailableView {
            Label("Nearby places", systemImage: systemImage)
        } description: {
            Text(text)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
