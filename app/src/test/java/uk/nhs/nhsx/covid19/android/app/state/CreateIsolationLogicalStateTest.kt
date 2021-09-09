package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class CreateIsolationLogicalStateTest {

    private val calculateContactExpiryDate = mockk<CalculateContactExpiryDate>()
    private val calculateIndexExpiryDate = mockk<CalculateIndexExpiryDate>()
    private val calculateIndexStartDate = mockk<CalculateIndexStartDate>()

    private val maxIsolation = 20
    private val isolationConfiguration = DurationDays(maxIsolation = maxIsolation)

    private val fixedClock = Clock.fixed(Instant.parse("2020-01-15T10:00:00Z"), ZoneOffset.UTC)

    private val createIsolationLogicalState = CreateIsolationLogicalState(
        calculateContactExpiryDate,
        calculateIndexExpiryDate,
        calculateIndexStartDate
    )

    //region Never isolating
    @Test
    fun `create state without isolation data`() {
        val isolationState = IsolationState(
            isolationConfiguration = isolationConfiguration
        )

        val isolationLogicalState = createIsolationLogicalState(isolationState)

        assertEquals(isolationConfiguration, isolationLogicalState.isolationConfiguration)
        assertTrue(isolationLogicalState is NeverIsolating)
        assertNull(isolationLogicalState.negativeTest)
    }

    @Test
    fun `create state with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            testResult = negativeTestResult
        )

        val isolationLogicalState = createIsolationLogicalState(isolationState)

        assertEquals(isolationConfiguration, isolationLogicalState.isolationConfiguration)
        assertTrue(isolationLogicalState is NeverIsolating)
        assertEquals(NegativeTest(negativeTestResult), isolationLogicalState.negativeTest)
    }
    //endregion

    //region Contact case
    @Test
    fun `create state with contact, without opt-out, with hasAcknowledgedEndOfIsolation false`() {
        `create state with contact`(
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock).minusDays(4),
                notificationDate = LocalDate.now(fixedClock).minusDays(3),
            ),
            hasAcknowledgedEndOfIsolation = false
        )
    }

    @Test
    fun `create state with contact, with opt-out, with hasAcknowledgedEndOfIsolation false`() {
        `create state with contact`(
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock).minusDays(4),
                notificationDate = LocalDate.now(fixedClock).minusDays(3),
                optOutOfContactIsolation = OptOutOfContactIsolation(LocalDate.now(fixedClock).minusDays(2))
            ),
            hasAcknowledgedEndOfIsolation = false
        )
    }

    @Test
    fun `create state with contact, with opt-out, with hasAcknowledgedEndOfIsolation true`() {
        `create state with contact`(
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock).minusDays(4),
                notificationDate = LocalDate.now(fixedClock).minusDays(3),
                optOutOfContactIsolation = OptOutOfContactIsolation(LocalDate.now(fixedClock).minusDays(2))
            ),
            hasAcknowledgedEndOfIsolation = true
        )
    }

    private fun `create state with contact`(
        contact: Contact,
        hasAcknowledgedEndOfIsolation: Boolean
    ) {
        val isolationState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = contact,
            hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
        )

        val contactExpiryDate = LocalDate.now(fixedClock).minusDays(2)
        every { calculateContactExpiryDate(contact, isolationConfiguration) } returns contactExpiryDate

        val isolationLogicalState = createIsolationLogicalState(isolationState)

        assertEquals(isolationConfiguration, isolationLogicalState.isolationConfiguration)
        assertTrue(isolationLogicalState is PossiblyIsolating)

        val expectedContactCase = ContactCase(
            exposureDate = contact.exposureDate,
            notificationDate = contact.notificationDate,
            optOutOfContactIsolation = contact.optOutOfContactIsolation,
            expiryDate = contactExpiryDate
        )
        assertEquals(expectedContactCase, isolationLogicalState.contactCase)

        assertNull(isolationLogicalState.indexInfo)
        assertEquals(hasAcknowledgedEndOfIsolation, isolationLogicalState.hasAcknowledgedEndOfIsolation)
        assertEquals(expectedContactCase.startDate, isolationLogicalState.startDate)
        assertEquals(expectedContactCase.expiryDate, isolationLogicalState.expiryDate)
    }

    @Test
    fun `create state with contact and negative test`() {
        val contact = Contact(
            exposureDate = LocalDate.now(fixedClock).minusDays(4),
            notificationDate = LocalDate.now(fixedClock).minusDays(3),
        )
        val hasAcknowledgedEndOfIsolation = false

        val isolationState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = contact,
            testResult = negativeTestResult,
            hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
        )

        val contactExpiryDate = LocalDate.now(fixedClock).minusDays(2)
        every { calculateContactExpiryDate(contact, isolationConfiguration) } returns contactExpiryDate

        val isolationLogicalState = createIsolationLogicalState(isolationState)

        assertEquals(isolationConfiguration, isolationLogicalState.isolationConfiguration)
        assertTrue(isolationLogicalState is PossiblyIsolating)

        val expectedContactCase = ContactCase(
            exposureDate = contact.exposureDate,
            notificationDate = contact.notificationDate,
            optOutOfContactIsolation = contact.optOutOfContactIsolation,
            expiryDate = contactExpiryDate
        )
        assertEquals(expectedContactCase, isolationLogicalState.contactCase)

        assertEquals(NegativeTest(negativeTestResult), isolationLogicalState.indexInfo)
        assertEquals(hasAcknowledgedEndOfIsolation, isolationLogicalState.hasAcknowledgedEndOfIsolation)
        assertEquals(expectedContactCase.startDate, isolationLogicalState.startDate)
        assertEquals(expectedContactCase.expiryDate, isolationLogicalState.expiryDate)
    }
    //endregion

    //region Index case
    @Test
    fun `create state with positive test result`() {
        `create state with index case`(
            selfAssessment = null,
            testResult = positiveTestResult,
            hasAcknowledgedEndOfIsolation = false
        )
    }

    @Test
    fun `create state with self-assessment`() {
        `create state with index case`(
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock).minusDays(3),
                onsetDate = LocalDate.now(fixedClock).minusDays(5)
            ),
            testResult = null,
            hasAcknowledgedEndOfIsolation = false
        )
    }

    @Test
    fun `create state with self-assessment and positive test result`() {
        `create state with index case`(
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock).minusDays(3),
                onsetDate = LocalDate.now(fixedClock).minusDays(5)
            ),
            testResult = positiveTestResult,
            hasAcknowledgedEndOfIsolation = false
        )
    }

    @Test
    fun `create state with self-assessment and negative test result`() {
        `create state with index case`(
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock).minusDays(3),
                onsetDate = LocalDate.now(fixedClock).minusDays(5)
            ),
            testResult = negativeTestResult,
            hasAcknowledgedEndOfIsolation = false
        )
    }

    @Test
    fun `create state with self-assessment and negative test result, with hasAcknowledgedEndOfIsolation true`() {
        `create state with index case`(
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock).minusDays(3),
                onsetDate = LocalDate.now(fixedClock).minusDays(5)
            ),
            testResult = negativeTestResult,
            hasAcknowledgedEndOfIsolation = true
        )
    }

    private fun `create state with index case`(
        selfAssessment: SelfAssessment?,
        testResult: AcknowledgedTestResult?,
        hasAcknowledgedEndOfIsolation: Boolean
    ) {
        if (selfAssessment == null && testResult?.isPositive() != true) {
            fail("This test is meant for data that will generate an index case. It must either have a self-assessment or a positive test result")
        }

        val isolationState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = selfAssessment,
            testResult = testResult,
            hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
        )

        val indexStartDate = LocalDate.now(fixedClock).minusDays(5)
        every { calculateIndexStartDate(selfAssessment, testResult) } returns indexStartDate

        val indexExpiryDate = LocalDate.now(fixedClock).plusDays(2)
        every { calculateIndexExpiryDate(selfAssessment, testResult, isolationConfiguration) } returns indexExpiryDate

        val isolationLogicalState = createIsolationLogicalState(isolationState)

        assertEquals(isolationConfiguration, isolationLogicalState.isolationConfiguration)
        assertTrue(isolationLogicalState is PossiblyIsolating)

        val expectedIndexCase = IndexCase(
            selfAssessment = selfAssessment,
            testResult = testResult,
            startDate = indexStartDate,
            expiryDate = indexExpiryDate
        )
        assertEquals(expectedIndexCase, isolationLogicalState.indexInfo)

        assertNull(isolationLogicalState.contactCase)
        assertEquals(hasAcknowledgedEndOfIsolation, isolationLogicalState.hasAcknowledgedEndOfIsolation)
        assertEquals(expectedIndexCase.startDate, isolationLogicalState.startDate)
        assertEquals(expectedIndexCase.expiryDate, isolationLogicalState.expiryDate)
    }
    //endregion

    //region Both cases, cap expiry date
    @Test
    fun `create state with index case and contact case, no capping`() {
        val indexExpiryDate = LocalDate.now(fixedClock).plusDays(maxIsolation.toLong())
        val contactExpiryDate = LocalDate.now(fixedClock).plusDays(maxIsolation.toLong() - 1)

        `create state with index and contact case, cap expiry date`(
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock),
                onsetDate = LocalDate.now(fixedClock)
            ),
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
            ),
            indexStartDate = LocalDate.now(fixedClock),
            uncappedIndexExpiryDate = indexExpiryDate,
            uncappedContactExpiryDate = contactExpiryDate,
            cappedIndexExpiryDate = indexExpiryDate,
            cappedContactExpiryDate = contactExpiryDate,
            isolationStartDate = LocalDate.now(fixedClock),
            isolationExpiryDate = indexExpiryDate
        )
    }

    @Test
    fun `create state with index case and contact case, cap index case`() {
        val indexExpiryDate = LocalDate.now(fixedClock).plusDays(maxIsolation.toLong() + 10)
        val contactExpiryDate = LocalDate.now(fixedClock).plusDays(maxIsolation.toLong() - 1)
        val cappedExpiryDate = LocalDate.now(fixedClock).plusDays(maxIsolation.toLong())

        `create state with index and contact case, cap expiry date`(
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock),
                onsetDate = LocalDate.now(fixedClock)
            ),
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
            ),
            indexStartDate = LocalDate.now(fixedClock),
            uncappedIndexExpiryDate = indexExpiryDate,
            uncappedContactExpiryDate = contactExpiryDate,
            cappedIndexExpiryDate = cappedExpiryDate,
            cappedContactExpiryDate = contactExpiryDate,
            isolationStartDate = LocalDate.now(fixedClock),
            isolationExpiryDate = cappedExpiryDate
        )
    }

    @Test
    fun `create state with index case and contact case, cap contact case`() {
        val indexExpiryDate = LocalDate.now(fixedClock).plusDays(maxIsolation.toLong())
        val contactExpiryDate = LocalDate.now(fixedClock).plusDays(maxIsolation.toLong() + 10)
        val cappedExpiryDate = LocalDate.now(fixedClock).plusDays(maxIsolation.toLong())

        `create state with index and contact case, cap expiry date`(
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock),
                onsetDate = LocalDate.now(fixedClock)
            ),
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
            ),
            indexStartDate = LocalDate.now(fixedClock),
            uncappedIndexExpiryDate = indexExpiryDate,
            uncappedContactExpiryDate = contactExpiryDate,
            cappedIndexExpiryDate = indexExpiryDate,
            cappedContactExpiryDate = cappedExpiryDate,
            isolationStartDate = LocalDate.now(fixedClock),
            isolationExpiryDate = cappedExpiryDate
        )
    }

    private fun `create state with index and contact case, cap expiry date`(
        selfAssessment: SelfAssessment,
        contact: Contact,
        indexStartDate: LocalDate,
        uncappedIndexExpiryDate: LocalDate,
        uncappedContactExpiryDate: LocalDate,
        cappedIndexExpiryDate: LocalDate,
        cappedContactExpiryDate: LocalDate,
        isolationStartDate: LocalDate,
        isolationExpiryDate: LocalDate
    ) {
        val hasAcknowledgedEndOfIsolation = false
        val isolationState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = selfAssessment,
            contact = contact,
            hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
        )
        every { calculateContactExpiryDate(contact, isolationConfiguration) } returns uncappedContactExpiryDate
        every { calculateIndexStartDate(selfAssessment, testResult = null) } returns indexStartDate
        every { calculateIndexExpiryDate(selfAssessment, testResult = null, isolationConfiguration) } returns uncappedIndexExpiryDate

        val isolationLogicalState = createIsolationLogicalState(isolationState)

        assertEquals(isolationConfiguration, isolationLogicalState.isolationConfiguration)
        assertTrue(isolationLogicalState is PossiblyIsolating)

        val expectedContactCase = ContactCase(
            exposureDate = contact.exposureDate,
            notificationDate = contact.notificationDate,
            optOutOfContactIsolation = contact.optOutOfContactIsolation,
            expiryDate = cappedContactExpiryDate
        )
        assertEquals(expectedContactCase, isolationLogicalState.contactCase)

        val expectedIndexCase = IndexCase(
            selfAssessment = selfAssessment,
            testResult = null,
            startDate = indexStartDate,
            expiryDate = cappedIndexExpiryDate
        )
        assertEquals(expectedIndexCase, isolationLogicalState.indexInfo)
        assertEquals(hasAcknowledgedEndOfIsolation, isolationLogicalState.hasAcknowledgedEndOfIsolation)
        assertEquals(isolationStartDate, isolationLogicalState.startDate)
        assertEquals(isolationExpiryDate, isolationLogicalState.expiryDate)
    }
    //endregion

    //region Impossible combinations
    @Test
    fun `create state with self-assessment but cannot compute index start date`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(3),
            onsetDate = LocalDate.now(fixedClock).minusDays(5)
        )
        val isolationState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = selfAssessment,
            testResult = null,
            hasAcknowledgedEndOfIsolation = false
        )

        // This shouldn't happen but if it somehow does, we discard the self-assessment
        every { calculateIndexStartDate(selfAssessment, testResult = null) } returns null

        val indexExpiryDate = LocalDate.now(fixedClock).plusDays(2)
        every { calculateIndexExpiryDate(selfAssessment, testResult = null, isolationConfiguration) } returns indexExpiryDate

        val isolationLogicalState = createIsolationLogicalState(isolationState)

        assertEquals(isolationConfiguration, isolationLogicalState.isolationConfiguration)
        assertTrue(isolationLogicalState is NeverIsolating)
        assertNull(isolationLogicalState.negativeTest)
    }

    @Test
    fun `create state with self-assessment but cannot compute index expiry date`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(3),
            onsetDate = LocalDate.now(fixedClock).minusDays(5)
        )
        val isolationState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = selfAssessment,
            testResult = null,
            hasAcknowledgedEndOfIsolation = false
        )

        val indexStartDate = LocalDate.now(fixedClock).minusDays(5)
        every { calculateIndexStartDate(selfAssessment, testResult = null) } returns indexStartDate

        // This shouldn't happen but if it somehow does, we discard the self-assessment
        every { calculateIndexExpiryDate(selfAssessment, testResult = null, isolationConfiguration) } returns null

        val isolationLogicalState = createIsolationLogicalState(isolationState)

        assertEquals(isolationConfiguration, isolationLogicalState.isolationConfiguration)
        assertTrue(isolationLogicalState is NeverIsolating)
        assertNull(isolationLogicalState.negativeTest)
    }
    //endregion

    //region Test data and helpers
    private val negativeTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock).minusDays(3),
        testResult = NEGATIVE,
        testKitType = LAB_RESULT,
        acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
        requiresConfirmatoryTest = false
    )

    private val positiveTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock).minusDays(3),
        testResult = POSITIVE,
        testKitType = RAPID_RESULT,
        acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
        requiresConfirmatoryTest = true,
        confirmedDate = LocalDate.now(fixedClock).minusDays(1),
        confirmatoryDayLimit = 2,
        confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED
    )
    //endregion
}
