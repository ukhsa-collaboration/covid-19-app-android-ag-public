package uk.nhs.nhsx.covid19.android.app.onboarding

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitOnboardingAnalyticsWorker
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationRequired
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.BatteryOptimization
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.EnableExposureNotifications
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.Status
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class PermissionViewModel @Inject constructor(
    private val onboardingCompletedProvider: OnboardingCompletedProvider,
    private val submitOnboardingAnalyticsWorkerScheduler: SubmitOnboardingAnalyticsWorker.Scheduler,
    private val periodicTasks: PeriodicTasks,
    private val batteryOptimizationRequired: BatteryOptimizationRequired,
    private val submittedOnboardingAnalyticsProvider: SubmittedOnboardingAnalyticsProvider,
    exposureNotificationPermissionHelperFactory: ExposureNotificationPermissionHelper.Factory
) : ViewModel(), ExposureNotificationPermissionHelper.Callback {

    private val permissionRequestLiveData = SingleLiveEvent<PermissionRequestResult>()
    fun permissionRequest(): LiveData<PermissionRequestResult> = permissionRequestLiveData

    private val navigationTargetLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    private val exposureNotificationPermissionHelper =
        exposureNotificationPermissionHelperFactory.create(this, viewModelScope)

    override fun onExposureNotificationsEnabled() {
        onboardingCompletedProvider.value = true
        if (submittedOnboardingAnalyticsProvider.value != true) {
            submitOnboardingAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent()
            submittedOnboardingAnalyticsProvider.value = true
        }
        periodicTasks.schedule()

        if (batteryOptimizationRequired()) {
            navigationTargetLiveData.postValue(BatteryOptimization)
        } else {
            navigationTargetLiveData.postValue(Status)
        }
    }

    override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
        permissionRequestLiveData.postValue(Request(permissionRequest))
    }

    override fun onPermissionDenied() {
        navigationTargetLiveData.postValue(EnableExposureNotifications)
    }

    override fun onError(error: Throwable) {
        Timber.e(error, "Could not start exposure notifications")
        permissionRequestLiveData.postValue(Error(error.message ?: "Error starting contact tracing"))
    }

    fun onContinueButtonClicked() {
        exposureNotificationPermissionHelper.startExposureNotifications()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        exposureNotificationPermissionHelper.onActivityResult(requestCode, resultCode)
    }

    sealed class NavigationTarget {
        object BatteryOptimization : NavigationTarget()
        object Status : NavigationTarget()
        object EnableExposureNotifications : NavigationTarget()
    }
}
