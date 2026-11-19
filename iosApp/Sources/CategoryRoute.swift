import Shared
import SwiftUI

/// One pushed category destination. Mirrors Android's `PlaceCategoryRoute(category, places)`:
/// the already-loaded places travel with the route, so the screen never fetches twice.
struct CategoryRoute: Hashable {
    let category: PlaceCategory
    let places: [Place]
}

struct CategoryPlacesScreen: View {
    let route: CategoryRoute

    var body: some View {
        NearbyPlacesView(places: route.places)
            .navigationTitle(route.category.title)
            .navigationBarTitleDisplayMode(.inline)
    }
}
