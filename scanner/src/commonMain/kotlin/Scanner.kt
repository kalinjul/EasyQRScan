package org.publicvalue.multiplatform.qrcode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp

/**
 * Code Scanner
 *
 * @param types Code types to scan.
 * @param onScanned Called when a code was scanned. The given lambda should return true
 *                  if scanning was successful and scanning should be aborted.
 *                  Return false if scanning should continue.
 * @param orientation Orientation used by the camera preview. `Device` follows physical
 *                    device rotation; the other values keep the preview fixed.
 */
@Composable
expect fun Scanner(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
    types: List<CodeType>,
    cameraPosition: CameraPosition = CameraPosition.BACK,
    enableTorch: Boolean,
    orientation: ScannerOrientation = ScannerOrientation.Device,
)

/**
 * Code Scanner with permission handling.
 *
 * @param types Code types to scan.
 * @param onScanned Called when a code was scanned. The given lambda should return true
 *                  if scanning was successful and scanning should be aborted.
 *                  Return false if scanning should continue.
 * @param permissionText Text to show if permission was denied.
 * @param openSettingsLabel Label to show on the "Go to settings" Button
 * @param orientation Orientation used by the camera preview.
 */
@Composable
fun ScannerWithPermissions(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
    types: List<CodeType>,
    cameraPosition: CameraPosition = CameraPosition.BACK,
    enableTorch: Boolean,
    permissionText: String = "Camera is required for QR Code scanning",
    openSettingsLabel: String = "Open Settings",
    orientation: ScannerOrientation = ScannerOrientation.Device,
) {
    ScannerWithPermissions(
        modifier = modifier.clipToBounds(),
        onScanned = onScanned,
        types = types,
        cameraPosition = cameraPosition,
        enableTorch = enableTorch,
        orientation = orientation,
        permissionDeniedContent = { permissionState ->
            Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    modifier = Modifier.padding(6.dp),
                    text = permissionText
                )
                Button(onClick = { permissionState.goToSettings() }) {
                    Text(openSettingsLabel)
                }
            }
        }
    )
}

/**
 * Code Scanner with permission handling.
 *
 * @param types Code types to scan.
 * @param onScanned Called when a code was scanned. The given lambda should return true
 *                  if scanning was successful and scanning should be aborted.
 *                  Return false if scanning should continue.
 * @param permissionDeniedContent Content to show if permission was denied.
 * @param orientation Orientation used by the camera preview.
 */
@Composable
fun ScannerWithPermissions(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
    types: List<CodeType>,
    cameraPosition: CameraPosition,
    enableTorch: Boolean,
    orientation: ScannerOrientation = ScannerOrientation.Device,
    permissionDeniedContent: @Composable (CameraPermissionState) -> Unit,
) {
    val permissionState = rememberCameraPermissionState()

    LaunchedEffect(Unit) {
        if (permissionState.status == CameraPermissionStatus.Denied) {
            permissionState.requestCameraPermission()
        }
    }

    if (permissionState.status == CameraPermissionStatus.Granted) {
        Scanner(
            modifier,
            types = types,
            onScanned = onScanned,
            cameraPosition = cameraPosition,
            enableTorch = enableTorch,
            orientation = orientation,
        )
    } else {
        permissionDeniedContent(permissionState)
    }
}