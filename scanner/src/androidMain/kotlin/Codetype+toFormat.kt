package org.publicvalue.multiplatform.qrcode

import com.google.mlkit.vision.barcode.common.Barcode

fun List<CodeType>.toFormat(): Int = map {
    when(it) {
        CodeType.Codabar -> Barcode.FORMAT_CODABAR
        CodeType.Code39 -> Barcode.FORMAT_CODE_39
        CodeType.Code93 -> Barcode.FORMAT_CODE_93
        CodeType.Code128 -> Barcode.FORMAT_CODE_128
        CodeType.EAN8 -> Barcode.FORMAT_EAN_8
        CodeType.EAN13 -> Barcode.FORMAT_EAN_13
        CodeType.ITF -> Barcode.FORMAT_ITF
        CodeType.UPCE -> Barcode.FORMAT_UPC_E
        CodeType.Aztec -> Barcode.FORMAT_AZTEC
        CodeType.DataMatrix -> Barcode.FORMAT_DATA_MATRIX
        CodeType.PDF417 -> Barcode.FORMAT_PDF417
        CodeType.QR -> Barcode.FORMAT_QR_CODE
    }
}.fold(0) { acc, next ->
    acc + next
}

fun Int.toCodeType(): CodeType? = when (this) {
    Barcode.FORMAT_CODABAR -> CodeType.Codabar
    Barcode.FORMAT_CODE_39 -> CodeType.Code39
    Barcode.FORMAT_CODE_93 -> CodeType.Code93
    Barcode.FORMAT_CODE_128 -> CodeType.Code128
    Barcode.FORMAT_EAN_8 -> CodeType.EAN8
    Barcode.FORMAT_EAN_13 -> CodeType.EAN13
    Barcode.FORMAT_ITF -> CodeType.ITF
    Barcode.FORMAT_UPC_E -> CodeType.UPCE
    Barcode.FORMAT_AZTEC -> CodeType.Aztec
    Barcode.FORMAT_DATA_MATRIX -> CodeType.DataMatrix
    Barcode.FORMAT_PDF417 -> CodeType.PDF417
    Barcode.FORMAT_QR_CODE -> CodeType.QR
    else -> null
}
