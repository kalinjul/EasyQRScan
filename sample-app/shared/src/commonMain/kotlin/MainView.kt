package org.publicvalue.multiplatform.qrcode.sample

import androidx.compose.runtime.Composable
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@Composable
fun MainView() {
    ScannerWithPermissions(onScanned = { println(it); true }, types = listOf(CodeType.QR))
}