package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.KeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.NoKeySharingPossible
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Instant
import kotlin.test.assertEquals

class CanShareKeysTest {

    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>()
    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val isolationStateMachineLazy = Lazy { isolationStateMachine }
    private val calculateKeySubmissionDateRange = mockk<CalculateKeySubmissionDateRange>()

    private val state = mockk<IsolationState>()
    private val dateRange = mockk<SubmissionDateRange>()

    val canShareKeys = CanShareKeys(keySharingInfoProvider, isolationStateMachineLazy, calculateKeySubmissionDateRange)

    @Before
    fun setUp() {
        every { isolationStateMachine.readState() } returns state
        every { calculateKeySubmissionDateRange(any(), any()) } returns dateRange
    }

    @Test
    fun `if there is no keySharingInfo return NoKeySharingPossible`() {
        givenKeySharingInfoNull()

        assertEquals(expected = NoKeySharingPossible, actual = canShareKeys())
    }

    @Test
    fun `if there is no symptomsOnsetDate return NoKeySharingPossible`() {
        givenKeySharingInfoNotNull()
        givenSymptomsOnsetDateNull()

        assertEquals(expected = NoKeySharingPossible, actual = canShareKeys())
    }

    @Test
    fun `if dateRange does not contain at least one day return NoKeySharingPossible`() {
        givenKeySharingInfoNotNull()
        givenSymptomsOnsetDateNotNull()
        givenDateRangeEmpty()

        assertEquals(expected = NoKeySharingPossible, actual = canShareKeys())
    }

    @Test
    fun `if dateRange contains at least one day return KeySharingPossible`() {
        val expectedKeySharingInfo = givenKeySharingInfoNotNull()
        givenSymptomsOnsetDateNotNull()
        givenDateRangeNotEmpty()

        assertEquals(expected = KeySharingPossible(expectedKeySharingInfo), actual = canShareKeys())
    }

    private fun givenKeySharingInfoNull() {
        every { keySharingInfoProvider.keySharingInfo } returns null
    }

    private fun givenKeySharingInfoNotNull(): KeySharingInfo {
        val keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = "token",
            acknowledgedDate = Instant.parse("1970-01-01T00:00:00Z"),
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        every { keySharingInfoProvider.keySharingInfo } returns keySharingInfo

        return keySharingInfo
    }

    private fun givenSymptomsOnsetDateNull() {
        every { state.assumedOnsetDateForExposureKeys } returns null
    }

    private fun givenSymptomsOnsetDateNotNull() {
        every { state.assumedOnsetDateForExposureKeys } returns mockk()
    }

    private fun givenDateRangeEmpty() {
        every { dateRange.containsAtLeastOneDay() } returns false
    }

    private fun givenDateRangeNotEmpty() {
        every { dateRange.containsAtLeastOneDay() } returns true
    }
}
