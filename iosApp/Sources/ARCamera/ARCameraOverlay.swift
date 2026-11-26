import Shared
import SwiftUI

/// Draws the marker cards. The cards use the same rects the model hands to the Metal renderer for
/// stencil holes, so the sharp hole and the card border always line up.
///
/// Content follows Android's `ARMarkerRenderer`: a square category icon box on the left, then the
/// place name (up to two lines) with the distance underneath. No category label, matching Android.
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

    private enum Metrics {
        static let padding: CGFloat = 12
        static let minIconBoxSide: CGFloat = 48
        static let iconFillFraction: CGFloat = 0.6
        static let nameDistanceGap: CGFloat = 8
        static let maxNameLines = 2
        static let unbounded = CGSize(width: 10_000, height: 10_000)
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

    private static func iconBoxSide(for rect: CGRect) -> CGFloat {
        max(rect.height - Metrics.padding * 2, Metrics.minIconBoxSide)
    }

    private func drawIcon(_ placement: ARPlacement, in context: inout GraphicsContext, rect: CGRect) {
        let boxSide = Self.iconBoxSide(for: rect)
        let box = CGRect(
            x: rect.minX + Metrics.padding,
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
        image.shading = .color(.white.opacity(0.9))
        let glyphInset = boxSide * (1 - Metrics.iconFillFraction) / 2
        context.draw(image, in: box.insetBy(dx: glyphInset, dy: glyphInset))
    }

    private func drawText(_ placement: ARPlacement, in context: inout GraphicsContext, rect: CGRect) {
        let left = rect.minX + Metrics.padding
            + Self.iconBoxSide(for: rect)
            + Metrics.padding / 2
        let width = max(rect.maxX - Metrics.padding - left, 1)
        let top = rect.minY + Metrics.padding
        let availableHeight = rect.height - Metrics.padding * 2

        let name = context.resolve(
            Text(placement.name)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(.white)
        )
        let intrinsicName = name.measure(in: Metrics.unbounded)
        let singleLineHeight = max(intrinsicName.height, 1)
        let nameLines = intrinsicName.width > width
            && singleLineHeight * 2 <= availableHeight
            ? Metrics.maxNameLines
            : 1
        let nameHeight = singleLineHeight * CGFloat(nameLines)
        context.draw(
            name,
            in: CGRect(x: left, y: top, width: width, height: nameHeight)
        )

        let distance = context.resolve(
            Text(formatMarkerDistance(meters: placement.distanceMeters))
                .font(.system(size: 12))
                .foregroundStyle(.white.opacity(0.75))
        )
        let distanceHeight = max(
            distance.measure(in: CGSize(width: width, height: Metrics.unbounded.height)).height,
            1
        )
        let distanceTop = top + nameHeight + Metrics.nameDistanceGap
        guard distanceTop + distanceHeight <= rect.maxY - Metrics.padding else { return }
        context.draw(
            distance,
            in: CGRect(x: left, y: distanceTop, width: width, height: distanceHeight)
        )
    }
}
