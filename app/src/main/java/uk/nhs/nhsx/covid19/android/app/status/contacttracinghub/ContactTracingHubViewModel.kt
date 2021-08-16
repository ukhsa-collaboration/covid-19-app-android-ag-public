package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper.Callback
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingHubViewModel.NavigationTarget.WhenNotToPauseContactTracing
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Duration

class ContactTracingHubViewModel @AssistedInject constructor(
    private val exposureNotificationManager: ExposureNotificationManager,
    private val notificationProvider: NotificationProvider,
    private val scheduleContactTracingActivationReminder: ScheduleContactTracingActivationReminder,
    @Assisted private var shouldTurnOnContactTracing: Boolean,
    exposureNotificationPermissionHelperFactory: ExposureNotificationPermissionHelper.Factory
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> = distinctUntilChanged(viewStateLiveData)

    private val permissionRequestLiveData = SingleLiveEvent<PermissionRequestResult>()
    fun permissionRequest(): LiveData<PermissionRequestResult> = permissionRequestLiveData

    private val navigationTargetData = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetData

    private val exposureNotificationPermissionHelper = exposureNotificationPermissionHelperFactory.create(
        object : Callback {
            override fun onExposureNotificationsEnabled() {
                updateViewState(showReminderDialog = false)
            }

            override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
                permissionRequestLiveData.postValue(Request(permissionRequest))
            }

            override fun onPermissionDenied() {
                Timber.d("Permission to start contact tracing denied")
            }

            override fun onError(error: Throwable) {
                Timber.e(error, "Could not start exposure notifications")
                permissionRequestLiveData.postValue(Error(error.message ?: "Error starting contact tracing"))
            }
        },
        viewModelScope
    )

    fun onCreate() {
        if (shouldTurnOnContactTracing) {
            shouldTurnOnContactTracing = false
            onTurnOnContactTracingExtraReceived()
        }
    }

    fun onResume() {
        updateViewState(showReminderDialog = viewStateLiveData.value?.showReminderDialog ?: false)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        exposureNotificationPermissionHelper.onActivityResult(requestCode, resultCode)
    }

    private fun onTurnOnContactTracingExtraReceived() {
        exposureNotificationPermissionHelper.startExposureNotifications()
        updateViewState(showReminderDialog = false)
    }

    fun onContactTracingToggleClicked() {
        viewModelScope.launch {
            if (exposureNotificationManager.isEnabled()) {
                onStopExposureNotificationsClicked()
            } else {
                exposureNotificationPermissionHelper.startExposureNotifications()
            }
        }
    }

    fun onReminderDelaySelected(delay: Duration) {
        viewModelScope.launch {
            scheduleContactTracingActivationReminder(delay)
            exposureNotificationManager.stopExposureNotifications()
            updateViewState(showReminderDialog = false)
        }
    }

    fun onExposureNotificationReminderDialogDismissed() {
        updateViewState(showReminderDialog = false)
    }

    private fun onStopExposureNotificationsClicked() {
        viewModelScope.launch {
            val isNotificationChannelEnabled =
                notificationProvider.isChannelEnabled(NotificationProvider.APP_CONFIGURATION_CHANNEL_ID)
            if (!isNotificationChannelEnabled) {
                exposureNotificationManager.stopExposureNotifications()
            }
            updateViewState(showReminderDialog = isNotificationChannelEnabled)
        }
    }

    private fun updateViewState(showReminderDialog: Boolean) {
        viewModelScope.launch {
            viewStateLiveData.postValue(
                ViewState(
                    exposureNotificationEnabled = exposureNotificationManager.isEnabled(),
                    showReminderDialog = showReminderDialog
                )
            )
        }
    }

    fun onWhenNotToPauseClicked() {
        navigationTargetData.postValue(WhenNotToPauseContactTracing)
    }

    data class ViewState(
        val exposureNotificationEnabled: Boolean,
        val showReminderDialog: Boolean
    )

    sealed class NavigationTarget {
        object WhenNotToPauseContactTracing : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(shouldTurnOnContactTracing: Boolean): ContactTracingHubViewModel
    }
}
