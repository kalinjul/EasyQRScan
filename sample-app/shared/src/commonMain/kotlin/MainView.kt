package org.publicvalue.multiplatform.qrcode.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
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
            var scannerVisible by remember {mutableStateOf(false)}
            Button(onClick = {
                scannerVisible = !scannerVisible
            }) {
                Text("Toggle scanner (visible: $scannerVisible)")
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
                    cameraPosition = CameraPosition.BACK
                )
            }
        }
    }
}