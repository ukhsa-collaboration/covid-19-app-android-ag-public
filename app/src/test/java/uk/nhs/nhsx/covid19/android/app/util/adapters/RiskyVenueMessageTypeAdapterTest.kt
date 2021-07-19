package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.Moshi
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RiskyVenueMessageTypeAdapterTest {

    private val moshi = Moshi.Builder().add(RiskyVenueMessageTypeAdapter()).build()

    private val testSubject = moshi.adapter(RiskyVenueMessageType::class.java)

    private val messageTypeJson =
        """"M2""""
    private val unknownMessageTypeJson =
        """"this-message-type-does-not-exist""""

    @Test
    fun `convert MessageType to json`() {
        val result = testSubject.toJson(BOOK_TEST)

        assertEquals(messageTypeJson, result)
    }

    @Test
    fun `parse MessageType from json`() {
        val result = testSubject.fromJson(messageTypeJson)

        assertNotNull(result)

        assertEquals(BOOK_TEST, result)
    }

    @Test
    fun `parse unknown MessageType from json falls back to INFORM`() {
        val result = testSubject.fromJson(unknownMessageTypeJson)

        assertNotNull(result)

        assertEquals(INFORM, result)
    }
}
