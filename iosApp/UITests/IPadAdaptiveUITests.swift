import XCTest

/// iPad-only adaptive layout pass: the universal target presents the Places & Location sheet as a
/// centered page sheet, and the sheet, tabs, and camera projection stay usable across all four iPad
/// orientations.
final class IPadAdaptiveUITests: XCTestCase {
    private enum Timeout {
        static let element: TimeInterval = 30
        static let rotation: TimeInterval = 20
    }

    override func setUp() {
        continueAfterFailure = false
    }

    override func tearDown() {
        XCUIDevice.shared.orientation = .portrait
    }

    func testPlacesSheetAndFourOrientations() throws {
        try XCTSkipIf(
            UIDevice.current.userInterfaceIdiom != .pad,
            "iPad-only adaptive layout test."
        )

        XCUIDevice.shared.orientation = .portrait
        let app = XCUIApplication.launchedForUITest(
            arguments: ["-arCamera", "-arSynthetic", "-arAttitude", "uprightNorth", "-arDebug"]
        )

        let layoutDebug = app.staticTexts["ar-layout-debug"]
        XCTAssertTrue(
            layoutDebug.waitForExistence(timeout: Timeout.element),
            "The AR camera layout debug text did not render."
        )
        XCTAssertTrue(
            waitForLabelContains(layoutDebug, text: " r0 ", timeout: Timeout.rotation),
            "The camera projection did not start in portrait; its text is \(layoutDebug.label)."
        )

        let cameraTab = app.mainTabButton("Camera")
        let mapTab = app.mainTabButton("Map")
        XCTAssertTrue(
            cameraTab.waitForExistence(timeout: Timeout.element),
            "The Camera tab did not render on iPad."
        )
        XCTAssertTrue(
            mapTab.waitForExistence(timeout: Timeout.element),
            "The Map tab did not render on iPad."
        )
        attachScreenshot(named: "ipad-camera-portrait")

        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertTrue(
            waitForLabelContains(layoutDebug, text: " r90 ", timeout: Timeout.rotation),
            "Landscape left did not map to Screen Rotation 90; its text is \(layoutDebug.label)."
        )
        attachScreenshot(named: "ipad-camera-landscape")
        XCTAssertTrue(
            waitForLandscapeViewport(layoutDebug, timeout: Timeout.rotation),
            "The camera viewport did not follow the landscape window; its text is \(layoutDebug.label)."
        )
        attachScreenshot(named: "ipad-camera-landscape-settled")

        mapTab.tap()
        let map = app.otherElements["nearby-map"]
        XCTAssertTrue(
            map.waitForExistence(timeout: Timeout.element),
            "The map page did not render in landscape on iPad."
        )
        attachScreenshot(named: "ipad-map-landscape")

        cameraTab.tap()
        let trigger = app.buttons["places-trigger"]
        XCTAssertTrue(
            trigger.waitForExistence(timeout: Timeout.element),
            "The camera page controls did not return in landscape on iPad."
        )

        XCUIDevice.shared.orientation = .portrait
        XCTAssertTrue(
            waitForLabelContains(layoutDebug, text: " r0 ", timeout: Timeout.rotation),
            "The camera projection did not return to portrait; its text is \(layoutDebug.label)."
        )

        let launchWindow = app.windows.firstMatch.frame
        trigger.tap()

        assertCenteredPageSheet(in: app, inside: launchWindow)
        XCTAssertTrue(
            app.buttons["category-toggle-SightsLandmarks"].exists,
            "The category grid is missing from the iPad page sheet."
        )
        attachScreenshot(named: "ipad-places-portrait")

        XCUIDevice.shared.orientation = .landscapeRight
        XCTAssertTrue(
            waitForLabelContains(layoutDebug, text: " r270 ", timeout: Timeout.rotation),
            "Landscape right did not map to Screen Rotation 270; its text is \(layoutDebug.label)."
        )

        let sheetDone = app.buttons["places-done"]
        XCTAssertTrue(
            app.navigationBars["Places & Location"].waitForExistence(timeout: Timeout.element),
            "The page sheet did not survive the rotation to landscape."
        )
        XCTAssertTrue(sheetDone.isHittable, "The page sheet's Done button is not tappable in landscape.")
        XCTAssertTrue(
            app.buttons["category-toggle-SightsLandmarks"].exists,
            "The category grid disappeared after rotating to landscape."
        )
        attachScreenshot(named: "ipad-places-landscape")

        sheetDone.tap()
        XCTAssertTrue(
            waitForNonExistence(of: app.navigationBars["Places & Location"], timeout: Timeout.element),
            "The page sheet's Done button did not dismiss it."
        )

        XCTAssertTrue(
            cameraTab.waitForExistence(timeout: Timeout.element),
            "The tabs are not reachable after dismissing the sheet on iPad."
        )

        XCUIDevice.shared.orientation = .portraitUpsideDown
        XCTAssertTrue(
            waitForLabelContains(layoutDebug, text: " r180 ", timeout: Timeout.rotation),
            "Portrait upside down did not map to Screen Rotation 180; its text is \(layoutDebug.label)."
        )
        attachScreenshot(named: "ipad-camera-upside-down")

        XCUIDevice.shared.orientation = .portrait
        XCTAssertTrue(
            waitForLabelContains(layoutDebug, text: " r0 ", timeout: Timeout.rotation),
            "The camera projection did not return to portrait; its text is \(layoutDebug.label)."
        )
    }

    private func assertCenteredPageSheet(in app: XCUIApplication, inside launchWindow: CGRect) {
        let done = app.buttons["places-done"]
        XCTAssertTrue(
            done.waitForExistence(timeout: Timeout.element),
            "The Places & Location sheet did not present on iPad."
        )

        let sheetBar = app.navigationBars["Places & Location"]
        XCTAssertTrue(
            sheetBar.waitForExistence(timeout: Timeout.element),
            "The sheet's navigation bar is missing on iPad."
        )

        XCTAssertLessThan(
            sheetBar.frame.width,
            launchWindow.width,
            "The sheet spans the whole window instead of presenting as a page sheet."
        )
        XCTAssertGreaterThan(
            sheetBar.frame.minY,
            launchWindow.minY,
            "The sheet is flush with the window top instead of presenting as a page sheet."
        )
    }

    private func waitForLandscapeViewport(
        _ layoutDebug: XCUIElement,
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if let viewport = viewportSize(in: layoutDebug.label), viewport.width > viewport.height {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
        return false
    }

    private func viewportSize(in text: String) -> CGSize? {
        let tokens = text.split(separator: " ")
        guard let marker = tokens.firstIndex(of: "vp"), tokens.indices.contains(marker + 1) else {
            return nil
        }
        let value = tokens[marker + 1]
        guard let separator = value.firstIndex(of: "x"),
            let width = Double(value[value.startIndex..<separator]),
            let height = Double(value[value.index(after: separator)...])
        else {
            return nil
        }
        return CGSize(width: width, height: height)
    }

    private func waitForNonExistence(of element: XCUIElement, timeout: TimeInterval) -> Bool {
        let expectation = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "exists == false"),
            object: element
        )
        return XCTWaiter().wait(for: [expectation], timeout: timeout) == .completed
    }
}
