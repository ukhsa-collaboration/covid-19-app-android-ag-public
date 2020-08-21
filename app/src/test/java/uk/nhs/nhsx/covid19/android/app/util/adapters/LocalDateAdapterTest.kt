package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.Moshi
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class LocalDateAdapterTest {

    private val moshi = Moshi.Builder().add(LocalDateAdapter()).build()

    private val testSubject = moshi.adapter(LocalDate::class.java)

    private val localDateString = "2007-12-03"
    private val localDateJson = "\"$localDateString\""

    @Test
    fun `convert LocalDate to json`() {
        val localDate = LocalDate.parse(localDateString)

        val result = testSubject.toJson(localDate)

        assertEquals(localDateJson, result)
    }

    @Test
    fun `parse LocalDate from json`() {
        val result = testSubject.fromJson(localDateJson)

        assertEquals(LocalDate.parse(localDateString), result)
    }
}
