package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsolationLogicalStateTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-01-15T10:00:00Z"), ZoneOffset.UTC)

    //region PossiblyIsolating.init
    @Test
    fun `create PossiblyIsolating without isolation data fails`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays()
        )

        assertFailsWith<IllegalArgumentException> {
            PossiblyIsolating(isolationState)
        }
    }

    @Test
    fun `create PossiblyIsolating without isolation data and negative test fails`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = NegativeTest(negativeTestResult)
        )

        assertFailsWith<IllegalArgumentException> {
            PossiblyIsolating(isolationState)
        }
    }
    //endregion

    //region PossiblyIsolating.isActiveIndexCase
    @Test
    fun `isActiveIndexCase returns false when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns false when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns false when index case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns true when index case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns true when index case expires tomorrow and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveIndexCase(fixedClock))
    }

    @Test
    fun `isActiveIndexCase returns true when index case expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveIndexCase(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.isActiveIndexCaseOnly
    @Test
    fun `isActiveIndexCaseOnly returns false when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns false when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns false when index case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns true when index case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns false when index case expires tomorrow and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveIndexCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveIndexCaseOnly returns true when index case expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveIndexCaseOnly(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.getActiveIndexCase
    @Test
    fun `getActiveIndexCase returns null when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns null when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns null when index case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns index case when index case expires tomorrow`() {
        val indexCase = IndexCase(
            isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = indexCase
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(indexCase, testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns index case when index case expires tomorrow and has active contact case`() {
        val indexCase = IndexCase(
            isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = indexCase
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(indexCase, testSubject.getActiveIndexCase(fixedClock))
    }

    @Test
    fun `getActiveIndexCase returns index case when index case expires tomorrow and has expired contact case`() {
        val indexCase = IndexCase(
            isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = indexCase
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(indexCase, testSubject.getActiveIndexCase(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.isActiveContactCase
    @Test
    fun `isActiveContactCase returns false when only index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveContactCase(fixedClock))
    }

    @Test
    fun `isActiveContactCase returns false when contact case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveContactCase(fixedClock))
    }

    @Test
    fun `isActiveContactCase returns true when contact case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveContactCase(fixedClock))
    }

    @Test
    fun `isActiveContactCase returns true when contact case expires tomorrow and has index contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveContactCase(fixedClock))
    }

    @Test
    fun `isActiveContactCase returns true when contact case expires tomorrow and has expired index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveContactCase(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.isActiveContactCaseOnly
    @Test
    fun `isActiveContactCaseOnly returns false when only index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveContactCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveContactCaseOnly returns false when contact case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveContactCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveContactCaseOnly returns true when contact case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveContactCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveContactCaseOnly returns false when contact case expires tomorrow and has active index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.isActiveContactCaseOnly(fixedClock))
    }

    @Test
    fun `isActiveContactCaseOnly returns true when contact case expires tomorrow and has expired index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.isActiveContactCaseOnly(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.getActiveContactCase
    @Test
    fun `getActiveContactCase returns null when only index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveContactCase(fixedClock))
    }

    @Test
    fun `getActiveContactCase returns null when contact case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveContactCase(fixedClock))
    }

    @Test
    fun `getActiveContactCase returns contact case when contact case expires tomorrow`() {
        val contactCase = ContactCase(
            exposureDate = LocalDate.now(fixedClock).minusDays(1),
            notificationDate = LocalDate.now(fixedClock).minusDays(1),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(contactCase, testSubject.getActiveContactCase(fixedClock))
    }

    @Test
    fun `getActiveContactCase returns contact case when contact case expires tomorrow and has active index case`() {
        val contactCase = ContactCase(
            exposureDate = LocalDate.now(fixedClock),
            notificationDate = LocalDate.now(fixedClock),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(contactCase, testSubject.getActiveContactCase(fixedClock))
    }

    @Test
    fun `getActiveContactCase returns contact case when contact case expires tomorrow and has expired index case`() {
        val contactCase = ContactCase(
            exposureDate = LocalDate.now(fixedClock).minusDays(1),
            notificationDate = LocalDate.now(fixedClock).minusDays(1),
            expiryDate = LocalDate.now(fixedClock).plusDays(1)
        )
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(contactCase, testSubject.getActiveContactCase(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.remembersIndexCase
    @Test
    fun `remembersIndexCase returns false when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns false when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expired yesterday`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expires tomorrow and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCase())
    }

    @Test
    fun `remembersIndexCase returns true when index case expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCase())
    }
    //endregion

    //region PossiblyIsolating.remembersIndexCaseOnly
    @Test
    fun `remembersIndexCaseOnly returns false when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns false when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns true when index case expired yesterday`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns true when index case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns true when index case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns false when index case expires tomorrow and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCaseOnly())
    }

    @Test
    fun `remembersIndexCaseOnly returns false when index case expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCaseOnly())
    }
    //endregion

    //region PossiblyIsolating.remembersIndexCaseWithSelfAssessment
    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expired yesterday`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expired yesterday and has positive test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires today and has positive test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires tomorrow and has positive test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when index case with positive test expired yesterday`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(testEndDate = positiveTestResultWithNoNeedForConfirmation.testEndDate),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when index case with positive test expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(testEndDate = positiveTestResultWithNoNeedForConfirmation.testEndDate),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns false when index case with positive test expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(testEndDate = positiveTestResultWithNoNeedForConfirmation.testEndDate),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires tomorrow and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }

    @Test
    fun `remembersIndexCaseWithSelfAssessment returns true when index case with self-assessment expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersIndexCaseWithSelfAssessment())
    }
    //endregion

    //region PossiblyIsolating.remembersContactCase
    @Test
    fun `remembersContactCase returns false when only index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expired yesterday`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expires tomorrow and has index contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersContactCase())
    }

    @Test
    fun `remembersContactCase returns true when contact case expires tomorrow and has expired index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersContactCase())
    }
    //endregion

    //region PossiblyIsolating.remembersContactCaseOnly
    @Test
    fun `remembersContactCaseOnly returns false when only index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns true when contact case expired yesterday`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns true when contact case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns true when contact case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns false when contact case expires tomorrow and has active index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersContactCaseOnly())
    }

    @Test
    fun `remembersContactCaseOnly returns false when contact case expires tomorrow and has expired index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersContactCaseOnly())
    }
    //endregion

    //region PossiblyIsolating.remembersBothCases
    @Test
    fun `remembersBothCases returns false when only index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns false when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns false when only contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns true when expired contact case and expired index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns true when active contact case and expired index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns true when expired contact case and active index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersBothCases())
    }

    @Test
    fun `remembersBothCases returns true when active contact case and active index case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.remembersBothCases())
    }
    //endregion

    //region PossiblyIsolating.getActiveTestResult
    @Test
    fun `getActiveTestResult returns null when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns null when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns null when index case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns test result when index case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns test result when index case expires tomorrow and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResult(fixedClock))
    }

    @Test
    fun `getActiveTestResult returns test result when index case expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResult(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.getActiveTestResultIfPositive
    @Test
    fun `getActiveTestResultIfPositive returns null when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns null when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns null when index case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns test result when index case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns test result when index case expires tomorrow and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResultIfPositive(fixedClock))
    }

    @Test
    fun `getActiveTestResultIfPositive returns test result when index case expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultWithNoNeedForConfirmation, testSubject.getActiveTestResultIfPositive(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.hasActivePositiveTestResult
    @Test
    fun `hasActivePositiveTestResult returns false when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns false when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns false when index case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns true when index case with positive test result expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns true when index case with positive test result expires tomorrow and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.hasActivePositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActivePositiveTestResult returns true when index case with positive test result expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.hasActivePositiveTestResult(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.hasActiveConfirmedPositiveTestResult
    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires today and test does not require confirmation`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow and test does not require confirmation`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow, test does not require confirmation and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow, test does not require confirmation and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultWithNoNeedForConfirmation,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow and test has been confirmed`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow, test has been confirmed and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns true when index case expires tomorrow, test has been confirmed and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertTrue(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow and test is not confirmed`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultUnconfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow, test is not confirmed and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultUnconfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow, test is not confirmed and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultUnconfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow and test is negative`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = negativeTestResult,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow, test is negative and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = negativeTestResult,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }

    @Test
    fun `hasActiveConfirmedPositiveTestResult returns false when index case expires tomorrow, test is negative and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = negativeTestResult,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertFalse(testSubject.hasActiveConfirmedPositiveTestResult(fixedClock))
    }
    //endregion

    //region PossiblyIsolating.getTestResultIfPositive
    @Test
    fun `getTestResultIfPositive returns null when only contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when contact case with negative test`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = NegativeTest(negativeTestResult)
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expired yesterday`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when index case expires today and test result is negative`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = negativeTestResult,
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when index case expires tomorrow and test result is negative`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = negativeTestResult,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when index case expires tomorrow, test result is negative and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = negativeTestResult,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns null when index case expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = negativeTestResult,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expires today`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expires tomorrow`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expires tomorrow and has active contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive returns test result when index case expires tomorrow and has expired contact case`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock)),
                testResult = positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        val testSubject = PossiblyIsolating(isolationState)

        assertEquals(positiveTestResultConfirmed, testSubject.getTestResultIfPositive())
    }
    //endregion

    //region canReportSymptoms tests
    @Test
    fun `when user is not is isolation and we don't remember about the previous isolation can report symptoms`() {
        val logicalIsolationState = NeverIsolating(isolationConfiguration = DurationDays(), negativeTest = null)
        assertTrue(logicalIsolationState.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is not in isolation and we remember about the previous isolation can report symptoms`() {
        val expiredIsolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock).minusDays(5)),
                testResult = positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val logicalIsolationState = PossiblyIsolating(expiredIsolationState)
        assertTrue(logicalIsolationState.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is in isolation due to risky contact can report symptoms`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = null
        )

        val logicalIsolationState = PossiblyIsolating(isolationState)
        assertTrue(logicalIsolationState.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is in isolation due to positive test result no onset date defined can report symptoms`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(LocalDate.now(fixedClock).minusDays(1)),
                expiryDate = LocalDate.now(fixedClock).plusDays(2)
            )
        )

        val logicalIsolationState = PossiblyIsolating(isolationState)
        assertTrue(logicalIsolationState.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is in isolation due to positive test result with onset date defined cannot report symptoms`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock).minusDays(5)),
                positiveTestResultConfirmed,
                expiryDate = LocalDate.now(fixedClock).plusDays(2)
            )
        )

        val logicalIsolationState = PossiblyIsolating(isolationState)
        assertFalse(logicalIsolationState.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user is in isolation due to completed questionnaire cannot report symptoms`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock).minusDays(5)),
                testResult = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(2)
            )
        )

        val logicalIsolationState = PossiblyIsolating(isolationState)
        assertFalse(logicalIsolationState.canReportSymptoms(fixedClock))
    }

    @Test
    fun `when user has expired self assessment index case can report symptoms`() {
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock).minusDays(1),
                notificationDate = LocalDate.now(fixedClock).minusDays(1),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock).minusDays(5)),
                testResult = null,
                expiryDate = LocalDate.now(fixedClock).minusDays(2)
            )
        )

        val logicalIsolationState = PossiblyIsolating(isolationState)
        assertTrue(logicalIsolationState.canReportSymptoms(fixedClock))
    }
    //endregion

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
}
