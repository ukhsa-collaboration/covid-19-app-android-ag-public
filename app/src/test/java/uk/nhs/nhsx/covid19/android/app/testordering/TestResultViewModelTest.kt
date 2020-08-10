package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.DateWindow
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Instant
import java.time.LocalDate

class TestResultViewModelTest {

    private val latestTestResultProvider = mockk<LatestTestResultProvider>()
    private val stateMachine = mockk<IsolationStateMachine>()
    private val submitTemporaryExposureKeys = mockk<SubmitTemporaryExposureKeys>()
    private val keyWindowCalculator = mockk<KeyWindowCalculator>()

    private val testSubject =
        TestResultViewModel(latestTestResultProvider, stateMachine, submitTemporaryExposureKeys, keyWindowCalculator)

    @Before
    fun setUp() {
        every { latestTestResultProvider.latestTestResult } returns LatestTestResult(
            "token", testEndDate = testEndDate, testResult = POSITIVE
        )
        coEvery { submitTemporaryExposureKeys.invoke(any()) } returns SubmitResult.Success
    }

    @Test
    fun `submits keys not called if calculated date window is null`() {
        every { keyWindowCalculator.calculateDateWindow() } returns null

        testSubject.submitKeys()
        coVerify { submitTemporaryExposureKeys wasNot called }
    }

    @Test
    fun `submits keys is called with appropriate date window`() {
        val dateWindow = DateWindow(
            LocalDate.parse("2020-08-01"),
            LocalDate.parse("2020-08-03")
        )
        every { keyWindowCalculator.calculateDateWindow() } returns dateWindow

        testSubject.submitKeys()
        coVerify { submitTemporaryExposureKeys.invoke(dateWindow) }
    }

    companion object {
        val testEndDate = Instant.parse("2020-07-25T12:00:00Z")!!
        val symptomsOnsetDate = LocalDate.parse("2020-07-20")!!
    }
}
