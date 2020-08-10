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
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.toExposureConfiguration
import uk.nhs.nhsx.covid19.android.app.fieldtests.utils.KeyFileWriter
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.OnNegativeTestResult
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingTokensProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

class DebugViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val periodicTasks: PeriodicTasks,
    private val latestTestResultProvider: LatestTestResultProvider,
    private val testOrderingTokensProvider: TestOrderingTokensProvider,
    private val venueStorage: VisitedVenuesStorage,
    private val userInbox: UserInbox,
    private val notificationProvider: NotificationProvider,
    private val riskyPostCodeDetectedProvider: RiskyPostCodeDetectedProvider,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val exposureConfigurationApi: ExposureConfigurationApi
) : ViewModel() {

    val exposureKeysResult = SingleLiveEvent<ExportToFileResult>()

    fun startDownloadTask() {
        periodicTasks.schedule(keepPrevious = false)
    }

    fun sendPositiveTestResult(context: Context) {
        val config = testOrderingTokensProvider.configs.firstOrNull()
        if (config == null) {
            Toast.makeText(context, "Order a test first!", LENGTH_LONG).show()
            return
        }
        viewModelScope.launch {
            delay(3_000)
            testOrderingTokensProvider.remove(config)
            val latestTestResult =
                LatestTestResult(config.diagnosisKeySubmissionToken, Instant.now(), POSITIVE)
            latestTestResultProvider.latestTestResult = latestTestResult

            isolationStateMachine.processEvent(
                OnPositiveTestResult(
                    Instant.now()
                )
            )
        }
    }

    fun sendNegativeTestResult(context: Context) {
        val config = testOrderingTokensProvider.configs.firstOrNull()
        if (config == null) {
            Toast.makeText(context, "Order a test first!", LENGTH_LONG).show()
            return
        }
        viewModelScope.launch {
            delay(3_000)
            testOrderingTokensProvider.remove(config)
            val latestTestResult =
                LatestTestResult(config.diagnosisKeySubmissionToken, Instant.now(), NEGATIVE)
            latestTestResultProvider.latestTestResult = latestTestResult

            isolationStateMachine.processEvent(
                OnNegativeTestResult(
                    Instant.now()
                )
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
        riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(HIGH)
    }

    fun importKeys(file: File) {
        viewModelScope.launch {
            val configuration =
                exposureConfigurationApi.getExposureConfiguration().toExposureConfiguration()
            exposureNotificationApi.provideDiagnosisKeys(
                listOf(file),
                configuration,
                "manual_import_" + UUID.randomUUID().toString()
            )
        }
    }

    fun exportKeys(context: Context) {
        viewModelScope.launch {
            val result = try {
                val keys: List<NHSTemporaryExposureKey> =
                    exposureNotificationApi.temporaryExposureKeyHistory()
                Timber.d("Keys: $keys")
                val writer = KeyFileWriter(context, signer = null)
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
            .setTransmissionRiskLevel(7)
            .build()
    }
}

sealed class ExportToFileResult {

    data class ResolutionRequired(val status: Status) : ExportToFileResult()

    data class Success(val file: File) : ExportToFileResult()

    data class Error(val exception: Exception) : ExportToFileResult()
}
