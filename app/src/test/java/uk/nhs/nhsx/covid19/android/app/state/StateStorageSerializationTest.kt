package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.StateJson.Companion.stateMoshiAdapter
import uk.nhs.nhsx.covid19.android.app.state.StateJson.ContactCaseJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.DefaultJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.IndexCaseJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.IsolationJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.PreviousIsolationJson
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertFailsWith

class StateStorageSerializationTest {

    private val moshi = Moshi.Builder()
        .add(stateMoshiAdapter)
        .add(LocalDateAdapter())
        .add(InstantAdapter())
        .build()

    private val testSubject = moshi.adapter(StateJson::class.java)

    @Test
    fun defaultStateSerialization() {

        val defaultJson = DefaultJson(
            previousIsolation = PreviousIsolationJson(
                isolationStart = Instant.now(), expiryDate = LocalDate.now(),
                indexCase = IndexCase(LocalDate.now(), LocalDate.now(), false),
                contactCase = ContactCase(Instant.now(), LocalDate.now()),
                isolationConfiguration = DurationDays()
            )
        )
        val res: String = testSubject.toJson(defaultJson)

        assertEquals(defaultJson, testSubject.fromJson(res))
    }

    private val start = Instant.ofEpochMilli(1594733801229)
    private val expiryDate = LocalDate.of(2020, 7, 22)

    @Test
    fun isolationStateSerialization() {
        val original = IsolationJson(
            start,
            expiryDate,
            indexCase = IndexCaseJson(
                LocalDate.parse("2020-08-08"),
                expiryDate,
                true
            ),
            contactCase = ContactCaseJson(Instant.now().minus(1, DAYS), expiryDate),
            isolationConfiguration = DurationDays()
        )
        val res = testSubject.toJson(original)

        assertEquals(original, testSubject.fromJson(res))
    }

    @Test
    fun invalidStateDeserialization() {

        assertFailsWith<JsonDataException> {
            testSubject.fromJson("""{"type":"INVALID","startDate":1594733801229,"expiryDate":1595338601229,"version":1}""")
        }
    }
}
