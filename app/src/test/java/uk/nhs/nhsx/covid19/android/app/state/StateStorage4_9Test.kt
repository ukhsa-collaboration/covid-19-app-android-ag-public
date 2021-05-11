@file:Suppress("DEPRECATION", "ClassName")

package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Default4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.ContactCase4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.IndexCase4_9
import uk.nhs.nhsx.covid19.android.app.state.StateJson4_9.Companion.stateMoshiAdapter4_9
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StateStorage4_9Test {

    private val moshi = Moshi.Builder()
        .add(stateMoshiAdapter4_9)
        .add(LocalDateAdapter())
        .add(InstantAdapter())
        .build()

    private val statusStringStorage = mockk<StateStringStorage4_9>(relaxed = true)
    private val isolationConfigurationProvider =
        mockk<IsolationConfigurationProvider>(relaxed = true)

    private val testSubject =
        StateStorage4_9(
            statusStringStorage,
            isolationConfigurationProvider,
            moshi
        )

    private val durationDays = DurationDays()

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun `parses default case properly`() {
        every { statusStringStorage.prefsValue } returns """[$DEFAULT_V2]"""

        val parsedState = testSubject.state

        assertEquals(Default4_9(), parsedState)
    }

    @Test
    fun `parses default case v1 properly`() {
        every { statusStringStorage.prefsValue } returns """[$DEFAULT_V1]"""

        val parsedState = testSubject.state

        assertEquals(Default4_9(), parsedState)
    }

    @Test
    fun `parses default case with previous contact case v2 properly`() {
        every { statusStringStorage.prefsValue } returns """[$DEFAULT_WITH_PREVIOUS_CONTACT_V2]"""

        val parsedState = testSubject.state

        assertEquals(
            Default4_9(
                previousIsolation = Isolation4_9(
                    startDate,
                    durationDays,
                    contactCase = ContactCase4_9(startDate, null, expiryDate)
                )
            ),
            parsedState
        )
    }

    @Test
    fun `parses index case properly`() {
        every { statusStringStorage.prefsValue } returns """[$INDEX_CASE_V4]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation4_9(startDate, durationDays, indexCase = IndexCase4_9(onsetDate, expiryDate, true)),
            parsedState
        )
    }

    @Test
    fun `parses index case v3 migration properly`() {
        every { statusStringStorage.prefsValue } returns """[$INDEX_CASE_V3]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation4_9(startDate, durationDays, indexCase = IndexCase4_9(onsetDate, expiryDate, true)),
            parsedState
        )
    }

    @Test
    fun `parses index case v2 migration properly`() {
        every { statusStringStorage.prefsValue } returns """[$INDEX_CASE_V2]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation4_9(startDate, durationDays, indexCase = IndexCase4_9(onsetDate, expiryDate, true)),
            parsedState
        )
    }

    @Test
    fun `parses index case v1 migration properly`() {
        every { statusStringStorage.prefsValue } returns """[$INDEX_CASE_V1]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation4_9(startDate, durationDays, indexCase = IndexCase4_9(onsetDate, expiryDate, true)),
            parsedState
        )
    }

    @Test
    fun `parses contact case properly`() {
        every { statusStringStorage.prefsValue } returns """[$CONTACT_CASE_V4]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation4_9(startDate, durationDays, contactCase = ContactCase4_9(startDate, notificationDate, expiryDate, dailyContactTestingOptInDate)),
            parsedState
        )
    }

    @Test
    fun `parses contact v3 case properly`() {
        every { statusStringStorage.prefsValue } returns """[$CONTACT_CASE_V3]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation4_9(startDate, durationDays, contactCase = ContactCase4_9(startDate, null, expiryDate)),
            parsedState
        )
    }

    @Test
    fun `parses contact case v2 migration properly`() {
        every { statusStringStorage.prefsValue } returns """[$CONTACT_CASE_V2]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation4_9(startDate, durationDays, contactCase = ContactCase4_9(startDate, null, expiryDate)),
            parsedState
        )
    }

    @Test
    fun `parses contact case v1 migration properly`() {
        every { statusStringStorage.prefsValue } returns """[$CONTACT_CASE_V1]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation4_9(startDate, durationDays, contactCase = ContactCase4_9(startDate, null, expiryDate)),
            parsedState
        )
    }

    @Test
    fun `parses invalid data as null`() {
        every { statusStringStorage.prefsValue } returns """[$INVALID_CASE]"""

        val parsedState = testSubject.state

        assertNull(parsedState)
    }

    @Test
    fun `parses partial data as null`() {
        every { statusStringStorage.prefsValue } returns """[{"type":"PositiveCase","testDate":"2020-05-21T10:00:00Z","version":1}]"""

        val parsedState = testSubject.state

        assertNull(parsedState)
    }

    companion object {
        private val startDate = Instant.parse("2020-05-21T10:00:00Z")
        private val expiryDate = LocalDate.of(2020, 7, 22)
        private val dailyContactTestingOptInDate = LocalDate.of(2020, 7, 22)
        private val notificationDate = Instant.parse("2020-05-22T10:00:00Z")
        private val onsetDate = LocalDate.parse("2020-05-21")

        const val DEFAULT_V1 =
            """{"type":"Default","version":1}"""
        const val DEFAULT_V2 =
            """{"type":"Default","version":2}"""
        const val DEFAULT_WITH_PREVIOUS_CONTACT_V2 =
            """{"type":"Default","previousIsolation":{"isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-06-11","contactCase":{"startDate":"2020-05-21T10:00:00Z","expiryDate":"2020-07-22"},"isolationConfiguration":{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14,"indexCaseSinceTestResultEndDate":11}},"version":2}"""

        const val INDEX_CASE_V1 =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-07-22","indexCase":{"symptomsOnsetDate":"2020-05-21"},"version":1}"""
        const val INDEX_CASE_V2 =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-06-11","indexCase":{"symptomsOnsetDate":"2020-05-21","expiryDate":"2020-07-22"},"isolationConfiguration":{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14},"version":2}"""
        const val INDEX_CASE_V3 =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-06-11","indexCase":{"symptomsOnsetDate":"2020-05-21","expiryDate":"2020-07-22","selfAssessment":true},"isolationConfiguration":{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14},"version":3}"""
        const val INDEX_CASE_V4 =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-06-11","indexCase":{"symptomsOnsetDate":"2020-05-21","expiryDate":"2020-07-22","selfAssessment":true},"isolationConfiguration":{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14,"indexCaseSinceTestResultEndDate":11},"version":4}"""

        const val CONTACT_CASE_V1 =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-07-22","contactCase":{"startDate":"2020-05-21T10:00:00Z"},"version":1}"""
        const val CONTACT_CASE_V2 =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-06-11","contactCase":{"startDate":"2020-05-21T10:00:00Z","expiryDate":"2020-07-22"},"isolationConfiguration":{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14},"version":2}"""
        const val CONTACT_CASE_V3 =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-06-11","contactCase":{"startDate":"2020-05-21T10:00:00Z","expiryDate":"2020-07-22"},"isolationConfiguration":{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14},"version":3}"""
        const val CONTACT_CASE_V4 =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-06-11","contactCase":{"startDate":"2020-05-21T10:00:00Z","notificationDate":"2020-05-22T10:00:00Z","expiryDate":"2020-07-22","dailyContactTestingOptInDate":"2020-07-22"},"isolationConfiguration":{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14,"indexCaseSinceTestResultEndDate":11},"version":4}"""

        const val INVALID_CASE =
            """{"type":"UnknownCase","testDate":1594733801229,"expiryDate":1595338601229,"version":1}"""
    }
}
