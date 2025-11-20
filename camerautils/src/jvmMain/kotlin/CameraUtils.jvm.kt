package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCameraUtils(): CameraUtils {
    return object : CameraUtils {
        override fun setTorchMode(
            cameraPosition: CameraPosition,
            value: Boolean
        ): Boolean {
            println("CameraUtils not implemented on JVM")
            return false
        }
    }
}