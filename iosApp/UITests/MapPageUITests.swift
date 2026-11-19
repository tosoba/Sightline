import XCTest

final class MapPageUITests: XCTestCase {
    func testMapPageRendersPlaces() {
        let app = XCUIApplication()
        app.launch()

        let mapButton = app.segmentedControls.buttons["Map"]
        XCTAssertTrue(mapButton.waitForExistence(timeout: 10))
        mapButton.tap()

        sleep(12)

        let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshot.name = "map-page"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
