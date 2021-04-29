package uk.nhs.nhsx.covid19.android.app.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class ExposureStatusViewModel @Inject constructor(
    private val exposureNotificationManager: ExposureNotificationManager,
    private val exposureNotificationReminderAlarmController: ExposureNotificationReminderAlarmController,
    private val resumeContactTracingNotificationTimeProvider: ResumeContactTracingNotificationTimeProvider,
    private val clock: Clock
) : ViewModel() {

    private val exposureNotificationActivationResult =
        SingleLiveEvent<ExposureNotificationActivationResult>()

    fun exposureNotificationActivationResult(): SingleLiveEvent<ExposureNotificationActivationResult> =
        exposureNotificationActivationResult

    private val exposureNotificationsChangedLiveData = MutableLiveData<Boolean>()

    fun exposureNotificationsChanged(): LiveData<Boolean> =
        distinctUntilChanged(exposureNotificationsChangedLiveData)

    private val exposureNotificationsEnabledLiveData = MutableLiveData<Boolean>()

    fun exposureNotificationsEnabled(): LiveData<Boolean> = exposureNotificationsEnabledLiveData

    fun checkExposureNotificationsChanged() {
        viewModelScope.launch {
            exposureNotificationsChangedLiveData.postValue(exposureNotificationManager.isEnabled())
        }
    }

    fun checkExposureNotificationsEnabled() {
        viewModelScope.launch {
            exposureNotificationsEnabledLiveData.postValue(exposureNotificationManager.isEnabled())
        }
    }

    fun startExposureNotifications() {
        viewModelScope.launch {
            val startResult = if (exposureNotificationManager.isEnabled()) {
                Success
            } else {
                val result = exposureNotificationManager.startExposureNotifications()
                checkExposureNotificationsChanged()
                result
            }
            exposureNotificationActivationResult.postValue(startResult)
            if (startResult == Success) {
                exposureNotificationReminderAlarmController.cancel()
                resumeContactTracingNotificationTimeProvider.value = null
            }
        }
    }

    fun stopExposureNotifications() {
        viewModelScope.launch {
            exposureNotificationManager.stopExposureNotifications()
            checkExposureNotificationsChanged()
        }
    }

    fun scheduleExposureNotificationReminder(delay: Duration) {
        val alarmTime = Instant.now(clock).plus(delay)
        resumeContactTracingNotificationTimeProvider.value = alarmTime.toEpochMilli()
        exposureNotificationReminderAlarmController.setup(alarmTime)
    }

    companion object {
        const val REQUEST_CODE_SUBMIT_KEYS_PERMISSION = 1338
    }
}
