import CoreLocation
import MapKit
import Shared
import SwiftUI

/// One pushed category destination. Places are read live from the model so the screen can pop when
/// the category is toggled off or its places are cleared.
struct CategoryRoute: Hashable {
    let category: PlaceCategory
}

struct CategoryPlacesScreen: View {
    let route: CategoryRoute
    let model: NearbyPlacesModel

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @State private var selectedPlaceID: String?

    private var places: [Place] {
        model.places(in: route.category)
    }

    private var focusedCoordinate: GeoLocation? {
        guard let selectedPlaceID,
              let place = places.first(where: { $0.id == selectedPlaceID })
        else {
            return nil
        }
        return place.location
    }

    var body: some View {
        GeometryReader { proxy in
            VStack(spacing: 0) {
                MapLibreMapView(
                    purpose: .category,
                    places: places,
                    colorScheme: colorScheme,
                    showsUserLocation: model.userLocationEnabled,
                    focusedCoordinate: focusedCoordinate,
                    accessibilityIdentifier: "category-map",
                    fitPadding: UIEdgeInsets(top: 40, left: 40, bottom: 40, right: 40)
                )
                .frame(height: max(180, proxy.size.height * 0.45))

                summaryHeader
                Divider()
                placeContent
            }
        }
        .navigationTitle(route.category.title)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: popIfUnloaded)
        .onChange(of: model.isActive(route.category)) { _, _ in
            popIfUnloaded()
        }
    }

    private var summaryHeader: some View {
        HStack(spacing: 12) {
            Image(route.category.icon)
                .font(.title3.weight(.semibold))
                .frame(width: 48, height: 48)
                .background(
                    Color.accentColor.opacity(0.14),
                    in: RoundedRectangle(cornerRadius: 12)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text(route.category.title)
                    .font(.headline)
                Text(\.places_count, places.count)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("category-summary")
    }

    @ViewBuilder
    private var placeContent: some View {
        if places.isEmpty {
            ContentUnavailableView {
                Label("No places found in this category", systemImage: "mappin.slash")
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            List(places, id: \.id) { place in
                row(place)
            }
            .listStyle(.plain)
            .accessibilityIdentifier("category-place-list")
        }
    }

    private func row(_ place: Place) -> some View {
        HStack(spacing: 12) {
            Button {
                selectedPlaceID = place.id
            } label: {
                HStack(spacing: 12) {
                    Text(place.initials)
                        .font(.headline)
                        .frame(width: 44, height: 44)
                        .background(
                            Color(uiColor: .secondarySystemBackground),
                            in: RoundedRectangle(cornerRadius: 10)
                        )
                    VStack(alignment: .leading, spacing: 2) {
                        Text(place.name)
                            .font(.body.weight(.medium))
                            .lineLimit(2)
                        if let activeLocation = model.activeLocation {
                            Text(
                                formatDistance(
                                    meters: activeLocation.distanceTo(other: place.location)
                                )
                            )
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        }
                    }
                    Spacer(minLength: 0)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("place-row-\(place.id)")
            .accessibilityLabel(place.name)

            Button {
                openInMaps(place)
            } label: {
                Image(systemName: "arrow.triangle.turn.up.right.diamond.fill")
                    .frame(width: 44, height: 44)
                    .background(Color.accentColor.opacity(0.12), in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("open-in-maps-\(place.id)")
            .accessibilityLabel("Open \(place.name) in Maps")
        }
        .padding(.vertical, 2)
    }

    private func openInMaps(_ place: Place) {
        let coordinate = CLLocationCoordinate2D(
            latitude: place.latitude,
            longitude: place.longitude
        )
        let item = MKMapItem(placemark: MKPlacemark(coordinate: coordinate))
        item.name = place.name
        _ = item.openInMaps(launchOptions: nil)
    }

    private func popIfUnloaded() {
        if !model.isActive(route.category) {
            dismiss()
        }
    }
}
