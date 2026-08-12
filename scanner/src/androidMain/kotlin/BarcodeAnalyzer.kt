package org.publicvalue.multiplatform.qrcode

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    formats: Int = Barcode.FORMAT_QR_CODE,
    private val onScanned: (String, CodeType) -> Boolean
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(formats)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            scanner.process(
                InputImage.fromMediaImage(
                    image, imageProxy.imageInfo.rotationDegrees
                )
            ).addOnSuccessListener { barcode ->
                barcode?.takeIf { it.isNotEmpty() }
                    ?.mapNotNull { scannedCode ->
                        val rawValue = scannedCode.rawValue ?: return@mapNotNull null
                        val codeType = scannedCode.format.toCodeType() ?: return@mapNotNull null
                        rawValue to codeType
                    }
                    ?.forEach {
                        if (onScanned(it.first, it.second)) {
                            scanner.close()
                        }
                    }
            }.addOnCompleteListener {
                imageProxy.close()
            }
        }
    }
}
