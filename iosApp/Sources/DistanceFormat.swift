import Foundation

/// Matches Android's `Float.formattedDistance()`: metres below one kilometre, one decimal above it,
/// and no trailing `.0`. The AR Marker card deliberately keeps its own always-one-decimal format.
enum DistanceFormat {
    static func meters(_ meters: Float) -> String {
        guard meters >= 1_000 else {
            return "\(Int(meters.rounded())) m"
        }
        let kilometers = (Double(meters) / 1_000.0 * 10.0).rounded() / 10.0
        return kilometers == kilometers.rounded()
            ? "\(Int(kilometers)) km"
            : String(format: "%.1f km", kilometers)
    }
}

extension String {
    /// Matches Android's `String.placeInitials()`: first characters of the first two words, uppercased.
    var placeInitials: String {
        let words = split(whereSeparator: \.isWhitespace)
        guard let first = words.first, let character = first.first else { return "?" }
        if words.count >= 2, let second = words[1].first {
            return "\(character)\(second)".uppercased()
        }
        return String(character).uppercased()
    }
}
