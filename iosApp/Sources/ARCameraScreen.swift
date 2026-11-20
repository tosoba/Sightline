import MetalKit
import Shared
import SwiftUI
import UIKit

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
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color.black.ignoresSafeArea()

                if let camera {
                    if let renderer = camera.renderer {
                        ARCameraMetalView(renderer: renderer)
                            .ignoresSafeArea()
                    } else {
                        messagePanel(
                            title: "Camera unavailable",
                            message: camera.errorMessage ?? "The camera renderer could not be created.",
                            showsSettings: false
                        )
                    }
                    ARCameraOverlay(placements: camera.placements)
                        .allowsHitTesting(false)
                    if let unavailability = camera.unavailability {
                        messagePanel(
                            title: unavailability.title,
                            message: unavailability.message,
                            showsSettings: unavailability.offersSettings
                        )
                    }
                    if camera.options.debugHUD {
                        hud(camera)
                    }
                    if camera.isSynthetic && camera.unavailability == nil && !camera.options.debugHUD {
                        syntheticBadge
                    }
                    if camera.pitchOutOfRange && camera.unavailability == nil {
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
            .onChange(of: scenePhase) { _, phase in
                if phase == .active {
                    camera?.resumeAfterSettings()
                }
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

    private func messagePanel(title: String, message: String, showsSettings: Bool) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "video.slash")
                .font(.system(size: 34, weight: .semibold))
            Text(title)
                .font(.headline)
            Text(message)
                .font(.subheadline)
                .multilineTextAlignment(.center)
            if showsSettings {
                Button("Open Settings", action: AppSettings.open)
                    .buttonStyle(.bordered)
                    .tint(.white)
            }
        }
        .foregroundStyle(.white)
        .padding(20)
        .frame(maxWidth: 320)
        .background(.black.opacity(0.86), in: RoundedRectangle(cornerRadius: 18))
        .padding(24)
    }

    private var syntheticBadge: some View {
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

private extension ARCameraUnavailability {
    var title: String {
        switch self {
        case .permissionDenied: "Camera access denied"
        case .configurationFailed: "Camera unavailable"
        }
    }

    var message: String {
        switch self {
        case .permissionDenied:
            "Enable camera access in Settings to see the live AR view."
        case .configurationFailed(let detail):
            detail
        }
    }

    var offersSettings: Bool {
        if case .permissionDenied = self { return true }
        return false
    }
}
