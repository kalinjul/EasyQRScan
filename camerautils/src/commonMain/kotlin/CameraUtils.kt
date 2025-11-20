package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable

interface CameraUtils {
    fun setTorchMode(cameraPosition: CameraPosition, value: Boolean): Boolean
}

@Composable
expect fun rememberCameraUtils(): CameraUtils