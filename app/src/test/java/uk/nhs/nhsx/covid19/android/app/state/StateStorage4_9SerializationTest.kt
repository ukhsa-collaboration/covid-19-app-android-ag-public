@file:Suppress("DEPRECATION", "ClassName")

package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.ContactCase4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.IndexCase4_9
import uk.nhs.nhsx.covid19.android.app.state.StateJson4_9.ContactCaseJson4_9
import uk.nhs.nhsx.covid19.android.app.state.StateJson4_9.DefaultJson4_9
import uk.nhs.nhsx.covid19.android.app.state.StateJson4_9.IndexCaseJson4_9
import uk.nhs.nhsx.covid19.android.app.state.StateJson4_9.IsolationJson4_9
import uk.nhs.nhsx.covid19.android.app.state.StateJson4_9.PreviousIsolationJson4_9
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertFailsWith

class StateStorage4_9SerializationTest {

    private val moshi = Moshi.Builder()
        .add(StateJson4_9.stateMoshiAdapter4_9)
        .add(LocalDateAdapter())
        .add(InstantAdapter())
        .build()

    private val testSubject = moshi.adapter(StateJson4_9::class.java)

    @Test
    fun defaultStateSerialization() {
        val defaultJson = DefaultJson4_9(
            previousIsolation = PreviousIsolationJson4_9(
                isolationStart = Instant.now(), expiryDate = LocalDate.now(),
                indexCase = IndexCase4_9(LocalDate.now(), LocalDate.now(), false),
                contactCase = ContactCase4_9(Instant.now(), null, LocalDate.now()),
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
        val original = IsolationJson4_9(
            start,
            expiryDate,
            indexCase = IndexCaseJson4_9(
                LocalDate.parse("2020-08-08"),
                expiryDate,
                true
            ),
            contactCase = ContactCaseJson4_9(Instant.now().minus(1, DAYS), null, expiryDate),
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
