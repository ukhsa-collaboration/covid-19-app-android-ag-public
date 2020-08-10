package uk.nhs.nhsx.covid19.android.app.utils

import uk.nhs.nhsx.covid19.android.app.util.Base64Decoder
import java.util.Base64

class Java8Base64Decoder : Base64Decoder {
    override fun decode(string: String): String {
        return Base64.getDecoder().decode(string).toString(Charsets.UTF_8)
    }

    override fun decodeToBytes(string: String): ByteArray {
        return Base64.getDecoder().decode(string)
    }

    override fun decodeUrl(string: String): String {
        return Base64.getUrlDecoder().decode(string).toString(Charsets.UTF_8)
    }

    override fun decodeUrlToBytes(string: String): ByteArray {
        return Base64.getUrlDecoder().decode(string)
    }
}
