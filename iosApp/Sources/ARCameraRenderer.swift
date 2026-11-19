import CoreVideo
import Metal
import MetalKit
import QuartzCore
import Shared
import os
import simd

/// Layout must match `ARShaderUniforms` in `ARCameraShaders.metal` (16-byte aligned).
struct ARShaderUniforms {
    var texTransform: simd_float4x4
    var width: Float
    var height: Float
    var x: Int32
    var y: Int32
    var cornerRadius: Float
    var lod: Float
    var minLod: Float
    var contrastingColor: simd_float4
    var contrastingColorMix: Float
    var pad0: Float = 0
    var pad1: Float = 0
    var pad2: Float = 0
}

/// A marker hole in drawable pixels (top-left origin), handed from the overlay to the renderer.
struct ARHoleRect: Equatable {
    var rect: CGRect
    var cornerRadius: Float
}

/// How the stencil holes are filled. `sharp` matches the ticket's AR goal (unblurred holes);
/// `frosted` reproduces the GL draw order literally (max-lod blur plus contrasting tint inside the
/// rounded rect), which is the look shipped in `screenshots/camera_*.png`.
enum ARHoleFillStyle: String {
    case sharp
    case frosted
}

final class ARCameraRenderer: NSObject, MTKViewDelegate {
    private static let log = Logger(subsystem: "com.trm.sightline", category: "arcamera")
    private static let maxInFlightFrames = 3
    private static let blurAnimationFrames: Float = 18

    let device: MTLDevice

    private let commandQueue: MTLCommandQueue
    private let sampler: MTLSamplerState
    private let textureCache: CVMetalTextureCache?
    private let inFlight = DispatchSemaphore(value: maxInFlightFrames)

    private var noBlurPipeline: MTLRenderPipelineState!
    private var stencilMaskPipeline: MTLRenderPipelineState!
    private var cameraVerticalPipeline: MTLRenderPipelineState!
    private var verticalPipeline: MTLRenderPipelineState!
    private var horizontalPipeline: MTLRenderPipelineState!
    private var finalHorizontalPipeline: MTLRenderPipelineState!
    private var stencilDisabled: MTLDepthStencilState!
    private var stencilWrite: MTLDepthStencilState!
    private var stencilEqual: MTLDepthStencilState!

    private var intermediates: [MTLTexture] = []
    private var intermediateSize = CGSize.zero

    private let animator = ArBlurAnimator()
    private var frozenFrame: ArBlurFrame?

    private let cameraLock = NSLock()
    private var latestCameraTexture: MTLTexture?
    private var latestCameraTextureRef: CVMetalTexture?
    private var syntheticTexture: MTLTexture?
    private var lastSourceSize = CGSize.zero
    private var texTransform = matrix_identity_float4x4

    var holeFillStyle: ARHoleFillStyle = .sharp
    private var holeRects: [ARHoleRect] = []

    private(set) var lastGPUSeconds: Double = 0
    private(set) var lastCPUSeconds: Double = 0
    private(set) var currentLOD: Float = -2

    private let minLod: Float = -2
    private let maxLod: Float = 2

    init?(device: MTLDevice, colorPixelFormat: MTLPixelFormat, stencilPixelFormat: MTLPixelFormat) {
        guard let queue = device.makeCommandQueue() else { return nil }
        guard let library = device.makeDefaultLibrary() else {
            Self.log.error("Metal default library missing - is ARCameraShaders.metal in the target?")
            return nil
        }

        let samplerDescriptor = MTLSamplerDescriptor()
        samplerDescriptor.minFilter = .linear
        samplerDescriptor.magFilter = .linear
        samplerDescriptor.mipFilter = .notMipmapped
        samplerDescriptor.sAddressMode = .clampToEdge
        samplerDescriptor.tAddressMode = .clampToEdge
        guard let sampler = device.makeSamplerState(descriptor: samplerDescriptor) else { return nil }

        self.device = device
        commandQueue = queue
        self.sampler = sampler
        var cache: CVMetalTextureCache?
        CVMetalTextureCacheCreate(kCFAllocatorDefault, nil, device, nil, &cache)
        textureCache = cache

        super.init()

        do {
            try buildPipelines(library: library, colorPixelFormat: colorPixelFormat, stencilPixelFormat: stencilPixelFormat)
        } catch {
            Self.log.error("Metal pipeline creation failed: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }

    func configure(view: MTKView) {
        view.device = device
        view.delegate = self
        view.colorPixelFormat = .bgra8Unorm
        view.depthStencilPixelFormat = .stencil8
        view.clearStencil = 0
        view.clearColor = MTLClearColor(red: 0, green: 0, blue: 0, alpha: 1)
        view.framebufferOnly = true
        view.isPaused = false
        view.enableSetNeedsDisplay = false
        view.preferredFramesPerSecond = 60
    }

    // MARK: - Pipeline setup

    private struct PipelineError: LocalizedError {
        let message: String
        var errorDescription: String? { message }
    }

    private func buildPipelines(
        library: MTLLibrary,
        colorPixelFormat: MTLPixelFormat,
        stencilPixelFormat: MTLPixelFormat
    ) throws {
        func function(_ name: String) throws -> MTLFunction {
            guard let function = library.makeFunction(name: name) else {
                throw PipelineError(message: "missing shader function \(name)")
            }
            return function
        }

        func pipeline(_ fragment: String, stencil: Bool, writesColor: Bool = true) throws -> MTLRenderPipelineState {
            let descriptor = MTLRenderPipelineDescriptor()
            descriptor.vertexFunction = try function("arVertex")
            descriptor.fragmentFunction = try function(fragment)
            descriptor.colorAttachments[0].pixelFormat = colorPixelFormat
            if !writesColor {
                descriptor.colorAttachments[0].writeMask = []
            }
            if stencil {
                descriptor.stencilAttachmentPixelFormat = stencilPixelFormat
            }
            return try device.makeRenderPipelineState(descriptor: descriptor)
        }

        noBlurPipeline = try pipeline("arNoBlurFragment", stencil: true)
        stencilMaskPipeline = try pipeline("arNoBlurFragment", stencil: true, writesColor: false)
        cameraVerticalPipeline = try pipeline("arBlurVerticalCameraFragment", stencil: false)
        verticalPipeline = try pipeline("arBlurVerticalFragment", stencil: false)
        horizontalPipeline = try pipeline("arBlurHorizontalFragment", stencil: false)
        finalHorizontalPipeline = try pipeline("arBlurHorizontalFragment", stencil: true)

        let disabledDescriptor = MTLDepthStencilDescriptor()
        disabledDescriptor.isDepthWriteEnabled = false
        disabledDescriptor.depthCompareFunction = .always
        guard let disabled = device.makeDepthStencilState(descriptor: disabledDescriptor) else {
            throw PipelineError(message: "depth/stencil disabled state")
        }
        stencilDisabled = disabled

        func stencilState(compare: MTLCompareFunction, pass: MTLStencilOperation, writeMask: UInt32) throws -> MTLDepthStencilState {
            let descriptor = MTLDepthStencilDescriptor()
            descriptor.isDepthWriteEnabled = false
            descriptor.depthCompareFunction = .always
            let stencil = MTLStencilDescriptor()
            stencil.stencilCompareFunction = compare
            stencil.stencilFailureOperation = .keep
            stencil.depthFailureOperation = .keep
            stencil.depthStencilPassOperation = pass
            stencil.readMask = 0xFF
            stencil.writeMask = writeMask
            descriptor.frontFaceStencil = stencil
            descriptor.backFaceStencil = stencil
            guard let state = device.makeDepthStencilState(descriptor: descriptor) else {
                throw PipelineError(message: "stencil state \(compare.rawValue)")
            }
            return state
        }

        stencilWrite = try stencilState(compare: .always, pass: .replace, writeMask: 0xFF)
        stencilEqual = try stencilState(compare: .equal, pass: .keep, writeMask: 0x0)
    }

    // MARK: - Camera frames

    /// Called on the capture queue; wrapping happens here so the texture stays zero-copy.
    func submit(pixelBuffer: CVPixelBuffer) {
        guard let cache = textureCache else { return }
        let width = CVPixelBufferGetWidth(pixelBuffer)
        let height = CVPixelBufferGetHeight(pixelBuffer)
        var cvTexture: CVMetalTexture?
        let status = CVMetalTextureCacheCreateTextureFromImage(
            kCFAllocatorDefault,
            cache,
            pixelBuffer,
            nil,
            .bgra8Unorm,
            width,
            height,
            0,
            &cvTexture
        )
        guard status == kCVReturnSuccess, let cvTexture, let texture = CVMetalTextureGetTexture(cvTexture) else {
            return
        }
        cameraLock.lock()
        latestCameraTextureRef = cvTexture
        latestCameraTexture = texture
        cameraLock.unlock()
    }

    func setSyntheticTexture(_ texture: MTLTexture?) {
        cameraLock.lock()
        syntheticTexture = texture
        cameraLock.unlock()
    }

    private func currentSource() -> (MTLTexture, CVMetalTexture?)? {
        cameraLock.lock()
        defer { cameraLock.unlock() }
        if let texture = latestCameraTexture {
            return (texture, latestCameraTextureRef)
        }
        if let texture = syntheticTexture {
            return (texture, nil)
        }
        return nil
    }

    // MARK: - Overlay handoff

    func setHoleRects(_ rects: [ARHoleRect]) {
        holeRects = rects
    }

    func setBlurEnabled(_ enabled: Bool, animated: Bool) {
        frozenFrame = nil
        animator.setBlurEnabled(enabled: enabled, animated: animated)
    }

    func setContrastingColor(red: Float, green: Float, blue: Float) {
        animator.setContrastingColor(red: red, green: green, blue: blue)
    }

    /// Debug hook: advance the Android 18-frame ramp to `strength` and stop advancing.
    func freezeBlur(strength: Float) {
        animator.reset()
        let clamped = min(max(strength, 0), 1)
        if clamped <= 0 {
            frozenFrame = animator.nextFrame()
            return
        }
        animator.setBlurEnabled(enabled: true, animated: true)
        let targetFrame = max(Int((clamped * Self.blurAnimationFrames).rounded()), 1)
        var frame = animator.nextFrame()
        var frameIndex = 1
        while frameIndex < targetFrame {
            frame = animator.nextFrame()
            frameIndex += 1
        }
        frozenFrame = frame
    }

    private func blurFrame() -> ArBlurFrame {
        if let frozenFrame {
            return frozenFrame
        }
        return animator.nextFrame()
    }

    // MARK: - Textures

    private func makeIntermediates(for size: CGSize) {
        let width = Int(size.width)
        let height = Int(size.height)
        let dimensions = [
            (max(width / 2, 1), max(height / 2, 1)),
            (max(width / 2, 1), max(height / 2, 1)),
            (max(width / 4, 1), max(height / 4, 1)),
            (max(width / 4, 1), max(height / 4, 1)),
            (max(width / 2, 1), max(height / 2, 1)),
            (max(width / 2, 1), max(height / 2, 1)),
            (max(width, 1), max(height, 1)),
        ]
        intermediates = dimensions.map { dimension in
            let descriptor = MTLTextureDescriptor.texture2DDescriptor(
                pixelFormat: .bgra8Unorm,
                width: dimension.0,
                height: dimension.1,
                mipmapped: false
            )
            descriptor.usage = [.renderTarget, .shaderRead]
            descriptor.storageMode = .private
            return device.makeTexture(descriptor: descriptor)!
        }
        intermediateSize = size
        updateTexTransform(sourceSize: lastSourceSize)
    }

    private func updateTexTransform(sourceSize: CGSize) {
        guard sourceSize.width > 0,
              sourceSize.height > 0,
              intermediateSize.width > 0,
              intermediateSize.height > 0 else {
            texTransform = matrix_identity_float4x4
            return
        }
        // Center-crop ("aspect fill"), mirroring OpenGLRenderer.calculateSurfaceTransform but
        // expressed as a UV transform because Metal has no SurfaceTexture matrix.
        let scaleX = intermediateSize.width / sourceSize.width
        let scaleY = intermediateSize.height / sourceSize.height
        let fill = max(scaleX, scaleY)
        let uScale = Float(scaleX / fill)
        let vScale = Float(scaleY / fill)
        var scale = matrix_identity_float4x4
        scale.columns.0.x = uScale
        scale.columns.1.y = vScale
        scale.columns.3.x = (1 - uScale) / 2
        scale.columns.3.y = (1 - vScale) / 2

        // `videoRotationAngle = 90` normally delivers portrait buffers. If a device hands back the
        // sensor-native landscape buffer instead, rotate the UVs as a fallback (device-unverified).
        var rotation = matrix_identity_float4x4
        if sourceSize.width > sourceSize.height && intermediateSize.height > intermediateSize.width {
            rotation = simd_float4x4(columns: (
                SIMD4<Float>(0, 1, 0, 0),
                SIMD4<Float>(-1, 0, 0, 0),
                SIMD4<Float>(0, 0, 1, 0),
                SIMD4<Float>(0, 0, 0, 1)
            ))
            Self.log.warning("camera buffer is landscape; applying the device-pending 90 degree UV fallback")
        }
        texTransform = rotation * scale
    }

    // MARK: - Encoding helpers

    private func uniforms(
        width: Float,
        height: Float,
        x: Int32 = 0,
        y: Int32 = 0,
        cornerRadius: Float = 0,
        lod: Float,
        contrast: Bool,
        frame: ArBlurFrame
    ) -> ARShaderUniforms {
        ARShaderUniforms(
            texTransform: texTransform,
            width: width,
            height: height,
            x: x,
            y: y,
            cornerRadius: cornerRadius,
            lod: lod,
            minLod: minLod,
            contrastingColor: contrast
                ? SIMD4<Float>(frame.contrastingRed, frame.contrastingGreen, frame.contrastingBlue, 0)
                : SIMD4<Float>(-1, -1, -1, 0),
            contrastingColorMix: contrast ? frame.contrastingColorMix : 0
        )
    }

    private func encodeFullScreen(
        _ encoder: MTLRenderCommandEncoder,
        pipeline: MTLRenderPipelineState,
        source: MTLTexture,
        state: MTLDepthStencilState,
        uniforms: ARShaderUniforms
    ) {
        var uniforms = uniforms
        encoder.setRenderPipelineState(pipeline)
        encoder.setDepthStencilState(state)
        encoder.setFragmentTexture(source, index: 0)
        encoder.setFragmentSamplerState(sampler, index: 0)
        encoder.setFragmentBytes(&uniforms, length: MemoryLayout<ARShaderUniforms>.stride, index: 0)
        encoder.drawPrimitives(type: .triangle, vertexStart: 0, vertexCount: 3)
    }

    private func encodeBlurPass(
        commandBuffer: MTLCommandBuffer,
        pipeline: MTLRenderPipelineState,
        source: MTLTexture,
        target: MTLTexture,
        uniforms: ARShaderUniforms,
        label: String
    ) {
        let descriptor = MTLRenderPassDescriptor()
        descriptor.colorAttachments[0].texture = target
        descriptor.colorAttachments[0].loadAction = .dontCare
        descriptor.colorAttachments[0].storeAction = .store
        guard let encoder = commandBuffer.makeRenderCommandEncoder(descriptor: descriptor) else { return }
        encoder.label = label
        encodeFullScreen(encoder, pipeline: pipeline, source: source, state: stencilDisabled, uniforms: uniforms)
        encoder.endEncoding()
    }

    private func encodeBlurChain(
        commandBuffer: MTLCommandBuffer,
        source: MTLTexture,
        frame: ArBlurFrame,
        contrast: Bool,
        lod: Float
    ) {
        let passes: [(MTLTexture, MTLRenderPipelineState)] = [
            (intermediates[0], cameraVerticalPipeline),
            (intermediates[1], horizontalPipeline),
            (intermediates[2], verticalPipeline),
            (intermediates[3], horizontalPipeline),
            (intermediates[4], verticalPipeline),
            (intermediates[5], horizontalPipeline),
            (intermediates[6], verticalPipeline),
        ]
        var input = source
        for (index, pass) in passes.enumerated() {
            let target = pass.0
            let uniforms = uniforms(
                width: Float(target.width),
                height: Float(target.height),
                lod: lod,
                contrast: contrast,
                frame: frame
            )
            encodeBlurPass(
                commandBuffer: commandBuffer,
                pipeline: pass.1,
                source: input,
                target: target,
                uniforms: uniforms,
                label: "AR blur pass \(index + 1)"
            )
            input = target
        }
    }

    private func makeDrawableEncoder(
        _ commandBuffer: MTLCommandBuffer,
        _ descriptor: MTLRenderPassDescriptor,
        loadAction: MTLLoadAction,
        label: String
    ) -> MTLRenderCommandEncoder? {
        descriptor.colorAttachments[0].loadAction = loadAction
        descriptor.colorAttachments[0].storeAction = .store
        if descriptor.stencilAttachment.texture != nil {
            descriptor.stencilAttachment.loadAction = loadAction
            descriptor.stencilAttachment.storeAction = .store
        }
        let encoder = commandBuffer.makeRenderCommandEncoder(descriptor: descriptor)
        encoder?.label = label
        return encoder
    }

    private func setScissor(_ encoder: MTLRenderCommandEncoder, rect: CGRect, size: CGSize) {
        let minX = max(Int(rect.minX.rounded(.down)), 0)
        let minY = max(Int(rect.minY.rounded(.down)), 0)
        let maxX = min(Int(rect.maxX.rounded(.up)), Int(size.width))
        let maxY = min(Int(rect.maxY.rounded(.up)), Int(size.height))
        encoder.setScissorRect(
            MTLScissorRect(x: minX, y: minY, width: max(maxX - minX, 1), height: max(maxY - minY, 1))
        )
    }

    // MARK: - MTKViewDelegate

    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
        makeIntermediates(for: size)
    }

    func draw(in view: MTKView) {
        let start = CACurrentMediaTime()
        let size = view.drawableSize
        guard size.width > 0, size.height > 0 else { return }
        if size != intermediateSize {
            makeIntermediates(for: size)
        }
        // Research pitfall: bail out before creating any encoder when the camera has not delivered.
        guard let drawable = view.currentDrawable,
              let descriptor = view.currentRenderPassDescriptor,
              let source = currentSource(),
              intermediates.count == 7,
              let commandBuffer = commandQueue.makeCommandBuffer() else {
            return
        }
        let (sourceTexture, retainedCameraTexture) = source
        if sourceTexture.width != Int(lastSourceSize.width) || sourceTexture.height != Int(lastSourceSize.height) {
            lastSourceSize = CGSize(width: sourceTexture.width, height: sourceTexture.height)
            updateTexTransform(sourceSize: lastSourceSize)
        }

        inFlight.wait()
        commandBuffer.addCompletedHandler { [weak self] buffer in
            _ = retainedCameraTexture
            guard let self else { return }
            self.inFlight.signal()
            let gpuStart = buffer.gpuStartTime
            let gpuEnd = buffer.gpuEndTime
            if gpuEnd > gpuStart {
                self.lastGPUSeconds = gpuEnd - gpuStart
            }
            self.lastCPUSeconds = CACurrentMediaTime() - start
        }

        let frame = blurFrame()
        currentLOD = frame.lod
        let fullSize = CGRect(origin: .zero, size: size)

        if frame.shouldBlur {
            encodeBlurChain(commandBuffer: commandBuffer, source: sourceTexture, frame: frame, contrast: false, lod: frame.lod)
        }

        guard var encoder = makeDrawableEncoder(commandBuffer, descriptor, loadAction: .clear, label: "AR drawable") else {
            inFlight.signal()
            return
        }
        if frame.shouldBlur {
            let finalUniforms = uniforms(
                width: Float(size.width),
                height: Float(size.height),
                lod: frame.lod,
                contrast: false,
                frame: frame
            )
            encodeFullScreen(
                encoder,
                pipeline: finalHorizontalPipeline,
                source: intermediates[6],
                state: stencilDisabled,
                uniforms: finalUniforms
            )
        } else {
            let fullUniforms = uniforms(
                width: Float(size.width),
                height: Float(size.height),
                lod: frame.lod,
                contrast: false,
                frame: frame
            )
            encodeFullScreen(
                encoder,
                pipeline: noBlurPipeline,
                source: sourceTexture,
                state: stencilDisabled,
                uniforms: fullUniforms
            )
        }

        let visibleRects = holeRects.filter { !$0.rect.intersection(fullSize).isNull }
        if !visibleRects.isEmpty {
            encoder.setStencilReferenceValue(1)
            encoder.setScissorRect(MTLScissorRect(x: 0, y: 0, width: Int(size.width), height: Int(size.height)))
            encodeStencilMask(encoder, rects: visibleRects, source: sourceTexture, size: size, frame: frame)

            switch holeFillStyle {
            case .sharp:
                encoder.setStencilReferenceValue(1)
                encoder.setScissorRect(MTLScissorRect(x: 0, y: 0, width: Int(size.width), height: Int(size.height)))
                let fillUniforms = uniforms(
                    width: Float(size.width),
                    height: Float(size.height),
                    lod: frame.lod,
                    contrast: false,
                    frame: frame
                )
                encodeFullScreen(
                    encoder,
                    pipeline: noBlurPipeline,
                    source: sourceTexture,
                    state: stencilEqual,
                    uniforms: fillUniforms
                )
                encoder.endEncoding()
                commandBuffer.present(drawable)
                commandBuffer.commit()
                return
            case .frosted:
                encoder.endEncoding()
                encodeBlurChain(
                    commandBuffer: commandBuffer,
                    source: sourceTexture,
                    frame: frame,
                    contrast: true,
                    lod: maxLod
                )
                guard let fillEncoder = makeDrawableEncoder(
                    commandBuffer,
                    descriptor,
                    loadAction: .load,
                    label: "AR drawable frosted holes"
                ) else {
                    inFlight.signal()
                    return
                }
                fillEncoder.setStencilReferenceValue(1)
                fillEncoder.setScissorRect(MTLScissorRect(x: 0, y: 0, width: Int(size.width), height: Int(size.height)))
                let frostedUniforms = uniforms(
                    width: Float(size.width),
                    height: Float(size.height),
                    lod: maxLod,
                    contrast: true,
                    frame: frame
                )
                encodeFullScreen(
                    fillEncoder,
                    pipeline: finalHorizontalPipeline,
                    source: intermediates[6],
                    state: stencilEqual,
                    uniforms: frostedUniforms
                )
                fillEncoder.endEncoding()
                commandBuffer.present(drawable)
                commandBuffer.commit()
                return
            }
        }

        encoder.endEncoding()
        commandBuffer.present(drawable)
        commandBuffer.commit()
    }

    /// Writes stencil = 1 inside each rounded rect. Colour writes are masked off: the GL version
    /// drew a sharp (rect-squashed) image here only to lay down the stencil shape.
    private func encodeStencilMask(
        _ encoder: MTLRenderCommandEncoder,
        rects: [ARHoleRect],
        source: MTLTexture,
        size: CGSize,
        frame: ArBlurFrame
    ) {
        encoder.setDepthStencilState(stencilWrite)
        encoder.setRenderPipelineState(stencilMaskPipeline)
        encoder.setFragmentTexture(source, index: 0)
        encoder.setFragmentSamplerState(sampler, index: 0)
        for hole in rects {
            let rect = hole.rect
            var maskUniforms = uniforms(
                width: Float(rect.width),
                height: Float(rect.height),
                x: Int32(rect.minX.rounded(.down)),
                y: Int32(rect.minY.rounded(.down)),
                cornerRadius: hole.cornerRadius,
                lod: frame.lod,
                contrast: false,
                frame: frame
            )
            setScissor(encoder, rect: rect, size: size)
            encoder.setFragmentBytes(&maskUniforms, length: MemoryLayout<ARShaderUniforms>.stride, index: 0)
            encoder.drawPrimitives(type: .triangle, vertexStart: 0, vertexCount: 3)
        }
    }
}
