import XCTest

final class MapPageUITests: XCTestCase {
    private enum Timeout {
        static let element: TimeInterval = 25
        static let loaded: TimeInterval = 90
    }

    override func setUp() {
        continueAfterFailure = false
    }

    func testMapPageRendersToggledCategoryPlaces() {
        let app = XCUIApplication.launchedForUITest(arguments: ["-arCamera", "-mapDebug"])

        XCTAssertTrue(
            app.tabBars.buttons["Map"].waitForExistence(timeout: Timeout.element)
        )
        app.tabBars.buttons["Map"].tap()

        let map = app.otherElements["nearby-map"]
        XCTAssertTrue(map.waitForExistence(timeout: Timeout.element))
        XCTAssertEqual(
            placeCount(on: map),
            0,
            "The map page loaded places before any Place Category was toggled."
        )

        openPlacesSheet(in: app)
        setUserLocationButton(true, in: app, answeringPrompt: { LocationPrompt.allow() })
        toggleCategory("FoodDrink", in: app)
        app.buttons["places-done"].tap()

        waitForNoAlerts(in: app)
        waitForPlaces(on: map)
        XCTAssertGreaterThan(placeCount(on: map), 0, "The map page reported an empty place count.")

        let debugCamera = app.staticTexts["map-camera"]
        XCTAssertTrue(debugCamera.waitForExistence(timeout: Timeout.element))
        XCTAssertNotEqual(
            stableValue(of: debugCamera, timeout: 20, differingFrom: "none"),
            "none",
            "The map camera was not recorded after the fit."
        )

        waitForNoAlerts(in: app)
        attachScreenshot(named: "map-page")
    }
}
