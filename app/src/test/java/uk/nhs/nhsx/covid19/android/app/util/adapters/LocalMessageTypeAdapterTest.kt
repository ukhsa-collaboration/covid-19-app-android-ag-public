package uk.nhs.nhsx.covid19.android.app.util.adapters

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageType.UNKNOWN
import kotlin.test.assertEquals

class LocalMessageTypeAdapterTest {
    private val testSubject = LocalMessageTypeAdapter()

    private val localMessageType = LocalMessageType.NOTIFICATION
    private val localMessageTypeJson = "notification"
    private val unsupportedLocalMessageTypeJson = "random-string"

    @Test
    fun `toJson converts LocalMessageType to json string`() {
        assertEquals(localMessageTypeJson, testSubject.toJson(localMessageType))
    }

    @Test
    fun `fromJson converts json string to LocalMessageType`() {
        assertEquals(localMessageType, testSubject.fromJson(localMessageTypeJson))
    }

    @Test
    fun `fromJson converts unsupported json string to UNKNOWN`() {
        assertEquals(UNKNOWN, testSubject.fromJson(unsupportedLocalMessageTypeJson))
    }
}
