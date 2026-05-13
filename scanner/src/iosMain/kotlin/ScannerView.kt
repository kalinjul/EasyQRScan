package org.publicvalue.multiplatform.qrcode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectType
import platform.AVFoundation.maxAvailableVideoZoomFactor
import platform.AVFoundation.minAvailableVideoZoomFactor
import platform.AVFoundation.setVideoZoomFactor
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

@Composable
@OptIn(ExperimentalForeignApi::class)
fun UiScannerView(
    modifier: Modifier = Modifier,
    // https://developer.apple.com/documentation/avfoundation/avmetadataobjecttype?language=objc
    allowedMetadataTypes: List<AVMetadataObjectType>,
    cameraPosition: CameraPosition,
    cameraZoomState: CameraZoomState?,
    onScanned: (String) -> Boolean,
    onStarted: () -> Unit,
) {
    val coordinator = remember {
        ScannerCameraCoordinator(
            onScanned = onScanned,
            cameraPosition = cameraPosition,
            onStarted = onStarted
        )
    }

    DisposableEffect(coordinator, cameraZoomState) {
        coordinator.bindZoomState(cameraZoomState)
        onDispose {
            coordinator.bindZoomState(null)
        }
    }

    LaunchedEffect(coordinator, cameraZoomState) {
        val state = cameraZoomState ?: return@LaunchedEffect
        snapshotFlow { state.zoomRatio }.collectLatest { ratio ->
            withContext(Dispatchers.Main) {
                coordinator.applyZoomRatio(ratio)
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = OrientationListener { orientation ->
            coordinator.setCurrentOrientation(orientation)
        }

        listener.register()

        onDispose {
            listener.unregister()
            coordinator.bindZoomState(null)
            coordinator.captureSession.stopRunning()
        }
    }

    UIKitView<UIView>(
        modifier = modifier.fillMaxSize(),
        factory = {
            val previewContainer = ScannerPreviewView(coordinator)
            coordinator.prepare(previewContainer.layer, allowedMetadataTypes)
            previewContainer
        },
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true,
        )
    )
}

@OptIn(ExperimentalForeignApi::class)
class ScannerPreviewView(private val coordinator: ScannerCameraCoordinator) : UIView(frame = cValue { CGRectZero }) {
    @OptIn(ExperimentalForeignApi::class)
    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)

        coordinator.setFrame(bounds)
        CATransaction.commit()
    }
}

@OptIn(ExperimentalForeignApi::class)
class ScannerCameraCoordinator(
    val onScanned: (String) -> Boolean,
    val onStarted: () -> Unit,
    val cameraPosition: CameraPosition
) : AVCaptureMetadataOutputObjectsDelegateProtocol, NSObject() {

    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    lateinit var captureSession: AVCaptureSession
    private var captureDevice: AVCaptureDevice? = null
    private var boundZoomState: CameraZoomState? = null

    fun bindZoomState(state: CameraZoomState?) {
        val previous = boundZoomState
        if (previous != state) {
            previous?.updateZoomRangeFromPlatform(null)
        }
        boundZoomState = state
        captureDevice?.let { publishDeviceZoomRange(it) }
    }

    private fun publishDeviceZoomRange(device: AVCaptureDevice) {
        val holder = boundZoomState ?: return
        val minF = device.minAvailableVideoZoomFactor.toFloat()
        val maxF = device.maxAvailableVideoZoomFactor.toFloat()
        if (maxF >= minF && minF > 0f && maxF.isFinite()) {
            holder.updateZoomRangeFromPlatform(CameraZoomRange(minZoomRatio = minF, maxZoomRatio = maxF))
        } else {
            val maxFmt = device.activeFormat.videoMaxZoomFactor.toFloat().coerceAtLeast(1f)
            holder.updateZoomRangeFromPlatform(CameraZoomRange(minZoomRatio = 1f, maxZoomRatio = maxFmt))
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun applyZoomRatio(ratio: Float) {
        val device = captureDevice ?: return
        val minZ = device.minAvailableVideoZoomFactor
        val maxZ = device.maxAvailableVideoZoomFactor
        val clamped = ratio.toDouble().coerceIn(minZ, maxZ)
        memScoped {
            val error: ObjCObjectVar<NSError?> = alloc<ObjCObjectVar<NSError?>>()
            device.lockForConfiguration(error.ptr)
            try {
                device.setVideoZoomFactor(clamped)
            } finally {
                device.unlockForConfiguration()
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun prepare(layer: CALayer, allowedMetadataTypes: List<AVMetadataObjectType>) {
        captureSession = AVCaptureSession()
        val device = AVCaptureDevice.getDevice(cameraPosition)

        if (device == null) {
            println("Device has no camera")
            return
        }

        val videoInput = memScoped {
            val error: ObjCObjectVar<NSError?> = alloc<ObjCObjectVar<NSError?>>()
            val videoInput = AVCaptureDeviceInput(device = device, error = error.ptr)
            if (error.value != null) {
                println(error.value)
                null
            } else {
                videoInput
            }
        }

        if (videoInput != null && captureSession.canAddInput(videoInput)) {
            captureSession.addInput(videoInput)
        } else {
            println("Could not add input")
            return
        }

        val metadataOutput = AVCaptureMetadataOutput()

        if (captureSession.canAddOutput(metadataOutput)) {
            captureSession.addOutput(metadataOutput)

            metadataOutput.setMetadataObjectsDelegate(this, queue = dispatch_get_main_queue())
            metadataOutput.metadataObjectTypes = allowedMetadataTypes
        } else {
            println("Could not add output")
            return
        }
        captureDevice = device

        previewLayer = AVCaptureVideoPreviewLayer(session = captureSession).also {
            it.frame = layer.bounds
            it.videoGravity = AVLayerVideoGravityResizeAspectFill
            layer.addSublayer(it)
        }

        publishDeviceZoomRange(device)

        GlobalScope.launch(Dispatchers.Default) {
            captureSession.startRunning()
            onStarted()
        }
    }

    fun setCurrentOrientation(newOrientation: UIDeviceOrientation) {
        when (newOrientation) {
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationLandscapeRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationLandscapeLeft
            UIDeviceOrientation.UIDeviceOrientationPortrait ->
                previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationPortrait
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationPortraitUpsideDown
            else ->
                previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationPortrait
        }
    }

    override fun captureOutput(
        output: platform.AVFoundation.AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: platform.AVFoundation.AVCaptureConnection
    ) {
        val metadataObject = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject
        metadataObject?.stringValue?.let { onFound(it) }
    }

    fun onFound(code: String) {
        val stopScanning = onScanned(code)
        if (stopScanning) {
            captureSession.stopRunning()
        }
    }

    fun setFrame(rect: CValue<CGRect>) {
        previewLayer?.setFrame(rect)

        setCurrentOrientation(newOrientation = UIDevice.currentDevice.orientation)
    }
}
