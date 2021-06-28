package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.work.ListenableWorker
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
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
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.ReceivedUnknownTestResultProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadVirologyTestResultWorkTest {

    private val virologyTestingApi = mockk<VirologyTestingApi>()
    private val testOrderTokensProvider = mockk<TestOrderingTokensProvider>(relaxUnitFun = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()
    private val receivedUnknownTestResultProvider = mockk<ReceivedUnknownTestResultProvider>(relaxUnitFun = true)
    private val clock = Clock.fixed(from, ZoneId.systemDefault())

    val testSubject = DownloadVirologyTestResultWork(
        virologyTestingApi,
        testOrderTokensProvider,
        stateMachine,
        isolationConfigurationProvider,
        localAuthorityPostCodeProvider,
        receivedUnknownTestResultProvider,
        clock
    )

    // FIXME: add additional tests

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns DurationDays()
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND
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
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns null

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
                RAPID_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = true,
                confirmatoryDayLimit = 2
            )
        )

        val result = testSubject.invoke()

        val testResult = ReceivedTestResult(
            config.diagnosisKeySubmissionToken,
            testResultDate,
            POSITIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = true,
            confirmatoryDayLimit = 2
        )
        verify { stateMachine.processEvent(OnTestResult(testResult, testOrderType = INSIDE_APP)) }

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
                requiresConfirmatoryTest = false,
                confirmatoryDayLimit = null
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
        verify { stateMachine.processEvent(OnTestResult(testResult, testOrderType = INSIDE_APP)) }

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
                requiresConfirmatoryTest = false,
                confirmatoryDayLimit = null
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
        verify { stateMachine.processEvent(OnTestResult(testResult, testOrderType = INSIDE_APP)) }

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
                requiresConfirmatoryTest = false,
                confirmatoryDayLimit = null
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
                requiresConfirmatoryTest = true,
                confirmatoryDayLimit = null
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
        verify { stateMachine.processEvent(OnTestResult(testResult2, testOrderType = INSIDE_APP)) }
        verify { stateMachine.processEvent(OnTestResult(testResult1, testOrderType = INSIDE_APP)) }
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
    fun `receiving http 204 returns NoTestResult`() = runBlocking {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        coEvery {
            virologyTestingApi.getTestResult(
                VirologyTestResultRequestBody(
                    "token",
                    SupportedCountry.ENGLAND
                )
            )
        } returns Response.success(204, mockk<VirologyTestResultResponse>())

        testSubject.invoke()

        verify(exactly = 0) { receivedUnknownTestResultProvider setProperty "value" value true }
        verify(exactly = 0) { testOrderTokensProvider.remove(config) }
    }

    @Test
    fun `receiving an unknown json format stores the flag`() = runBlocking {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        coEvery {
            virologyTestingApi.getTestResult(
                VirologyTestResultRequestBody(
                    "token",
                    SupportedCountry.ENGLAND
                )
            )
        } throws JsonDataException()

        testSubject.invoke()

        verify { receivedUnknownTestResultProvider setProperty "value" value true }
        verify { testOrderTokensProvider.remove(config) }
    }

    @Test
    fun `receiving wrongly encoded response stores the flag`() = runBlocking {
        val config = TestOrderPollingConfig(from, "token", "submission_token")
        every { testOrderTokensProvider.configs } returns listOf(config)
        coEvery {
            virologyTestingApi.getTestResult(
                VirologyTestResultRequestBody(
                    "token",
                    SupportedCountry.ENGLAND
                )
            )
        } throws JsonEncodingException(null)

        testSubject.invoke()

        verify { receivedUnknownTestResultProvider setProperty "value" value true }
        verify { testOrderTokensProvider.remove(config) }
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
                requiresConfirmatoryTest = false,
                confirmatoryDayLimit = null
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
