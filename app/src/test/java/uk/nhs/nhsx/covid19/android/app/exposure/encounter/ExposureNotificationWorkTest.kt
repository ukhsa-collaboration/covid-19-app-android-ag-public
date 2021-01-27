package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.ExposureWindow.Builder
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandlePollingExposureNotification.PollingCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.DayRisk
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowRiskManager
import uk.nhs.nhsx.covid19.android.app.payment.CheckIsolationPaymentToken
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.CIRCUIT_BREAKER
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExposureNotificationWorkTest {

    private val handleInitialExposureNotification = mockk<HandleInitialExposureNotification>()
    private val handlePollingExposureNotification = mockk<HandlePollingExposureNotification>()
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val submitEmptyData = mockk<SubmitEmptyData>(relaxed = true)
    private val submitFakeExposureWindows = mockk<SubmitFakeExposureWindows>(relaxed = true)
    private val submitEpidemiologyData = mockk<SubmitEpidemiologyData>(relaxed = true)
    private val checkIsolationPaymentToken = mockk<CheckIsolationPaymentToken>(relaxed = true)
    private val exposureCircuitBreakerInfoProvider = mockk<ExposureCircuitBreakerInfoProvider>(relaxed = true)
    private val exposureWindowRiskManager = mockk<ExposureWindowRiskManager>(relaxed = true)
    private val epidemiologyEventProvider = mockk<EpidemiologyEventProvider>(relaxed = true)
    private val clock = Clock.fixed(Instant.parse("2020-12-24T20:00:00Z"), ZoneOffset.UTC)

    private val testSubject = ExposureNotificationWork(
        handleInitialExposureNotification,
        handlePollingExposureNotification,
        stateMachine,
        submitEmptyData,
        submitFakeExposureWindows,
        submitEpidemiologyData,
        checkIsolationPaymentToken,
        exposureCircuitBreakerInfoProvider,
        exposureWindowRiskManager,
        epidemiologyEventProvider,
        clock
    )

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `handle no matches calls fake circuit breaker then submits fake exposure windows and returns success`() =
        runBlocking {
            val result = testSubject.handleNoMatchesFound()

            verify { submitEmptyData(CIRCUIT_BREAKER) }
            verify(exactly = 1) { submitFakeExposureWindows(EXPOSURE_WINDOW) }

            assertTrue { result is Success }
        }

    @Test
    fun `calling handleUnprocessedRequests without circuit breaker info items return success`() = runBlocking {
        every { exposureCircuitBreakerInfoProvider.info } returns emptyList()

        val result = testSubject.handleUnprocessedRequests()

        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }

        assertEquals(Success(Unit), result)
    }

    @Test
    fun `calling handleUnprocessedRequests without approval token and circuit breaker responds yes then trigger exposure`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithoutToken)
            coEvery { handleInitialExposureNotification.invoke(infoWithoutToken) } returns Success(
                InitialCircuitBreakerResult.Yes
            )

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                handleInitialExposureNotification.invoke(infoWithoutToken)
                stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(infoWithoutToken.startOfDayMillis)))
                exposureCircuitBreakerInfoProvider.remove(infoWithoutToken)
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests without approval token and circuit breaker responds no then remove info item`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithoutToken)
            coEvery { handleInitialExposureNotification.invoke(infoWithoutToken) } returns Success(
                InitialCircuitBreakerResult.No
            )

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                handleInitialExposureNotification.invoke(infoWithoutToken)
                exposureCircuitBreakerInfoProvider.remove(infoWithoutToken)
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests without approval token and circuit breaker responds pending then update info item`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithoutToken)
            coEvery { handleInitialExposureNotification.invoke(infoWithoutToken) } returns Success(
                InitialCircuitBreakerResult.Pending("token")
            )

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                handleInitialExposureNotification.invoke(infoWithoutToken)
                exposureCircuitBreakerInfoProvider.setApprovalToken(infoWithoutToken, "token")
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests without approval token and circuit breaker responds Failure then do nothing`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithoutToken)
            coEvery { handleInitialExposureNotification.invoke(infoWithoutToken) } returns Failure(Exception())

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                handleInitialExposureNotification.invoke(infoWithoutToken)
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests with approval token and circuit breaker responds yes then trigger exposure`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithToken)
            coEvery { handlePollingExposureNotification.invoke("token") } returns Success(
                PollingCircuitBreakerResult.Yes
            )

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                handlePollingExposureNotification.invoke("token")
                stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(infoWithToken.startOfDayMillis)))
                exposureCircuitBreakerInfoProvider.remove(infoWithToken)
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests with approval token and circuit breaker responds no then remove info item`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithToken)
            coEvery { handlePollingExposureNotification.invoke("token") } returns Success(
                PollingCircuitBreakerResult.No
            )

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                handlePollingExposureNotification.invoke("token")
                exposureCircuitBreakerInfoProvider.remove(infoWithToken)
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests with approval token and circuit breaker responds pending then do nothing`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithToken)
            coEvery { handlePollingExposureNotification.invoke("token") } returns Success(
                PollingCircuitBreakerResult.Pending
            )

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                handlePollingExposureNotification.invoke("token")
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests with approval token and circuit breaker responds Failure then do nothing`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithToken)
            coEvery { handlePollingExposureNotification.invoke("token") } returns Failure(Exception())

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                handlePollingExposureNotification.invoke("token")
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests with two exposure circuit breaker info items with yes response returns success`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithoutToken, infoWithToken)
            coEvery { handleInitialExposureNotification.invoke(infoWithoutToken) } returns Success(
                InitialCircuitBreakerResult.Yes
            )
            coEvery { handlePollingExposureNotification.invoke("token") } returns Success(
                PollingCircuitBreakerResult.Yes
            )

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                handleInitialExposureNotification.invoke(infoWithoutToken)
                stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(infoWithoutToken.startOfDayMillis)))
                exposureCircuitBreakerInfoProvider.remove(infoWithoutToken)

                handlePollingExposureNotification.invoke("token")
                stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(infoWithToken.startOfDayMillis)))
                exposureCircuitBreakerInfoProvider.remove(infoWithToken)

                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleNewExposure with no risk should submit empty circuit breaker exposure windows`() =
        runBlocking {
            coEvery { exposureWindowRiskManager.getRisk() } returns null

            val result = testSubject.handleNewExposure()

            verify { submitEmptyData(CIRCUIT_BREAKER) }
            verify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW, numberOfExposureWindowsSent = 0) }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleNewExposure with risk should add exposure circuit breaker info item, add and submit epidemiology events and attempt initial call when feature flag enabled`() =
        runBlocking {
            FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)

            val dayRisk = DayRisk(
                123L,
                10.0,
                2,
                1,
                listOf(getExposureWindow())
            )
            val expectedEpidemiologyEvents = getEpidemiologyEvents(dayRisk)
            val expectedInfo = getExposureCircuitBreakerInfo(dayRisk)

            coEvery { exposureWindowRiskManager.getRisk() } returns dayRisk
            coEvery { handleInitialExposureNotification(expectedInfo) } returns Success(
                InitialCircuitBreakerResult.Yes
            )

            val result = testSubject.handleNewExposure()

            coVerifyOrder {
                exposureCircuitBreakerInfoProvider.add(expectedInfo)
                epidemiologyEventProvider.add(expectedEpidemiologyEvents)
                submitEpidemiologyData.submit(expectedEpidemiologyEvents)
                handleInitialExposureNotification(expectedInfo)
                stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(expectedInfo.startOfDayMillis)))
                exposureCircuitBreakerInfoProvider.remove(expectedInfo)
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleNewExposure with risk should add exposure circuit breaker info item, add and submit epidemiology events and attempt initial call when feature flag disabled`() =
        runBlocking {
            FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)

            val dayRisk = DayRisk(
                123L,
                10.0,
                2,
                1,
                listOf(getExposureWindow())
            )
            val expectedEpidemiologyEvents = getEpidemiologyEvents(dayRisk)
            val expectedInfo = getExposureCircuitBreakerInfo(dayRisk)

            coEvery { exposureWindowRiskManager.getRisk() } returns dayRisk
            coEvery { handleInitialExposureNotification(expectedInfo) } returns Success(
                InitialCircuitBreakerResult.Yes
            )

            val result = testSubject.handleNewExposure()

            coVerifyOrder {
                exposureCircuitBreakerInfoProvider.add(expectedInfo)
                submitEpidemiologyData.submit(expectedEpidemiologyEvents)
                handleInitialExposureNotification(expectedInfo)
                stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(expectedInfo.startOfDayMillis)))
                exposureCircuitBreakerInfoProvider.remove(expectedInfo)
                checkIsolationPaymentToken.invoke()
            }

            verify(exactly = 0) { epidemiologyEventProvider.add(any()) }

            assertEquals(Success(Unit), result)
        }

    private fun getExposureWindow(): ExposureWindow =
        Builder().apply {
            setDateMillisSinceEpoch(123L)
            setReportType(ReportType.CONFIRMED_TEST)
            setInfectiousness(Infectiousness.HIGH)
        }.build()

    private fun getEpidemiologyEvents(dayRisk: DayRisk): List<EpidemiologyEvent> =
        dayRisk.exposureWindows.map {
            EpidemiologyEvent(
                version = 1,
                payload = EpidemiologyEventPayload(
                    date = Instant.ofEpochMilli(dayRisk.startOfDayMillis),
                    infectiousness = uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.fromInt(
                        it.infectiousness
                    ),
                    scanInstances = it.scanInstances.map { scanInstance ->
                        EpidemiologyEventPayloadScanInstance(
                            minimumAttenuation = scanInstance.minAttenuationDb,
                            secondsSinceLastScan = scanInstance.secondsSinceLastScan,
                            typicalAttenuation = scanInstance.typicalAttenuationDb
                        )
                    },
                    riskScore = dayRisk.calculatedRisk,
                    riskCalculationVersion = dayRisk.riskCalculationVersion
                )
            )
        }

    private fun getExposureCircuitBreakerInfo(dayRisk: DayRisk): ExposureCircuitBreakerInfo =
        ExposureCircuitBreakerInfo(
            maximumRiskScore = dayRisk.calculatedRisk,
            startOfDayMillis = dayRisk.startOfDayMillis,
            matchedKeyCount = dayRisk.matchedKeyCount,
            riskCalculationVersion = dayRisk.riskCalculationVersion,
            exposureNotificationDate = clock.instant().toEpochMilli(),
            approvalToken = null
        )

    companion object {
        private val now = Instant.now().toEpochMilli()
        private val infoWithoutToken = ExposureCircuitBreakerInfo(1.0, now, 1, 2, now)
        private val infoWithToken = ExposureCircuitBreakerInfo(1.0, Instant.now().toEpochMilli(), 1, 2, now, "token")
    }
}
