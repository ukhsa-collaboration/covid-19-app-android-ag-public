package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.work.ListenableWorker
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadVirologyTestResultWorkTest {

    private val virologyTestingApi = mockk<VirologyTestingApi>(relaxed = true)
    private val testOrderTokensProvider = mockk<TestOrderingTokensProvider>(relaxed = true)
    private val latestTestResultProvider = mockk<LatestTestResultProvider>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val isolationConfigurationProvider =
        mockk<IsolationConfigurationProvider>(relaxed = true)
    private val analyticsManager = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val clock = Clock.fixed(from, ZoneId.systemDefault())

    val testSubject = DownloadVirologyTestResultWork(
        virologyTestingApi,
        testOrderTokensProvider,
        latestTestResultProvider,
        stateMachine,
        isolationConfigurationProvider,
        analyticsManager,
        clock
    )

    // FIXME: add additional tests

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns DurationDays()
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `when feature disabled return success without side effects`() = runBlocking {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.TEST_ORDERING)

        val actual = testSubject.invoke()

        val expected = ListenableWorker.Result.success()

        assertEquals(expected, actual)
        verify(exactly = 0) { testOrderTokensProvider.configs }
        checkNoSideEffects()
    }

    @Test
    fun `when token provider is empty return success without side effects`() = runBlocking {
        every { testOrderTokensProvider.configs } returns listOf()

        val actual = testSubject.invoke()

        val expected = ListenableWorker.Result.success()

        assertEquals(expected, actual)
        checkNoSideEffects()
    }

    @Test
    fun `calls virologyTestingApi for each configuration`() = runBlocking {
        val config1 = TestOrderPollingConfig(from, "token1", "submission_token1")
        val config2 = TestOrderPollingConfig(from, "token2", "submission_token2")
        every { testOrderTokensProvider.configs } returns listOf(config1, config2)

        testSubject.invoke()

        coVerifyOrder {
            virologyTestingApi.getTestResult(VirologyTestResultRequestBody("token1"))
            virologyTestingApi.getTestResult(VirologyTestResultRequestBody("token2"))
        }
    }

    @Test
    fun `on positive notify isolation state machine`() = runBlocking {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        val testResultDate = from.plus(1, ChronoUnit.DAYS)
        coEvery { virologyTestingApi.getTestResult(VirologyTestResultRequestBody("token")) } returns Response.success(
            VirologyTestResultResponse(
                testResultDate, POSITIVE
            )
        )

        val result = testSubject.invoke()

        val testResult = ReceivedTestResult(
            config.diagnosisKeySubmissionToken,
            testResultDate,
            POSITIVE
        )
        verify { stateMachine.processEvent(OnTestResult(testResult)) }

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `on negative notify isolation state machine`() = runBlocking {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        val testResultDate = from.plus(1, ChronoUnit.DAYS)
        coEvery { virologyTestingApi.getTestResult(VirologyTestResultRequestBody("token")) } returns Response.success(
            VirologyTestResultResponse(
                testResultDate, NEGATIVE
            )
        )

        val result = testSubject.invoke()

        val testResult = ReceivedTestResult(
            config.diagnosisKeySubmissionToken,
            testResultDate,
            NEGATIVE
        )
        verify { stateMachine.processEvent(OnTestResult(testResult)) }

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `on void notify isolation state machine`() = runBlocking {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        val testResultDate = from.plus(1, ChronoUnit.DAYS)
        coEvery { virologyTestingApi.getTestResult(VirologyTestResultRequestBody("token")) } returns Response.success(
            VirologyTestResultResponse(
                testResultDate, VOID
            )
        )

        val result = testSubject.invoke()

        val testResult = ReceivedTestResult(
            config.diagnosisKeySubmissionToken,
            testResultDate,
            VOID
        )
        verify { stateMachine.processEvent(OnTestResult(testResult)) }

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `notify about all test results`() = runBlocking {
        every { testOrderTokensProvider.configs } returns listOf(config1, config2)
        val testResultDate1 = from.plus(1, ChronoUnit.DAYS)
        val testResultDate2 = from.plus(2, ChronoUnit.DAYS)
        coEvery { virologyTestingApi.getTestResult(VirologyTestResultRequestBody("token1")) } returns Response.success(
            VirologyTestResultResponse(
                testResultDate1, NEGATIVE
            )
        )
        coEvery { virologyTestingApi.getTestResult(VirologyTestResultRequestBody("token2")) } returns Response.success(
            VirologyTestResultResponse(
                testResultDate2, POSITIVE
            )
        )

        testSubject.invoke()
        val testResult1 = ReceivedTestResult(
            config1.diagnosisKeySubmissionToken,
            testResultDate1,
            NEGATIVE
        )
        val testResult2 = ReceivedTestResult(
            config2.diagnosisKeySubmissionToken,
            testResultDate2,
            POSITIVE
        )
        verify { stateMachine.processEvent(OnTestResult(testResult2)) }
        verify { stateMachine.processEvent(OnTestResult(testResult1)) }
    }

    @Test
    fun `remove test ordering token if it is too old`() {
        every { isolationConfigurationProvider.durationDays } returns DurationDays(
            pendingTasksRetentionPeriod = 7
        )
        val config =
            TestOrderPollingConfig(from.minus(8, ChronoUnit.DAYS), "token", "submission_token")

        val result = testSubject.removeIfOld(config)

        assertTrue(result)

        verify { testOrderTokensProvider.remove(config) }
    }

    @Test
    fun `do not remove test ordering token if it is not too old`() {
        every { isolationConfigurationProvider.durationDays } returns DurationDays(
            pendingTasksRetentionPeriod = 7
        )
        val config =
            TestOrderPollingConfig(from.minus(6, ChronoUnit.DAYS), "token", "submission_token")

        val result = testSubject.removeIfOld(config)

        assertFalse(result)

        verify { testOrderTokensProvider wasNot called }
    }

    private fun checkNoSideEffects() {
        coVerify(exactly = 0) { virologyTestingApi.getTestResult(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
    }

    companion object {
        private val from = Instant.parse("2020-07-21T10:00:00Z")
        val config1 = TestOrderPollingConfig(from, "token1", "submission_token1")
        val config2 = TestOrderPollingConfig(from, "token2", "submission_token2")
    }
}
