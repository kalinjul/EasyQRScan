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
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode

private fun List<CodeType>.toFormat(): List<platform.AVFoundation.AVMetadataObjectType> = map {
    when(it) {
        CodeType.Codabar -> AVMetadataObjectTypeCodabarCode
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