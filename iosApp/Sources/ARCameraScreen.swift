import MetalKit
import Shared
import SwiftUI
import UIKit

struct ARCameraMetalView: UIViewRepresentable {
    let renderer: ARCameraRenderer
    var isPaused: Bool = false

    func makeUIView(context: Context) -> MTKView {
        let view = MTKView(frame: .zero, device: renderer.device)
        renderer.configure(view: view)
        // The camera preview is inert; letting taps reach SwiftUI keeps the overlay toggle working.
        view.isUserInteractionEnabled = false
        return view
    }

    func updateUIView(_ uiView: MTKView, context: Context) {
        uiView.isPaused = isPaused
    }
}

/// The AR camera page. It takes the shared places model (owned by `MainScreen`) so the camera does
/// not observe location a second time, and it starts capture/motion only while the page is active.
struct ARCameraScreen: View {
    let nearbyModel: NearbyPlacesModel
    let isActive: Bool
    let toolbarsVisible: Bool
    let sheetObscuresCamera: Bool
    let onCanvasTap: () -> Void

    @State private var camera: ARCameraModel?
    @Environment(\.displayScale) private var displayScale
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color.black.ignoresSafeArea()

                if let camera {
                    if let renderer = camera.renderer {
                        ARCameraMetalView(renderer: renderer, isPaused: !camera.isRunning)
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
                    if camera.unavailability == nil && !sheetObscuresCamera {
                        Color.clear
                            .contentShape(Rectangle())
                            .onTapGesture(perform: onCanvasTap)
                    }
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
                    if toolbarsVisible && !camera.blurEnabled && camera.totalPages > 1 {
                        ARMarkerPagePicker(
                            currentPage: camera.currentPage,
                            totalPages: camera.totalPages,
                            onSelect: camera.selectPage
                        )
                        .transition(.opacity)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                        .padding(12)
                    }
                } else {
                    ProgressView("Starting camera...")
                        .tint(.white)
                        .foregroundStyle(.white)
                }
            }
            .animation(.easeInOut(duration: 0.2), value: toolbarsVisible)
            .task {
                if camera == nil {
                    let model = ARCameraModel(nearby: nearbyModel)
                    camera = model
                    model.updateContrastingColor(for: colorScheme)
                    model.sheetObscuresCamera = sheetObscuresCamera
                    syncViewport(model, size: proxy.size, insets: proxy.safeAreaInsets, proxy: proxy)
                    model.setActive(isActive)
                }
            }
            .onAppear {
                camera?.updateContrastingColor(for: colorScheme)
                if let camera {
                    syncViewport(camera, size: proxy.size, insets: proxy.safeAreaInsets, proxy: proxy)
                }
                camera?.setActive(isActive)
            }
            .onDisappear {
                camera?.setActive(false)
            }
            .onChange(of: proxy.size) { _, size in
                if let camera {
                    syncViewport(camera, size: size, insets: proxy.safeAreaInsets, proxy: proxy)
                }
            }
            .onChange(of: proxy.safeAreaInsets) { _, insets in
                if let camera {
                    syncViewport(camera, size: proxy.size, insets: insets, proxy: proxy)
                }
            }
            .onChange(of: isActive) { _, active in
                camera?.setActive(active)
            }
            .onChange(of: sheetObscuresCamera) { _, obscured in
                camera?.sheetObscuresCamera = obscured
            }
            .onChange(of: colorScheme) { _, scheme in
                camera?.updateContrastingColor(for: scheme)
            }
            .onChange(of: scenePhase) { _, phase in
                switch phase {
                case .active:
                    camera?.resumeAfterSettings()
                    if isActive { camera?.setActive(true) }
                case .background:
                    camera?.setActive(false)
                default:
                    break
                }
            }
            .onChange(of: nearbyModel.allPlaces) { _, _ in
                camera?.placesChanged()
            }
            .onChange(of: nearbyModel.activeLocation) { _, _ in
                camera?.locationChanged()
            }
        }
    }

    private func syncViewport(
        _ camera: ARCameraModel,
        size: CGSize,
        insets: EdgeInsets,
        proxy: GeometryProxy
    ) {
        // The Metal view ignores the safe area, so its drawable origin sits at the window corner
        // while the overlay and marker layout live in the inset viewport. Hand the difference over
        // so stencil holes can be moved into the drawable's space.
        camera.updateViewport(
            size,
            displayScale: displayScale,
            safeAreaInsets: insets,
            overlayOrigin: proxy.frame(in: .global).origin
        )
    }

    @ViewBuilder
    private func hud(_ camera: ARCameraModel) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(camera.statusText)
            Text(camera.orientationText)
            Text(camera.layoutDebugText)
                .accessibilityIdentifier("ar-layout-debug")
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
                Text(camera.blurEnabled ? "blur on" : "blur off")
                Button(camera.debugBlurForced ? "Force blur on" : "Force blur off") {
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
            .allowsHitTesting(false)
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
            .allowsHitTesting(false)
    }
}

/// Floating page picker for overflowing Marker Slots. Mirrors Android's `CameraPreviewPagePicker`:
/// at most three 1-based page numbers at a time, sliding with the current page.
private struct ARMarkerPagePicker: View {
    let currentPage: Int
    let totalPages: Int
    let onSelect: (Int) -> Void

    private var pageWindow: [Int] {
        MarkerPageWindow.shared
            .compute(
                currentPage: Int32(currentPage),
                totalPages: Int32(totalPages),
                maxVisiblePages: 3
            )
            .map { Int(truncating: $0) }
    }

    var body: some View {
        VStack(spacing: 2) {
            ForEach(pageWindow, id: \.self) { page in
                Button {
                    onSelect(page)
                } label: {
                    Text("\(page + 1)")
                        .font(.subheadline.weight(.semibold))
                        .frame(width: 36, height: 32)
                        .foregroundStyle(page == currentPage ? Color.white : Color.primary)
                        .background(
                            Circle().fill(page == currentPage ? Color.accentColor : Color.clear)
                        )
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("ar-page-\(page + 1)")
                .accessibilityLabel("Page \(page + 1)")
                .accessibilityAddTraits(page == currentPage ? .isSelected : [])
            }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 6)
        .background(.regularMaterial, in: Capsule())
        .overlay(Capsule().stroke(.white.opacity(0.18), lineWidth: 1))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("ar-page-picker")
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
