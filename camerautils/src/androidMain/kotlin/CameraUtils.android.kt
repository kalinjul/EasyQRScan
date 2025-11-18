package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberCameraUtils(): CameraUtils {
    val context = LocalContext.current
    return remember(context) {
        AndroidCameraUtils(context)
    }
}