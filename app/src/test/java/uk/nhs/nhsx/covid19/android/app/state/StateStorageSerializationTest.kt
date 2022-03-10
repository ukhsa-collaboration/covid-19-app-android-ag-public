package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.QUESTIONNAIRE
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StateStorageSerializationTest {

    private val moshi = Moshi.Builder()
        .add(LocalDateAdapter())
        .add(InstantAdapter())
        .build()

    private val testSubject = moshi.adapter(IsolationStateJson::class.java)

    @Test
    fun `serialize and deserialize never isolating`() {
        val stateJson = IsolationStateJson(
            configuration = IsolationConfiguration()
        )
        val res: String = testSubject.toJson(stateJson)

        Assert.assertEquals(stateJson, testSubject.fromJson(res))
    }

    @Test
    fun `serialize and deserialize isolating with all fields set`() {
        val fixedClock = Clock.fixed(Instant.parse("2020-01-10T10:00:00Z"), ZoneOffset.UTC)

        val original = IsolationStateJson(
            configuration = IsolationConfiguration(),
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(2),
                optOutOfContactIsolation = OptOutOfContactIsolation(LocalDate.now(fixedClock).minusDays(3)),
            ),
            testResult = AcknowledgedTestResult(
                testEndDate = LocalDate.now(fixedClock).minusDays(5),
                acknowledgedDate = LocalDate.now(fixedClock).minusDays(6),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false,
                confirmedDate = LocalDate.now(fixedClock).minusDays(7)
            ),
            symptomatic = SymptomaticCase(
                selfDiagnosisDate = LocalDate.now(fixedClock).minusDays(8),
                onsetDate = LocalDate.now(fixedClock).minusDays(9)
            ),
            hasAcknowledgedEndOfIsolation = true
        )
        val res = testSubject.toJson(original)

        Assert.assertEquals(original, testSubject.fromJson(res))
    }

    @Test
    fun `serialize and deserialize isolating with optional fields missing`() {
        val fixedClock = Clock.fixed(Instant.parse("2020-01-10T10:00:00Z"), ZoneOffset.UTC)

        val original = IsolationStateJson(
            configuration = IsolationConfiguration(),
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(2),
            ),
            testResult = AcknowledgedTestResult(
                testEndDate = LocalDate.now(fixedClock).minusDays(5),
                acknowledgedDate = LocalDate.now(fixedClock).minusDays(6),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false
            ),
            symptomatic = SymptomaticCase(
                selfDiagnosisDate = LocalDate.now(fixedClock).minusDays(8)
            ),
            hasAcknowledgedEndOfIsolation = false
        )
        val res = testSubject.toJson(original)

        Assert.assertEquals(original, testSubject.fromJson(res))
    }

    @Test
    fun `deserialize invalid state`() {
        assertFailsWith<JsonDataException> {
            testSubject.fromJson("""{"corruptString":"invalidValue"}""")
        }
    }

    @Test
    fun `deserialize contact case with opt-out defaults if no reason exists`() {
        val configuration =
            """{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14,"indexCaseSinceTestResultEndDate":11,"testResultPollingTokenRetentionPeriod":28}"""
        val exposureDate = "2020-01-09"
        val notificationDate = "2020-01-08"
        val optOutOfContactIsolationDate = "2020-01-07"
        val contactWithOptOutOfContactIsolation =
            """{"exposureDate":"$exposureDate","notificationDate":"$notificationDate","optOutOfContactIsolation":{"date":"$optOutOfContactIsolationDate"}}"""
        val state =
            """{"configuration":$configuration,"contact":$contactWithOptOutOfContactIsolation,"hasAcknowledgedEndOfIsolation":true,"version":1}"""

        val isolationStateJson = testSubject.fromJson(state)
        val expected = IsolationStateJson(
            configuration = IsolationConfiguration(),
            contact = Contact(
                exposureDate = LocalDate.parse(exposureDate),
                notificationDate = LocalDate.parse(notificationDate),
                optOutOfContactIsolation = OptOutOfContactIsolation(LocalDate.parse(optOutOfContactIsolationDate), reason = QUESTIONNAIRE),
            ),
            hasAcknowledgedEndOfIsolation = true
        )
        assertEquals(expected, isolationStateJson)
    }
}
