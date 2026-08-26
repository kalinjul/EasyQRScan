package org.publicvalue.multiplatform.qrcode.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScanAreaBorderStyle
import org.publicvalue.multiplatform.qrcode.ScanAreaDefaults
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions
import org.publicvalue.multiplatform.qrcode.rememberCameraUtils
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
fun MainView() {
    val snackbarHostState = remember() { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Text("Scan QR-Code below")
            var scannerVisible by remember { mutableStateOf(false) }
            var enableTorch by remember { mutableStateOf(false) }
            var scanAreaEnabled by remember { mutableStateOf(false) }
            var scanAreaOutlineStyle by remember { mutableStateOf(false) }

            val cameraUtils = rememberCameraUtils()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Toggle scanner")
                    Switch(
                        checked = scannerVisible,
                        onCheckedChange = {
                            scannerVisible = it
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Toggle torch")
                    Switch(
                        checked = enableTorch,
                        onCheckedChange = {
                            enableTorch = it
                            if (!scannerVisible) { // android needs separate handling when capturing video
                                cameraUtils.setTorchMode(CameraPosition.BACK, it)
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Toggle scan area")
                    Switch(
                        checked = scanAreaEnabled,
                        onCheckedChange = {
                            scanAreaEnabled = it
                        }
                    )
                }
                if (scanAreaEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scan area: outline instead of brackets")
                        Switch(
                            checked = scanAreaOutlineStyle,
                            onCheckedChange = {
                                scanAreaOutlineStyle = it
                            }
                        )
                    }
                }
            }
            var lastCode by remember { mutableStateOf<String?>(null)}
            var snackbarJob by remember { mutableStateOf<Job?>(null)}
            var lastSnackbar by remember { mutableStateOf(TimeSource.Monotonic.markNow().minus(1.minutes))}
            if (scannerVisible) {
                val scope = rememberCoroutineScope()
                ScannerWithPermissions(
                    modifier = Modifier.padding(16.dp),
                    onScanned = {
                        if (lastCode == it) { return@ScannerWithPermissions false }
                        if (TimeSource.Monotonic.markNow().minus(lastSnackbar) < 1.seconds) {
                            return@ScannerWithPermissions false
                        }
                        snackbarJob?.cancel()
                        lastSnackbar = TimeSource.Monotonic.markNow()
                        snackbarJob = scope.launch {
                            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                            if (lastCode == it) {
                                lastCode = null
                            }
                        }
                        lastCode = it
                        false // continue scanning
                    },
                    types = listOf(CodeType.QR),
                    cameraPosition = CameraPosition.BACK,
                    enableTorch = enableTorch,
                    scanArea = if (scanAreaEnabled) {
                        ScanAreaDefaults.scanArea(
                            borderStyle = if (scanAreaOutlineStyle) {
                                ScanAreaBorderStyle.Outline
                            } else {
                                ScanAreaBorderStyle.Brackets
                            },
                        )
                    } else null,
                )
            }
        }
    }
}