import XCTest

/// End-to-end smoke passes for the full-screen tabs, Places & Location sheet, custom location
/// search, category drill-down, and persistence across a relaunch.
///
/// Every run starts by resetting the shared preferences through `-resetPreferences`, so the pass
/// does not depend on state left by an earlier run. A UI test cannot call `simctl`; the simulator's
/// location scenario and the live places/address network are part of the environment.
final class SmokeUITests: XCTestCase {
    private enum Timeout {
        static let element: TimeInterval = 25
        static let loaded: TimeInterval = 90
    }

    override func setUp() {
        continueAfterFailure = false
    }

    func testLaunchesWithNoPlacesBeforeCategoryToggle() {
        let app = XCUIApplication.launchedForUITest(arguments: ["-arCamera"])

        XCTAssertTrue(
            app.mainTabButton("Camera").waitForExistence(timeout: Timeout.element),
            "The Camera tab did not render."
        )
        app.mainTabButton("Map").tap()

        let map = app.otherElements["nearby-map"]
        XCTAssertTrue(map.waitForExistence(timeout: Timeout.element), "The map page did not render.")
        XCTAssertEqual(map.label, "Nearby places map, 0 places")

        Thread.sleep(forTimeInterval: 3)
        XCTAssertEqual(
            map.label,
            "Nearby places map, 0 places",
            "The app fetched places before any Place Category was toggled."
        )
        attachScreenshot(named: "smoke-launch-zero-places")

        openPlacesSheet(in: app)
        let card = app.buttons["category-card-SightsLandmarks"]
        XCTAssertTrue(card.waitForExistence(timeout: Timeout.element))
        XCTAssertEqual(card.label, "Open Sights & landmarks, Not selected")
        XCTAssertFalse(
            app.buttons["category-toggle-SightsLandmarks"].isEnabled,
            "A category toggle was enabled without an Active Location."
        )

        app.swipeUp()
        app.swipeUp()
        let attribution = app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "OpenStreetMap contributors")
        ).firstMatch
        XCTAssertTrue(
            attribution.waitForExistence(timeout: 10),
            "The OpenStreetMap attribution footer is missing."
        )
        attachScreenshot(named: "smoke-places-grid")
    }

    func testCustomLocationSearchEnablesCategoryToggles() {
        let app = XCUIApplication.launchedForUITest(arguments: ["-arCamera"])

        openPlacesSheet(in: app)
        XCTAssertEqual(
            app.buttons["use-my-location-button"].value as? String,
            "Off",
            "Use my location was not off at launch."
        )

        let field = app.textFields["location-search-field"]
        XCTAssertTrue(field.waitForExistence(timeout: Timeout.element))
        field.tap()
        field.typeText("Wa")
        XCTAssertTrue(
            app.staticTexts["Your query must be at least 3 characters long."]
                .waitForExistence(timeout: 15),
            "The query-too-short prompt did not appear."
        )
        attachScreenshot(named: "smoke-custom-location-too-short")

        field.typeText("wel")
        XCTAssertTrue(
            waitForValue(of: field, equals: "Wawel", timeout: 15),
            "The search field text did not settle; its value is \(String(describing: field.value))."
        )
        let firstResult = app.buttons["custom-location-row-0"]
        XCTAssertTrue(
            firstResult.waitForExistence(timeout: Timeout.loaded),
            "The custom location search returned no results."
        )
        firstResult.tap()
        attachScreenshot(named: "smoke-custom-location-selected")

        let toggle = app.buttons["category-toggle-SightsLandmarks"]
        XCTAssertTrue(toggle.waitForExistence(timeout: Timeout.element))
        XCTAssertTrue(
            waitForEnabled(toggle, timeout: Timeout.element),
            "The category toggle stayed disabled after picking a Custom Location."
        )
        toggle.tap()
        waitForCategoryPlaces(
            named: "SightsLandmarks",
            title: "Sights & landmarks",
            in: app
        )

        toggleCategory("FoodDrink", in: app)
        waitForCategoryPlaces(named: "FoodDrink", title: "Food & drink", in: app)
        XCTAssertTrue(
            categoryCardShowsCount(
                app.buttons["category-card-SightsLandmarks"],
                title: "Sights & landmarks"
            ),
            "The first category card lost its count while a second category was active."
        )
        attachScreenshot(named: "smoke-custom-location-two-categories")

        app.buttons["category-toggle-SightsLandmarks"].tap()
        let sightsCard = app.buttons["category-card-SightsLandmarks"]
        let deselected = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "label == %@", "Open Sights & landmarks, Not selected"),
            object: sightsCard
        )
        XCTAssertEqual(
            XCTWaiter().wait(for: [deselected], timeout: Timeout.element),
            .completed,
            "Toggling a category off did not clear it; its label is \(sightsCard.label)."
        )

        Thread.sleep(forTimeInterval: 2)
        waitForCategoryPlaces(named: "FoodDrink", title: "Food & drink", in: app)
        XCTAssertTrue(
            categoryCardShowsCount(
                app.buttons["category-card-FoodDrink"],
                title: "Food & drink"
            ),
            "The remaining category lost its count after the other category was toggled off."
        )
        attachScreenshot(named: "smoke-custom-location-independent-categories")

        app.buttons["places-done"].tap()
        app.mainTabButton("Map").tap()

        let map = app.otherElements["nearby-map"]
        XCTAssertTrue(map.waitForExistence(timeout: Timeout.element))
        waitForPlaces(on: map)
        attachScreenshot(named: "smoke-custom-location-map")
    }

    func testUserLocationRadiusAndCategoryDrillDown() {
        let app = XCUIApplication.launchedForUITest(arguments: ["-arCamera"])

        openPlacesSheet(in: app)
        setUserLocationButton(true, in: app, answeringPrompt: { LocationPrompt.allow() })
        app.buttons["places-done"].tap()

        chooseRadius("2 km", in: app)

        openPlacesSheet(in: app)
        toggleCategory("SightsLandmarks", in: app)
        waitForCategoryPlaces(
            named: "SightsLandmarks",
            title: "Sights & landmarks",
            in: app
        )

        app.buttons["category-card-SightsLandmarks"].tap()
        XCTAssertTrue(
            app.navigationBars["Sights & landmarks"].waitForExistence(timeout: Timeout.element),
            "Tapping a loaded category did not push CategoryPlacesScreen."
        )

        let categoryMap = app.otherElements["category-map"]
        XCTAssertTrue(
            categoryMap.waitForExistence(timeout: Timeout.element),
            "The category map did not render."
        )

        let placeRow = app.buttons
            .matching(NSPredicate(format: "identifier BEGINSWITH 'place-row-'"))
            .firstMatch
        XCTAssertTrue(
            placeRow.waitForExistence(timeout: Timeout.loaded),
            "The category place list stayed empty."
        )
        XCTAssertTrue(
            app.buttons
                .matching(NSPredicate(format: "identifier BEGINSWITH 'open-in-maps-'"))
                .firstMatch
                .exists,
            "The place rows are missing their open-in-maps button."
        )
        attachScreenshot(named: "smoke-category-places")

        app.navigationBars["Sights & landmarks"].buttons.firstMatch.tap()
        XCTAssertTrue(
            app.buttons["places-done"].waitForExistence(timeout: Timeout.element),
            "Back navigation did not return to the category grid."
        )
    }

    func testMapCameraAndPreferencesSurviveRelaunch() {
        let app = XCUIApplication.launchedForUITest(arguments: ["-arCamera", "-mapDebug"])

        openPlacesSheet(in: app)
        setUserLocationButton(true, in: app, answeringPrompt: { LocationPrompt.allow() })
        toggleCategory("FoodDrink", in: app)
        app.buttons["places-done"].tap()

        chooseRadius("2 km", in: app)

        app.mainTabButton("Map").tap()
        let map = app.otherElements["nearby-map"]
        XCTAssertTrue(map.waitForExistence(timeout: Timeout.element))
        waitForPlaces(on: map)

        let debugCamera = app.staticTexts["map-camera"]
        XCTAssertTrue(debugCamera.waitForExistence(timeout: Timeout.element))
        let fittedCamera = stableValue(of: debugCamera, timeout: 20, differingFrom: "none")
        XCTAssertNotEqual(fittedCamera, "none", "The map camera was not recorded after the fit.")

        map.pinch(withScale: 1.8, velocity: 1)
        let savedCamera = stableValue(of: debugCamera, timeout: 20, differingFrom: fittedCamera)
        XCTAssertNotEqual(
            savedCamera,
            fittedCamera,
            "Changing the map camera did not persist a new value."
        )
        attachScreenshot(named: "smoke-map-panned")

        app.terminate()
        app.launchArguments = ["-arCamera", "-mapDebug"]
        app.launch()

        XCTAssertTrue(
            app.mainTabButton("Map").waitForExistence(timeout: Timeout.element),
            "The tabs did not render after relaunch."
        )
        app.mainTabButton("Map").tap()

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

        openPlacesSheet(in: app)
        XCTAssertEqual(
            app.buttons["use-my-location-button"].value as? String,
            "On",
            "Use my location did not survive a relaunch."
        )
        attachScreenshot(named: "smoke-places-restored")
        app.buttons["places-done"].tap()

        XCTAssertTrue(
            waitForLabelContains(app.buttons["radius-menu"], text: "2 km", timeout: Timeout.element),
            "The Search Radius did not survive a relaunch."
        )

        chooseRadius("1 km", in: app)
    }

    func testDeniedLocationShowsOpenSettingsAlert() {
        let app = XCUIApplication.launchedForUITest(arguments: ["-arCamera"])

        openPlacesSheet(in: app)
        let button = app.buttons["use-my-location-button"]
        XCTAssertTrue(button.waitForExistence(timeout: Timeout.element))
        XCTAssertEqual(button.value as? String, "Off")

        button.tap()
        LocationPrompt.deny()
        XCTAssertTrue(
            waitForValue(of: button, equals: "Off", timeout: 30),
            "Use my location did not reset after the permission was denied; its value is \(String(describing: button.value))."
        )

        button.tap()
        let alert = app.alerts.firstMatch
        XCTAssertTrue(
            alert.waitForExistence(timeout: 10),
            "Tapping Use my location after denial did not present the Settings alert."
        )
        XCTAssertTrue(alert.buttons["Open Settings"].exists)
        XCTAssertTrue(alert.buttons["Cancel"].exists)
        attachScreenshot(named: "smoke-location-denied")
        alert.buttons["Cancel"].tap()
    }
}
