package org.publicvalue.multiplatform.qrcode

import platform.UIKit.UIDevice

fun iosVersion() = UIDevice.currentDevice.systemVersion
fun iosVersionIsMin(major: Int, minor: Int) = iosVersion()
    .split('.')
    .map { it.toIntOrNull() ?: 0 }
    .let {
        val systemMajor = it.getOrElse(0) { 0 }
        val systemMinor = it.getOrElse(1) { 0 }
        systemMajor > major || systemMajor == major && systemMinor >= minor
    }
