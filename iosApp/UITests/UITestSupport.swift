import XCTest

enum UITestLaunchArgument {
    /// Resets every shared preference through the Kotlin repository before the first frame.
    static let resetPreferences = "-resetPreferences"
}

extension XCUIApplication {
    static func launchedForUITest(arguments: [String] = []) -> XCUIApplication {
        let app = XCUIApplication()
        app.resetAuthorizationStatus(for: .location)
        app.launchArguments = [UITestLaunchArgument.resetPreferences] + arguments
        app.launch()
        return app
    }
}

enum LocationPrompt {
    /// The first SpringBoard query in a test session also connects the automation session, which
    /// can take longer than the alert itself.
    static func allow(timeout: TimeInterval = 30) {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let alert = springboard.alerts.firstMatch
        guard alert.waitForExistence(timeout: timeout) else { return }

        for title in ["Allow While Using App", "Allow Once", "Allow"] {
            let button = alert.buttons[title]
            if button.exists {
                button.tap()
                return
            }
        }
    }

    static func deny(timeout: TimeInterval = 30) {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let alert = springboard.alerts.firstMatch
        guard alert.waitForExistence(timeout: timeout) else { return }

        for title in ["Don't Allow", "Don\u{2019}t Allow"] {
            let button = alert.buttons[title]
            if button.exists {
                button.tap()
                return
            }
        }
    }
}

extension XCTestCase {
    /// A tap on a `Form` row's label does not move its switch, so tap the knob at the trailing
    /// edge and wait for the accessibility value to prove the state actually changed.
    func setUserLocationToggle(
        _ enabled: Bool,
        in app: XCUIApplication,
        answeringPrompt answer: () -> Void,
        timeout: TimeInterval = 25
    ) {
        let toggle = app.switches["user-location-toggle"]
        XCTAssertTrue(
            toggle.waitForExistence(timeout: timeout),
            "The user-location toggle is missing from the preferences sheet."
        )

        let wanted = enabled ? "1" : "0"
        if (toggle.value as? String) == wanted { return }

        for offset in [0.92, 0.98] {
            toggle.coordinate(withNormalizedOffset: CGVector(dx: offset, dy: 0.5)).tap()
            answer()
            if waitForValue(of: toggle, equals: wanted, timeout: 5) { return }
        }

        XCTFail(
            "The user-location toggle did not change to \(wanted); its value is \(String(describing: toggle.value))."
        )
    }

    func waitForValue(
        of element: XCUIElement,
        equals value: String,
        timeout: TimeInterval
    ) -> Bool {
        let expectation = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value == %@", value),
            object: element
        )
        return XCTWaiter().wait(for: [expectation], timeout: timeout) == .completed
    }

    func waitForNoAlerts(in app: XCUIApplication, timeout: TimeInterval = 10) {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        for query in [springboard.alerts.firstMatch, app.alerts.firstMatch] {
            let gone = XCTNSPredicateExpectation(
                predicate: NSPredicate(format: "exists == false"),
                object: query
            )
            XCTAssertEqual(
                XCTWaiter().wait(for: [gone], timeout: timeout),
                .completed,
                "A system alert is blocking the UI."
            )
        }
    }
}
