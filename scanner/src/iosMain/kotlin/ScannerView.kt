package org.publicvalue.multiplatform.qrcode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
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
import kotlinx.cinterop.useContents
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
import platform.CoreGraphics.CGRectMake
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
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
@OptIn(ExperimentalForeignApi::class)
fun UiScannerView(
    modifier: Modifier = Modifier,
    // https://developer.apple.com/documentation/avfoundation/avmetadataobjecttype?language=objc
    allowedMetadataTypes: List<AVMetadataObjectType>,
    cameraPosition: CameraPosition,
    onScanned: (String) -> Boolean,
    onStarted: () -> Unit,
    scanArea: ScanArea? = null,
) {
    val coordinator = remember(scanArea) {
        ScannerCameraCoordinator(
            onScanned = onScanned,
            cameraPosition = cameraPosition,
            onStarted = onStarted,
            scanArea = scanArea,
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
    val onScanned: (String) -> Boolean,
    val onStarted: () -> Unit,
    val cameraPosition: CameraPosition,
    val scanArea: ScanArea? = null,
): AVCaptureMetadataOutputObjectsDelegateProtocol, NSObject() {

    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var metadataOutput: AVCaptureMetadataOutput? = null
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
            this.metadataOutput = metadataOutput
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
        updateRectOfInterest()

        GlobalScope.launch(Dispatchers.Default) {
            captureSession.startRunning()
            // AVCaptureMetadataOutput.rectOfInterest set before the session (and its
            // connections) are fully live is unreliable - some devices silently reset it back
            // to the full frame once the capture connection is actually established. Re-apply
            // it now that startRunning() (a blocking call) has returned, guaranteeing the
            // connection exists.
            dispatch_async(dispatch_get_main_queue()) {
                updateRectOfInterest()
            }
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
        updateRectOfInterest()
    }

    /**
     * Restricts hardware barcode detection to [scanArea], if set, by mapping its
     * [ScanArea.cutoutRect] (in preview layer coordinates) to the metadata output's
     * coordinate space via [AVCaptureVideoPreviewLayer.metadataOutputRectOfInterestForRect].
     * Without a [scanArea] the whole frame ({{0,0},{1,1}}) remains eligible for detection.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun updateRectOfInterest() {
        val output = metadataOutput ?: return
        val layer = previewLayer ?: return
        val area = scanArea

        if (area == null) {
            output.rectOfInterest = CGRectMake(0.0, 0.0, 1.0, 1.0)
            return
        }

        val bounds = layer.bounds.useContents { this }
        if (bounds.size.width <= 0.0 || bounds.size.height <= 0.0) {
            return
        }

        val rect = area.cutoutRect(
            containerWidth = bounds.size.width.toFloat(),
            containerHeight =bounds.size.height.toFloat(),
            density = Density(1f), // iOS UIKit is always 1:1, no density scaling like on Android
        )

        val cutoutRect = CGRectMake(
            rect.left.toDouble(),
            rect.top.toDouble(),
            rect.width.toDouble(),
            rect.height.toDouble(),
        )

        output.rectOfInterest = layer.metadataOutputRectOfInterestForRect(rectInLayerCoordinates = cutoutRect)
    }
}
