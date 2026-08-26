# EasyQRScan: Compose Multiplatform QR-Code Scanner
[![CI Status](https://img.shields.io/github/actions/workflow/status/kalinjul/EasyQRScan/main.yml)]((https://github.com/kalinjul/EasyQRScan/actions/workflows/main.yml))
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kalinjul.easyqrscan/scanner)](https://repo1.maven.org/maven2/io/github/kalinjul/easyqrscan/scanner/)
[![Snapshot](https://img.shields.io/nexus/s/io.github.kalinjul.easyqrscan/scanner?server=https%3A%2F%2Fs01.oss.sonatype.org&label=latest%20snapshot)](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/kalinjul/easyqrscan/scanner/)
![Kotlin Version](https://kotlin-version.aws.icerock.dev/kotlin-version?group=io.github.kalinjul.easyqrscan&name=scanner)
![Compose Version](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fkalinjul%2FEasyQRScan%2Fmain%2Fgradle%2Flibs.versions.toml&query=%24.versions%5B'compose-multiplatform'%5D&label=Compose%20Version)

QR-Code (or other 2D/3D-Codes, see below) Scanner for Compose Multiplatform (Android/iOS).
Currently, the implementation is rather rudimentary.

Supported Compose version:

 | Compose version | EasyQRScan Version |
|-----------------|--------------------|
| 1.6.x           | 0.1.0+             |
| 1.7             | 0.2 - 0.3          |
| 1.8             | 0.4.0              |
| 1.9             | 0.5.0 - 0.6.x      |
| 1.10            | 0.7.x              |

# Dependency
Add the dependency to your commonMain sourceSet (KMP) / Android dependencies (android only):
```kotlin
implementation("io.github.kalinjul.easyqrscan:scanner:0.7.2")
```

Or, for your libs.versions.toml:
```toml
[versions]
easyqrscan = "0.7.2"
[libraries]
easyqrscan = { module = "io.github.kalinjul.easyqrscan:scanner", version.ref = "easyqrscan" }
```

# Setup Camera Permissions
Include this at root level in your AndroidManifest.xml:
```xml
<uses-feature android:name="android.hardware.camera"/>
<uses-feature android:name="android.hardware.camera.autofocus"/>
<uses-permission android:name="android.permission.CAMERA"/>
```

Add this key to the Info.plist in your xcode project:
```NSCameraUsageDescription``` and provide a description as value

# Usage

The scanner is included by calling a single composable function ```Scanner()``` or ```ScannerWithPermissions```:

```kotlin
// basic permission handling included:
ScannerWithPermissions(
    onScanned = { println(it); true }, // return true to disable the scanner, false to continue scanning
    types = listOf(CodeType.QR),
    cameraPosition = CameraPosition.BACK,
    enableTorch = false // toggle this to enable/disable the flashlight
)

// or, if you handle permissions yourself:
Scanner(onScanned = { println(it); true }, types = listOf(CodeType.QR))
```

The camera preview follows your app's interface orientation, so an app that locks its
orientation gets a fixed preview without any extra configuration.

## Restricting scanning to a centered area

By default, the whole camera frame is scanned. You can opt in to a "viewfinder" area that
restricts scanning to a rectangle and darkens everything around it, by passing a `scanArea`.
Use `ScanAreaDefaults.scanArea(...)` to build one with sensible, overridable defaults
(following the same pattern as e.g. Compose Material's `TextFieldDefaults.colors(...)`):

```kotlin
ScannerWithPermissions(
    onScanned = { println(it); true },
    types = listOf(CodeType.QR),
    cameraPosition = CameraPosition.BACK,
    enableTorch = false,
    scanArea = ScanAreaDefaults.scanArea(
        // Size: a fraction of the smaller screen dimension (default), or an absolute Dp size.
        size = ScanAreaSize.Relative(sizeFraction = 0.7f, aspectRatio = 1f),
        // size = ScanAreaSize.Fixed(width = 260.dp, height = 260.dp),

        // Position: any Alignment (Center, TopCenter, BottomCenter, ...) + a fine-tuning offset.
        alignment = Alignment.Center,
        offset = DpOffset.Zero,

        // One radius per corner.
        cornerRadii = ScanAreaCornerRadii.all(16.dp),

        colors = ScanAreaDefaults.colors(
            overlayColor = Color.Black.copy(alpha = 0.6f),
            borderColor = Color.White,
        ),
        borderWidth = 4.dp,

        // Brackets (classic viewfinder corners) or a single continuous Outline.
        borderStyle = ScanAreaBorderStyle.Brackets,
        cornerLength = 24.dp, // only used by ScanAreaBorderStyle.Brackets
        strokeCap = StrokeCap.Round,
        strokeJoin = StrokeJoin.Round,
    ),
)
```

Leave `scanArea` as `null` (the default) to keep the previous full-frame behavior with no
overlay. On Android, the visible cutout and the actually scanned region are kept in sync via
a `BoxFit.cover`-style coordinate mapping (matching the camera preview's default scale type);
on iOS this is handled natively via `AVCaptureMetadataOutput.rectOfInterest`.

Check out the [sample app](./sample-app) included in the repository.

# Code Types
Code types supported are:
Codabar, Code39, Code93, Code128, EAN8, EAN13, ITF, UPCE, Aztec, DataMatrix, PDF417, QR