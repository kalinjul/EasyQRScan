package org.publicvalue.multiplatform.qrcode

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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

    /**
     * Size (in pixels) of the Composable container the camera preview and [ScanAreaOverlay]
     * are drawn into. Updated from `Scanner.android.kt` via `Modifier.onSizeChanged`, and
     * used together with [density] to compute the exact same cutout rect that is visualized
     * on screen, so the region we restrict scanning to matches the darkened overlay
     * pixel-for-pixel (see [analyzePartialFrame]).
     */
    @Volatile
    var containerSize: IntSize = IntSize.Zero

    /** Density used to resolve Dp values in [ScanArea] to pixels, set alongside [containerSize]. */
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
        // A ScanArea is set: restrict the frame that is analyzed to the same rect that is
        // visualized by ScanAreaOverlay. PreviewView uses FILL_CENTER (a "BoxFit.cover"-style
        // scale type) by default, so we map the cutout rect from container (widget)
        // coordinates to the analysis buffer's coordinates by replicating that same
        // cover-scaling + centering, then correcting for the buffer's sensor rotation - this
        // avoids needing a CameraX ViewPort (which previously caused a black-screen bug) while
        // still keeping the scanned region in sync with what's drawn on screen.
        val container = containerSize
        if (container.width <= 0 || container.height <= 0) {
            imageProxy.close()
            return
        }

        val cutoutRect = scanArea.cutoutRect(
            container.width.toFloat(),
            container.height.toFloat(),
            density,
        )

        val bufferRect = imageProxy.cropRect
        val rotation = ((imageProxy.imageInfo.rotationDegrees % 360) + 360) % 360
        val subRect = mapContainerRectToBufferRect(
            containerRect = cutoutRect,
            containerSize = container,
            bufferWidth = bufferRect.width(),
            bufferHeight = bufferRect.height(),
            rotationDegrees = rotation,
        ).also {
            it.offset(bufferRect.left, bufferRect.top)
        }

        if (subRect.width() <= 0 || subRect.height() <= 0) {
            imageProxy.close()
            return
        }

        imageProxy.setCropRect(subRect)

        val bitmap = runCatching { imageProxy.toBitmap() }.getOrNull()
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
    val originX = (containerSize.width - scaledWidth) / 2f
    val originY = (containerSize.height - scaledHeight) / 2f

    fun toDisplay(x: Float, y: Float): Pair<Float, Float> {
        return (x - originX) / scale to (y - originY) / scale
    }

    fun displayToBuffer(dx: Float, dy: Float): Pair<Float, Float> {
        return when (rotationDegrees) {
            90 -> dy to (displayWidth - dx)
            180 -> (displayWidth - dx) to (displayHeight - dy)
            270 -> (displayHeight - dy) to dx
            else -> dx to dy
        }
    }

    // Map all four corners (not just top-left/bottom-right) since rotation can swap axes.
    val corners = listOf(
        containerRect.left to containerRect.top,
        containerRect.right to containerRect.top,
        containerRect.left to containerRect.bottom,
        containerRect.right to containerRect.bottom,
    ).map { (x, y) ->
        val (dx, dy) = toDisplay(x, y)
        displayToBuffer(dx, dy)
    }

    val left = corners.minOf { it.first }.coerceIn(0f, bufferWidth.toFloat())
    val right = corners.maxOf { it.first }.coerceIn(0f, bufferWidth.toFloat())
    val top = corners.minOf { it.second }.coerceIn(0f, bufferHeight.toFloat())
    val bottom = corners.maxOf { it.second }.coerceIn(0f, bufferHeight.toFloat())

    return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
}
