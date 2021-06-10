package uk.nhs.nhsx.covid19.android.app.exposure

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals

class TransmissionRiskLevelApplierTest {
    private val stateMachine = mockk<IsolationStateMachine>()
    private val calculateKeySubmissionDateRange = mockk<CalculateKeySubmissionDateRange>()

    private val testSubject: TransmissionRiskLevelApplier = TransmissionRiskLevelApplier(
        stateMachine,
        calculateKeySubmissionDateRange
    )

    private lateinit var generatedExposureKeys: List<NHSTemporaryExposureKey>
    private lateinit var exposureKeysWithRiskLevels: List<NHSTemporaryExposureKey>

    @Test
    fun `apply transmission risk level based on distance from onset day`() {
        // Transmission risk level = MAX transmission risk level - daysFromOnset
        givenSubmissionDateRange(from = 10, to = 13)
        givenExposureKeysForDays(from = 10, to = 13)
        givenIndexCaseWithOnsetDate(LocalDate.of(2020, 7, 12))

        whenApplyingRiskLevels()

        //                                10 11 12 13
        thenRiskLevelsMatch(mutableListOf(5, 6, 7, 6))
    }

    @Test
    fun `min transmission risk level does not go below 0`() {
        givenSubmissionDateRange(from = 5, to = 14)
        givenExposureKeysForDays(from = 5, to = 14)
        givenIndexCaseWithOnsetDate(LocalDate.of(2020, 7, 6))

        whenApplyingRiskLevels()

        //                                5  6  7  8  9  10 11 12 13 14
        thenRiskLevelsMatch(mutableListOf(6, 7, 6, 5, 4, 3, 2, 1, 0, 0))
    }

    @Test
    fun `keys outside of submissionDateRange are set to risk 0`() {
        givenSubmissionDateRange(from = 6, to = 13)
        givenExposureKeysForDays(from = 1, to = 14)
        givenIndexCaseWithOnsetDate(LocalDate.of(2020, 7, 8))

        whenApplyingRiskLevels()

        //                                1  2  3  4  5  6  7  8  9  10 11 12 13 14
        thenRiskLevelsMatch(mutableListOf(0, 0, 0, 0, 0, 5, 6, 7, 6, 5, 4, 3, 2, 0))
    }

    @Test
    fun `returns risk level 0 when isolation state does not have index case`() {
        givenSubmissionDateRange(from = 1, to = 5)
        givenExposureKeysForDays(from = 1, to = 5)
        givenIsolationWithoutIndexCase()

        whenApplyingRiskLevels()

        thenAllRiskLevelsAre0()
    }

    private fun givenExposureKeysForDays(from: Int, to: Int) {
        generatedExposureKeys = (from..to).map { dayOfMonth ->
            val date = LocalDate.of(2020, 7, dayOfMonth)
            NHSTemporaryExposureKey(
                key = UUID.randomUUID().toString(),
                rollingStartNumber = date.toRollingStartNumber()
            )
        }
    }

    private fun givenSubmissionDateRange(from: Int, to: Int) {
        every { calculateKeySubmissionDateRange(keySharingInfo.acknowledgedDate, any<LocalDate>()) } returns SubmissionDateRange(
            firstSubmissionDate = LocalDate.of(2020, 7, from),
            lastSubmissionDate = LocalDate.of(2020, 7, to)
        )
    }

    private fun givenIndexCaseWithOnsetDate(symptomsOnsetDate: LocalDate) {
        val isolation = IsolationState(
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = LocalDate.parse("2020-07-21"),
                    symptomsOnsetDate
                ),
                expiryDate = LocalDate.parse("2020-07-27")
            ),
            isolationConfiguration = DurationDays()
        )
        every { stateMachine.readState() } returns isolation
    }

    private fun givenIsolationWithoutIndexCase() {
        every { stateMachine.readState() } returns
            IsolationState(
                contactCase = ContactCase(
                    exposureDate = LocalDate.parse("2020-07-21"),
                    notificationDate = LocalDate.parse("2020-07-21"),
                    expiryDate = LocalDate.parse("2020-08-01")
                ),
                isolationConfiguration = DurationDays()
            )
    }

    private fun whenApplyingRiskLevels() {
        exposureKeysWithRiskLevels = testSubject.applyTransmissionRiskLevels(generatedExposureKeys, keySharingInfo)
    }

    private fun thenRiskLevelsMatch(expectedRiskLevels: MutableList<Int>) {
        exposureKeysWithRiskLevels.sortedBy { it.rollingStartNumber }
            .forEach { key ->
                val expectedRiskLevel = expectedRiskLevels.removeAt(0)
                assertEquals(expectedRiskLevel, key.transmissionRiskLevel)
            }
    }

    private fun thenAllRiskLevelsAre0() {
        exposureKeysWithRiskLevels.forEach { key ->
            assertEquals(0, key.transmissionRiskLevel)
        }
    }

    private val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = "token",
        acknowledgedDate = Instant.parse("2020-07-12T12:00:00Z"),
        notificationSentDate = null
    )
}

private fun LocalDate.toRollingStartNumber(): Int {
    val secondsSince1970 = atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() / 1000
    return (secondsSince1970 / (60 * 10)).toInt()
}
