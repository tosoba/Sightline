import CoreLocation
import MapLibre
import Shared
import SwiftUI
import os

struct MapLibreMapView: UIViewRepresentable {
    let places: [Place]
    let colorScheme: ColorScheme
    let lastMapPosition: MapCameraPosition?
    let onCameraChanged: (MapCameraPosition) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onCameraChanged: onCameraChanged)
    }

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero)
        mapView.delegate = context.coordinator
        mapView.showsUserLocation = true
        mapView.isZoomEnabled = true
        mapView.isScrollEnabled = true
        mapView.isRotateEnabled = true
        mapView.isPitchEnabled = true
        mapView.accessibilityIdentifier = "nearby-map"
        mapView.accessibilityLabel = "Nearby places map"
        mapView.accessibilityValue = "0 places"

        context.coordinator.attach(mapView)
        context.coordinator.applyStyle(colorScheme, to: mapView)
        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        context.coordinator.onCameraChanged = onCameraChanged
        context.coordinator.applyStyle(colorScheme, to: mapView)
        context.coordinator.update(lastMapPosition: lastMapPosition)
        context.coordinator.update(places: places)
    }

    final class Coordinator: NSObject, MLNMapViewDelegate {
        private static let log = Logger(subsystem: "com.trm.sightline", category: "map")

        var onCameraChanged: (MapCameraPosition) -> Void

        private weak var mapView: MLNMapView?
        private var colorScheme: ColorScheme?
        private var places: [Place] = []
        private var annotations: [MLNPointAnnotation] = []
        private var styleLoaded = false
        private var lastFittedBounds: GeoBounds?
        private var pendingLastMapPosition: MapCameraPosition?
        private var hasAppliedInitialCamera = false
        private var hasFittedPlaces = false

        init(onCameraChanged: @escaping (MapCameraPosition) -> Void) {
            self.onCameraChanged = onCameraChanged
        }

        func attach(_ mapView: MLNMapView) {
            self.mapView = mapView
        }

        func applyStyle(_ scheme: ColorScheme, to mapView: MLNMapView) {
            guard colorScheme != scheme else { return }
            colorScheme = scheme
            styleLoaded = false

            let name = scheme == .dark ? "dark_style" : "light_style"
            guard let styleURL = Bundle.main.url(forResource: name, withExtension: "json") else {
                assertionFailure("Missing bundled map style \(name).json")
                return
            }
            mapView.styleURL = styleURL
        }

        /// The persisted camera restores once per map instance. The first bounds fit is skipped so
        /// the restore is not immediately overwritten by the places fetch; later category changes
        /// fit again as before.
        func update(lastMapPosition: MapCameraPosition?) {
            if let lastMapPosition {
                pendingLastMapPosition = lastMapPosition
            }
            applyRestoredCameraIfNeeded()
        }

        func update(places: [Place]) {
            guard places.map(\.id) != self.places.map(\.id) else { return }
            self.places = places
            syncAnnotations(force: true)
            fitPlacesIfNeeded(force: true)
        }

        func mapView(_: MLNMapView, didFinishLoading _: MLNStyle) {
            styleLoaded = true
            applyRestoredCameraIfNeeded()
            syncAnnotations(force: true)
            fitPlacesIfNeeded(force: true)
        }

        func mapView(_ mapView: MLNMapView, regionDidChangeAnimated _: Bool) {
            guard styleLoaded, hasAppliedInitialCamera || hasFittedPlaces else { return }

            let position = MapCameraPosition(
                latitude: mapView.centerCoordinate.latitude,
                longitude: mapView.centerCoordinate.longitude,
                zoom: mapView.zoomLevel,
                bearing: mapView.direction,
                pitch: Double(mapView.camera.pitch)
            )
            onCameraChanged(position)
        }

        func mapView(
            _: MLNMapView,
            annotationCanShowCallout _: any MLNAnnotation
        ) -> Bool {
            true
        }

        private func applyRestoredCameraIfNeeded() {
            guard let mapView, styleLoaded, !hasAppliedInitialCamera,
                let position = pendingLastMapPosition
            else {
                return
            }

            hasAppliedInitialCamera = true
            mapView.setCenter(
                CLLocationCoordinate2D(latitude: position.latitude, longitude: position.longitude),
                zoomLevel: position.zoom,
                direction: position.bearing,
                animated: false
            )
            let camera = mapView.camera
            camera.pitch = CGFloat(position.pitch)
            mapView.setCamera(camera, animated: false)

            Self.log.info(
                "restored map camera \(position.latitude, privacy: .public), \(position.longitude, privacy: .public), zoom \(position.zoom, privacy: .public), bearing \(position.bearing, privacy: .public), pitch \(position.pitch, privacy: .public)"
            )
        }

        private func syncAnnotations(force: Bool) {
            guard let mapView, styleLoaded else { return }
            guard force || annotations.count != places.count else { return }

            if !annotations.isEmpty {
                mapView.removeAnnotations(annotations)
            }

            annotations = places.map { place in
                let annotation = MLNPointAnnotation()
                annotation.coordinate = CLLocationCoordinate2D(
                    latitude: place.latitude,
                    longitude: place.longitude
                )
                annotation.title = place.name
                annotation.subtitle = place.category.title
                return annotation
            }

            if !annotations.isEmpty {
                mapView.addAnnotations(annotations)
            }

            mapView.accessibilityValue =
                places.count == 1 ? "1 place" : "\(places.count) places"
        }

        private func fitPlacesIfNeeded(force: Bool) {
            guard let mapView, styleLoaded else { return }

            guard let bounds = GeoBounds.companion.paddedAround(
                places: places,
                percentageIncrease: 0.1
            ) else {
                lastFittedBounds = nil
                return
            }

            if !hasFittedPlaces && hasAppliedInitialCamera {
                hasFittedPlaces = true
                return
            }

            guard force || bounds != lastFittedBounds else { return }
            lastFittedBounds = bounds
            hasFittedPlaces = true

            let coordinateBounds = MLNCoordinateBoundsMake(
                CLLocationCoordinate2D(
                    latitude: bounds.south,
                    longitude: bounds.west
                ),
                CLLocationCoordinate2D(
                    latitude: bounds.north,
                    longitude: bounds.east
                )
            )

            mapView.setVisibleCoordinateBounds(
                coordinateBounds,
                edgePadding: UIEdgeInsets(top: 64, left: 40, bottom: 120, right: 40),
                animated: force,
                completionHandler: nil
            )
        }
    }
}
