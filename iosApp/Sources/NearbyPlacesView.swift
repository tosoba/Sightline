import Shared
import SwiftUI

/// Content-only list of places. `MainScreen` owns the model and navigation; this view renders a
/// list that a caller has already loaded.
struct NearbyPlacesView: View {
    let places: [Place]

    var body: some View {
        if places.isEmpty {
            ContentUnavailableView {
                Label("Nearby places", systemImage: "mappin.slash")
            } description: {
                Text("No places found nearby.")
            }
        } else {
            List(places, id: \.id) { place in
                VStack(alignment: .leading, spacing: 4) {
                    Text(place.name)
                        .font(.headline)
                    Text(place.category.title)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 2)
            }
            .listStyle(.plain)
        }
    }
}
