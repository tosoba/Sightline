import CoreLocation
import MapLibre
import os
import Shared
import SwiftUI

private enum MapFitPadding {
    static let standard = UIEdgeInsets(top: 64, left: 40, bottom: 120, right: 40)
}

/// One MapLibre wrapper for both the full-screen map and the Places sheet's category map.
///
/// The main purpose restores and persists the camera and fits the given places. The category purpose
/// never reports camera changes (so panning it cannot overwrite the persisted main-map camera), can
/// center on a selected place, and uses its own accessibility identifier so XCUITest can tell the two
/// maps apart.
struct MapLibreMapView: UIViewRepresentable {
    enum Purpose {
        case main
        case category
    }

    var purpose: Purpose = .main
    let places: [Place]
    let colorScheme: ColorScheme
    let showsUserLocation: Bool
    var lastMapPosition: MapCameraPosition? = nil
    var focusedCoordinate: GeoLocation? = nil
    var onCameraChanged: ((MapCameraPosition) -> Void)? = nil
    var accessibilityIdentifier = "nearby-map"
    var fitPadding = MapFitPadding.standard

    func makeCoordinator() -> Coordinator {
        Coordinator(purpose: purpose, onCameraChanged: onCameraChanged)
    }

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero)
        mapView.delegate = context.coordinator
        // Location is opt-in: assigning true starts CoreLocation and prompts for authorization.
        mapView.showsUserLocation = false
        mapView.isZoomEnabled = true
        mapView.isScrollEnabled = true
        mapView.isRotateEnabled = true
        mapView.isPitchEnabled = true
        // The app's shared location provider owns the authorization prompt; this map only shows the dot.
        mapView.shouldRequestAuthorizationToUseLocationServices = false
        mapView.accessibilityIdentifier = accessibilityIdentifier
        mapView.accessibilityLabel = "\(context.coordinator.labelPrefix), 0 places"

        context.coordinator.attach(mapView)
        context.coordinator.applyStyle(colorScheme, to: mapView)
        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        context.coordinator.onCameraChanged = onCameraChanged
        if mapView.showsUserLocation != showsUserLocation {
            mapView.showsUserLocation = showsUserLocation
        }
        context.coordinator.applyStyle(colorScheme, to: mapView)
        if purpose == .main {
            context.coordinator.update(lastMapPosition: lastMapPosition)
        }
        context.coordinator.update(focusedCoordinate: focusedCoordinate)
        context.coordinator.update(places: places, fitPadding: fitPadding)
    }

    final class Coordinator: NSObject, MLNMapViewDelegate {
        private static let log = Logger(subsystem: "com.trm.sightline", category: "map")

        let purpose: Purpose
        let labelPrefix: String

        var onCameraChanged: ((MapCameraPosition) -> Void)?

        private weak var mapView: MLNMapView?
        private var colorScheme: ColorScheme?
        private var places: [Place] = []
        private var annotations: [MLNPointAnnotation] = []
        private var styleLoaded = false
        private var lastPlacesFingerprint = ""
        private var lastFittedFingerprint: String?
        private var pendingLastMapPosition: MapCameraPosition?
        private var hasAppliedInitialCamera = false
        private var hasFittedPlaces = false
        private var pendingFocus: GeoLocation?
        private var lastAppliedFocus: GeoLocation?
        private var fitPadding: UIEdgeInsets?

        init(purpose: Purpose, onCameraChanged: ((MapCameraPosition) -> Void)?) {
            self.purpose = purpose
            labelPrefix = purpose == .category ? "Category map" : "Nearby places map"
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

        func update(focusedCoordinate: GeoLocation?) {
            guard let focusedCoordinate else { return }
            if let lastAppliedFocus,
               lastAppliedFocus.latitude == focusedCoordinate.latitude,
               lastAppliedFocus.longitude == focusedCoordinate.longitude
            {
                return
            }
            pendingFocus = focusedCoordinate
            applyFocusIfNeeded()
        }

        func update(places: [Place], fitPadding: UIEdgeInsets) {
            let fingerprint = Self.placesFingerprint(places)
            guard fingerprint != lastPlacesFingerprint else { return }
            lastPlacesFingerprint = fingerprint
            self.places = places
            syncAnnotations()
            fitPlacesIfNeeded(padding: fitPadding)
        }

        func mapView(_: MLNMapView, didFinishLoading _: MLNStyle) {
            styleLoaded = true
            applyRestoredCameraIfNeeded()
            syncAnnotations()
            fitPlacesIfNeeded(padding: nil)
            applyFocusIfNeeded()
        }

        func mapView(_ mapView: MLNMapView, regionDidChangeAnimated _: Bool) {
            guard purpose == .main,
                  styleLoaded,
                  hasAppliedInitialCamera || hasFittedPlaces,
                  let onCameraChanged
            else {
                return
            }

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
            guard purpose == .main,
                  let mapView,
                  styleLoaded,
                  !hasAppliedInitialCamera,
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

        private func applyFocusIfNeeded() {
            guard let mapView, styleLoaded, let focus = pendingFocus else { return }
            pendingFocus = nil
            lastAppliedFocus = focus
            mapView.setCenter(
                CLLocationCoordinate2D(latitude: focus.latitude, longitude: focus.longitude),
                zoomLevel: max(15, mapView.zoomLevel),
                direction: mapView.direction,
                animated: true
            )
        }

        private func syncAnnotations() {
            guard let mapView, styleLoaded else { return }

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

            let count = places.count
            mapView.accessibilityLabel = "\(labelPrefix), \(count) \(count == 1 ? "place" : "places")"
        }

        private func fitPlacesIfNeeded(padding: UIEdgeInsets?) {
            if let padding {
                fitPadding = padding
            }
            guard let mapView, styleLoaded, let place = places.first else {
                lastFittedFingerprint = nil
                return
            }

            let fingerprint = Self.placesFingerprint(places)
            if !hasFittedPlaces && hasAppliedInitialCamera {
                hasFittedPlaces = true
                lastFittedFingerprint = fingerprint
                return
            }

            guard fingerprint != lastFittedFingerprint else { return }
            lastFittedFingerprint = fingerprint
            hasFittedPlaces = true

            let bounds = GeoBounds.companion.paddedAround(
                places: places,
                percentageIncrease: 0.1
            )

            // A single place, or places sharing a coordinate, produce a zero-span box that zooms to
            // maximum. Center on the place instead.
            let isDegenerate = bounds.map {
                $0.north - $0.south < 1e-5 && $0.east - $0.west < 1e-5
            } ?? true
            guard places.count > 1, let bounds, !isDegenerate else {
                mapView.setCenter(
                    CLLocationCoordinate2D(latitude: place.latitude, longitude: place.longitude),
                    zoomLevel: 15,
                    direction: mapView.direction,
                    animated: false
                )
                return
            }

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
                edgePadding: fitPadding ?? MapFitPadding.standard,
                animated: true,
                completionHandler: nil
            )
        }

        private static func placesFingerprint(_ places: [Place]) -> String {
            places
                .map { "\($0.id)|\($0.latitude)|\($0.longitude)|\($0.name)" }
                .joined(separator: "\n")
        }
    }
}
