package org.publicvalue.multiplatform.qrcode

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    formats: Int = Barcode.FORMAT_QR_CODE,
    private val scanArea: ScanArea? = null,
    private val onScanned: (String) -> Boolean
) : ImageAnalysis.Analyzer {
    @Volatile
    var containerSize: IntSize = IntSize.Zero

    @Volatile
    var density: Density = Density(1f)

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(formats)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }

        val scanWindow = scanArea?.let {
            computeScanWindow(it, imageProxy) ?: run {
                imageProxy.close()
                return
            }
        }

        scanner.process(InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes -> handleResult(barcodes, scanWindow) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun computeScanWindow(scanArea: ScanArea, imageProxy: ImageProxy): Rect? {
        if (containerSize.width <= 0 || containerSize.height <= 0) {
            return null
        }

        val cutoutRect = scanArea.cutoutRect(
            containerWidth = containerSize.width.toFloat(),
            containerHeight = containerSize.height.toFloat(),
            density = density,
        )

        val rotation = ((imageProxy.imageInfo.rotationDegrees % 360) + 360) % 360
        return mapContainerRectToBufferRect(
            containerRect = cutoutRect,
            containerSize = containerSize,
            bufferWidth = imageProxy.width,
            bufferHeight = imageProxy.height,
            rotationDegrees = rotation,
        )
    }

    private fun handleResult(barcodes: List<Barcode>?, scanWindow: Rect?) {
        barcodes?.takeIf { it.isNotEmpty() }
            ?.filter { scanWindow == null || isBarcodeInScanWindow(scanWindow, it) }
            ?.mapNotNull { it.rawValue }
            ?.forEach {
                if (onScanned(it)) {
                    scanner.close()
                }
            }
    }
}

private fun isBarcodeInScanWindow(scanWindow: Rect, barcode: Barcode): Boolean {
    val cornerPoints = barcode.cornerPoints ?: return false
    return cornerPoints.all { scanWindow.contains(it.x, it.y) }
}

internal fun mapContainerRectToBufferRect(
    containerRect: ComposeRect,
    containerSize: IntSize,
    bufferWidth: Int,
    bufferHeight: Int,
    rotationDegrees: Int,
): Rect {
    // Buffer dimensions as they appear on screen, after applying the sensor rotation.
    val displayWidth = if (rotationDegrees % 180 == 0) bufferWidth else bufferHeight
    val displayHeight = if (rotationDegrees % 180 == 0) bufferHeight else bufferWidth

    if (displayWidth <= 0 || displayHeight <= 0) {
        return Rect(0, 0, bufferWidth, bufferHeight)
    }

    // "Cover" scaling: the displayed image is scaled up uniformly until it fills the
    // container, then centered - overflow on one axis is cropped equally on both sides.
    val scale = maxOf(
        containerSize.width.toFloat() / displayWidth,
        containerSize.height.toFloat() / displayHeight,
    )
    val scaledWidth = displayWidth * scale
    val scaledHeight = displayHeight * scale
    val displayOrigin = Offset(
        x = (containerSize.width - scaledWidth) / 2f,
        y = (containerSize.height - scaledHeight) / 2f,
    )

    // Undoes the "cover" scaling+centering: a point in container (widget) coordinates maps
    // to the same physical point in the (still upright, un-rotated-back) display-space image.
    fun containerPointToDisplayPoint(point: Offset): Offset = Offset(
        x = (point.x - displayOrigin.x) / scale,
        y = (point.y - displayOrigin.y) / scale,
    )

    // Undoes the sensor rotation: a point in display-space (upright, as shown on screen) maps
    // to the corresponding point in the analysis buffer's own (un-rotated) coordinate space.
    fun displayPointToBufferPoint(point: Offset): Offset = when (rotationDegrees) {
        90 -> Offset(x = point.y, y = displayWidth - point.x)
        180 -> Offset(x = displayWidth - point.x, y = displayHeight - point.y)
        270 -> Offset(x = displayHeight - point.y, y = point.x)
        else -> point
    }

    fun containerPointToBufferPoint(point: Offset): Offset =
        displayPointToBufferPoint(containerPointToDisplayPoint(point))

    // Map all four corners (not just top-left/bottom-right) since rotation can swap axes.
    val bufferCorners = listOf(
        Offset(containerRect.left, containerRect.top),
        Offset(containerRect.right, containerRect.top),
        Offset(containerRect.left, containerRect.bottom),
        Offset(containerRect.right, containerRect.bottom),
    ).map(::containerPointToBufferPoint)

    val left = bufferCorners.minOf { it.x }.coerceIn(0f, bufferWidth.toFloat())
    val right = bufferCorners.maxOf { it.x }.coerceIn(0f, bufferWidth.toFloat())
    val top = bufferCorners.minOf { it.y }.coerceIn(0f, bufferHeight.toFloat())
    val bottom = bufferCorners.maxOf { it.y }.coerceIn(0f, bufferHeight.toFloat())

    return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
}