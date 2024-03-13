package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {
    return object: CameraPermissionState {
        override val status: CameraPermissionStatus
            get() = CameraPermissionStatus.Denied

        override fun requestCameraPermission() {
            println("requestCameraPermission() not implemented on JVM")
        }

        override fun goToSettings() {
            println("goToSettings() not implemented on JVM")
        }

    }
}