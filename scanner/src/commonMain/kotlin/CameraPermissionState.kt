package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable

interface CameraPermissionState {
    val status: CameraPermissionStatus
    fun requestCameraPermission()
    fun goToSettings()
}

@Composable
expect fun rememberCameraPermissionState(): CameraPermissionState

enum class CameraPermissionStatus {
    Denied, Granted
}

