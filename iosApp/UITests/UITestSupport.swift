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
    // MARK: - Places & Location sheet

    func openPlacesSheet(in app: XCUIApplication, timeout: TimeInterval = 25) {
        let trigger = app.buttons["places-trigger"]
        XCTAssertTrue(
            trigger.waitForExistence(timeout: timeout),
            "The Places & Location trigger pill is missing."
        )
        trigger.tap()
        XCTAssertTrue(
            app.buttons["places-done"].waitForExistence(timeout: timeout),
            "The Places & Location sheet did not present."
        )
    }

    func setUserLocationButton(
        _ enabled: Bool,
        in app: XCUIApplication,
        answeringPrompt answer: () -> Void,
        timeout: TimeInterval = 40
    ) {
        let button = app.buttons["use-my-location-button"]
        XCTAssertTrue(
            button.waitForExistence(timeout: 25),
            "The Use my location button is missing from the sheet."
        )

        let wanted = enabled ? "On" : "Off"
        if (button.value as? String) == wanted { return }

        button.tap()
        answer()
        XCTAssertTrue(
            waitForValue(of: button, equals: wanted, timeout: timeout),
            "Use my location did not change to \(wanted); its value is \(String(describing: button.value))."
        )
    }

    func chooseRadius(_ title: String, in app: XCUIApplication) {
        let menu = app.buttons["radius-menu"]
        XCTAssertTrue(menu.waitForExistence(timeout: 25), "The Search Radius menu is missing.")
        menu.tap()

        let option = app.buttons[title]
        XCTAssertTrue(option.waitForExistence(timeout: 10), "The \(title) radius option is missing.")
        option.tap()

        XCTAssertTrue(
            waitForLabelContains(menu, text: title, timeout: 10),
            "The Search Radius menu did not select \(title); its label is \(menu.label)."
        )
    }

    func toggleCategory(_ name: String, in app: XCUIApplication) {
        let toggle = app.buttons["category-toggle-\(name)"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 25), "The \(name) category toggle is missing.")
        XCTAssertTrue(waitForEnabled(toggle, timeout: 40), "The \(name) category toggle never enabled.")
        toggle.tap()
    }

    // MARK: - Map

    /// Waits for a positive place count, not merely a value other than the empty placeholder.
    func waitForPlaces(
        on map: XCUIElement,
        prefix: String = "Nearby places map",
        timeout: TimeInterval = 90
    ) {
        let populated = NSPredicate(
            format: "label MATCHES %@",
            "^\(prefix), [1-9][0-9]* places?$"
        )
        let expectation = XCTNSPredicateExpectation(predicate: populated, object: map)
        XCTAssertEqual(
            XCTWaiter().wait(for: [expectation], timeout: timeout),
            .completed,
            "The map never received nearby places; its label is \(map.label)."
        )
    }

    func placeCount(on map: XCUIElement, prefix: String = "Nearby places map") -> Int {
        let value = map.label.replacingOccurrences(of: "\(prefix), ", with: "")
        guard let count = value.split(separator: " ").first else { return 0 }
        return Int(count) ?? 0
    }

    func waitForCategoryPlaces(
        named name: String,
        title: String,
        in app: XCUIApplication,
        timeout: TimeInterval = 90
    ) {
        let card = app.buttons["category-card-\(name)"]
        let loaded = NSPredicate(
            format: "label MATCHES %@",
            "Open \(title), [1-9][0-9]* places?"
        )
        XCTAssertEqual(
            XCTWaiter().wait(
                for: [XCTNSPredicateExpectation(predicate: loaded, object: card)],
                timeout: timeout
            ),
            .completed,
            "The \(name) category never loaded places; its label is \(card.label)."
        )
    }

    // MARK: - Waiting

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

    func waitForEnabled(_ element: XCUIElement, timeout: TimeInterval) -> Bool {
        let expectation = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "enabled == true"),
            object: element
        )
        return XCTWaiter().wait(for: [expectation], timeout: timeout) == .completed
    }

    func waitForLabelContains(
        _ element: XCUIElement,
        text: String,
        timeout: TimeInterval
    ) -> Bool {
        let expectation = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "label CONTAINS %@", text),
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

    /// Waits for a debug label to settle on a new value, so the assertion compares the debounced
    /// camera that was actually persisted instead of a value mid-gesture.
    func stableValue(
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

    func attachScreenshot(named name: String) {
        let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshot.name = name
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
