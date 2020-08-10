package uk.nhs.nhsx.covid19.android.app.util

interface Base64Decoder {
    fun decode(string: String): String
    fun decodeToBytes(string: String): ByteArray
    fun decodeUrl(string: String): String
    fun decodeUrlToBytes(string: String): ByteArray
}
