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
        case .camera: "camera"
        case .map: "map"
        }
    }
}

struct MainScreen: View {
    @State private var model: NearbyPlacesModel
    @State private var page: MainPage = ARCameraLaunchOptions.current.requestedPage ?? .camera
    @State private var path: [CategoryRoute] = []
    @State private var showsPreferences = false

    init(shared: SightlineShared) {
        _model = State(initialValue: NearbyPlacesModel(shared: shared))
    }

    var body: some View {
        NavigationStack(path: $path) {
            VStack(spacing: 0) {
                header
                Divider()
                locationStatus
                TabView(selection: $page) {
                    ARCameraScreen(nearbyModel: model, isActive: page == .camera)
                        .tag(MainPage.camera)
                    MapScreen(model: model)
                        .tag(MainPage.map)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
            .navigationTitle("Nearby places")
            .navigationDestination(for: CategoryRoute.self) { route in
                CategoryPlacesScreen(route: route)
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showsPreferences = true
                    } label: {
                        Label("Preferences", systemImage: "gearshape")
                    }
                }
            }
            .sheet(isPresented: $showsPreferences) {
                PreferencesView(model: model)
            }
            .task {
                await withTaskGroup(of: Void.self) { group in
                    group.addTask { await model.observeAuthorization() }
                    group.addTask { await model.observePreferences() }
                }
            }
        }
    }

    private var header: some View {
        VStack(spacing: 10) {
            categoryRow

            Picker("Page", selection: $page) {
                ForEach(MainPage.allCases) { page in
                    Label(page.title, systemImage: page.systemImage).tag(page)
                }
            }
            .pickerStyle(.segmented)
            .fixedSize()
        }
        .padding(.vertical, 8)
    }

    private var categoryRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(PlaceCategory.allCases, id: \.self) { category in
                    Button {
                        open(category)
                    } label: {
                        Text(category.title)
                            .font(.subheadline.weight(.medium))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .foregroundStyle(
                                category == model.selectedCategory ? Color.accentColor : .primary
                            )
                            .background(
                                category == model.selectedCategory
                                    ? Color.accentColor.opacity(0.16)
                                    : Color.secondary.opacity(0.12),
                                in: Capsule()
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal)
        }
    }

    @ViewBuilder
    private var locationStatus: some View {
        if model.isLocationDenied {
            LocationAccessBanner(
                message: "Location access is off. Enable it in Settings to see nearby places.",
                actionTitle: "Settings",
                action: AppSettings.open
            )
        } else if !model.userLocationEnabled {
            LocationAccessBanner(
                message: "Location is off.",
                actionTitle: "Turn on",
                action: { model.setUserLocationEnabled(true) }
            )
        }
    }

    private func open(_ category: PlaceCategory) {
        Task {
            await model.select(category)
            path.append(CategoryRoute(category: category, places: model.places))
        }
    }
}
