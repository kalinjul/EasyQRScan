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
 */
@Composable
expect fun Scanner(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
    types: List<CodeType>,
    cameraPosition: CameraPosition = CameraPosition.BACK,
    defaultOrientation: CameraOrientation? = null
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
 */
@Composable
fun ScannerWithPermissions(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
    types: List<CodeType>,
    cameraPosition: CameraPosition = CameraPosition.BACK,
    permissionText: String = "Camera is required for QR Code scanning",
    openSettingsLabel: String = "Open Settings",
    defaultOrientation: CameraOrientation?
) {
    ScannerWithPermissions(
        modifier = modifier.clipToBounds(),
        onScanned = onScanned,
        types = types,
        cameraPosition = cameraPosition,
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
        },
        defaultOrientation
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
 */
@Composable
fun ScannerWithPermissions(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
    types: List<CodeType>,
    cameraPosition: CameraPosition,
    permissionDeniedContent: @Composable (CameraPermissionState) -> Unit,
    defaultOrientation: CameraOrientation?
) {
    val permissionState = rememberCameraPermissionState()

    LaunchedEffect(Unit) {
        if (permissionState.status == CameraPermissionStatus.Denied) {
            permissionState.requestCameraPermission()
        }
    }

    if (permissionState.status == CameraPermissionStatus.Granted) {
        Scanner(modifier, types = types, onScanned = onScanned, cameraPosition = cameraPosition, defaultOrientation = defaultOrientation)
    } else {
        permissionDeniedContent(permissionState)
    }
}
