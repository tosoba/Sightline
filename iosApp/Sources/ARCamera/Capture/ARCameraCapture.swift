import AVFoundation
import Foundation
import os

/// Why the page cannot show a live feed. Kept apart from the synthetic feed: a genuine no-camera
/// device still renders the generated texture, while these states show a message and keep the real
/// nearby places instead of the synthetic fixture.
enum ARCameraUnavailability: Equatable {
    case permissionDenied
    case configurationFailed(String)
}

/// Owns the `AVCaptureSession` that feeds `ARCameraRenderer`. Nothing runs until `start` is called
/// and `start` is only called while the camera page is visible.
final class ARCameraCapture: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    private static let log = Logger(subsystem: "com.trm.sightline", category: "arcamera.capture")

    private let session = AVCaptureSession()
    private let sessionQueue = DispatchQueue(label: "com.trm.sightline.arcamera.session")
    private let videoQueue = DispatchQueue(label: "com.trm.sightline.arcamera.video")
    private weak var renderer: ARCameraRenderer?
    private var isConfigured = false
    private var videoConnection: AVCaptureConnection?
    /// Touched on `sessionQueue` only; the default matches `UIInterfaceOrientation.portrait`.
    private var requestedVideoRotationAngle: CGFloat = 90

    deinit {
        // Safety net: if the object is deallocated without an explicit stop (e.g. app termination),
        // ensure the hardware is released so other apps can use the camera.
        if session.isRunning {
            session.stopRunning()
        }
    }

    static var isCameraAvailable: Bool {
        AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) != nil
    }

    static var isAuthorized: Bool {
        AVCaptureDevice.authorizationStatus(for: .video) == .authorized
    }

    var onUnavailable: (@MainActor (ARCameraUnavailability) -> Void)?

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

    /// Keeps the delivered buffers aligned with the interface orientation. Safe to call before the
    /// session is configured: the angle is stored and applied once the output exists.
    func setVideoRotationAngle(_ angle: CGFloat) {
        sessionQueue.async { [weak self] in
            guard let self else { return }
            self.requestedVideoRotationAngle = angle
            self.applyVideoRotationAngle()
        }
    }

    private func applyVideoRotationAngle() {
        guard let connection = videoConnection else { return }
        guard connection.isVideoRotationAngleSupported(requestedVideoRotationAngle) else {
            Self.log.notice(
                "video rotation angle \(self.requestedVideoRotationAngle, privacy: .public) is unsupported"
            )
            return
        }
        guard connection.videoRotationAngle != requestedVideoRotationAngle else { return }
        session.beginConfiguration()
        connection.videoRotationAngle = requestedVideoRotationAngle
        session.commitConfiguration()
    }

    func stop() {
        Self.log.info("stop requested: session \(self.session.isRunning ? "running" : "idle", privacy: .public)")
        // Stop synchronously on the session queue so the camera hardware is fully released before
        // this method returns. This is critical on app background / termination: an async dispatch
        // may never execute, leaving the AVCaptureSession attached to the camera device and
        // corrupting the hardware pipeline device-wide (black / green frames in other apps).
        sessionQueue.sync { [session] in
            guard session.isRunning else { return }
            session.stopRunning()
            Self.log.info("capture session stopped")
        }
        renderer = nil
    }

    private func report(_ failure: ARCameraUnavailability) {
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
        sessionQueue.async { [weak self] in
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
              let input = try? AVCaptureDeviceInput(device: camera)
        else {
            Self.log.error("camera input unavailable")
            return false
        }

        let output = AVCaptureVideoDataOutput()
        output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        output.alwaysDiscardsLateVideoFrames = true
        output.setSampleBufferDelegate(self, queue: videoQueue)

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
            videoConnection = connection
            // Ask AVFoundation for upright buffers instead of rotating in the shader. The renderer
            // keeps a UV fallback for devices that ignore this.
            if connection.isVideoRotationAngleSupported(requestedVideoRotationAngle) {
                connection.videoRotationAngle = requestedVideoRotationAngle
            }
        }
        session.commitConfiguration()
        return true
    }

    func captureOutput(
        _: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from _: AVCaptureConnection
    ) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        renderer?.submit(pixelBuffer: pixelBuffer)
    }
}
