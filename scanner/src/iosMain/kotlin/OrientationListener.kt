package org.publicvalue.multiplatform.qrcode

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class OrientationListener(
    val orientationChanged: (UIDeviceOrientation) -> Unit
) : NSObject() {

    val notificationName = platform.UIKit.UIDeviceOrientationDidChangeNotification

    @Suppress("UNUSED_PARAMETER")
    @ObjCAction
    fun orientationDidChange(arg: NSNotification) {
        orientationChanged(UIDevice.currentDevice.orientation)
    }

    fun register() {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(
                OrientationListener::orientationDidChange.name + ":"
            ),
            name = notificationName,
            `object` = null
        )
    }

    fun unregister() {
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = this,
            name = notificationName,
            `object` = null
        )
    }
}