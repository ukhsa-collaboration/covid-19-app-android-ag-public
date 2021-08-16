@file:Suppress("DEPRECATION", "ClassName")

package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Default4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.ContactCase4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.IndexCase4_9
import uk.nhs.nhsx.covid19.android.app.state.StateStorage4_9.Companion.STATE_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.Instant
import java.time.LocalDate

class StateStorage4_9Test : ProviderTest<StateStorage4_9, State4_9?>() {

    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    override val getTestSubject: (Moshi, SharedPreferences) -> StateStorage4_9 =
        { moshi, sharedPreferences ->
            StateStorage4_9(
                isolationConfigurationProvider = isolationConfigurationProvider,
                _moshi = moshi,
                sharedPreferences = sharedPreferences
            )
        }
    override val property = StateStorage4_9::state
    override val key = STATE_KEY
    override val defaultValue: State4_9? = null
    override val expectations: List<ProviderTestExpectation<State4_9?>> = listOf(
        ProviderTestExpectation(json = "[$DEFAULT_V2]", objectValue = Default4_9(), direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$DEFAULT_V1]", objectValue = Default4_9(), direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$DEFAULT_WITH_PREVIOUS_CONTACT_V2]", objectValue = defaultWithPreviousContact, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$INDEX_CASE_V4]", objectValue = indexCase, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$INDEX_CASE_V3]", objectValue = indexCase, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$INDEX_CASE_V2]", objectValue = indexCase, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$INDEX_CASE_V1]", objectValue = indexCase, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$CONTACT_CASE_V4]", objectValue = contactCaseV4, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$CONTACT_CASE_V3]", objectValue = contactCase, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$CONTACT_CASE_V2]", objectValue = contactCase, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$CONTACT_CASE_V1]", objectValue = contactCase, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = "[$INVALID_CASE]", objectValue = null, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = PARTIAL_JSON, objectValue = null, direction = JSON_TO_OBJECT)
    )

    @BeforeEach
    fun setUpMock() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun `clear sets state back to empty`() {
        testSubject.clear()

        assertSharedPreferenceSetsValue(null)
    }

    companion object {
        private val durationDays = DurationDays()
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
        private val defaultWithPreviousContact = Default4_9(
            previousIsolation = Isolation4_9(
                startDate,
                durationDays,
                contactCase = ContactCase4_9(startDate, null, expiryDate)
            )
        )
        private val indexCase = Isolation4_9(startDate, durationDays, indexCase = IndexCase4_9(onsetDate, expiryDate, true))
        private val contactCaseV4 = Isolation4_9(
            startDate,
            durationDays,
            contactCase = ContactCase4_9(startDate, notificationDate, expiryDate, dailyContactTestingOptInDate)
        )

        private val contactCase = Isolation4_9(startDate, durationDays, contactCase = ContactCase4_9(startDate, null, expiryDate))
        private const val PARTIAL_JSON =
            """[{"type":"PositiveCase","testDate":"2020-05-21T10:00:00Z","version":1}]"""
    }
}
