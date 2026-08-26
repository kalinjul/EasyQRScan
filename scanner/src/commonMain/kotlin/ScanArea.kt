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
 * @param widthFraction Width of the scan rectangle as a fraction of the scanner's width (0f..1f).
 * @param heightFraction Height of the scan rectangle as a fraction of the scanner's height (0f..1f).
 * @param cornerRadius Corner radius of the scan rectangle cutout.
 * @param colors Colors used to draw the overlay and the border around the cutout.
 * @param borderWidth Width of the border drawn around the cutout, if [ScanAreaColors.borderColor] is set.
 */
data class ScanArea(
    val widthFraction: Float,
    val heightFraction: Float,
    val cornerRadius: Dp,
    val colors: ScanAreaColors,
    val borderWidth: Dp,
)

/**
 * Colors used to draw a [ScanArea] overlay.
 *
 * @param overlayColor Color of the area outside the scan rectangle.
 * @param borderColor Color of the border drawn around the scan rectangle. Pass `null` to draw no border.
 */
data class ScanAreaColors(
    val overlayColor: Color,
    val borderColor: Color?,
)

/**
 * Contains default values used by [ScanArea].
 */
object ScanAreaDefaults {
    const val WidthFraction = 0.7f
    const val HeightFraction = 0.7f
    val CornerRadius = 16.dp
    val BorderWidth = 2.dp

    /**
     * Creates a [ScanArea] with default values, following the same "Defaults" pattern as
     * Compose Material components (e.g. `TextFieldDefaults`).
     */
    @Composable
    fun scanArea(
        widthFraction: Float = WidthFraction,
        heightFraction: Float = HeightFraction,
        cornerRadius: Dp = CornerRadius,
        colors: ScanAreaColors = colors(),
        borderWidth: Dp = BorderWidth,
    ): ScanArea = ScanArea(
        widthFraction = widthFraction,
        heightFraction = heightFraction,
        cornerRadius = cornerRadius,
        colors = colors,
        borderWidth = borderWidth,
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
