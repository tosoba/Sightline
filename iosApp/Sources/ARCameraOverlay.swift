import Shared
import SwiftUI

/// Draws the marker cards. The cards use the same rects the model hands to the Metal renderer for
/// stencil holes, so the sharp hole and the card border always line up.
struct ARCameraOverlay: View {
    let placements: [ARPlacement]

    var body: some View {
        Canvas { context, _ in
            for placement in placements {
                draw(placement, in: &context)
            }
        }
        .accessibilityHidden(true)
    }

    private func draw(_ placement: ARPlacement, in context: inout GraphicsContext) {
        let rect = placement.rect
        let card = Path(roundedRect: rect, cornerRadius: placement.cornerRadius)

        context.fill(card, with: .color(.black.opacity(0.26)))
        context.stroke(card, with: .color(.white.opacity(0.24)), lineWidth: 2)

        context.drawLayer { layer in
            layer.clip(to: card)
            drawIcon(placement, in: &layer, rect: rect)
            drawText(placement, in: &layer, rect: rect)
        }
    }

    private static let iconInset: CGFloat = 12
    private static let iconBoxMax: CGFloat = 52

    private static func iconBoxSide(for rect: CGRect) -> CGFloat {
        min(rect.height - iconInset * 2, iconBoxMax)
    }

    private func drawIcon(_ placement: ARPlacement, in context: inout GraphicsContext, rect: CGRect) {
        let boxSide = Self.iconBoxSide(for: rect)
        let box = CGRect(
            x: rect.minX + Self.iconInset,
            y: rect.midY - boxSide / 2,
            width: boxSide,
            height: boxSide
        )
        context.fill(
            Path(roundedRect: box, cornerRadius: placement.cornerRadius),
            with: .color(.white.opacity(0.18))
        )

        var image = context.resolve(
            Image(systemName: placement.category.symbolName)
                .symbolRenderingMode(.monochrome)
        )
        image.shading = .color(.white.opacity(0.92))
        let iconInset = boxSide * 0.2
        context.draw(image, in: box.insetBy(dx: iconInset, dy: iconInset))
    }

    private func drawText(_ placement: ARPlacement, in context: inout GraphicsContext, rect: CGRect) {
        let left = rect.minX + Self.iconInset + Self.iconBoxSide(for: rect) + 10
        let width = max(rect.maxX - 12 - left, 10)

        let name = context.resolve(
            Text(placement.name)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
        )
        context.draw(name, in: CGRect(x: left, y: rect.minY + 18, width: width, height: 18))

        let detail = context.resolve(
            Text("\(placement.category.title) · \(Self.distanceText(placement.distanceMeters))")
                .font(.system(size: 11))
                .foregroundStyle(.white.opacity(0.78))
        )
        context.draw(detail, in: CGRect(x: left, y: rect.minY + 39, width: width, height: 15))
    }

    private static func distanceText(_ meters: Float) -> String {
        if meters >= 1_000 {
            return String(format: "%.1f km", meters / 1_000)
        }
        return String(format: "%.0f m", meters)
    }
}
