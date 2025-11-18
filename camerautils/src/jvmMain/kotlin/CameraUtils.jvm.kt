package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCameraUtils(): CameraUtils {
    println("CameraUtils not implemented on JVM")
    return object : CameraUtils {}
}