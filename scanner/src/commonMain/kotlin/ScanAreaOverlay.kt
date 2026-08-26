package org.publicvalue.multiplatform.qrcode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath

/**
 * Draws a darkened overlay covering the whole available size with a transparent cutout
 * matching [scanArea], plus a border around it - either four corner markers (the classic
 * "viewfinder" look) or a single continuous outline, depending on [ScanArea.borderStyle].
 * Used by [Scanner] to visualize the region that is actually being scanned when a [ScanArea]
 * is provided.
 */
@Composable
fun ScanAreaOverlay(
    scanArea: ScanArea,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cutoutRect = scanArea.cutoutRect(size.width, size.height, this)

        val topLeftRadius = scanArea.cornerRadii.topLeft.toPx()
        val topRightRadius = scanArea.cornerRadii.topRight.toPx()
        val bottomLeftRadius = scanArea.cornerRadii.bottomLeft.toPx()
        val bottomRightRadius = scanArea.cornerRadii.bottomRight.toPx()

        val roundRect = RoundRect(
            rect = cutoutRect,
            topLeft = CornerRadius(topLeftRadius),
            topRight = CornerRadius(topRightRadius),
            bottomRight = CornerRadius(bottomRightRadius),
            bottomLeft = CornerRadius(bottomLeftRadius),
        )

        val cutoutPath = Path().apply { addRoundRect(roundRect) }

        clipPath(path = cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(color = scanArea.colors.overlayColor)
        }

        scanArea.colors.borderColor?.let { borderColor ->
            val strokeWidthPx = scanArea.borderWidth.toPx()
            val stroke = Stroke(
                width = strokeWidthPx,
                cap = scanArea.strokeCap,
                join = scanArea.strokeJoin,
            )

            when (scanArea.borderStyle) {
                ScanAreaBorderStyle.Outline -> {
                    drawPath(path = Path().apply { addRoundRect(roundRect) }, color = borderColor, style = stroke)
                }
                ScanAreaBorderStyle.Brackets -> {
                    drawCornerBrackets(
                        rect = cutoutRect,
                        topLeftRadius = topLeftRadius,
                        topRightRadius = topRightRadius,
                        bottomLeftRadius = bottomLeftRadius,
                        bottomRightRadius = bottomRightRadius,
                        cornerLengthPx = scanArea.cornerLength.toPx(),
                        color = borderColor,
                        stroke = stroke,
                    )
                }
            }
        }
    }
}

/**
 * Draws four L-shaped corner markers around [rect]. Each marker follows the same rounding as
 * the cutout: a straight arm, an arc matching the corner's own radius, then the perpendicular
 * arm - so the marker's curve lines up exactly with the darkened area's rounded edge.
 */
private fun DrawScope.drawCornerBrackets(
    rect: Rect,
    topLeftRadius: Float,
    topRightRadius: Float,
    bottomLeftRadius: Float,
    bottomRightRadius: Float,
    cornerLengthPx: Float,
    color: Color,
    stroke: Stroke,
) {
    fun armLength(radius: Float) = cornerLengthPx.coerceAtMost(
        minOf(rect.width, rect.height) / 2f - radius
    ).coerceAtLeast(0f)

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

    val topLeftArm = armLength(topLeftRadius)
    drawPath(
        path = corner(
            arcRect = Rect(rect.left, rect.top, rect.left + 2 * topLeftRadius, rect.top + 2 * topLeftRadius),
            startAngle = 270f,
            sweepAngle = -90f,
            armStart = Offset(rect.left + topLeftRadius + topLeftArm, rect.top),
            armEnd = Offset(rect.left + topLeftRadius, rect.top),
            otherArmEnd = Offset(rect.left, rect.top + topLeftRadius + topLeftArm),
        ),
        color = color,
        style = stroke,
    )
    val topRightArm = armLength(topRightRadius)
    drawPath(
        path = corner(
            arcRect = Rect(rect.right - 2 * topRightRadius, rect.top, rect.right, rect.top + 2 * topRightRadius),
            startAngle = 270f,
            sweepAngle = 90f,
            armStart = Offset(rect.right - topRightRadius - topRightArm, rect.top),
            armEnd = Offset(rect.right - topRightRadius, rect.top),
            otherArmEnd = Offset(rect.right, rect.top + topRightRadius + topRightArm),
        ),
        color = color,
        style = stroke,
    )
    val bottomLeftArm = armLength(bottomLeftRadius)
    drawPath(
        path = corner(
            arcRect = Rect(rect.left, rect.bottom - 2 * bottomLeftRadius, rect.left + 2 * bottomLeftRadius, rect.bottom),
            startAngle = 90f,
            sweepAngle = 90f,
            armStart = Offset(rect.left + bottomLeftRadius + bottomLeftArm, rect.bottom),
            armEnd = Offset(rect.left + bottomLeftRadius, rect.bottom),
            otherArmEnd = Offset(rect.left, rect.bottom - bottomLeftRadius - bottomLeftArm),
        ),
        color = color,
        style = stroke,
    )
    val bottomRightArm = armLength(bottomRightRadius)
    drawPath(
        path = corner(
            arcRect = Rect(rect.right - 2 * bottomRightRadius, rect.bottom - 2 * bottomRightRadius, rect.right, rect.bottom),
            startAngle = 90f,
            sweepAngle = -90f,
            armStart = Offset(rect.right - bottomRightRadius - bottomRightArm, rect.bottom),
            armEnd = Offset(rect.right - bottomRightRadius, rect.bottom),
            otherArmEnd = Offset(rect.right, rect.bottom - bottomRightRadius - bottomRightArm),
        ),
        color = color,
        style = stroke,
    )
}
