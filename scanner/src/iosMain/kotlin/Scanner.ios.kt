package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString


@Composable
actual fun Scanner(
    modifier: Modifier,
    onScanned: (String) -> Boolean, // return true to abort scanning
    types: List<CodeType>
) {
    UiScannerView(
        modifier = modifier,
        onScanned = {
            onScanned(it)
        }
    )
}

@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {
    return remember {
        IosMutableCameraPermissionState()
    }
}

abstract class MutableCameraPermissionState: CameraPermissionState {
    override var status: CameraPermissionStatus by mutableStateOf(getCameraPermissionStatus())

}

class IosMutableCameraPermissionState: MutableCameraPermissionState() {
    override fun requestCameraPermission() {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) {
            this.status = getCameraPermissionStatus()
        }
    }

    override fun goToSettings() {
        val appSettingsUrl = NSURL(string = UIApplicationOpenSettingsURLString)
        if (UIApplication.sharedApplication.canOpenURL(appSettingsUrl)) {
            UIApplication.sharedApplication.openURL(appSettingsUrl)
        }
    }
}

fun getCameraPermissionStatus(): CameraPermissionStatus {
    val authorizationStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
    return if (authorizationStatus == AVAuthorizationStatusAuthorized) CameraPermissionStatus.Granted else CameraPermissionStatus.Denied
}