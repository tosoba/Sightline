import XCTest

final class MapPageUITests: XCTestCase {
    private enum Timeout {
        static let element: TimeInterval = 25
        static let loaded: TimeInterval = 60
    }

    override func setUp() {
        continueAfterFailure = false
    }

    func testMapPageRendersPlaces() {
        let app = XCUIApplication.launchedForUITest()

        let preferences = app.buttons["Preferences"]
        XCTAssertTrue(preferences.waitForExistence(timeout: Timeout.element))
        preferences.tap()

        XCTAssertEqual(
            app.switches["user-location-toggle"].value as? String,
            "0",
            "The preferences were not reset to their defaults at launch."
        )
        setUserLocationToggle(true, in: app, answeringPrompt: { LocationPrompt.allow() })
        app.buttons["Done"].tap()

        let mapButton = app.segmentedControls.buttons["Map"]
        XCTAssertTrue(mapButton.waitForExistence(timeout: Timeout.element))
        mapButton.tap()

        let map = app.otherElements["nearby-map"]
        XCTAssertTrue(map.waitForExistence(timeout: Timeout.element))

        waitForNoAlerts(in: app)

        let populated = NSPredicate(
            format: "label MATCHES %@",
            "^Nearby places map, [1-9][0-9]* places?$"
        )
        let expectation = XCTNSPredicateExpectation(predicate: populated, object: map)
        XCTAssertEqual(
            XCTWaiter().wait(for: [expectation], timeout: Timeout.loaded),
            .completed,
            "The map page did not load any places; its label is \(map.label)."
        )
        XCTAssertGreaterThan(placeCount(on: map), 0, "The map page reported an empty place count.")

        waitForNoAlerts(in: app)

        let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshot.name = "map-page"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    private func placeCount(on map: XCUIElement) -> Int {
        let value = map.label.replacingOccurrences(of: "Nearby places map, ", with: "")
        guard let count = value.split(separator: " ").first else { return 0 }
        return Int(count) ?? 0
    }
}
