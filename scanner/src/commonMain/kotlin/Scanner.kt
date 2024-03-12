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

@Composable
expect fun Scanner(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
    types: List<CodeType>
)

@Composable
fun ScannerWithPermissions(
    modifier: Modifier = Modifier.clipToBounds(),
    onScanned: (String) -> Boolean,
    types: List<CodeType>,
    permissionText: String = "Camera is required for QR Code scanning",
    openSettingsLabel: String = "Open Settings",
) {
    ScannerWithPermissions(
        modifier = modifier,
        onScanned = onScanned,
        types = types,
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

@Composable
fun ScannerWithPermissions(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
    types: List<CodeType>,
    permissionDeniedContent: @Composable (CameraPermissionState) -> Unit,
) {
    val permissionState = rememberCameraPermissionState()

    LaunchedEffect(Unit) {
        if (permissionState.status == CameraPermissionStatus.Denied) {
            permissionState.requestCameraPermission()
        }
    }

    if (permissionState.status == CameraPermissionStatus.Granted) {
        Scanner(modifier, types = types, onScanned = onScanned)
    } else {
        permissionDeniedContent(permissionState)
    }
}