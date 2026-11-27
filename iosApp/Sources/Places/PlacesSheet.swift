import Shared
import SwiftUI

/// The native Places & Location sheet: location header, custom-location search mode, the 13-category
/// grid, and the category drill-down stack.
struct PlacesSheet: View {
    let model: NearbyPlacesModel
    @Binding var sheetDetent: PresentationDetent
    @Binding var path: [CategoryRoute]

    @Environment(\.dismiss) private var dismiss
    @FocusState private var isSearchFocused: Bool
    @State private var searchText = ""
    @State private var showsSettingsAlert = false

    private var isSearchMode: Bool {
        isSearchFocused && !model.userLocationEnabled
    }

    var body: some View {
        NavigationStack(path: $path) {
            VStack(spacing: 0) {
                locationHeader
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 12)

                if isSearchMode {
                    searchStateView
                } else {
                    categoryGrid
                }
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .navigationTitle("Places & Location")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .accessibilityIdentifier("places-done")
                }
            }
            .navigationDestination(for: CategoryRoute.self) { route in
                CategoryPlacesScreen(route: route, model: model)
            }
        }
        .presentationDetents([.medium, .large], selection: $sheetDetent)
        .presentationDragIndicator(.visible)
        .presentationBackgroundInteraction(.enabled(upThrough: .medium))
        .presentationContentInteraction(.scrolls)
        .interactiveDismissDisabled(!path.isEmpty)
        .alert("Location access is off", isPresented: $showsSettingsAlert) {
            Button(MR.strings().open_settings.localized) {
                AppSettings.open()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Enable location access in Settings to use your current location.")
        }
        .onAppear {
            searchText = model.customLocationAddress
            if model.authorization == .denied || model.authorization == .restricted,
               model.userLocationEnabled
            {
                model.setUserLocationEnabled(false)
            }
        }
        .onChange(of: model.customLocationAddress) { _, address in
            if searchText != address {
                searchText = address
            }
        }
        .onChange(of: isSearchFocused) { _, focused in
            if focused {
                sheetDetent = .large
            }
        }
    }

    // MARK: - Location header

    private var locationHeader: some View {
        HStack(spacing: 8) {
            if model.userLocationEnabled {
                userLocationField
            } else {
                searchField
                if isSearchFocused {
                    Button(MR.strings().cancel.localized) {
                        cancelSearch()
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.tint)
                    .accessibilityIdentifier("location-search-cancel")
                }
            }

            Button {
                toggleUserLocation()
            } label: {
                Image(systemName: model.userLocationEnabled ? "location.fill" : "location")
                    .frame(width: 44, height: 44)
                    .foregroundStyle(model.userLocationEnabled ? Color.white : Color.accentColor)
                    .background(
                        model.userLocationEnabled
                            ? Color.accentColor
                            : Color.accentColor.opacity(0.12),
                        in: Circle()
                    )
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("use-my-location-button")
            .accessibilityLabel("Use my location")
            .accessibilityValue(model.userLocationEnabled ? "On" : "Off")
        }
    }

    private var userLocationField: some View {
        HStack(spacing: 8) {
            if model.userLocation == nil {
                ProgressView()
                Text(\.loading_location)
            } else if case .loading = onEnum(of: model.userLocationAddress) {
                ProgressView()
                Text(\.loading_location_address)
            } else {
                Image(systemName: "location.fill")
                    .foregroundStyle(.secondary)
                Text(locationAddressText)
            }
            Spacer(minLength: 0)
        }
        .font(.subheadline)
        .lineLimit(1)
        .padding(.horizontal, 12)
        .frame(height: 44)
        .frame(maxWidth: .infinity)
        .background(
            Color(uiColor: .secondarySystemBackground),
            in: RoundedRectangle(cornerRadius: 12)
        )
    }

    private var locationAddressText: String {
        guard case let .loaded(loaded) = onEnum(of: model.userLocationAddress),
              !loaded.address.isEmpty
        else {
            return MR.strings().location_address_not_found.localized
        }
        return loaded.address
    }

    private var searchField: some View {
        HStack(spacing: 6) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField(MR.strings().enter_location_placeholder.localized, text: addressBinding)
                .textFieldStyle(.plain)
                .focused($isSearchFocused)
                .submitLabel(.search)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .accessibilityIdentifier("location-search-field")
            if isSearchFocused && !searchText.isEmpty {
                Button {
                    searchText = ""
                    model.setCustomLocationAddress("")
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("location-search-clear")
                .accessibilityLabel("Clear location search")
            }
        }
        .padding(.horizontal, 12)
        .frame(height: 44)
        .frame(maxWidth: .infinity)
        .background(
            Color(uiColor: .secondarySystemBackground),
            in: RoundedRectangle(cornerRadius: 12)
        )
    }

    private var addressBinding: Binding<String> {
        Binding(
            get: { searchText },
            set: { newValue in
                searchText = newValue
                guard newValue != model.customLocationAddress else { return }
                model.setCustomLocationAddress(newValue)
            }
        )
    }

    // MARK: - Search mode

    @ViewBuilder
    private var searchStateView: some View {
        switch onEnum(of: model.customLocationSearchResults) {
        case .idle:
            prompt(MR.strings().start_typing_to_search.localized)
        case .queryTooShort:
            prompt(MR.strings().query_too_short.localized)
        case .loading:
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case let .loaded(loaded):
            if loaded.results.isEmpty {
                prompt("No locations found.")
            } else {
                List(Array(loaded.results.enumerated()), id: \.offset) { index, result in
                    Button {
                        select(result)
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "mappin.and.ellipse")
                                .foregroundStyle(.secondary)
                            Text(result.address)
                                .foregroundStyle(.primary)
                            Spacer(minLength: 0)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("custom-location-row-\(index)")
                }
                .listStyle(.plain)
            }
        }
    }

    private func prompt(_ text: String) -> some View {
        Text(text)
            .font(.headline)
            .multilineTextAlignment(.center)
            .foregroundStyle(.secondary)
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Category grid

    private var categoryGrid: some View {
        ScrollView {
            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: 160), spacing: 12)],
                spacing: 12
            ) {
                ForEach(PlaceCategory.allCases, id: \.self) { category in
                    CategoryGridCard(
                        category: category,
                        model: model,
                        onOpen: { open(category) }
                    )
                }
            }
            .padding(.horizontal, 16)

            Link(
                "© OpenStreetMap contributors",
                destination: URL(string: "https://www.openstreetmap.org/copyright")!
            )
            .font(.footnote)
            .foregroundStyle(.secondary)
            .padding(.vertical, 16)
            .accessibilityIdentifier("osm-attribution")
        }
    }

    // MARK: - Actions

    private func toggleUserLocation() {
        if model.userLocationEnabled {
            model.setUserLocationEnabled(false)
            return
        }
        if model.authorization == .denied || model.authorization == .restricted
            || model.isLocationServicesDisabled
        {
            showsSettingsAlert = true
            return
        }
        model.setUserLocationEnabled(true)
    }

    private func select(_ location: CustomLocation) {
        model.onCustomLocationSearchResultClick(location)
        isSearchFocused = false
    }

    private func cancelSearch() {
        model.cancelCustomLocationSearch()
        isSearchFocused = false
    }

    private func open(_ category: PlaceCategory) {
        sheetDetent = .large
        path.append(CategoryRoute(category: category))
    }
}

/// One category card with two separate hit zones: the leading toggle and the drill-down body.
private struct CategoryGridCard: View {
    let category: PlaceCategory
    let model: NearbyPlacesModel
    let onOpen: () -> Void

    private var isActive: Bool {
        model.isActive(category)
    }

    private var places: [Place] {
        model.places(in: category)
    }

    private var subtitle: String {
        guard isActive else { return "Not selected" }
        return model.isLoading(category) ? "Loading..." : MR.strings().places_count.localized([places.count])
    }

    var body: some View {
        HStack(spacing: 12) {
            Button {
                model.onTogglePlaceCategory(category)
            } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(isActive ? Color.accentColor : Color.secondary.opacity(0.15))
                    if model.isLoading(category) {
                        ProgressView()
                            .tint(isActive ? .white : .accentColor)
                    } else {
                        Image(category.icon)
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(isActive ? Color.white : Color.secondary)
                    }
                }
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(!isActive && model.activeLocation == nil)
            .accessibilityIdentifier("category-toggle-\(category.name)")
            .accessibilityLabel("Toggle \(category.title)")

            Button(action: onOpen) {
                HStack(spacing: 8) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(category.title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.primary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                        Text(subtitle)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                    if model.isLoaded(category) {
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(!model.isLoaded(category))
            .accessibilityIdentifier("category-card-\(category.name)")
            .accessibilityLabel("Open \(category.title), \(subtitle)")
        }
        .padding(10)
        .background(
            Color(uiColor: .secondarySystemGroupedBackground),
            in: RoundedRectangle(cornerRadius: 14)
        )
    }
}
