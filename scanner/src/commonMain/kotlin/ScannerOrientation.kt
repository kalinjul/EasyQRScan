package org.publicvalue.multiplatform.qrcode

/**
 * Controls how the scanner preview is oriented.
 *
 * [Device] follows physical device rotation. The other values keep the preview fixed.
 */
enum class ScannerOrientation {
    Device,
    LandscapeLeft,
    LandscapeRight,
    Portrait,
    PortraitUpsideDown,
}
