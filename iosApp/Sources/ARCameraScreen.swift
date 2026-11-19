import MetalKit
import Shared
import SwiftUI

struct ARCameraMetalView: UIViewRepresentable {
    let renderer: ARCameraRenderer

    func makeUIView(context: Context) -> MTKView {
        let view = MTKView(frame: .zero, device: renderer.device)
        renderer.configure(view: view)
        return view
    }

    func updateUIView(_ uiView: MTKView, context: Context) {}
}

/// The AR camera page. It takes the shared places model (owned by `MainScreen`) so the camera does
/// not observe location a second time, and it starts capture/motion only while the page is active.
struct ARCameraScreen: View {
    let nearbyModel: NearbyPlacesModel
    let isActive: Bool

    @State private var camera: ARCameraModel?
    @Environment(\.displayScale) private var displayScale

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color.black.ignoresSafeArea()

                if let camera {
                    if let renderer = camera.renderer {
                        ARCameraMetalView(renderer: renderer)
                            .ignoresSafeArea()
                    }
                    ARCameraOverlay(placements: camera.placements)
                        .allowsHitTesting(false)
                    if camera.options.debugHUD {
                        hud(camera)
                    }
                    if camera.isSynthetic && !camera.options.debugHUD {
                        syntheticBadge(camera)
                    }
                    if camera.pitchOutOfRange {
                        holdUprightHint
                    }
                } else {
                    ProgressView("Starting camera...")
                        .tint(.white)
                        .foregroundStyle(.white)
                }
            }
            .task {
                if camera == nil {
                    let model = ARCameraModel(nearby: nearbyModel)
                    camera = model
                    model.updateViewport(proxy.size, displayScale: displayScale)
                    model.setActive(isActive)
                }
            }
            .onAppear {
                camera?.updateViewport(proxy.size, displayScale: displayScale)
                camera?.setActive(isActive)
            }
            .onDisappear {
                camera?.setActive(false)
            }
            .onChange(of: proxy.size) { _, size in
                camera?.updateViewport(size, displayScale: displayScale)
            }
            .onChange(of: isActive) { _, active in
                camera?.setActive(active)
            }
            .onChange(of: nearbyModel.places) { _, _ in
                camera?.placesChanged()
            }
            .onChange(of: nearbyModel.currentLocation) { _, _ in
                camera?.locationChanged()
            }
        }
    }

    @ViewBuilder
    private func hud(_ camera: ARCameraModel) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(camera.statusText)
            Text(camera.orientationText)
            Text(camera.layoutDebugText)
            TimelineView(.periodic(from: .now, by: 0.5)) { _ in
                if let renderer = camera.renderer {
                    Text(String(
                        format: "lod %+.2f  gpu %.2f ms  cpu %.2f ms  markers %d",
                        renderer.currentLOD,
                        renderer.lastGPUSeconds * 1_000,
                        renderer.lastCPUSeconds * 1_000,
                        camera.markerCount
                    ))
                }
            }
            HStack(spacing: 12) {
                Button(camera.blurEnabled ? "Blur on" : "Blur off") {
                    camera.toggleBlur()
                }
                Text(camera.options.holeFill.rawValue)
            }
            .buttonStyle(.bordered)
            .tint(.white)
        }
        .font(.system(size: 11, weight: .medium, design: .monospaced))
        .foregroundStyle(.white)
        .padding(10)
        .background(.black.opacity(0.45), in: RoundedRectangle(cornerRadius: 10))
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(12)
    }

    private func syntheticBadge(_ camera: ARCameraModel) -> some View {
        Text("Synthetic feed - no camera on this device")
            .font(.caption2.weight(.medium))
            .foregroundStyle(.white.opacity(0.85))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(.black.opacity(0.45), in: Capsule())
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
            .padding(12)
    }

    private var holdUprightHint: some View {
        Text("Hold the phone upright to see places")
            .font(.callout.weight(.medium))
            .foregroundStyle(.white)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(.black.opacity(0.55), in: Capsule())
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
            .padding(.bottom, 56)
    }
}
