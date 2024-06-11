package org.publicvalue.multiplatform.qrcode.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@Composable
fun MainView() {
    Column() {
        Text("Scan QR-Code below")
        var scannerVisible by remember {mutableStateOf(false)}
        Button(onClick = {
            scannerVisible = !scannerVisible
        }) {
            Text("Toggle scanner (visible: $scannerVisible)")
        }
        if (scannerVisible) {
            ScannerWithPermissions(
                modifier = Modifier.padding(16.dp),
                onScanned = { println(it); true }, types = listOf(CodeType.QR)
            )
        }
    }
}