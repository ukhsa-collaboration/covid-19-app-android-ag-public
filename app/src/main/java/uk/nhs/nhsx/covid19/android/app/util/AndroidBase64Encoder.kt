package uk.nhs.nhsx.covid19.android.app.util

import android.util.Base64

class AndroidBase64Encoder : Base64Encoder {
    override fun encodeUrl(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.URL_SAFE or Base64.NO_WRAP)
    }
}
