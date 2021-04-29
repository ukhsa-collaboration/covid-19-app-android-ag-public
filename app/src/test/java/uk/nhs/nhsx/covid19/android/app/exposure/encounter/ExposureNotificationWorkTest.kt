package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.google.android.gms.nearby.exposurenotification.CalibrationConfidence
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.ExposureWindow.Builder
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.DayRisk
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowRiskManager
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowWithRisk
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.PartitionExposureWindowsResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.RiskCalculationResult
import uk.nhs.nhsx.covid19.android.app.payment.CheckIsolationPaymentToken
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExposureNotificationWorkTest {

    private val submitEmptyData = mockk<SubmitEmptyData>(relaxUnitFun = true)
    private val checkIsolationPaymentToken = mockk<CheckIsolationPaymentToken>(relaxUnitFun = true)
    private val exposureCircuitBreaker = mockk<ExposureCircuitBreaker>(relaxUnitFun = true)
    private val exposureCircuitBreakerInfoProvider = mockk<ExposureCircuitBreakerInfoProvider>(relaxUnitFun = true)
    private val exposureWindowRiskManager = mockk<ExposureWindowRiskManager>(relaxUnitFun = true)
    private val epidemiologyDataManager = mockk<EpidemiologyDataManager>(relaxUnitFun = true)
    private val hasSuccessfullyProcessedNewExposureProvider =
        mockk<HasSuccessfullyProcessedNewExposureProvider>(relaxUnitFun = true)
    private val clock = Clock.fixed(Instant.parse("2020-12-24T20:00:00Z"), ZoneOffset.UTC)

    private val testSubject = ExposureNotificationWork(
        submitEmptyData,
        checkIsolationPaymentToken,
        exposureCircuitBreaker,
        exposureCircuitBreakerInfoProvider,
        exposureWindowRiskManager,
        epidemiologyDataManager,
        hasSuccessfullyProcessedNewExposureProvider,
        clock
    )

    @Before
    fun setUp() {
        every { hasSuccessfullyProcessedNewExposureProvider.value } returns null
    }

    @Test
    fun `handle no matches calls fake circuit breaker then submits fake exposure windows and returns success`() =
        runBlocking {
            val result = testSubject.handleNoMatchesFound()

            verify { submitEmptyData() }
            coVerify(exactly = 1) { epidemiologyDataManager.submitEmptyExposureWindows() }
            coVerify(exactly = 0) { checkIsolationPaymentToken.invoke() }
            coVerify(exactly = 0) { exposureCircuitBreakerInfoProvider.add(any()) }
            coVerify(exactly = 0) { exposureCircuitBreaker.handleInitial(any()) }
            coVerify(exactly = 0) { exposureCircuitBreaker.handlePolling(any()) }

            assertTrue { result is Success }
        }

    @Test
    fun `calling handleUnprocessedRequests without circuit breaker info items return success`() = runBlocking {
        every { exposureCircuitBreakerInfoProvider.info } returns emptyList()

        coEvery { checkIsolationPaymentToken.invoke() } returns Success(Unit)

        val result = testSubject.handleUnprocessedRequests()

        coVerify(exactly = 0) { exposureCircuitBreaker.handleInitial(any()) }
        coVerify(exactly = 0) { exposureCircuitBreaker.handlePolling(any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }

        assertEquals(Success(Unit), result)
    }

    @Test
    fun `calling handleUnprocessedRequests without approval token trigger initial circuit breaker and return success`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithoutToken)

            coEvery { checkIsolationPaymentToken.invoke() } returns Success(Unit)

            val result = testSubject.handleUnprocessedRequests()

            coVerify(exactly = 1) { exposureCircuitBreaker.handleInitial(infoWithoutToken) }
            coVerify(exactly = 0) { exposureCircuitBreaker.handlePolling(any()) }
            coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests with approval token trigger polling circuit breaker and return success`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithToken)

            coEvery { checkIsolationPaymentToken.invoke() } returns Success(Unit)

            val result = testSubject.handleUnprocessedRequests()

            coVerify(exactly = 0) { exposureCircuitBreaker.handleInitial(any()) }
            coVerify(exactly = 1) { exposureCircuitBreaker.handlePolling(infoWithToken) }
            coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleUnprocessedRequests with two exposure circuit breaker info items returns success`() =
        runBlocking {
            every { exposureCircuitBreakerInfoProvider.info } returns listOf(infoWithoutToken, infoWithToken)

            coEvery { checkIsolationPaymentToken.invoke() } returns Success(Unit)

            val result = testSubject.handleUnprocessedRequests()

            coVerifyOrder {
                exposureCircuitBreaker.handleInitial(infoWithoutToken)
                exposureCircuitBreaker.handlePolling(infoWithToken)
                checkIsolationPaymentToken.invoke()
            }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleNewExposure with no risk should provide exposure windows to epidemiology data manager`() =
        runBlocking {
            coEvery { exposureWindowRiskManager.getRisk() } returns RiskCalculationResult(
                relevantRisk = null,
                partitionedExposureWindows = PartitionExposureWindowsResult(emptyList(), emptyList())
            )

            coEvery { checkIsolationPaymentToken.invoke() } returns Success(Unit)

            coEvery { checkIsolationPaymentToken.invoke() } returns Success(Unit)

            val result = testSubject.handleNewExposure()

            coVerifyOrder {
                hasSuccessfullyProcessedNewExposureProvider setProperty "value" value eq(false)
                submitEmptyData()
                hasSuccessfullyProcessedNewExposureProvider setProperty "value" value eq(true)
                epidemiologyDataManager.storeAndSubmit(
                    PartitionExposureWindowsResult(
                        riskyExposureWindows = emptyList(),
                        nonRiskyExposureWindows = emptyList()
                    )
                )
                checkIsolationPaymentToken.invoke()
            }

            coVerify(exactly = 0) { exposureCircuitBreakerInfoProvider.add(any()) }
            coVerify(exactly = 0) { exposureCircuitBreaker.handleInitial(any()) }
            coVerify(exactly = 0) { exposureCircuitBreaker.handlePolling(any()) }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleNewExposure with risk should add exposure circuit breaker info item, provide exposure windows to epidemiology data manager and attempt initial call`() =
        runBlocking {
            val relevantRisk = DayRisk(
                123L,
                10.0,
                2,
                1
            )
            val riskyExposureWindows = getExposureWindowsWithRisk(isConsideredRisky = true, 7.0, 10.0)
            val nonRiskyExposureWindows = getExposureWindowsWithRisk(isConsideredRisky = false, 0.1, 0.2)

            val expectedInfo = getExposureCircuitBreakerInfo(relevantRisk)

            coEvery { exposureWindowRiskManager.getRisk() } returns RiskCalculationResult(
                relevantRisk,
                PartitionExposureWindowsResult(
                    riskyExposureWindows = riskyExposureWindows,
                    nonRiskyExposureWindows = nonRiskyExposureWindows
                )
            )

            coEvery { checkIsolationPaymentToken.invoke() } returns Success(Unit)

            val result = testSubject.handleNewExposure()

            coVerifyOrder {
                hasSuccessfullyProcessedNewExposureProvider setProperty "value" value eq(false)
                exposureCircuitBreakerInfoProvider.add(expectedInfo)
                hasSuccessfullyProcessedNewExposureProvider setProperty "value" value eq(true)
                epidemiologyDataManager.storeAndSubmit(
                    PartitionExposureWindowsResult(
                        riskyExposureWindows = riskyExposureWindows,
                        nonRiskyExposureWindows = nonRiskyExposureWindows
                    )
                )
                exposureCircuitBreaker.handleInitial(expectedInfo)
                checkIsolationPaymentToken.invoke()
            }

            coVerify(exactly = 0) { exposureCircuitBreaker.handlePolling(any()) }

            assertEquals(Success(Unit), result)
        }

    @Test
    fun `calling handleNewExposure results in exception during risk score calculation`() =
        runBlocking {
            val expectedException = Exception()

            coEvery { exposureWindowRiskManager.getRisk() } throws expectedException

            coEvery { checkIsolationPaymentToken.invoke() } returns Success(Unit)

            val result = testSubject.handleNewExposure()

            verify(exactly = 1) { hasSuccessfullyProcessedNewExposureProvider setProperty "value" value eq(false) }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.add(any()) }
            verify(exactly = 0) { hasSuccessfullyProcessedNewExposureProvider setProperty "value" value eq(true) }
            coVerify(exactly = 0) { epidemiologyDataManager.storeAndSubmit(any()) }
            coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }

            assertEquals(Failure(expectedException), result)
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

        fun getExposureWindowsWithRisk(isConsideredRisky: Boolean, vararg riskValues: Double): List<ExposureWindowWithRisk> {
            return riskValues.map { risk ->
                ExposureWindowWithRisk(
                    getExposureWindow(),
                    calculatedRisk = risk,
                    riskCalculationVersion = 2,
                    matchedKeyCount = 1,
                    isConsideredRisky = isConsideredRisky
                )
            }
        }

        private fun getExposureWindow(): ExposureWindow =
            Builder()
                .setDateMillisSinceEpoch(123L)
                .setReportType(ReportType.CONFIRMED_TEST)
                .setInfectiousness(Infectiousness.HIGH)
                .setCalibrationConfidence(CalibrationConfidence.HIGH)
                .setScanInstances(
                    listOf(
                        ScanInstance.Builder().setMinAttenuationDb(10)
                            .setSecondsSinceLastScan(20)
                            .setTypicalAttenuationDb(30)
                            .build()
                    )
                )
                .build()
    }
}
