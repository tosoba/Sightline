import XCTest

final class MapPageUITests: XCTestCase {
    func testMapPageRendersPlaces() {
        let app = XCUIApplication()
        app.resetAuthorizationStatus(for: .location)
        app.launch()
        allowLocationPromptIfNeeded()

        let preferences = app.buttons["Preferences"]
        XCTAssertTrue(preferences.waitForExistence(timeout: 25))
        preferences.tap()

        let toggle = app.switches["Use my location"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 25))
        if (toggle.value as? String) == "0" {
            toggle.tap()
            allowLocationPromptIfNeeded()
        }
        app.buttons["Done"].tap()

        let mapButton = app.segmentedControls.buttons["Map"]
        XCTAssertTrue(mapButton.waitForExistence(timeout: 25))
        mapButton.tap()

        let map = app.otherElements["nearby-map"]
        XCTAssertTrue(map.waitForExistence(timeout: 25))

        let populated = NSPredicate(format: "value != %@", "0 places")
        let expectation = XCTNSPredicateExpectation(predicate: populated, object: map)
        XCTAssertEqual(XCTWaiter().wait(for: [expectation], timeout: 60), .completed)

        let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshot.name = "map-page"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    private func allowLocationPromptIfNeeded() {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let alert = springboard.alerts.firstMatch
        guard alert.waitForExistence(timeout: 5) else { return }

        for title in ["Allow While Using App", "Allow Once", "Allow"] {
            let button = alert.buttons[title]
            if button.exists {
                button.tap()
                return
            }
        }
    }
}
