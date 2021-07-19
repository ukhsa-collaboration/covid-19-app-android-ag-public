package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import android.util.Base64
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey.TemporaryExposureKeyBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalyticsAlarmController
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.di.ApplicationClock
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfo
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfoProvider
import uk.nhs.nhsx.covid19.android.app.fieldtests.utils.KeyFileWriter
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenueConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderPollingConfig
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingTokensProvider
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.ReceivedUnknownTestResultProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class DebugViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val periodicTasks: PeriodicTasks,
    private val testOrderingTokensProvider: TestOrderingTokensProvider,
    private val venueStorage: VisitedVenuesStorage,
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
    private val notificationProvider: NotificationProvider,
    private val riskyPostCodeIndicatorProvider: RiskyPostCodeIndicatorProvider,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val riskyVenueConfigurationProvider: RiskyVenueConfigurationProvider,
    private val submitAnalyticsAlarmController: SubmitAnalyticsAlarmController,
    private val exposureCircuitBreakerInfoProvider: ExposureCircuitBreakerInfoProvider,
    private val receivedUnknownTestResultProvider: ReceivedUnknownTestResultProvider,
    private val clock: Clock,
    private val dateChangeReceiver: DateChangeReceiver,
) : ViewModel() {

    val exposureKeysResult = SingleLiveEvent<ExportToFileResult>()

    fun startDownloadTask() {
        periodicTasks.schedule()
    }

    fun submitAnalyticsUsingAlarmManager() {
        submitAnalyticsAlarmController.onAlarmTriggered()
    }

    fun sendPositiveConfirmedTestResult(context: Context) {
        sendConfirmedTestResult(context, POSITIVE)
    }

    fun sendNegativeConfirmedTestResult(context: Context) {
        sendConfirmedTestResult(context, NEGATIVE)
    }

    fun sendVoidConfirmedTestResult(context: Context) {
        sendConfirmedTestResult(context, VOID)
    }

    fun sendPlodConfirmedTestResult(context: Context) {
        sendConfirmedTestResult(context, PLOD)
    }

    fun sendPositiveUnconfirmedTestResult() {
        sendTestResult(
            virologyTestResult = POSITIVE,
            testKitType = RAPID_RESULT,
            requiresConfirmatoryTest = true,
            confirmatoryDayLimit = 2,
            token = "token"
        )
    }

    private fun sendConfirmedTestResult(
        context: Context,
        virologyTestResult: VirologyTestResult
    ) {
        val config = testOrderingTokensProvider.configs.firstOrNull()
        if (config == null) {
            Toast.makeText(context, "Order a test first!", LENGTH_LONG).show()
            return
        }
        sendTestResult(
            virologyTestResult = virologyTestResult,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = false,
            confirmatoryDayLimit = null,
            config = config
        )
    }

    private fun sendTestResult(
        virologyTestResult: VirologyTestResult,
        testKitType: VirologyTestKitType,
        requiresConfirmatoryTest: Boolean = false,
        confirmatoryDayLimit: Int? = null,
        config: TestOrderPollingConfig? = null,
        token: String? = config?.diagnosisKeySubmissionToken
    ) {
        viewModelScope.launch {
            delay(3_000)
            config?.let { testOrderingTokensProvider.remove(it) }

            val receivedTestResult = ReceivedTestResult(
                token,
                Instant.now(clock),
                virologyTestResult,
                testKitType,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = requiresConfirmatoryTest,
                confirmatoryDayLimit = confirmatoryDayLimit
            )

            isolationStateMachine.processEvent(
                OnTestResult(receivedTestResult, testOrderType = INSIDE_APP)
            )
        }
    }

    fun sendUnknownTestResult() {
        receivedUnknownTestResultProvider.value = true
    }

    fun setDefaultState() {
        isolationStateMachine.reset()
    }

    fun setIndexState() {
        isolationStateMachine.reset()
        isolationStateMachine.processEvent(OnPositiveSelfAssessment(CannotRememberDate))
    }

    fun setContactState() {
        isolationStateMachine.reset()
        sendExposureNotification()
    }

    fun setRiskyVenue(type: RiskyVenueMessageType) {
        viewModelScope.launch {
            venueStorage.finishLastVisitAndAddNewVenue(
                Venue(
                    id = "Risky Venue Id",
                    organizationPartName = "Risky Venue Name",
                    postCode = "PO367GZ"
                )
            )
            if (type == BOOK_TEST) {
                lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = LastVisitedBookTestTypeVenueDate(
                    LocalDate.now(),
                    riskyVenueConfigurationProvider.durationDays
                )
            }
            riskyVenueAlertProvider.riskyVenueAlert = RiskyVenueAlert("Risky Venue Id", type)
            notificationProvider.showRiskyVenueVisitNotification(type)
        }
    }

    fun setRiskyPostCode() {
        riskyPostCodeIndicatorProvider.riskyPostCodeIndicator = RiskIndicatorWrapper(
            "low",
            RiskIndicator(
                colorScheme = GREEN,
                colorSchemeV2 = GREEN,
                name = TranslatableString(mapOf("en" to "Tier1")),
                heading = TranslatableString(mapOf("en" to "Data from the NHS shows that the spread of coronavirus in your area is low.")),
                content = TranslatableString(
                    mapOf(
                        "en" to "Your local authority has normal measures for coronavirus in place. It’s important that you continue to follow the latest official government guidance to help control the virus.\n" +
                            "\n" +
                            "Find out the restrictions for your local area to help reduce the spread of coronavirus."
                    )
                ),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://faq.covid19.nhs.uk/article/KA-01270/en-us")),
                policyData = null
            )
        )
    }

    fun importKeys(file: File) {
        viewModelScope.launch {
            exposureNotificationApi.provideDiagnosisKeys(listOf(file))
        }
    }

    fun exportKeys(context: Context) {
        viewModelScope.launch {
            val result = try {
                val keys: List<NHSTemporaryExposureKey> =
                    exposureNotificationApi.temporaryExposureKeyHistory()
                Timber.d("Keys: $keys")
                val writer = KeyFileWriter(context)
                val files = writer.writeForKeys(
                    keys.map(::decodeObject),
                    Instant.now().minus(14, ChronoUnit.DAYS),
                    Instant.now(),
                    "GB"
                )
                val fileForExport = files[0]

                ExportToFileResult.Success(fileForExport)
            } catch (apiException: ApiException) {
                if (apiException.status.hasResolution()) {
                    ExportToFileResult.ResolutionRequired(apiException.status)
                } else {
                    ExportToFileResult.Error(apiException)
                }
            } catch (exception: Exception) {
                ExportToFileResult.Error(exception)
            }

            exposureKeysResult.postValue(result)
        }
    }

    fun sendExposureNotification() {
        exposureCircuitBreakerInfoProvider.add(
            ExposureCircuitBreakerInfo(
                maximumRiskScore = 101.0,
                startOfDayMillis = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
                matchedKeyCount = 1,
                riskCalculationVersion = 2,
                exposureNotificationDate = Instant.now().toEpochMilli(),
                approvalToken = null
            )
        )
        startDownloadTask()
    }

    fun decodeObject(temporaryTracingKey: NHSTemporaryExposureKey): TemporaryExposureKey {
        return TemporaryExposureKeyBuilder()
            .setKeyData(Base64.decode(temporaryTracingKey.key, Base64.DEFAULT))
            .setRollingStartIntervalNumber(temporaryTracingKey.rollingStartNumber)
            .setRollingPeriod(temporaryTracingKey.rollingPeriod)
            .setTransmissionRiskLevel(temporaryTracingKey.transmissionRiskLevel ?: 0)
            .build()
    }

    fun onOffsetDaysChanged(offsetDays: Long) {
        (clock as ApplicationClock).offsetDays(offsetDays)
    }

    fun getDateChangeReceiver() = dateChangeReceiver
}

sealed class ExportToFileResult {

    data class ResolutionRequired(val status: Status) : ExportToFileResult()

    data class Success(val file: File) : ExportToFileResult()

    data class Error(val exception: Exception) : ExportToFileResult()
}
