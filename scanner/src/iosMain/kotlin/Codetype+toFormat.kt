package org.publicvalue.multiplatform.qrcode

import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCodabarCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeITF14Code
import platform.AVFoundation.AVMetadataObjectType
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode

fun List<CodeType>.toFormat(): List<AVMetadataObjectType> = map {
    when(it) {
        CodeType.Codabar -> if (iosVersionIsMin(15,4)) { AVMetadataObjectTypeCodabarCode } else error("AVMetadataObjectTypeCodabarCode not available on iOS ${iosVersion()}")
        CodeType.Code39 -> AVMetadataObjectTypeCode39Code
        CodeType.Code93 -> AVMetadataObjectTypeCode93Code
        CodeType.Code128 -> AVMetadataObjectTypeCode128Code
        CodeType.EAN8 -> AVMetadataObjectTypeEAN8Code
        CodeType.EAN13 -> AVMetadataObjectTypeEAN13Code
        CodeType.ITF -> AVMetadataObjectTypeITF14Code
        CodeType.UPCE -> AVMetadataObjectTypeUPCECode
        CodeType.Aztec -> AVMetadataObjectTypeAztecCode
        CodeType.DataMatrix -> AVMetadataObjectTypeDataMatrixCode
        CodeType.PDF417 -> AVMetadataObjectTypePDF417Code
        CodeType.QR -> AVMetadataObjectTypeQRCode
    }
}

fun AVMetadataObjectType.toCodeType(): CodeType? = when (this) {
    AVMetadataObjectTypeCodabarCode -> CodeType.Codabar
    AVMetadataObjectTypeCode39Code -> CodeType.Code39
    AVMetadataObjectTypeCode93Code -> CodeType.Code93
    AVMetadataObjectTypeCode128Code -> CodeType.Code128
    AVMetadataObjectTypeEAN8Code -> CodeType.EAN8
    AVMetadataObjectTypeEAN13Code -> CodeType.EAN13
    AVMetadataObjectTypeITF14Code -> CodeType.ITF
    AVMetadataObjectTypeUPCECode -> CodeType.UPCE
    AVMetadataObjectTypeAztecCode -> CodeType.Aztec
    AVMetadataObjectTypeDataMatrixCode -> CodeType.DataMatrix
    AVMetadataObjectTypePDF417Code -> CodeType.PDF417
    AVMetadataObjectTypeQRCode -> CodeType.QR
    else -> null
}
