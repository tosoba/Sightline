import Shared
import SwiftUI

extension PlaceCategory {
    var title: String {
        switch self {
        case .sightsLandmarks: "Sights"
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
}
