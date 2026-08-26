package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Describes the size of a [ScanArea] cutout.
 *
 * Use [Relative] (the default) to size the cutout as a fraction of the scanner's own size,
 * or [Fixed] for a pixel-precise, absolute size (analogous to `cutOutWidth`/`cutOutHeight`
 * in comparable Flutter scanner libraries).
 */
sealed interface ScanAreaSize {

    /**
     * Sizes the cutout as a fraction of the scanner's smaller dimension.
     *
     * @param sizeFraction Size of the scan rectangle as a fraction of the scanner's smaller
     *                      dimension (0f..1f). Combined with [aspectRatio], this determines
     *                      the cutout's width and height.
     * @param aspectRatio Width-to-height ratio of the scan rectangle. Defaults to `1f`, i.e.
     *                     a square - the shape most scanner apps use for their viewfinder.
     */
    data class Relative(
        val sizeFraction: Float,
        val aspectRatio: Float = 1f,
    ) : ScanAreaSize

    /**
     * Sizes the cutout with an absolute, fixed size.
     *
     * @param width Fixed width of the scan rectangle.
     * @param height Fixed height of the scan rectangle.
     */
    data class Fixed(
        val width: Dp,
        val height: Dp,
    ) : ScanAreaSize
}

/**
 * Corner radii of a [ScanArea] cutout, one value per corner (analogous to `BorderRadius` in
 * comparable Flutter scanner libraries). Use [ScanAreaCornerRadii.all] for the common case of
 * a single radius applied to all four corners.
 */
data class ScanAreaCornerRadii(
    val topLeft: Dp,
    val topRight: Dp,
    val bottomLeft: Dp,
    val bottomRight: Dp,
) {
    companion object {
        /** Creates [ScanAreaCornerRadii] with the same [radius] applied to all four corners. */
        fun all(radius: Dp) = ScanAreaCornerRadii(radius, radius, radius, radius)
    }
}

/**
 * Style used to draw the border around a [ScanArea] cutout.
 */
enum class ScanAreaBorderStyle {
    /** Draws four L-shaped corner markers - the classic "viewfinder" look. */
    Brackets,

    /** Draws a single, continuous outline around the whole cutout. */
    Outline,
}

/**
 * Describes a scan area (a "viewfinder") that restricts scanning to the region inside it and
 * darkens the surrounding area.
 *
 * Use [ScanAreaDefaults.scanArea] to build instances with sensible, themeable defaults.
 *
 * @param size Size of the scan rectangle, see [ScanAreaSize].
 * @param alignment Alignment of the scan rectangle within the scanner, e.g.
 *                   [Alignment.Center] (default), [Alignment.TopCenter], ...
 * @param offset Additional offset applied on top of [alignment], for fine-tuned positioning.
 * @param cornerRadii Corner radii of the scan rectangle cutout, one per corner.
 * @param colors Colors used to draw the overlay and the border around the cutout.
 * @param borderWidth Thickness of the border drawn around the cutout, if
 *                     [ScanAreaColors.borderColor] is set.
 * @param borderStyle Whether the border is drawn as corner markers ([ScanAreaBorderStyle.Brackets])
 *                     or as a single continuous outline ([ScanAreaBorderStyle.Outline]).
 * @param cornerLength Length of each of the four corner marker lines. Only used when
 *                      [borderStyle] is [ScanAreaBorderStyle.Brackets].
 * @param strokeCap Cap used at the end of border strokes.
 * @param strokeJoin Join style used where border strokes meet.
 */
data class ScanArea(
    val size: ScanAreaSize,
    val alignment: Alignment = Alignment.Center,
    val offset: DpOffset = DpOffset.Zero,
    val cornerRadii: ScanAreaCornerRadii,
    val colors: ScanAreaColors,
    val borderWidth: Dp,
    val borderStyle: ScanAreaBorderStyle = ScanAreaBorderStyle.Brackets,
    val cornerLength: Dp,
    val strokeCap: StrokeCap = StrokeCap.Round,
    val strokeJoin: StrokeJoin = StrokeJoin.Round,
)

/**
 * Colors used to draw a [ScanArea] overlay.
 *
 * @param overlayColor Color of the area outside the scan rectangle.
 * @param borderColor Color of the border around the scan rectangle. Pass `null` to draw no
 *                     border.
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
        size: ScanAreaSize = ScanAreaSize.Relative(SizeFraction, AspectRatio),
        alignment: Alignment = Alignment.Center,
        offset: DpOffset = DpOffset.Zero,
        cornerRadii: ScanAreaCornerRadii = ScanAreaCornerRadii.all(CornerRadius),
        colors: ScanAreaColors = colors(),
        borderWidth: Dp = BorderWidth,
        borderStyle: ScanAreaBorderStyle = ScanAreaBorderStyle.Brackets,
        cornerLength: Dp = CornerLength,
        strokeCap: StrokeCap = StrokeCap.Round,
        strokeJoin: StrokeJoin = StrokeJoin.Round,
    ): ScanArea = ScanArea(
        size = size,
        alignment = alignment,
        offset = offset,
        cornerRadii = cornerRadii,
        colors = colors,
        borderWidth = borderWidth,
        borderStyle = borderStyle,
        cornerLength = cornerLength,
        strokeCap = strokeCap,
        strokeJoin = strokeJoin,
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
 * Computes the cutout width/height (in pixels) for this [ScanArea] within a container of the
 * given size (in pixels).
 */
private fun ScanArea.cutoutSizePx(containerWidth: Float, containerHeight: Float, density: Density): Size {
    return when (val s = size) {
        is ScanAreaSize.Relative -> {
            val side = minOf(containerWidth, containerHeight) * s.sizeFraction
            val width = if (s.aspectRatio >= 1f) side * s.aspectRatio else side
            val height = if (s.aspectRatio >= 1f) side else side / s.aspectRatio
            Size(width, height)
        }
        is ScanAreaSize.Fixed -> with(density) { Size(s.width.toPx(), s.height.toPx()) }
    }
}

/**
 * Computes the cutout's position + size (in pixels) for this [ScanArea] within a container of
 * the given size (in pixels), taking [ScanArea.alignment] and [ScanArea.offset] into account.
 * This is the single source of truth for where the cutout is placed, shared by the Compose
 * overlay and the native (Android/iOS) scan-restriction logic, so the visible cutout always
 * matches the actually scanned region.
 */
internal fun ScanArea.cutoutRect(containerWidth: Float, containerHeight: Float, density: Density): Rect {
    val cutoutSize = cutoutSizePx(containerWidth, containerHeight, density)

    val containerSize = IntSize(containerWidth.toInt(), containerHeight.toInt())
    val cutoutIntSize = IntSize(cutoutSize.width.toInt(), cutoutSize.height.toInt())
    val alignedOffset = alignment.align(cutoutIntSize, containerSize, LayoutDirection.Ltr)

    val offsetPx = with(density) { Pair(offset.x.toPx(), offset.y.toPx()) }

    val left = alignedOffset.x + offsetPx.first
    val top = alignedOffset.y + offsetPx.second

    return Rect(
        left = left,
        top = top,
        right = left + cutoutSize.width,
        bottom = top + cutoutSize.height,
    )
}
