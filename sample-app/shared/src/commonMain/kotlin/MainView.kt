package org.publicvalue.multiplatform.qrcode.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@Composable
fun MainView() {
    Box() {
        val snackbarHostState = remember() { SnackbarHostState() }

        Column() {
            Text("Scan QR-Code below")
            var scannerVisible by remember {mutableStateOf(false)}
            Button(onClick = {
                scannerVisible = !scannerVisible
            }) {
                Text("Toggle scanner (visible: $scannerVisible)")
            }
            if (scannerVisible) {
                val scope = rememberCoroutineScope()
                ScannerWithPermissions(
                    modifier = Modifier.padding(16.dp),
                    onScanned = {
                        scope.launch {
                            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                        }
                        false // continue scanning
                    }, types = listOf(CodeType.QR)
                )
            }
        }
        SnackbarHost(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            hostState = snackbarHostState
        )
    }
}