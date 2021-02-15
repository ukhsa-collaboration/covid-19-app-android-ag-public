package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.work.ListenableWorker
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostalDistrictProviderWrapper
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult

class DownloadVirologyTestResultWorkTest {

    private val virologyTestingApi = mockk<VirologyTestingApi>(relaxed = true)
    private val testOrderTokensProvider = mockk<TestOrderingTokensProvider>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val isolationConfigurationProvider =
        mockk<IsolationConfigurationProvider>(relaxed = true)
    private val postalDistrictProviderWrapper = mockk<PostalDistrictProviderWrapper>(relaxed = true)
    private val analyticsManager = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val clock = Clock.fixed(from, ZoneId.systemDefault())

    val testSubject = DownloadVirologyTestResultWork(
        virologyTestingApi,
        testOrderTokensProvider,
        stateMachine,
        isolationConfigurationProvider,
        postalDistrictProviderWrapper,
        analyticsManager,
        clock
    )

    // FIXME: add additional tests

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns DurationDays()
        coEvery { postalDistrictProviderWrapper.getPostCodeDistrict() } returns ENGLAND
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
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
    fun `when no supported country return success without side effects`() = runBlocking {
        val config1 = TestOrderPollingConfig(from, "token1", "submission_token1")
        val config2 = TestOrderPollingConfig(from, "token2", "submission_token2")
        every { testOrderTokensProvider.configs } returns listOf(config1, config2)
        coEvery { postalDistrictProviderWrapper.getPostCodeDistrict() } returns null

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
            virologyTestingApi.getTestResult(VirologyTestResultRequestBody("token1", SupportedCountry.ENGLAND))
            virologyTestingApi.getTestResult(VirologyTestResultRequestBody("token2", SupportedCountry.ENGLAND))
        }
    }

    @Test
    fun `on positive notify isolation state machine`() = runBlocking {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        val testResultDate = from.plus(1, DAYS)
        coEvery {
            virologyTestingApi.getTestResult(
                VirologyTestResultRequestBody(
                    "token",
                    SupportedCountry.ENGLAND
                )
            )
        } returns Response.success(
            VirologyTestResultResponse(
                testResultDate,
                POSITIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        val result = testSubject.invoke()

        val testResult = ReceivedTestResult(
            config.diagnosisKeySubmissionToken,
            testResultDate,
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        verify { stateMachine.processEvent(OnTestResult(testResult)) }

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `on negative notify isolation state machine`() = runBlocking {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        val testResultDate = from.plus(1, DAYS)
        coEvery {
            virologyTestingApi.getTestResult(
                VirologyTestResultRequestBody(
                    "token",
                    SupportedCountry.ENGLAND
                )
            )
        } returns Response.success(
            VirologyTestResultResponse(
                testResultDate,
                NEGATIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        val result = testSubject.invoke()

        val testResult = ReceivedTestResult(
            config.diagnosisKeySubmissionToken,
            testResultDate,
            NEGATIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        verify { stateMachine.processEvent(OnTestResult(testResult)) }

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `on void notify isolation state machine`() = runBlocking {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        val testResultDate = from.plus(1, DAYS)
        coEvery {
            virologyTestingApi.getTestResult(
                VirologyTestResultRequestBody(
                    "token",
                    SupportedCountry.ENGLAND
                )
            )
        } returns Response.success(
            VirologyTestResultResponse(
                testResultDate,
                VOID,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        val result = testSubject.invoke()

        val testResult = ReceivedTestResult(
            config.diagnosisKeySubmissionToken,
            testResultDate,
            VOID,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        verify { stateMachine.processEvent(OnTestResult(testResult)) }

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `notify about all test results`() = runBlocking {
        every { testOrderTokensProvider.configs } returns listOf(config1, config2)
        val testResultDate1 = from.plus(1, DAYS)
        val testResultDate2 = from.plus(2, DAYS)
        coEvery {
            virologyTestingApi.getTestResult(
                VirologyTestResultRequestBody(
                    "token1",
                    SupportedCountry.ENGLAND
                )
            )
        } returns Response.success(
            VirologyTestResultResponse(
                testResultDate1,
                NEGATIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )
        coEvery {
            virologyTestingApi.getTestResult(
                VirologyTestResultRequestBody(
                    "token2",
                    SupportedCountry.ENGLAND
                )
            )
        } returns Response.success(
            VirologyTestResultResponse(
                testResultDate2,
                POSITIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = true
            )
        )

        testSubject.invoke()
        val testResult1 = ReceivedTestResult(
            config1.diagnosisKeySubmissionToken,
            testResultDate1,
            NEGATIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        val testResult2 = ReceivedTestResult(
            config2.diagnosisKeySubmissionToken,
            testResultDate2,
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = true
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
            TestOrderPollingConfig(from.minus(8, DAYS), "token", "submission_token")

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
            TestOrderPollingConfig(from.minus(6, DAYS), "token", "submission_token")

        val result = testSubject.removeIfOld(config)

        assertFalse(result)

        verify { testOrderTokensProvider wasNot called }
    }

    @Test
    fun `track analytics events on PCR negative result`() = runBlocking {
        setResult(NEGATIVE, LAB_RESULT)

        testSubject.invoke()

        coVerifyAll {
            analyticsManager.track(NegativeResultReceived)
            analyticsManager.track(ResultReceived(NEGATIVE, LAB_RESULT, INSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on PCR positive result`() = runBlocking {
        setResult(POSITIVE, LAB_RESULT)

        testSubject.invoke()

        coVerifyAll {
            analyticsManager.track(PositiveResultReceived)
            analyticsManager.track(ResultReceived(POSITIVE, LAB_RESULT, INSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on PCR void result`() = runBlocking {
        setResult(VOID, LAB_RESULT)

        testSubject.invoke()

        coVerifyAll {
            analyticsManager.track(VoidResultReceived)
            analyticsManager.track(ResultReceived(VOID, LAB_RESULT, INSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on assisted LFD negative result`() = runBlocking {
        setResult(NEGATIVE, RAPID_RESULT)

        testSubject.invoke()

        coVerifyAll {
            analyticsManager.track(NegativeResultReceived)
            analyticsManager.track(ResultReceived(NEGATIVE, RAPID_RESULT, INSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on unassisted LFD negative result`() = runBlocking {
        setResult(NEGATIVE, RAPID_SELF_REPORTED)

        testSubject.invoke()

        coVerifyAll {
            analyticsManager.track(NegativeResultReceived)
            analyticsManager.track(ResultReceived(NEGATIVE, RAPID_SELF_REPORTED, INSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on assisted LFD positive result`() = runBlocking {
        setResult(POSITIVE, RAPID_RESULT)

        testSubject.invoke()

        coVerifyAll {
            analyticsManager.track(PositiveResultReceived)
            analyticsManager.track(ResultReceived(POSITIVE, RAPID_RESULT, INSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on unassisted LFD positive result`() = runBlocking {
        setResult(POSITIVE, RAPID_SELF_REPORTED)

        testSubject.invoke()

        coVerifyAll {
            analyticsManager.track(PositiveResultReceived)
            analyticsManager.track(ResultReceived(POSITIVE, RAPID_SELF_REPORTED, INSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on assisted LFD void result`() = runBlocking {
        setResult(VOID, RAPID_RESULT)

        testSubject.invoke()

        coVerifyAll {
            analyticsManager.track(VoidResultReceived)
            analyticsManager.track(ResultReceived(VOID, RAPID_RESULT, INSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on unassisted LFD void result`() = runBlocking {
        setResult(VOID, RAPID_SELF_REPORTED)

        testSubject.invoke()

        coVerifyAll {
            analyticsManager.track(VoidResultReceived)
            analyticsManager.track(ResultReceived(VOID, RAPID_SELF_REPORTED, INSIDE_APP))
        }
    }

    private fun setResult(
        result: VirologyTestResult,
        testKitType: VirologyTestKitType
    ) {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        val testResultDate = from.plus(1, DAYS)
        coEvery {
            virologyTestingApi.getTestResult(
                VirologyTestResultRequestBody(
                    "token",
                    SupportedCountry.ENGLAND
                )
            )
        } returns Response.success(
            VirologyTestResultResponse(
                testResultDate,
                result,
                testKitType,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )
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
