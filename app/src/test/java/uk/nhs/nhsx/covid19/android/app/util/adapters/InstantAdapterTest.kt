package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.Moshi
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class InstantAdapterTest {

    private val moshi = Moshi.Builder().add(InstantAdapter()).build()

    private val testSubject = moshi.adapter(Instant::class.java)

    private val isoInstantString = "2007-12-03T10:15:30Z"
    private val isoInstantJson = "\"$isoInstantString\""

    @Test
    fun `convert Instant to json`() {
        val instant = Instant.parse(isoInstantString)

        val result = testSubject.toJson(instant)

        assertEquals(isoInstantJson, result)
    }

    @Test
    fun `parse Instant from json`() {
        val result = testSubject.fromJson(isoInstantJson)

        assertEquals(Instant.parse(isoInstantString), result)
    }
}
