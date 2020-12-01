package uk.nhs.nhsx.covid19.android.app.util

interface Base64Encoder {
    fun encodeUrl(byteArray: ByteArray): String
}
