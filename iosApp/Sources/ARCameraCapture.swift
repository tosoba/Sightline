import AVFoundation
import Foundation
import os

/// Owns the `AVCaptureSession` that feeds `ARCameraRenderer`. Nothing runs until `start` is called
/// and `start` is only called while the camera page is visible.
final class ARCameraCapture: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    private static let log = Logger(subsystem: "com.trm.sightline", category: "arcamera.capture")

    private let session = AVCaptureSession()
    private let queue = DispatchQueue(label: "com.trm.sightline.arcamera.capture")
    private weak var renderer: ARCameraRenderer?
    private var isConfigured = false

    static var isCameraAvailable: Bool {
        AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) != nil
    }

    var onUnavailable: (@MainActor (String) -> Void)?

    func start(renderer: ARCameraRenderer) {
        self.renderer = renderer
        guard Self.isCameraAvailable else {
            Task { @MainActor [onUnavailable] in onUnavailable?("No camera on this device") }
            return
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            startSession()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                guard let self else { return }
                if granted {
                    self.startSession()
                } else {
                    Task { @MainActor [onUnavailable] in onUnavailable?("Camera access denied") }
                }
            }
        default:
            Task { @MainActor [onUnavailable] in onUnavailable?("Camera access denied") }
        }
    }

    func stop() {
        queue.async { [session] in
            if session.isRunning {
                session.stopRunning()
            }
        }
        renderer = nil
    }

    private func startSession() {
        queue.async { [weak self] in
            guard let self else { return }
            if !self.isConfigured {
                guard self.configure() else { return }
                self.isConfigured = true
            }
            if !self.session.isRunning {
                self.session.startRunning()
            }
        }
    }

    private func configure() -> Bool {
        guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: camera) else {
            Self.log.error("camera input unavailable")
            return false
        }

        let output = AVCaptureVideoDataOutput()
        output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        output.alwaysDiscardsLateVideoFrames = true
        output.setSampleBufferDelegate(self, queue: queue)

        session.beginConfiguration()
        session.sessionPreset = .hd1280x720
        guard session.canAddInput(input), session.canAddOutput(output) else {
            session.commitConfiguration()
            Self.log.error("capture session rejected camera input/output")
            return false
        }
        session.addInput(input)
        session.addOutput(output)
        if let connection = output.connection(with: .video) {
            connection.isVideoMirrored = false
            // Portrait-only app: ask AVFoundation for upright buffers instead of rotating in the
            // shader. The renderer keeps a landscape fallback for devices that ignore this.
            if connection.isVideoRotationAngleSupported(90) {
                connection.videoRotationAngle = 90
            }
        }
        session.commitConfiguration()
        return true
    }

    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        renderer?.submit(pixelBuffer: pixelBuffer)
    }
}
