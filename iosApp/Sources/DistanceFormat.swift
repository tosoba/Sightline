import Foundation

/// Matches Android's `Float.formattedDistance()`: metres below one kilometre, one decimal above it,
/// and no trailing `.0`. The AR Marker card deliberately keeps its own always-one-decimal format.
enum DistanceFormat {
    static func formatted(meters: Float) -> String {
        guard meters >= 1_000 else {
            return "\(Int(meters.rounded())) m"
        }
        let kilometers = (Double(meters) / 1_000.0 * 10.0).rounded() / 10.0
        return kilometers == kilometers.rounded()
            ? "\(Int(kilometers)) km"
            : String(format: "%.1f km", kilometers)
    }
}
