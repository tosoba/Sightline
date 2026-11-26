import CoreGraphics
import Metal
import UIKit

/// Generates a static test pattern so the blur, animated lod, contrast mix, stencil holes and the
/// rounded-rect SDF can all be exercised on a simulator, which has no camera.
enum ARCameraSyntheticFeed {
    static func makeTexture(device: MTLDevice, size: CGSize = CGSize(width: 1080, height: 1920)) -> MTLTexture? {
        let width = Int(size.width)
        let height = Int(size.height)
        guard let context = CGContext(
            data: nil,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            // Opaque straight BGRA, matching the opaque camera feed. The blur shader no longer
            // un-premultiplies by alpha, so a premultiplied test pattern would render darker.
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue
        ) else {
            return nil
        }

        let bounds = CGRect(x: 0, y: 0, width: width, height: height)
        let sky = CGGradient(
            colorsSpace: CGColorSpaceCreateDeviceRGB(),
            colors: [
                UIColor(red: 0.10, green: 0.36, blue: 0.72, alpha: 1).cgColor,
                UIColor(red: 0.62, green: 0.83, blue: 0.95, alpha: 1).cgColor,
            ] as CFArray,
            locations: [0, 1]
        )
        if let sky {
            context.drawLinearGradient(sky, start: .zero, end: CGPoint(x: 0, y: bounds.height * 0.7), options: [])
        }

        let ground = CGRect(x: 0, y: 0, width: bounds.width, height: bounds.height * 0.32)
        context.setFillColor(UIColor(red: 0.16, green: 0.32, blue: 0.16, alpha: 1).cgColor)
        context.fill(ground)

        // Saturated checker cells make the Gaussian blur strength obvious.
        let colors: [UIColor] = [
            UIColor(red: 0.95, green: 0.35, blue: 0.25, alpha: 0.85),
            UIColor(red: 0.98, green: 0.82, blue: 0.20, alpha: 0.85),
            UIColor(red: 0.20, green: 0.75, blue: 0.55, alpha: 0.85),
            UIColor(red: 0.90, green: 0.40, blue: 0.80, alpha: 0.85),
        ]
        let cell = 112
        for row in 0 ..< (height / cell + 1) {
            for column in 0 ..< (width / cell + 1) {
                guard (row + column) % 3 != 0 else { continue }
                context.setFillColor(colors[(row * 3 + column) % colors.count].cgColor)
                context.fill(CGRect(x: column * cell, y: row * cell, width: cell, height: cell))
            }
        }

        // A coarse grid and hard-edged rings so blur and the rounded hole corners are both visible.
        context.setStrokeColor(UIColor.white.withAlphaComponent(0.95).cgColor)
        context.setLineWidth(6)
        for x in stride(from: 0, to: width, by: 186) {
            context.move(to: CGPoint(x: x, y: 0))
            context.addLine(to: CGPoint(x: x, y: height))
        }
        for y in stride(from: 0, to: height, by: 186) {
            context.move(to: CGPoint(x: 0, y: y))
            context.addLine(to: CGPoint(x: width, y: y))
        }
        context.strokePath()

        context.setLineWidth(18)
        let center = CGPoint(x: bounds.midX, y: bounds.midY)
        for radius in stride(from: 150, through: 750, by: 150) {
            context.setStrokeColor(UIColor.white.withAlphaComponent(0.85).cgColor)
            context.strokeEllipse(in: CGRect(
                x: center.x - CGFloat(radius),
                y: center.y - CGFloat(radius),
                width: CGFloat(radius) * 2,
                height: CGFloat(radius) * 2
            ))
        }

        context.setFillColor(UIColor.black.withAlphaComponent(0.85).cgColor)
        context.fill(CGRect(x: 0, y: bounds.midY - 10, width: bounds.width, height: 20))

        guard let data = context.data else { return nil }
        let descriptor = MTLTextureDescriptor.texture2DDescriptor(
            pixelFormat: .bgra8Unorm,
            width: width,
            height: height,
            mipmapped: false
        )
        descriptor.usage = .shaderRead
        descriptor.storageMode = .shared
        guard let texture = device.makeTexture(descriptor: descriptor) else { return nil }
        texture.replace(
            region: MTLRegionMake2D(0, 0, width, height),
            mipmapLevel: 0,
            withBytes: data,
            bytesPerRow: width * 4
        )
        return texture
    }
}
