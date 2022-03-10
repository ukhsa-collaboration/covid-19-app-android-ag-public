package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsolationLogicalStateTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-01-15T10:00:00Z"), ZoneOffset.UTC)

    //region PossiblyIsolating.hasExpired
    @Test
    fun `hasExpired returns true when expiry date is yesterday`() {
        val expiryDateYesterday = PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).minusDays(1)
        )

        assertTrue(expiryDateYesterday.hasExpired(fixedClock))
    }

    @Test
    fun `hasExpired returns true when expiry date is today`() {
        val expiryDateToday = PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            ),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock)
        )

        assertTrue(expiryDateToday.hasExpired(fixedClock))
    }

    @Test
    fun `hasExpired returns false when expiry date is tomorrow`() {
        val expiryDateTomorrow = PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )

        assertFalse(expiryDateTomorrow.hasExpired(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.isActiveIndexCase
    @Test
    fun `isActiveIndexCase returns false when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns false when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFalse(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns false when index case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns true when index case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns true when index case expires tomorrow and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns true when index case expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.isActiveIndexCase(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.isActiveIndexCaseOnly
    @Test
    fun `isActiveIndexCaseOnly returns false when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns false when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFalse(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns false when index case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns true when index case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns false when index case expires tomorrow and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns true when index case expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.isActiveIndexCaseOnly(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.getActiveIndexCase
    @Test
    fun `getActiveIndexCase returns null when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertNull(testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns null when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertNull(testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns null when index case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertNull(testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns index case when index case expires tomorrow`() {
        val indexCase = IndexCase(
            selfAssessment = mockk(),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = indexCase
        )

        assertEquals(indexCase, testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns index case when index case expires tomorrow and has active contact case`() {
        val indexCase = IndexCase(
            selfAssessment = mockk(),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = indexCase
        )

        assertEquals(indexCase, testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns index case when index case expires tomorrow and has expired contact case`() {
        val indexCase = IndexCase(
            selfAssessment = mockk(),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = indexCase
        )

        assertEquals(indexCase, testSubject.getActiveIndexCase(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.isActiveContactCase
    @Test
    fun `isActiveContactCase returns false when only index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.isActiveContactCase(fixedClock))
    }

    @Test
    fun `isActiveContactCase returns false when contact case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.isActiveContactCase(fixedClock))
    }

    @Test
    fun `isActiveContactCase returns true when contact case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.isActiveContactCase(fixedClock))
    }

    @Test
    fun `isActiveContactCase returns true when contact case expires tomorrow and has index contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertTrue(testSubject.isActiveContactCase(fixedClock))
    }

    @Test
    fun `isActiveContactCase returns true when contact case expires tomorrow and has expired index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.isActiveContactCase(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.isActiveContactCaseOnly
    @Test
    fun `isActiveContactCaseOnly returns false when only index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.isActiveContactCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveContactCaseOnly returns false when contact case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.isActiveContactCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveContactCaseOnly returns true when contact case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.isActiveContactCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveContactCaseOnly returns false when contact case expires tomorrow and has active index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.isActiveContactCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveContactCaseOnly returns true when contact case expires tomorrow and has expired index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.isActiveContactCaseOnly(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.getActiveContactCase
    @Test
    fun `getActiveContactCase returns null when only index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertNull(testSubject.getActiveContactCase(fixedClock))
    }

    @Test
    fun `getActiveContactCase returns null when contact case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertNull(testSubject.getActiveContactCase(fixedClock))
    }

    @Test
    fun `getActiveContactCase returns contact case when contact case expires tomorrow`() {
        val contactCase = ContactCase(
            exposureDate = mockkDate(),
            notificationDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = contactCase
        )

        assertEquals(contactCase, testSubject.getActiveContactCase(fixedClock))
    }

    @Test
    fun `getActiveContactCase returns contact case when contact case expires tomorrow and has active index case`() {
        val contactCase = ContactCase(
            exposureDate = mockkDate(),
            notificationDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = contactCase,
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertEquals(contactCase, testSubject.getActiveContactCase(fixedClock))
    }

    @Test
    fun `getActiveContactCase returns contact case when contact case expires tomorrow and has expired index case`() {
        val contactCase = ContactCase(
            exposureDate = mockkDate(),
            notificationDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = contactCase,
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertEquals(contactCase, testSubject.getActiveContactCase(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.remembersIndexCase
    @Test
    fun `remembersIndexCase returns false when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns false when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFalse(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expired yesterday`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertTrue(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expires tomorrow and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCase())
    }
    //endregion

    //region PossiblyIsolating.remembersIndexCaseOnly
    @Test
    fun `remembersIndexCaseOnly returns false when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns false when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFalse(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns true when index case expired yesterday`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns true when index case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertTrue(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns true when index case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns false when index case expires tomorrow and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns false when index case expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.remembersIndexCaseOnly())
    }
    //endregion

    //region PossiblyIsolating.remembersIndexCaseWithSelfAssessment
    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expired yesterday`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expired yesterday and has positive test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires today and has positive test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires tomorrow and has positive test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when index case with positive test expired yesterday`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when index case with positive test expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when index case with positive test expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires tomorrow and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }
    //endregion

    //region PossiblyIsolating.remembersContactCase
    @Test
    fun `remembersContactCase returns false when only index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expired yesterday`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertTrue(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expires tomorrow and has index contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertTrue(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expires tomorrow and has expired index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.remembersContactCase())
    }
    //endregion

    //region PossiblyIsolating.remembersContactCaseOnly
    @Test
    fun `remembersContactCaseOnly returns false when only index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns true when contact case expired yesterday`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns true when contact case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertTrue(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns true when contact case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns false when contact case expires tomorrow and has active index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns false when contact case expires tomorrow and has expired index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertFalse(testSubject.remembersContactCaseOnly())
    }
    //endregion

    //region PossiblyIsolating.remembersBothCases
    @Test
    fun `remembersBothCases returns false when only index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns false when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns false when only contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFalse(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns true when expired contact case and expired index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns true when active contact case and expired index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns true when expired contact case and active index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns true when active contact case and active index case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.remembersBothCases())
    }
    //endregion

    //region PossiblyIsolating.getActiveTestResult
    @Test
    fun `getActiveTestResult returns null when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertNull(testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns null when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertNull(testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns null when index case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertNull(testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns test result when index case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns test result when index case expires tomorrow and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns test result when index case expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResult(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.getActiveTestResultIfPositive
    @Test
    fun `getActiveTestResultIfPositive returns null when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertNull(testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns null when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertNull(testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns null when index case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertNull(testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns test result when index case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns test result when index case expires tomorrow and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns test result when index case expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResultIfPositive(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.hasActivePositiveTestResult
    @Test
    fun `hasActivePositiveTestResult returns false when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns false when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFalse(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns false when index case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns true when index case with positive test result expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns true when index case with positive test result expires tomorrow and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns true when index case with positive test result expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.hasActivePositiveTestResult(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.hasCompletedPositiveTestResult
    @Test
    fun `hasCompletedPositiveTestResult returns false when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.hasCompletedPositiveTestResult())
    }

    @Test
    fun `hasCompletedPositiveTestResult returns false when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFalse(testSubject.hasCompletedPositiveTestResult())
    }

    @Test
    fun `hasCompletedPositiveTestResult returns false when index case expired yesterday and positive test result not completed`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed
                    .copy(
                        confirmatoryTestCompletionStatus = null,
                        confirmedDate = null
                    ),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertFalse(testSubject.hasCompletedPositiveTestResult())
    }

    @Test
    fun `hasCompletedPositiveTestResult returns true when index case expired yesterday and positive test result completed`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed.copy(confirmatoryTestCompletionStatus = COMPLETED),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.hasCompletedPositiveTestResult())
    }

    @Test
    fun `hasCompletedPositiveTestResult returns true when index case expired yesterday and positive test result completed and confirmed`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed.copy(confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED),
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertTrue(testSubject.hasCompletedPositiveTestResult())
    }

    @Test
    fun `hasCompletedPositiveTestResult returns false when index case expires today and test result is negative`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.hasCompletedPositiveTestResult())
    }

    @Test
    fun `hasCompletedPositiveTestResult returns false when index case expires tomorrow and test result is negative`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.hasCompletedPositiveTestResult())
    }

    @Test
    fun `hasCompletedPositiveTestResult returns false when index case expires tomorrow, test result is negative and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.hasCompletedPositiveTestResult())
    }
    //endregion

    //region PossiblyIsolating.hasActiveConfirmedPositiveTestResult
    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires today and test does not require confirmation`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow and test does not require confirmation`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow, test does not require confirmation and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow, test does not require confirmation and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow and test has been confirmed`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow, test has been confirmed and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow, test has been confirmed and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow and test is not confirmed`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultUnconfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow, test is not confirmed and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultUnconfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow, test is not confirmed and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultUnconfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow and test is negative`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow, test is negative and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow, test is negative and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.getTestResultIfPositive
    @Test
    fun `getTestResultIfPositive returns null when only contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when contact case with negative test`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expired yesterday`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when index case expires today and test result is negative`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when index case expires tomorrow and test result is negative`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when index case expires tomorrow, test result is negative and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when index case expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = negativeTestResult,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expires today`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expires tomorrow`() {
        val testSubject = possiblyIsolatingWithMockDates(
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expires tomorrow and has active contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expires tomorrow and has expired contact case`() {
        val testSubject = possiblyIsolatingWithMockDates(
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = mockk(),
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }
    //endregion

    //region NeverIsolating
    @Test
    fun `remembersContactCase returns false when never isolating`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)

        assertFalse(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersIndexCase returns false when never isolating`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)

        assertFalse(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when never isolating`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `isActiveIsolation returns false when never isolating`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)

        assertFalse(testSubject.isActiveIsolation(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns false when never isolating`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)

        assertFalse(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveContactCase returns false when never isolating`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)

        assertFalse(testSubject.isActiveContactCase(fixedClock))
    }

    @Test
    fun `getIndexCase returns null when never isolating`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)

        assertNull(testSubject.getIndexCase())
    }
    @Test
    fun `getActiveIndexCase returns null when never isolating`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)

        assertNull(testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveContactCase returns null when never isolating`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)

        assertNull(testSubject.getActiveContactCase(fixedClock))
    }
    //endregion

    //region canReportSymptoms tests
    @Test
    fun `when user is not is isolation and we don't remember about the previous isolation can report symptoms`() {
        val testSubject = NeverIsolating(isolationConfiguration = IsolationConfiguration(), negativeTest = null)
        assertTrue(testSubject.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is not in isolation and we remember about the previous isolation can report symptoms`() {
        val testSubject = PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock).minusDays(5)),
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).minusDays(1)
        )

        assertTrue(testSubject.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is in isolation due to risky contact can report symptoms`() {
        val testSubject = PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = null,
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )

        assertTrue(testSubject.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is in isolation due to positive test result no onset date defined can report symptoms`() {
        val testSubject = PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                testResult = positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(2)
            ),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(2)
        )

        assertTrue(testSubject.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is in isolation due to positive test result with onset date defined cannot report symptoms`() {
        val testSubject = PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock).minusDays(5)),
                positiveTestResultConfirmed,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(2)
            ),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(2)
        )

        assertFalse(testSubject.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is in isolation due to completed questionnaire cannot report symptoms`() {
        val testSubject = PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock).minusDays(5)),
                testResult = null,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(2)
            ),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(2)
        )

        assertFalse(testSubject.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user has expired self assessment index case can report symptoms`() {
        val testSubject = PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = ContactCase(
                exposureDate = mockkDate(),
                notificationDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                selfAssessment = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock).minusDays(5)),
                testResult = null,
                startDate = mockkDate(),
                expiryDate = LocalDate.now(fixedClock).minusDays(2)
            ),
            startDate = mockkDate(),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )

        assertTrue(testSubject.canReportSymptoms(fixedClock))
    }
    //endregion

    //region Test data and helpers
    private val positiveTestResultWithNoNeedForConfirmation = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock).minusDays(3),
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
        requiresConfirmatoryTest = false
    )
    private val positiveTestResultConfirmed = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock).minusDays(3),
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
        requiresConfirmatoryTest = true,
        confirmedDate = LocalDate.now(fixedClock).minusDays(1),
        confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED
    )
    private val positiveTestResultUnconfirmed = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock).minusDays(3),
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
        requiresConfirmatoryTest = true
    )
    private val negativeTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock).minusDays(3),
        testResult = NEGATIVE,
        testKitType = LAB_RESULT,
        acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
        requiresConfirmatoryTest = false
    )

    private fun mockkDate(): LocalDate {
        val date = mockk<LocalDate>()
        every { date.minusDays(any()) } returns mockk()
        every { date.plusDays(any()) } returns mockk()
        return date
    }

    /**
     * Create a [PossiblyIsolating] object where the start and expiry dates are irrelevant
     */
    private fun possiblyIsolatingWithMockDates(
        contactCase: ContactCase? = null,
        indexInfo: IndexInfo? = null
    ): PossiblyIsolating =
        PossiblyIsolating(
            isolationConfiguration = IsolationConfiguration(),
            contactCase = contactCase,
            indexInfo = indexInfo,
            startDate = mockkDate(),
            expiryDate = mockkDate()
        )
    //endregion
}
