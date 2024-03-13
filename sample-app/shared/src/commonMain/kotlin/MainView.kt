package org.publicvalue.multiplatform.qrcode.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@Composable
fun MainView() {
    Column() {
        Text("Scan QR-Code below")
        ScannerWithPermissions(
            modifier = Modifier.padding(16.dp).clipToBounds(),
            onScanned = { println(it); true }, types = listOf(CodeType.QR)
        )
    }
}