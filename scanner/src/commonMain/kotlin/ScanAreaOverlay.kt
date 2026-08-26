package org.publicvalue.multiplatform.qrcode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath

/**
 * Draws a darkened overlay covering the whole available size with a transparent, rounded
 * cutout in the center matching [scanArea]. Used by [Scanner] to visualize the region that
 * is actually being scanned when a [ScanArea] is provided.
 */
@Composable
fun ScanAreaOverlay(
    scanArea: ScanArea,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cutoutSize = Size(
            width = size.width * scanArea.widthFraction,
            height = size.height * scanArea.heightFraction,
        )
        val cutoutTopLeft = Offset(
            x = (size.width - cutoutSize.width) / 2f,
            y = (size.height - cutoutSize.height) / 2f,
        )
        val cornerRadius = CornerRadius(scanArea.cornerRadius.toPx())

        val cutoutPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(cutoutTopLeft, cutoutSize),
                    cornerRadius = cornerRadius,
                )
            )
        }

        clipPath(path = cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(color = scanArea.colors.overlayColor)
        }

        scanArea.colors.borderColor?.let { borderColor ->
            drawRoundRect(
                color = borderColor,
                topLeft = cutoutTopLeft,
                size = cutoutSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = scanArea.borderWidth.toPx()),
            )
        }
    }
}
