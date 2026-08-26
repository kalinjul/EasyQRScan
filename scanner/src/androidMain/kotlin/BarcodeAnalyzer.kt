package org.publicvalue.multiplatform.qrcode

import android.annotation.SuppressLint
import android.graphics.Bitmap
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
    private fun analyzeFullFrame(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }

        processScanResults(
            image = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees),
            imageProxy = imageProxy
        )
    }

    private fun analyzePartialFrame(scanArea: ScanArea, imageProxy: ImageProxy) {
        if (containerSize.width <= 0 || containerSize.height <= 0) {
            imageProxy.close()
            return
        }

        val cutoutRect = scanArea.cutoutRect(
            containerWidth = containerSize.width.toFloat(),
            containerHeight = containerSize.height.toFloat(),
            density = density,
        )

        val bufferWidth = imageProxy.width
        val bufferHeight = imageProxy.height
        val rotation = ((imageProxy.imageInfo.rotationDegrees % 360) + 360) % 360
        val subRect = mapContainerRectToBufferRect(
            containerRect = cutoutRect,
            containerSize = containerSize,
            bufferWidth = bufferWidth,
            bufferHeight = bufferHeight,
            rotationDegrees = rotation,
        )

        if (subRect.width() <= 0 || subRect.height() <= 0) {
            imageProxy.close()
            return
        }

        val fullBitmap = runCatching { imageProxy.toBitmap() }.getOrNull()
        if (fullBitmap == null) {
            imageProxy.close()
            return
        }

        val safeRect = Rect(
            subRect.left.coerceIn(0, fullBitmap.width),
            subRect.top.coerceIn(0, fullBitmap.height),
            subRect.right.coerceIn(0, fullBitmap.width),
            subRect.bottom.coerceIn(0, fullBitmap.height),
        )

        if (safeRect.width() <= 0 || safeRect.height() <= 0) {
            imageProxy.close()
            return
        }

        val bitmap = runCatching {
            Bitmap.createBitmap(
                fullBitmap,
                safeRect.left,
                safeRect.top,
                safeRect.width(),
                safeRect.height()
            )
        }.getOrNull()

        if (bitmap == null) {
            imageProxy.close()
            return
        }

        processScanResults(
            image = InputImage.fromBitmap(bitmap, imageProxy.imageInfo.rotationDegrees),
            imageProxy = imageProxy
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (scanArea != null) {
            return analyzePartialFrame(scanArea, imageProxy)
        }

        return analyzeFullFrame(imageProxy)
    }

    private fun processScanResults(image: InputImage, imageProxy: ImageProxy) {
        scanner.process(image)
            .addOnSuccessListener { barcodes -> handleResult(barcodes) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun handleResult(barcodes: List<Barcode>?) {
        barcodes?.takeIf { it.isNotEmpty() }
            ?.mapNotNull { it.rawValue }
            ?.forEach {
                if (onScanned(it)) {
                    scanner.close()
                }
            }
    }
}

/**
 * Maps [containerRect] (in pixel coordinates of a widget of size [containerSize], e.g. the
 * `ScanAreaOverlay`'s Composable Box) to the corresponding rect in an analysis buffer of size
 * [bufferWidth] x [bufferHeight], assuming the widget displays that buffer with a
 * "BoxFit.cover"-style scale type (matching `PreviewView`'s default `FILL_CENTER`), and that
 * the buffer must be rotated clockwise by [rotationDegrees] (as reported by
 * `ImageProxy.imageInfo.rotationDegrees`) to appear upright the way it's shown on screen.
 *
 * This lets us restrict ML Kit scanning to exactly the region that's visually darkened by
 * [ScanAreaOverlay], without relying on a shared CameraX `ViewPort` between `Preview` and
 * `ImageAnalysis`.
 */
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