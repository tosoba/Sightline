import Shared
import SwiftUI

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

/// Preferences backed by the shared `PreferencesFacade`; every change is written through it, so the
/// values land in the same DataStore file Android uses.
struct PreferencesView: View {
    let model: NearbyPlacesModel

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Search radius", selection: radiusBinding) {
                        ForEach(PlaceSearchRadius.allCases, id: \.self) { radius in
                            Text(radius.title).tag(radius)
                        }
                    }
                    .pickerStyle(.segmented)

                    Toggle("Use my location", isOn: locationBinding)
                } footer: {
                    Text("Nearby places are searched within the selected radius around your location.")
                }
            }
            .navigationTitle("Preferences")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private var radiusBinding: Binding<PlaceSearchRadius> {
        Binding(
            get: { model.searchRadius },
            set: { model.setSearchRadius($0) }
        )
    }

    private var locationBinding: Binding<Bool> {
        Binding(
            get: { model.userLocationEnabled },
            set: { model.setUserLocationEnabled($0) }
        )
    }
}
