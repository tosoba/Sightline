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
    @State private var page: MainPage = .camera

    init(shared: SightlineShared) {
        _model = State(initialValue: NearbyPlacesModel(shared: shared))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                toolbar
                Divider()
                TabView(selection: $page) {
                    ARCameraScreen(nearbyModel: model, isActive: page == .camera)
                        .tag(MainPage.camera)
                    MapScreen(model: model)
                        .tag(MainPage.map)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
            .navigationTitle("Nearby places")
            .task {
                await withTaskGroup(of: Void.self) { group in
                    group.addTask { await model.observeAuthorization() }
                    group.addTask { await model.observeLocation() }
                }
            }
        }
    }

    private var toolbar: some View {
        HStack(spacing: 16) {
            Picker(
                "Category",
                selection: Binding(
                    get: { model.selectedCategory },
                    set: { model.select($0) }
                )
            ) {
                ForEach(PlaceCategory.allCases, id: \.self) { category in
                    Text(category.title).tag(category)
                }
            }
            .pickerStyle(.menu)

            Spacer(minLength: 0)

            Picker("Page", selection: $page) {
                ForEach(MainPage.allCases) { page in
                    Label(page.title, systemImage: page.systemImage).tag(page)
                }
            }
            .pickerStyle(.segmented)
            .fixedSize()
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
}
