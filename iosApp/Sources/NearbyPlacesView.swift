import Shared
import SwiftUI

struct NearbyPlacesView: View {
    let model: NearbyPlacesModel

    var body: some View {
        content
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
