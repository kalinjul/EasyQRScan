package org.publicvalue.multiplatform.qrcode

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    formats: Int = Barcode.FORMAT_QR_CODE,
    private val scanArea: ScanArea? = null,
    private val onScanned: (String) -> Boolean
) : ImageAnalysis.Analyzer {

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
        // A ScanArea is set: restrict the frame that is analyzed to the centered sub-rect of
        // the (already viewport-aligned) crop rect, so scanning matches the visible cutout.
        val visibleRect = imageProxy.cropRect
        val subRect = Rect(
            visibleRect.left + (visibleRect.width() * (1 - scanArea.widthFraction) / 2f).toInt(),
            visibleRect.top + (visibleRect.height() * (1 - scanArea.heightFraction) / 2f).toInt(),
            visibleRect.right - (visibleRect.width() * (1 - scanArea.widthFraction) / 2f).toInt(),
            visibleRect.bottom - (visibleRect.height() * (1 - scanArea.heightFraction) / 2f).toInt(),
        )
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
