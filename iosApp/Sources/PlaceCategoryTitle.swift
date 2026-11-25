import Shared
import SwiftUI

extension PlaceCategory {
    var title: String {
        switch self {
        case .sightsLandmarks: "Sights & landmarks"
        case .artsCulture: "Arts & culture"
        case .foodDrink: "Food & drink"
        case .nightlife: "Nightlife"
        case .shopping: "Shopping"
        case .natureOutdoors: "Nature & outdoors"
        case .library: "Library"
        case .accommodation: "Accommodation"
        case .gettingAround: "Getting around"
        case .roadTrip: "Road trip"
        case .moneyBanking: "Money & banking"
        case .health: "Health"
        case .practical: "Practical"
        }
    }

    /// One SF Symbol per Place Category, shared by AR Marker cards, grid cards and detail headers.
    var symbolName: String {
        switch self {
        case .sightsLandmarks: "building.columns"
        case .artsCulture: "theatermasks"
        case .foodDrink: "fork.knife"
        case .nightlife: "moon.stars"
        case .shopping: "bag"
        case .natureOutdoors: "leaf"
        case .library: "books.vertical"
        case .accommodation: "bed.double"
        case .gettingAround: "tram"
        case .roadTrip: "car"
        case .moneyBanking: "banknote"
        case .health: "cross.case"
        case .practical: "wrench.and.screwdriver"
        }
    }
}
