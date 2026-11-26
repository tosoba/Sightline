import MapLibre
import Shared
import SwiftUI

struct MapScreen: View {
    let model: NearbyPlacesModel

    @Environment(\.colorScheme) private var colorScheme

    private let showsDebugCamera = ProcessInfo.processInfo.arguments.contains("-mapDebug")

    var body: some View {
        MapLibreMapView(
            places: model.allPlaces,
            colorScheme: colorScheme,
            showsUserLocation: model.userLocationEnabled,
            lastMapPosition: model.lastMapPosition,
            onCameraChanged: model.saveMapPosition
        )
        .overlay(alignment: .top) {
            if model.isLoading || (model.hasLocation && model.allPlaces.isEmpty) {
                statusBadge
                    .padding(.top, 12)
            }
        }
        .overlay(alignment: .bottomLeading) {
            if showsDebugCamera {
                debugCamera
                    .padding(12)
            }
        }
    }

    private var debugCamera: some View {
        Text(cameraText)
            .font(.system(size: 10, design: .monospaced))
            .foregroundStyle(.secondary)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
            .accessibilityIdentifier("map-camera")
    }

    private var cameraText: String {
        guard let position = model.lastMapPosition else { return "none" }
        return String(
            format: "%.4f,%.4f,%.2f,%.1f,%.1f",
            position.latitude,
            position.longitude,
            position.zoom,
            position.bearing,
            position.pitch
        )
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
