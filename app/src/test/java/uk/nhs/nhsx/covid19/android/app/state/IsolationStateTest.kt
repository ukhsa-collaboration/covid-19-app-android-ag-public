package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import java.time.LocalDate
import kotlin.test.assertEquals

class IsolationStateTest {

    val selfAssessmentDate = mockk<LocalDate>()

    @Test
    fun `get assumedOnsetDateForExposureKeys is calculated from isolation info`() {
        val toIsolationInfo = mockk<IsolationInfo>()

        val spyIsolationState = spyk(
            IsolationState(
                isolationConfiguration = DurationDays()
            )
        )

        every { spyIsolationState.toIsolationInfo() } returns toIsolationInfo
        every { toIsolationInfo.assumedOnsetDateForExposureKeys } returns mockk()

        // Under testing
        spyIsolationState.assumedOnsetDateForExposureKeys

        verify { spyIsolationState.assumedOnsetDateForExposureKeys }
        verify { spyIsolationState.toIsolationInfo() }
        verify { toIsolationInfo.assumedOnsetDateForExposureKeys }
        confirmVerified(spyIsolationState)
    }

    @Test
    fun `isolation state can be transformed to isolation info`() {
        val isolationConfiguration = mockk<DurationDays>()
        val selfAssessment = mockk<SelfAssessment>()
        val testResult = mockk<AcknowledgedTestResult>()
        val contact = mockk<Contact>()

        every { testResult.isPositive() } returns false
        every { selfAssessment.assumedOnsetDate } returns mockk()

        val isolationState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = selfAssessment,
            testResult = testResult,
            contact = contact,
            hasAcknowledgedEndOfIsolation = true
        )

        val isolationInfo = IsolationInfo(
            selfAssessment = selfAssessment,
            testResult = testResult,
            contact = contact,
            hasAcknowledgedEndOfIsolation = true
        )

        assertEquals(isolationInfo, isolationState.toIsolationInfo())
    }
}
