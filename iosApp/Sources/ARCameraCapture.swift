import AVFoundation
import Foundation
import os

enum ARCameraCaptureFailure {
    case permissionDenied
    case configurationFailed(String)
}

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

    static var isAuthorized: Bool {
        AVCaptureDevice.authorizationStatus(for: .video) == .authorized
    }

    var onUnavailable: (@MainActor (ARCameraCaptureFailure) -> Void)?

    func start(renderer: ARCameraRenderer) {
        self.renderer = renderer
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        Self.log.info(
            "start requested: camera \(Self.isCameraAvailable ? "present" : "absent", privacy: .public), authorization \(Self.describe(status), privacy: .public)"
        )
        guard Self.isCameraAvailable else {
            Self.log.notice("no camera device; the caller should be using the synthetic feed")
            report(.configurationFailed("No camera is available on this device."))
            return
        }
        switch status {
        case .authorized:
            startSession()
        case .notDetermined:
            Self.log.info("requesting camera access")
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                guard let self else { return }
                Self.log.info("camera access \(granted ? "granted" : "denied", privacy: .public)")
                if granted {
                    self.startSession()
                } else {
                    self.report(.permissionDenied)
                }
            }
        default:
            Self.log.notice("camera access is \(Self.describe(status), privacy: .public)")
            report(.permissionDenied)
        }
    }

    func stop() {
        Self.log.info("stop requested: session \(self.session.isRunning ? "running" : "idle", privacy: .public)")
        queue.async { [session] in
            guard session.isRunning else { return }
            session.stopRunning()
            Self.log.info("capture session stopped")
        }
        renderer = nil
    }

    private func report(_ failure: ARCameraCaptureFailure) {
        let report = onUnavailable
        Task { @MainActor in report?(failure) }
    }

    private static func describe(_ status: AVAuthorizationStatus) -> String {
        switch status {
        case .authorized: "authorized"
        case .notDetermined: "not determined"
        case .denied: "denied"
        case .restricted: "restricted"
        @unknown default: "unknown"
        }
    }

    private func startSession() {
        queue.async { [weak self] in
            guard let self else { return }
            if !self.isConfigured {
                guard self.configure() else {
                    self.report(.configurationFailed("The camera session could not be configured."))
                    return
                }
                self.isConfigured = true
            }
            if !self.session.isRunning {
                Self.log.info("starting capture session")
                self.session.startRunning()
                Self.log.info("capture session running")
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
