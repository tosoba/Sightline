import XCTest

/// End-to-end smoke pass for the iOS app: camera page, map with places, category push/pop,
/// a location fix, and preferences surviving a relaunch.
///
/// Location must be granted and a Krakow fix set before the run, because a UI test cannot call
/// `simctl`. See `.scratch/kmp-ios-migration/issues/25-ios-preferences-navigation-smoke.md`.
final class SmokeUITests: XCTestCase {
    private enum Timeout {
        static let element: TimeInterval = 25
        static let loaded: TimeInterval = 60
    }

    override func setUp() {
        continueAfterFailure = false
    }

    func testFullSmokePass() {
        let app = XCUIApplication()
        app.resetAuthorizationStatus(for: .location)
        app.launchArguments = ["-arCamera", "-mapDebug"]
        app.launch()
        allowLocationPromptIfNeeded()

        XCTAssertTrue(
            app.staticTexts["Synthetic feed - no camera on this device"]
                .waitForExistence(timeout: Timeout.element),
            "The camera page did not render its synthetic feed state."
        )
        attachScreenshot(named: "smoke-camera")

        enableUserLocationAndSetRadius(in: app)

        let mapButton = app.segmentedControls.buttons["Map"]
        XCTAssertTrue(mapButton.waitForExistence(timeout: Timeout.element))
        mapButton.tap()

        let map = app.otherElements["nearby-map"]
        XCTAssertTrue(map.waitForExistence(timeout: Timeout.element), "The map page did not render.")
        waitForPlaces(on: map)
        attachScreenshot(named: "smoke-map")

        let category = app.buttons["Sights"]
        XCTAssertTrue(category.waitForExistence(timeout: Timeout.element))
        category.tap()

        let categoryTitle = app.navigationBars["Sights"]
        XCTAssertTrue(
            categoryTitle.waitForExistence(timeout: Timeout.loaded),
            "Tapping a category did not push the category screen."
        )
        XCTAssertTrue(
            app.cells.firstMatch.waitForExistence(timeout: Timeout.loaded),
            "The category screen did not show any place rows."
        )
        attachScreenshot(named: "smoke-category")

        categoryTitle.buttons.firstMatch.tap()
        XCTAssertTrue(
            app.segmentedControls.buttons["Map"].waitForExistence(timeout: Timeout.element),
            "Back navigation did not return to the pager."
        )
        XCTAssertTrue(
            app.buttons["Sights"].exists,
            "The selected category was not preserved after back navigation."
        )

        let debugCamera = app.staticTexts["map-camera"]
        XCTAssertTrue(debugCamera.waitForExistence(timeout: Timeout.element))
        let fittedCamera = debugCamera.label
        XCTAssertNotEqual(fittedCamera, "none", "The map camera was not recorded after the fit.")
        map.pinch(withScale: 1.8, velocity: 1)
        let savedCamera = stableValue(of: debugCamera, timeout: 15, differingFrom: fittedCamera)
        XCTAssertNotEqual(savedCamera, fittedCamera, "Changing the map camera did not persist a new value.")
        print("SIGHTLINE_TEST map camera fitted=\(fittedCamera) saved=\(savedCamera)")
        attachScreenshot(named: "smoke-map-panned")

        app.terminate()
        app.launch()
        allowLocationPromptIfNeeded()

        let mapButtonAfterRelaunch = app.segmentedControls.buttons["Map"]
        XCTAssertTrue(mapButtonAfterRelaunch.waitForExistence(timeout: Timeout.element))
        mapButtonAfterRelaunch.tap()

        let restoredCamera = app.staticTexts["map-camera"]
        XCTAssertTrue(restoredCamera.waitForExistence(timeout: Timeout.element))
        let restored = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "label == %@", savedCamera),
            object: restoredCamera
        )
        XCTAssertEqual(
            XCTWaiter().wait(for: [restored], timeout: Timeout.loaded),
            .completed,
            "The map camera did not survive a relaunch."
        )
        attachScreenshot(named: "smoke-map-restored")

        openPreferences(in: app)
        XCTAssertTrue(
            app.segmentedControls.buttons["2 km"].waitForExistence(timeout: Timeout.element)
        )
        XCTAssertTrue(
            app.segmentedControls.buttons["2 km"].isSelected,
            "The search radius did not survive a relaunch."
        )
        XCTAssertEqual(
            app.switches["Use my location"].value as? String,
            "1",
            "The user-location toggle did not survive a relaunch."
        )
        attachScreenshot(named: "smoke-preferences-restored")
        app.buttons["Done"].tap()

        openPreferences(in: app)
        app.segmentedControls.buttons["1 km"].tap()
        app.buttons["Done"].tap()
    }

    /// User story 10: a denied location permission offers the app's Settings deep link, the same
    /// affordance the camera denial uses.
    func testLocationDenialOffersSettings() {
        let app = XCUIApplication()
        app.resetAuthorizationStatus(for: .location)
        app.launch()
        denyLocationPromptIfNeeded()

        openPreferences(in: app)
        let toggle = app.switches["Use my location"]
        XCTAssertTrue(toggle.waitForExistence(timeout: Timeout.element))
        if (toggle.value as? String) == "0" {
            toggle.tap()
            denyLocationPromptIfNeeded()
        }
        app.buttons["Done"].tap()

        XCTAssertTrue(
            app.buttons["Settings"].waitForExistence(timeout: Timeout.element),
            "A denied location permission did not offer the Settings deep link."
        )
        attachScreenshot(named: "smoke-location-denied")
    }

    private func enableUserLocationAndSetRadius(in app: XCUIApplication) {
        openPreferences(in: app)

        let toggle = app.switches["Use my location"]
        XCTAssertTrue(toggle.waitForExistence(timeout: Timeout.element))
        if (toggle.value as? String) == "0" {
            toggle.tap()
            allowLocationPromptIfNeeded()
            XCTAssertEqual(toggle.value as? String, "1", "The user-location toggle did not turn on.")
        }

        let radius = app.segmentedControls.buttons["2 km"]
        XCTAssertTrue(radius.waitForExistence(timeout: Timeout.element))
        radius.tap()

        app.buttons["Done"].tap()
    }

    /// The app asks for when-in-use authorization when the toggle first starts the location stream.
    /// The system prompt is owned by SpringBoard, so answer it directly instead of relying on the
    /// test runner's automatic dismissal, which picks "Don't Allow".
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

    private func denyLocationPromptIfNeeded() {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let alert = springboard.alerts.firstMatch
        guard alert.waitForExistence(timeout: 5) else { return }

        let deny = alert.buttons["Don't Allow"]
        if deny.exists {
            deny.tap()
        }
    }

    private func openPreferences(in app: XCUIApplication) {
        let button = app.buttons["Preferences"]
        XCTAssertTrue(button.waitForExistence(timeout: Timeout.element))
        button.tap()
        XCTAssertTrue(app.navigationBars["Preferences"].waitForExistence(timeout: Timeout.element))
    }

    private func waitForPlaces(on map: XCUIElement) {
        let populated = NSPredicate(format: "value != %@", "0 places")
        let expectation = XCTNSPredicateExpectation(predicate: populated, object: map)
        XCTAssertEqual(
            XCTWaiter().wait(for: [expectation], timeout: Timeout.loaded),
            .completed,
            "The map never received nearby places."
        )
    }

    /// Waits for a debug label to settle on a new value, so the assertion compares the debounced
    /// camera that was actually persisted instead of a value mid-gesture.
    private func stableValue(
        of element: XCUIElement,
        timeout: TimeInterval,
        differingFrom initial: String
    ) -> String {
        let deadline = Date().addingTimeInterval(timeout)
        var last = element.label
        while Date() < deadline {
            RunLoop.current.run(until: Date().addingTimeInterval(0.5))
            let current = element.label
            if current != initial, current == last, current != "none" {
                return current
            }
            last = current
        }
        return last
    }

    private func attachScreenshot(named name: String) {
        let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshot.name = name
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
