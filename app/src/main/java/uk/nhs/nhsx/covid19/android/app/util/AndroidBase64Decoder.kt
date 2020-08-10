package uk.nhs.nhsx.covid19.android.app.util

import android.util.Base64

class AndroidBase64Decoder : Base64Decoder {
    override fun decode(string: String): String {
        return Base64.decode(string, Base64.DEFAULT).toString(Charsets.UTF_8)
    }

    override fun decodeToBytes(string: String): ByteArray {
        return Base64.decode(string, Base64.DEFAULT)
    }

    override fun decodeUrl(string: String): String {
        return Base64.decode(string, Base64.URL_SAFE).toString(Charsets.UTF_8)
    }

    override fun decodeUrlToBytes(string: String): ByteArray {
        return Base64.decode(string, Base64.URL_SAFE)
    }
}
