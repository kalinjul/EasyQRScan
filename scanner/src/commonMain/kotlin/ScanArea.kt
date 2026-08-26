package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Describes a centered scan area (a "viewfinder") that restricts scanning to the region
 * inside it and darkens the surrounding area.
 *
 * Use [ScanAreaDefaults.scanArea] to build instances with sensible, themeable defaults.
 *
 * @param sizeFraction Size of the scan rectangle as a fraction of the scanner's smaller
 *                      dimension (0f..1f). Combined with [aspectRatio], this determines the
 *                      cutout's width and height.
 * @param aspectRatio Width-to-height ratio of the scan rectangle. Defaults to `1f`, i.e. a
 *                     square - the shape most scanner apps use for their viewfinder.
 * @param cornerRadius Corner radius of the scan rectangle cutout.
 * @param colors Colors used to draw the overlay and the corner markers around the cutout.
 * @param borderWidth Thickness of the corner markers drawn around the cutout, if
 *                     [ScanAreaColors.borderColor] is set.
 * @param cornerLength Length of each of the four corner marker lines.
 */
data class ScanArea(
    val sizeFraction: Float,
    val aspectRatio: Float,
    val cornerRadius: Dp,
    val colors: ScanAreaColors,
    val borderWidth: Dp,
    val cornerLength: Dp,
)

/**
 * Colors used to draw a [ScanArea] overlay.
 *
 * @param overlayColor Color of the area outside the scan rectangle.
 * @param borderColor Color of the corner markers around the scan rectangle. Pass `null` to
 *                     draw no markers.
 */
data class ScanAreaColors(
    val overlayColor: Color,
    val borderColor: Color?,
)

/**
 * Contains default values used by [ScanArea].
 */
object ScanAreaDefaults {
    const val SizeFraction = 0.7f
    const val AspectRatio = 1f
    val CornerRadius = 16.dp
    val BorderWidth = 4.dp
    val CornerLength = 24.dp

    /**
     * Creates a [ScanArea] with default values, following the same "Defaults" pattern as
     * Compose Material components (e.g. `TextFieldDefaults`). Defaults to a centered square
     * cutout with corner markers, matching the viewfinder look used by most scanner apps.
     */
    @Composable
    fun scanArea(
        sizeFraction: Float = SizeFraction,
        aspectRatio: Float = AspectRatio,
        cornerRadius: Dp = CornerRadius,
        colors: ScanAreaColors = colors(),
        borderWidth: Dp = BorderWidth,
        cornerLength: Dp = CornerLength,
    ): ScanArea = ScanArea(
        sizeFraction = sizeFraction,
        aspectRatio = aspectRatio,
        cornerRadius = cornerRadius,
        colors = colors,
        borderWidth = borderWidth,
        cornerLength = cornerLength,
    )

    /**
     * Creates a [ScanAreaColors] with default values.
     */
    @Composable
    fun colors(
        overlayColor: Color = Color.Black.copy(alpha = 0.6f),
        borderColor: Color? = Color.White,
    ): ScanAreaColors = ScanAreaColors(
        overlayColor = overlayColor,
        borderColor = borderColor,
    )
}

/**
 * Computes the cutout width/height (in the same unit as [containerWidth]/[containerHeight],
 * e.g. pixels) for this [ScanArea] centered within a container of the given size.
 */
fun ScanArea.cutoutSize(containerWidth: Float, containerHeight: Float): Pair<Float, Float> {
    val side = minOf(containerWidth, containerHeight) * sizeFraction
    val width = if (aspectRatio >= 1f) side * aspectRatio else side
    val height = if (aspectRatio >= 1f) side else side / aspectRatio
    return width to height
}
