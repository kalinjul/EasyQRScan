package org.publicvalue.multiplatform.qrcode

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.hasTorch
import platform.AVFoundation.setTorchMode
import platform.Foundation.NSError

class IosCameraUtils: CameraUtils {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun setTorchMode(
        cameraPosition: CameraPosition,
        value: Boolean
    ): Boolean {
        val device = AVCaptureDevice.getDevice(cameraPosition)
        if (device == null || !device.hasTorch()) {
            return false
        }

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            device.lockForConfiguration(error.ptr)
            if (error.value != null) {
                throw Exception("Error locking device: ${error.value}")
            }
        }
        device.setTorchMode(
            if (value) {
                AVCaptureTorchModeOn
            } else {
                AVCaptureTorchModeOff
            }
        )

        device.unlockForConfiguration()
        return true
    }

}
