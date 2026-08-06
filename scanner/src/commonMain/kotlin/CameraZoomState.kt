package org.publicvalue.multiplatform.qrcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Device-reported zoom bounds once the camera is bound. Ratios are defined by the
 * underlying platform (CameraX on Android, AVFoundation on iOS); see README for details.
 */
data class CameraZoomRange(
    val minZoomRatio: Float,
    val maxZoomRatio: Float,
)

/**
 * Snapshot-backed zoom state for [Scanner], similar in usage to state-based Compose APIs
 * (hold the instance, pass it into [Scanner], read [zoomRange] / [zoomRatio], call [setZoomRatio] from UI).
 *
 * [zoomRange] is null until the camera session is ready. Until then, [setZoomRatio] accepts any positive
 * ratio and applies clamping once the range is known.
 */
@Stable
class CameraZoomState(initialZoomRatio: Float = 1f) {

    private val _zoomRatio = mutableFloatStateOf(initialZoomRatio.coerceAtLeast(MIN_ZOOM_BEFORE_RANGE))
    private val _zoomRange = mutableStateOf<CameraZoomRange?>(null)

    /** Current zoom ratio; always within [zoomRange] when the range is non-null. */
    val zoomRatio: Float get() = _zoomRatio.floatValue

    /** Null until the camera reports capabilities. */
    val zoomRange: State<CameraZoomRange?> get() = _zoomRange

    fun setZoomRatio(ratio: Float) {
        val clamped = clampRatio(ratio)
        _zoomRatio.floatValue = clamped
    }

    internal fun updateZoomRangeFromPlatform(range: CameraZoomRange?) {
        _zoomRange.value = range
        if (range != null) {
            _zoomRatio.floatValue = clampRatio(_zoomRatio.floatValue)
        }
    }

    private fun clampRatio(ratio: Float): Float {
        val range = _zoomRange.value
        val safe = ratio.coerceAtLeast(MIN_ZOOM_BEFORE_RANGE)
        return if (range != null) {
            safe.coerceIn(range.minZoomRatio, range.maxZoomRatio)
        } else {
            safe
        }
    }

    private companion object {
        const val MIN_ZOOM_BEFORE_RANGE = 0.01f
    }
}

@Composable
fun rememberCameraZoomState(initialZoomRatio: Float = 1f): CameraZoomState {
    return remember { CameraZoomState(initialZoomRatio) }
}
