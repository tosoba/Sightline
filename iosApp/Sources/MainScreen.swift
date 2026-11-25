import Shared
import SwiftUI

enum MainPage: String, CaseIterable, Identifiable {
    case camera
    case map

    var id: Self { self }

    var title: String {
        switch self {
        case .camera: "Camera"
        case .map: "Map"
        }
    }

    var systemImage: String {
        switch self {
        case .camera: "camera.viewfinder"
        case .map: "map"
        }
    }
}

struct MainScreen: View {
    @State private var model: NearbyPlacesModel
    @State private var page: MainPage = ARCameraLaunchOptions.current.requestedPage ?? .camera
    @State private var showsPlacesSheet = false
    @State private var sheetDetent: PresentationDetent = .medium
    @State private var categoryPath: [CategoryRoute] = []
    @State private var toolbarsVisible = true
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass

    init(shared: SightlineShared) {
        _model = State(initialValue: NearbyPlacesModel(shared: shared))
    }

    private var sheetObscuresCamera: Bool {
        showsPlacesSheet
            && (horizontalSizeClass == .regular
                || verticalSizeClass == .compact
                || sheetDetent == .large)
    }

    var body: some View {
        TabView(selection: $page) {
            tabContent(showsControls: toolbarsVisible) {
                ARCameraScreen(
                    nearbyModel: model,
                    isActive: page == .camera,
                    toolbarsVisible: toolbarsVisible,
                    sheetObscuresCamera: sheetObscuresCamera,
                    onCanvasTap: toggleToolbars
                )
            }
            .tag(MainPage.camera)
            .tabItem {
                Label(MainPage.camera.title, systemImage: MainPage.camera.systemImage)
            }

            tabContent(showsControls: true) {
                MapScreen(model: model)
            }
            .tag(MainPage.map)
            .tabItem {
                Label(MainPage.map.title, systemImage: MainPage.map.systemImage)
            }
        }
        .sheet(isPresented: $showsPlacesSheet, onDismiss: resetSheetState) {
            PlacesSheet(model: model, sheetDetent: $sheetDetent, path: $categoryPath)
        }
        .task {
            model.refreshLocationServices()
            await withTaskGroup(of: Void.self) { group in
                group.addTask { await model.observeAuthorization() }
                group.addTask { await model.observePreferences() }
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                model.refreshLocationServices()
            }
        }
        .onChange(of: page) { _, _ in
            toolbarsVisible = true
        }
    }

    @ViewBuilder
    private func tabContent<Content: View>(
        showsControls: Bool,
        @ViewBuilder content: () -> Content
    ) -> some View {
        content()
            .overlay(alignment: .bottomLeading) {
                if showsControls {
                    floatingControls
                        .transition(.opacity)
                }
            }
            .overlay(alignment: .top) {
                errorBanner
            }
            .animation(.easeInOut(duration: 0.2), value: showsControls)
            .animation(.easeInOut(duration: 0.2), value: model.errorMessage)
    }

    private var floatingControls: some View {
        VStack(alignment: .leading, spacing: 8) {
            radiusMenu
            placesTrigger
        }
        .padding(.leading, 16)
        .safeAreaPadding(.bottom, 12)
    }

    private var radiusMenu: some View {
        Menu {
            ForEach(PlaceSearchRadius.allCases, id: \.self) { radius in
                Button {
                    model.setSearchRadius(radius)
                } label: {
                    if radius == model.searchRadius {
                        Label(radius.title, systemImage: "checkmark")
                    } else {
                        Text(radius.title)
                    }
                }
                .accessibilityIdentifier("radius-option-\(radius.meters)")
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "scope")
                Text(model.searchRadius.title)
            }
            .font(.subheadline.weight(.medium))
            .padding(.horizontal, 12)
            .frame(height: 40)
            .background(.regularMaterial, in: Capsule())
        }
        .accessibilityIdentifier("radius-menu")
        .accessibilityLabel("Search radius, \(model.searchRadius.title)")
    }

    private var placesTrigger: some View {
        Button {
            showsPlacesSheet = true
        } label: {
            HStack(spacing: 6) {
                Image(systemName: model.userLocationEnabled ? "location.fill" : "location")
                Text(locationSummary)
                    .lineLimit(1)
            }
            .font(.subheadline.weight(.medium))
            .padding(.horizontal, 12)
            .frame(height: 40)
            .frame(maxWidth: 260, alignment: .leading)
            .background(.regularMaterial, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("places-trigger")
        .accessibilityLabel("Places & Location, \(locationSummary)")
    }

    private var locationSummary: String {
        let categoryCount = model.activeCategories.count
        if categoryCount > 0 {
            return categoryCount == 1 ? "1 category" : "\(categoryCount) categories"
        }
        if model.userLocationEnabled {
            return "My location"
        }
        if !model.customLocationAddress.isEmpty {
            return model.customLocationAddress
        }
        return "Set location"
    }

    @ViewBuilder
    private var errorBanner: some View {
        if let message = model.errorMessage {
            HStack(spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(.red)
                Text(message)
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(.primary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(.regularMaterial, in: Capsule())
            .padding(.horizontal, 16)
            .safeAreaPadding(.top, 8)
            .transition(.move(edge: .top).combined(with: .opacity))
            .accessibilityElement(children: .combine)
            .accessibilityIdentifier("error-banner")
        }
    }

    private func toggleToolbars() {
        guard !showsPlacesSheet else { return }
        withAnimation(.easeInOut(duration: 0.2)) {
            toolbarsVisible.toggle()
        }
    }

    private func resetSheetState() {
        sheetDetent = .medium
        categoryPath.removeAll()
    }
}

extension PlaceSearchRadius {
    var title: String {
        switch self {
        case .fiveHundred: "500 m"
        case .oneKilometer: "1 km"
        case .twoKilometers: "2 km"
        case .fiveKilometers: "5 km"
        }
    }
}
