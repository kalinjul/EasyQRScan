package org.publicvalue.multiplatform.qrcode

import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.position

fun AVCaptureDevice.Companion.getDevice(cameraPosition: CameraPosition): AVCaptureDevice? {
    val devices = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo).map { it as AVCaptureDevice }

    return devices.firstOrNull { device ->
        when(cameraPosition) {
            CameraPosition.FRONT -> device.position == AVCaptureDevicePositionFront
            CameraPosition.BACK -> device.position == AVCaptureDevicePositionBack
        }
    } ?: run {
        println("Could not find camera with position: $cameraPosition, using default camera")
        AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    }
}