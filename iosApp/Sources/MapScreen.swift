import MapLibre
import Shared
import SwiftUI

struct MapScreen: View {
    let model: NearbyPlacesModel

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        MapLibreMapView(places: model.places, colorScheme: colorScheme)
            .overlay(alignment: .top) {
                if model.isLoading || (model.hasLocation && model.places.isEmpty) {
                    statusBadge
                        .padding(.top, 12)
                }
            }
    }

    private var statusBadge: some View {
        HStack(spacing: 8) {
            if model.isLoading {
                ProgressView()
            }
            Text(model.isLoading ? "Fetching places..." : "No places found nearby.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(.thinMaterial, in: Capsule())
    }
}
