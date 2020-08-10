package uk.nhs.nhsx.covid19.android.app.fieldtests.ui.main

import android.app.Application
import android.content.pm.PackageManager
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.fieldtests.utils.CollectedKeysHandler
import uk.nhs.nhsx.covid19.android.app.fieldtests.utils.RequestCodes
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.fieldtests.nearby.SubmitDataWorker
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.ApiClient
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.DeviceInfo
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.ErrorResponse
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.ExperimentInfo
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.MatchingResult
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.Results
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.TemporaryTracingKey
import uk.nhs.nhsx.covid19.android.app.fieldtests.notifications.NotificationHelper
import uk.nhs.nhsx.covid19.android.app.fieldtests.storage.ExperimentSettingsProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Instant
import java.time.format.DateTimeFormatter

class FieldTestViewModel(application: Application) : AndroidViewModel(application) {

    private val moshi = Moshi.Builder().build()
    private val errorResponseAdapter = moshi.adapter(ErrorResponse::class.java)

    data class ResolutionRequiredEvent(
        val requestCode: Int,
        val apiException: ApiException
    )

    private val notificationHelper = NotificationHelper(application)

    private val experimentSettingsProvider =
        ExperimentSettingsProvider(
            application
        )

    val resolutionRequiredLiveEvent = SingleLiveEvent<ResolutionRequiredEvent>()
    val displaySnackbarLiveEvent = SingleLiveEvent<String>()
    val displayParticipantsLiveEvent = SingleLiveEvent<List<String>>()
    val hasAccessToKeyHistoryData = MutableLiveData(false)

    private val exposureNotificationClient = Nearby.getExposureNotificationClient(getApplication())

    fun onResume() {
        viewModelScope.launch {
            hasAccessToKeyHistoryData.postValue(hasAccessToKeyHistory())
        }
    }

    fun toggleExposureNotifications() {
        viewModelScope.launch {
            if (hasAccessToKeyHistory()) {
                stopExposureApi()
            } else {
                startExposureApi()
            }
        }
    }

    private suspend fun hasAccessToKeyHistory(): Boolean {
        return try {
            exposureNotificationClient.temporaryExposureKeyHistory.await()
            true
        } catch (exception: Exception) {
            false
        }
    }

    private fun startExposureApi(
        onSuccess: () -> Unit = {
            if (experimentSettingsProvider.getExperimentId().isNotEmpty()) {
                rescheduleNotifications()
            }
        }
    ) {
        viewModelScope.launch {
            try {
                if (tryStartExposureNotificationClient() && tryGetExposureKeyHistoryPermission()) {
                    onSuccess()
                }
            } catch (exception: Exception) {
                Timber.e(exception)
                displaySnackbarLiveEvent.postValue("Exposure API is not available on this phone")
            }
        }
    }

    private suspend fun tryStartExposureNotificationClient(): Boolean {
        try {
            exposureNotificationClient.start().await()
            return true
        } catch (apiException: ApiException) {
            if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                resolutionRequiredLiveEvent.postValue(
                    ResolutionRequiredEvent(
                        RequestCodes.REQUEST_CODE_START_EXPOSURE_NOTIFICATION,
                        apiException
                    )
                )
            } else {
                throw apiException
            }
        }
        return false
    }

    private suspend fun tryGetExposureKeyHistoryPermission(): Boolean {
        try {
            exposureNotificationClient.temporaryExposureKeyHistory.await()
            hasAccessToKeyHistoryData.postValue(true)
            return true
        } catch (apiException: ApiException) {
            if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                resolutionRequiredLiveEvent.postValue(
                    ResolutionRequiredEvent(
                        RequestCodes.REQUEST_CODE_GET_TEMP_EXPOSURE_KEY_HISTORY,
                        apiException
                    )
                )
            } else {
                throw apiException
            }
        }
        return false
    }

    /**
     * Handles {@value android.app.Activity#RESULT_OK} for a resolution. User accepted opt-in.
     */
    fun resolutionResultOk(joinExperiment: Boolean = false) {
        if (joinExperiment) {
            startExposureApi { joinExperimentInternal() }
        } else {
            startExposureApi()
        }
    }

    fun joinExperiment() {
        startExposureApi { joinExperimentInternal() }
    }

    private fun joinExperimentInternal() {
        val googlePlayServicesVersion =
            getVersionNameForPackage(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)
        val teamId = experimentSettingsProvider.teamId
        val deviceName = experimentSettingsProvider.deviceName
        viewModelScope.launch {
            val keys = exposureNotificationClient.temporaryExposureKeyHistory.await()
                .map { key ->
                    TemporaryTracingKey(
                        key = Base64.encodeToString(key.keyData, Base64.NO_WRAP),
                        intervalNumber = key.rollingStartIntervalNumber,
                        intervalCount = 144
                    )
                }
            try {
                val experiment = ApiClient.service.getLatestExperiment(teamId)
                val response = ApiClient.service.joinExperiment(
                    teamId,
                    experiment.experimentId,
                    DeviceInfo(
                        deviceName,
                        googlePlayServicesVersion = googlePlayServicesVersion,
                        temporaryTracingKeys = keys,
                        results = listOf<Results>()
                    )
                )
                if (response.isSuccessful) {
                    experimentSettingsProvider.putConfigurations(experiment.requestedConfigurations)
                    experimentSettingsProvider.experimentName = experiment.experimentName
                    experimentSettingsProvider.setExperimentId(experiment.experimentId)
                    experimentSettingsProvider.automaticDetectionFrequency =
                        experiment.automaticDetectionFrequency

                    rescheduleNotifications()
                } else {
                    val source = response.errorBody()?.string()
                    if (source != null) {
                        val error = errorResponseAdapter.fromJson(source)
                        val message = error?.error?.message ?: "Can't join experiment"
                        displaySnackbarLiveEvent.postValue(message)
                    }
                }
            } catch (exception: Exception) {
                displaySnackbarLiveEvent.postValue("Can't join experiment: ${exception.message}")
                Timber.e(exception, "Can't join experiment")
            }
        }
    }

    private fun rescheduleNotifications() {
        val submitDataWorker = SubmitDataWorker(getApplication())
        submitDataWorker.cancelAlarm()
        if (experimentSettingsProvider.automaticDetectionFrequency > 0) {
            submitDataWorker.scheduleSubmit()
        }
    }

    fun seeExperimentDetails() {
        val teamId = experimentSettingsProvider.teamId
        val experimentId = experimentSettingsProvider.getExperimentId()
        viewModelScope.launch {
            try {
                val info = ApiClient.service.getExperimentInfo(teamId, experimentId)
                val participantNames = info.participants.map { "${it.deviceName} has uploaded ${it.results?.size} keys" }
                displayParticipantsLiveEvent.postValue(participantNames)
            } catch (exception: Exception) {
                displaySnackbarLiveEvent.postValue("Can't see the experiment's details")
            }
        }
    }

    fun createNewExperiment(googlePlayServicesVersion: String) {
        viewModelScope.launch {
            val teamId = experimentSettingsProvider.teamId
            val deviceName = experimentSettingsProvider.deviceName
            try {
                val keys = exposureNotificationClient.temporaryExposureKeyHistory.await()
                    .map { key ->
                        TemporaryTracingKey(
                            key = Base64.encodeToString(key.keyData, Base64.NO_WRAP),
                            intervalNumber = key.rollingStartIntervalNumber,
                            intervalCount = 144
                        )
                    }

                val experimentInfo = ExperimentInfo(
                    iosAppVersion = "Any",
                    androidAppVersion = "Any",
                    lead = DeviceInfo(
                        deviceName,
                        googlePlayServicesVersion = googlePlayServicesVersion,
                        temporaryTracingKeys = keys,
                        results = listOf()

                    )
                )
                val resultingExperimentInfo =
                    ApiClient.service.createExperiment(teamId, experimentInfo)
                experimentSettingsProvider.experimentName = resultingExperimentInfo.experimentName
                experimentSettingsProvider.setExperimentId(resultingExperimentInfo.experimentId)
            } catch (exception: Exception) {
                displaySnackbarLiveEvent.postValue("Can't create a new experiment")
            }
        }
    }

    fun processAndPostResults() {
        val teamId = experimentSettingsProvider.teamId
        val deviceName = experimentSettingsProvider.deviceName
        val experimentId = experimentSettingsProvider.getExperimentId()
        val configurations = experimentSettingsProvider.getConfigurations()
        viewModelScope.launch {
            try {
                notificationHelper.showNotification()
                val info = ApiClient.service.getExperimentInfo(teamId, experimentId)
                val collectedKeysHandler = CollectedKeysHandler(getApplication())
                val exposureEvents = collectedKeysHandler.handle(configurations, info)
                exposureEvents.entries.forEach { (configuration, exposureEvents) ->
                    val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    val result = MatchingResult(timestamp, configuration, exposureEvents)
                    ApiClient.service.sendResult(teamId, experimentId, deviceName, result)
                }

                Timber.d("Exposure events: $exposureEvents")
                displaySnackbarLiveEvent.postValue("Results uploaded: ${exposureEvents.size}")
            } catch (exception: Exception) {
                Timber.e(exception)
                displaySnackbarLiveEvent.postValue("Can't process and post results")
            } finally {
                notificationHelper.hideNotification()
            }
        }
    }

    private fun stopExposureApi() {
        val submitDataWorker = SubmitDataWorker(getApplication())
        submitDataWorker.cancelAlarm()
        viewModelScope.launch {
            try {
                exposureNotificationClient.stop()
                hasAccessToKeyHistoryData.postValue(false)
            } catch (exception: Exception) {
                displaySnackbarLiveEvent.postValue("Can't stop Exposure Notifications")
            }
        }
    }

    private fun getVersionNameForPackage(packageName: String): String {
        try {
            return getApplication<Application>().packageManager.getPackageInfo(
                packageName,
                0
            ).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Couldn't get the app version")
        }
        return "Not available"
    }
}
