package org.publicvalue.multiplatform.qrcode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
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
    onScanned: (String, CodeType) -> Boolean,
    onStarted: () -> Unit,
) {
    val coordinator = remember {
        ScannerCameraCoordinator(
            onScanned = onScanned,
            cameraPosition = cameraPosition,
            onStarted = onStarted
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            // stop capture
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
class ScannerPreviewView(private val coordinator: ScannerCameraCoordinator): UIView(frame = cValue { CGRectZero }) {

    private var observingRotation = false

    @OptIn(ExperimentalForeignApi::class)
    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)

        coordinator.setFrame(bounds)
        pushInterfaceOrientation()
        CATransaction.commit()
    }

    /**
     * layoutSubviews alone does not cover every rotation: a 180° flip leaves the bounds
     * unchanged. Observe the device notification as an additional trigger - the value we
     * apply is always taken from the window scene, not from the device.
     */
    override fun didMoveToWindow() {
        super.didMoveToWindow()
        val shouldObserve = window != null
        if (shouldObserve == observingRotation) return
        observingRotation = shouldObserve

        if (shouldObserve) {
            UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()
            NSNotificationCenter.defaultCenter.addObserver(
                observer = this,
                selector = NSSelectorFromString("deviceOrientationDidChange:"),
                name = UIDeviceOrientationDidChangeNotification,
                `object` = null
            )
            pushInterfaceOrientation()
        } else {
            NSNotificationCenter.defaultCenter.removeObserver(
                observer = this,
                name = UIDeviceOrientationDidChangeNotification,
                `object` = null
            )
            UIDevice.currentDevice.endGeneratingDeviceOrientationNotifications()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @ObjCAction
    @OptIn(BetaInteropApi::class)
    fun deviceOrientationDidChange(notification: NSNotification) {
        pushInterfaceOrientation()
    }

    private fun pushInterfaceOrientation() {
        coordinator.setInterfaceOrientation(
            window?.windowScene?.interfaceOrientation ?: UIInterfaceOrientationPortrait
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
class ScannerCameraCoordinator(
    val onScanned: (String, CodeType) -> Boolean,
    val onStarted: () -> Unit,
    val cameraPosition: CameraPosition,
): AVCaptureMetadataOutputObjectsDelegateProtocol, NSObject() {

    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    lateinit var captureSession: AVCaptureSession

    private var interfaceOrientation: UIInterfaceOrientation = UIInterfaceOrientationPortrait

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
        previewLayer = AVCaptureVideoPreviewLayer(session = captureSession).also {
            it.frame = layer.bounds
            it.videoGravity = AVLayerVideoGravityResizeAspectFill
            layer.addSublayer(it)
        }
        applyVideoOrientation()

        GlobalScope.launch(Dispatchers.Default) {
            captureSession.startRunning()
            onStarted()
        }
    }

    fun setInterfaceOrientation(newOrientation: UIInterfaceOrientation) {
        interfaceOrientation = newOrientation
        applyVideoOrientation()
    }

    /**
     * An AVCaptureVideoPreviewLayer is a CALayer and therefore never rotates on its own -
     * the connection has to be told explicitly. Following the interface orientation means
     * an app that locks its orientation keeps a fixed preview automatically.
     */
    private fun applyVideoOrientation() {
        val connection = previewLayer?.connection ?: return
        val videoOrientation = when (interfaceOrientation) {
            UIInterfaceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeLeft
            UIInterfaceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeRight
            UIInterfaceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
            else -> AVCaptureVideoOrientationPortrait
        }

        if (connection.videoOrientation != videoOrientation) {
            connection.videoOrientation = videoOrientation
        }
    }

    override fun captureOutput(output: platform.AVFoundation.AVCaptureOutput, didOutputMetadataObjects: List<*>, fromConnection: platform.AVFoundation.AVCaptureConnection) {
        val metadataObject = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject
        val code = metadataObject?.stringValue ?: return
        val codeType = metadataObject.type.toCodeType() ?: return
        onFound(code, codeType)
    }

    fun onFound(code: String, codeType: CodeType) {
        val stopScanning = onScanned(code, codeType)
        if (stopScanning) {
            captureSession.stopRunning()
        }
    }

    fun setFrame(rect: CValue<CGRect>) {
        previewLayer?.setFrame(rect)
    }
}
