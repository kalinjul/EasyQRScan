package org.publicvalue.multiplatform.qrcode

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat

class AndroidCameraUtils(context: Context) : CameraUtils {
    private val cameraManager = ContextCompat.getSystemService(context,CameraManager::class.java)

    override fun setTorchMode(
        cameraPosition: CameraPosition,
        value: Boolean
    ): Boolean {
        val cameraId = cameraManager?.cameraIdList?.find {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == cameraPosition.toCharacteristics()
        }
        if (cameraId == null) {
            return false
        }
        cameraId.let { cameraManager.setTorchMode(it, value) }
        return true
    }
}

fun CameraPosition.toCharacteristics() = when(this) {
    CameraPosition.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
    CameraPosition.BACK -> CameraCharacteristics.LENS_FACING_BACK
}
