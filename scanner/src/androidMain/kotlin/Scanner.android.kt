package org.publicvalue.multiplatform.qrcode

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@Composable
actual fun Scanner(
    modifier: Modifier,
    onScanned: (String) -> Boolean,
    types: List<CodeType>
) {
    val analyzer = remember() {
        BarcodeAnalyzer(types.toFormat(), onScanned)
    }
    CameraView(modifier, analyzer)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {
    val accPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val context = LocalContext.current
    val wrapper = remember(accPermissionState) { AccompanistPermissionWrapper(accPermissionState, context) }

    return wrapper
}

@OptIn(ExperimentalPermissionsApi::class)
class AccompanistPermissionWrapper (val accPermissionState: PermissionState, private val context: Context): CameraPermissionState {
    override val status: CameraPermissionStatus
        get() = accPermissionState.status.toCameraPermissionStatus()

    override fun requestCameraPermission() {
        accPermissionState.launchPermissionRequest()
    }

    override fun goToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + context.packageName)
        ContextCompat.startActivity(context, intent, null)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun PermissionStatus.toCameraPermissionStatus(): CameraPermissionStatus {
    return when (this) {
        is PermissionStatus.Denied -> CameraPermissionStatus.Denied
        PermissionStatus.Granted -> CameraPermissionStatus.Granted
    }
}
