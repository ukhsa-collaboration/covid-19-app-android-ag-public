package uk.nhs.nhsx.covid19.android.app.isolation

import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.google.android.gms.nearby.exposurenotification.ScanInstance.Builder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreaker
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationWork
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Yes
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.CalculateExposureRisk
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EvaluateIfConsideredRisky
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EvaluateMostRelevantRiskyExposure
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowRiskCalculator
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowRiskManager
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.RiskScoreCalculatorProvider
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureNotification
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.InitialData
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.ObservationType.log
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.PowerLossParameters
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.RssiParameters
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.SmootherParameters
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import java.time.LocalDate

class SendExposureNotification(
    isolationStateMachine: IsolationStateMachine,
    getLatestConfiguration: GetLatestConfiguration,
    private val clock: Clock
) {
    operator fun invoke(exposureDate: LocalDate) = runBlocking {
        coEvery { exposureNotificationApi.getExposureWindows() } returns getExposureWindows(exposureDate)
        coEvery { exposureConfigurationApi.getExposureConfiguration() } returns exposureConfigurationResponse
        coEvery { exposureNotificationApi.getDiagnosisKeysDataMapping() } returns diagnosisKeyDataMapping
        every { calculateExposureRisk(any(), any(), any()) } returns 2.0
        every { riskScoreCalculatorProvider.getRiskCalculationVersion() } returns 2
        coEvery { handleInitialExposureNotification.invoke(any()) } returns Success(Yes)

        exposureNotificationWork.evaluateRisk()
    }

    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val exposureConfigurationApi = mockk<ExposureConfigurationApi>()
    private val calculateExposureRisk = mockk<CalculateExposureRisk>()
    private val riskScoreCalculatorProvider = mockk<RiskScoreCalculatorProvider>()
    private val handleInitialExposureNotification = mockk<HandleInitialExposureNotification>()

    private val evaluateIfConsideredRisky = EvaluateIfConsideredRisky(
        isolationStateMachine,
        getLatestConfiguration,
        clock
    )

    private val exposureWindowRiskCalculator = ExposureWindowRiskCalculator(
        evaluateMostRelevantRiskyExposure = EvaluateMostRelevantRiskyExposure(),
        evaluateIfConsideredRisky = evaluateIfConsideredRisky,
        calculateExposureRisk = calculateExposureRisk,
        riskScoreCalculatorProvider = riskScoreCalculatorProvider,
        analyticsEventProcessor = mockk(relaxUnitFun = true)
    )

    private val exposureWindowRiskManager = ExposureWindowRiskManager(
        exposureConfigurationApi = exposureConfigurationApi,
        exposureNotificationApi = exposureNotificationApi,
        exposureWindowRiskCalculator = exposureWindowRiskCalculator
    )

    private val exposureCircuitBreaker = ExposureCircuitBreaker(
        handleInitialExposureNotification = handleInitialExposureNotification,
        handlePollingExposureNotification = mockk(),
        stateMachine = isolationStateMachine,
        exposureCircuitBreakerInfoProvider = mockk(relaxUnitFun = true),
        analyticsEventProcessor = mockk(relaxUnitFun = true)
    )

    private val exposureNotificationWork = ExposureNotificationWork(
        submitEmptyData = mockk(),
        checkIsolationPaymentToken = mockk(relaxed = true),
        exposureCircuitBreaker = exposureCircuitBreaker,
        exposureCircuitBreakerInfoProvider = mockk(relaxUnitFun = true),
        exposureWindowRiskManager = exposureWindowRiskManager,
        epidemiologyDataManager = mockk(relaxUnitFun = true),
        hasSuccessfullyProcessedNewExposureProvider = mockk(relaxUnitFun = true),
        clock = clock
    )

    private fun getExposureWindows(exposureDate: LocalDate) =
        listOf(
            ExposureWindow.Builder()
                .setDateMillisSinceEpoch(
                    exposureDate.atStartOfDay(clock.zone).toInstant().toEpochMilli()
                )
                .setInfectiousness(Infectiousness.HIGH)
                .setScanInstances(
                    listOf(
                        Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                        Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                        Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                        Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                        Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                        Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build()
                    )
                )
                .build()
        )

    companion object {
        private val exposureConfigurationResponse = ExposureConfigurationResponse(
            exposureNotification = ExposureNotification(
                minimumRiskScore = 11,
                attenuationDurationThresholds = listOf(55, 63),
                attenuationLevelValues = listOf(0, 1, 1, 1, 1, 1, 1, 1),
                daysSinceLastExposureLevelValues = listOf(5, 5, 5, 5, 5, 5, 5, 5),
                durationLevelValues = listOf(0, 0, 0, 1, 1, 1, 1, 0),
                transmissionRiskLevelValues = listOf(1, 3, 4, 5, 6, 7, 8, 6),
                attenuationWeight = 50.0,
                daysSinceLastExposureWeight = 20,
                durationWeight = 50.0,
                transmissionRiskWeight = 50.0
            ),
            v2RiskCalculation = V2RiskCalculation(
                daysSinceOnsetToInfectiousness = listOf(
                    0, 0, 0, 0, 0, 0, 0, 0, 0,
                    1, 1, 1,
                    2, 2, 2, 2, 2, 2,
                    1, 1, 1, 1, 1, 1,
                    0, 0, 0, 0, 0
                ),
                infectiousnessWeights = listOf(0.0, 0.4, 1.0),
                reportTypeWhenMissing = 1,
                riskThreshold = 1.0
            ),
            riskScore = RiskScore(
                sampleResolution = 1.0,
                expectedDistance = 0.1,
                minimumDistance = 1.0,
                rssiParameters = RssiParameters(
                    weightCoefficient = 0.1270547531082051,
                    intercept = 4.2309333657856945,
                    covariance = 0.4947614361027773
                ),
                powerLossParameters = PowerLossParameters(
                    wavelength = 0.125,
                    pathLossFactor = 20.0,
                    refDeviceLoss = 0.0
                ),
                observationType = log,
                initialData = InitialData(
                    mean = 2.0,
                    covariance = 10.0
                ),
                smootherParameters = SmootherParameters(
                    alpha = 1.0,
                    beta = 0.0,
                    kappa = 0.0
                )
            )
        )
        private val diagnosisKeyDataMapping = DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
            .setDaysSinceOnsetToInfectiousness(mapOf())
            .setReportTypeWhenMissing(ReportType.SELF_REPORT)
            .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.NONE)
            .build()
    }
}
