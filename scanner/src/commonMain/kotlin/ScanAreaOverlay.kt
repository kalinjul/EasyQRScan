package org.publicvalue.multiplatform.qrcode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath

/**
 * Draws a darkened overlay covering the whole available size with a transparent, square (by
 * default) cutout in the center matching [scanArea], plus four corner markers around it -
 * the classic "viewfinder" look used by most scanner apps. Used by [Scanner] to visualize
 * the region that is actually being scanned when a [ScanArea] is provided.
 */
@Composable
fun ScanAreaOverlay(
    scanArea: ScanArea,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val (cutoutWidth, cutoutHeight) = scanArea.cutoutSize(size.width, size.height)
        val cutoutSize = Size(cutoutWidth, cutoutHeight)
        val cutoutTopLeft = Offset(
            x = (size.width - cutoutSize.width) / 2f,
            y = (size.height - cutoutSize.height) / 2f,
        )
        val cornerRadiusPx = scanArea.cornerRadius.toPx()
        val cornerRadius = CornerRadius(cornerRadiusPx)

        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(cutoutTopLeft, cutoutSize),
                    cornerRadius = cornerRadius,
                )
            )
        }

        clipPath(path = cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(color = scanArea.colors.overlayColor)
        }

        scanArea.colors.borderColor?.let { borderColor ->
            val radius = cornerRadiusPx.coerceAtMost(minOf(cutoutSize.width, cutoutSize.height) / 2f)
            val armLength = scanArea.cornerLength.toPx().coerceAtMost(
                minOf(cutoutSize.width, cutoutSize.height) / 2f - radius
            ).coerceAtLeast(0f)
            val strokeWidthPx = scanArea.borderWidth.toPx()
            val rect = Rect(cutoutTopLeft, cutoutSize)
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

            // Each corner marker follows the same rounding as the cutout: a straight arm,
            // an arc matching cornerRadius, then the perpendicular arm - so the marker's
            // curve lines up exactly with the darkened area's rounded edge.
            fun corner(
                arcRect: Rect,
                startAngle: Float,
                sweepAngle: Float,
                armStart: Offset,
                armEnd: Offset,
                otherArmEnd: Offset,
            ) = Path().apply {
                moveTo(armStart.x, armStart.y)
                lineTo(armEnd.x, armEnd.y)
                arcTo(arcRect, startAngle, sweepAngle, false)
                lineTo(otherArmEnd.x, otherArmEnd.y)
            }

            // Top-left
            drawPath(
                path = corner(
                    arcRect = Rect(rect.left, rect.top, rect.left + 2 * radius, rect.top + 2 * radius),
                    startAngle = 270f,
                    sweepAngle = -90f,
                    armStart = Offset(rect.left + radius + armLength, rect.top),
                    armEnd = Offset(rect.left + radius, rect.top),
                    otherArmEnd = Offset(rect.left, rect.top + radius + armLength),
                ),
                color = borderColor,
                style = stroke,
            )
            // Top-right
            drawPath(
                path = corner(
                    arcRect = Rect(rect.right - 2 * radius, rect.top, rect.right, rect.top + 2 * radius),
                    startAngle = 270f,
                    sweepAngle = 90f,
                    armStart = Offset(rect.right - radius - armLength, rect.top),
                    armEnd = Offset(rect.right - radius, rect.top),
                    otherArmEnd = Offset(rect.right, rect.top + radius + armLength),
                ),
                color = borderColor,
                style = stroke,
            )
            // Bottom-left
            drawPath(
                path = corner(
                    arcRect = Rect(rect.left, rect.bottom - 2 * radius, rect.left + 2 * radius, rect.bottom),
                    startAngle = 90f,
                    sweepAngle = 90f,
                    armStart = Offset(rect.left + radius + armLength, rect.bottom),
                    armEnd = Offset(rect.left + radius, rect.bottom),
                    otherArmEnd = Offset(rect.left, rect.bottom - radius - armLength),
                ),
                color = borderColor,
                style = stroke,
            )
            // Bottom-right
            drawPath(
                path = corner(
                    arcRect = Rect(rect.right - 2 * radius, rect.bottom - 2 * radius, rect.right, rect.bottom),
                    startAngle = 90f,
                    sweepAngle = -90f,
                    armStart = Offset(rect.right - radius - armLength, rect.bottom),
                    armEnd = Offset(rect.right - radius, rect.bottom),
                    otherArmEnd = Offset(rect.right, rect.bottom - radius - armLength),
                ),
                color = borderColor,
                style = stroke,
            )
        }
    }
}
