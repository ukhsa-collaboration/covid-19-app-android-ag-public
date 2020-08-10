package uk.nhs.nhsx.covid19.android.app.util

import org.json.JSONObject
import org.junit.Test

class AndroidBase64DecoderTest {

    @Test
    fun decodeUrl() {
        val testSubject = AndroidBase64Decoder()

        val decodeUrl =
            testSubject.decodeUrl("eyJpZCI6IjRXVDU5TTVZIiwidHlwIjoiZW50cnkiLCJvcG4iOiJJc2xlIE9mIE1hbiBHb3Zlcm5tZW50IE9mZmljZSBPZiBIdW1hbiBSZXNvdXJjZXMiLCJhZHIiOiJJc2xlIE9mIE1hbiBHb3Zlcm5tZW50IE9mZmljZSBPZiBIdW1hbiBSZXNvdXJjZXNcbkxlYXJuaW5nIEVkdWNhdGlvbiBhbmQgRGV2ZWxvcG1lbnRcblRoZSBMb2RnZSBFZHVjYXRpb24gYW5kIFRyYWluaW5nIENlbnRyZSwgQnJhZGRhbiBSb2FkLCBTdHJhbmcsIERvdWdsYXNcbklTTEUgT0YgTUFOLCBJTTQ0UU4iLCJwdCI6IklTTEUgT0YgTUFOIiwicGMiOiJJTTQgNFFOIn0")
        val jsonObject = JSONObject(decodeUrl)

        assert(jsonObject.has("id"))
    }
}
