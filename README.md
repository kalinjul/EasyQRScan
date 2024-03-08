# EasyQRScan: Compose Multiplatform QR-Code Scanner
[![CI Status](https://img.shields.io/github/actions/workflow/status/kalinjul/EasyQRScan/main.yml)]((https://github.com/kalinjul/EasyQRScan/actions/workflows/main.yml))
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kalinjul.easyqrscan/scanner)](https://repo1.maven.org/maven2/io/github/kalinjul/kotlin/easyqrscan/scanner/)
![Kotlin Version](https://kotlin-version.aws.icerock.dev/kotlin-version?group=io.github.kalinjul.easyqrscan&name=scanner)
![Compose Version](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fkalinjul%2FEasyQRScan%2Fmain%2Fgradle%2Flibs.versions.toml&query=%24.versions%5B'compose-multiplatform'%5D&label=Compose%20Version)

QR-Code (or other 2D/3D-Codes, see below) Scanner for Compose Multiplatform.
Currently, the implementation is rather rudimentary.

# Usage
## Camera Permissions
Include this at root level in your AndroidManifest.xml:
```xml
<uses-feature android:name="android.hardware.camera"/>
<uses-feature android:name="android.hardware.camera.autofocus"/>
<uses-permission android:name="android.permission.CAMERA"/>
```

Add this key to the Info.plist in your xcode project:
```NSCameraUsageDescription``` and provide a description as value

## Compose UI
```kotlin
// basic permission handling included:
ScannerWithPermissions(onScanned = { println(it); true }, types = listOf(CodeType.QR))

// or, if you handle permissions yourself:
Scanner(onScanned = { println(it); true }, types = listOf(CodeType.QR))
```

Check out the [sample app](./sample-app) included in the repository.

# Code Types
Code types supported are:
Codabar, Code39, Code93, Code128, EAN8, EAN13, ITF, UPCE, Aztec, DataMatrix, PDF417, QR