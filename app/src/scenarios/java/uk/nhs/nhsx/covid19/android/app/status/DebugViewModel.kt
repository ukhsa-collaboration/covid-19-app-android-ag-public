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
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.fieldtests.utils.KeyFileWriter
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingTokensProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class DebugViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val periodicTasks: PeriodicTasks,
    private val testOrderingTokensProvider: TestOrderingTokensProvider,
    private val venueStorage: VisitedVenuesStorage,
    private val userInbox: UserInbox,
    private val notificationProvider: NotificationProvider,
    private val riskyPostCodeIndicatorProvider: RiskyPostCodeIndicatorProvider,
    private val exposureNotificationApi: ExposureNotificationApi
) : ViewModel() {

    val exposureKeysResult = SingleLiveEvent<ExportToFileResult>()

    fun startDownloadTask() {
        periodicTasks.schedule()
    }

    fun sendPositiveTestResult(context: Context, testKitType: VirologyTestKitType) {
        sendTestResult(context, POSITIVE, testKitType)
    }

    fun sendNegativeTestResult(context: Context, testKitType: VirologyTestKitType) {
        sendTestResult(context, NEGATIVE, testKitType)
    }

    fun sendVoidTestResult(context: Context, testKitType: VirologyTestKitType) {
        sendTestResult(context, VOID, testKitType)
    }

    private fun sendTestResult(
        context: Context,
        virologyTestResult: VirologyTestResult,
        testKitType: VirologyTestKitType
    ) {
        val config = testOrderingTokensProvider.configs.firstOrNull()
        if (config == null) {
            Toast.makeText(context, "Order a test first!", LENGTH_LONG).show()
            return
        }
        viewModelScope.launch {
            delay(3_000)
            testOrderingTokensProvider.remove(config)

            val receivedTestResult = ReceivedTestResult(
                config.diagnosisKeySubmissionToken,
                Instant.now(),
                virologyTestResult,
                testKitType,
                diagnosisKeySubmissionSupported = true
            )

            isolationStateMachine.processEvent(
                OnTestResult(receivedTestResult)
            )
        }
    }

    fun setDefaultState() {
        isolationStateMachine.reset()
    }

    fun setIndexState() {
        isolationStateMachine.reset()
        isolationStateMachine.processEvent(
            OnPositiveSelfAssessment(
                CannotRememberDate
            )
        )
    }

    fun setContactState() {
        isolationStateMachine.reset()
        isolationStateMachine.processEvent(OnExposedNotification(Instant.now()))
    }

    fun setRiskyVenue() {
        viewModelScope.launch {
            venueStorage.finishLastVisitAndAddNewVenue(
                Venue(
                    id = "Risky Venue Id",
                    organizationPartName = "Risky Venue Name"
                )
            )
            userInbox.addUserInboxItem(ShowVenueAlert("Risky Venue Id"))
            notificationProvider.showRiskyVenueVisitNotification()
        }
    }

    fun setRiskyPostCode() {
        riskyPostCodeIndicatorProvider.riskyPostCodeIndicator = RiskIndicatorWrapper(
            "low",
            RiskIndicator(
                colorScheme = GREEN,
                name = Translatable(mapOf("en" to "Tier1")),
                heading = Translatable(mapOf("en" to "Data from the NHS shows that the spread of coronavirus in your area is low.")),
                content = Translatable(
                    mapOf(
                        "en" to "Your local authority has normal measures for coronavirus in place. It’s important that you continue to follow the latest official government guidance to help control the virus.\n" +
                            "\n" +
                            "Find out the restrictions for your local area to help reduce the spread of coronavirus."
                    )
                ),
                linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                linkUrl = Translatable(mapOf("en" to "https://faq.covid19.nhs.uk/article/KA-01270/en-us")),
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

    fun decodeObject(temporaryTracingKey: NHSTemporaryExposureKey): TemporaryExposureKey {
        return TemporaryExposureKeyBuilder()
            .setKeyData(Base64.decode(temporaryTracingKey.key, Base64.DEFAULT))
            .setRollingStartIntervalNumber(temporaryTracingKey.rollingStartNumber)
            .setRollingPeriod(temporaryTracingKey.rollingPeriod)
            .setTransmissionRiskLevel(temporaryTracingKey.transmissionRiskLevel ?: 0)
            .build()
    }

    fun sendExposureNotification() {
        isolationStateMachine.processEvent(OnExposedNotification(Instant.now()))
    }
}

sealed class ExportToFileResult {

    data class ResolutionRequired(val status: Status) : ExportToFileResult()

    data class Success(val file: File) : ExportToFileResult()

    data class Error(val exception: Exception) : ExportToFileResult()
}
