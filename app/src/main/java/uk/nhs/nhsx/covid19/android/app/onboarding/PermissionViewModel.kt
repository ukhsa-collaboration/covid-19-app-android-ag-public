package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitOnboardingAnalyticsWorker
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationRequired
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.BATTERY_OPTIMIZATION
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.STATUS_ACTIVITY
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class PermissionViewModel @Inject constructor(
    private val onboardingCompletedProvider: OnboardingCompletedProvider,
    private val submitOnboardingAnalyticsWorkerScheduler: SubmitOnboardingAnalyticsWorker.Scheduler,
    private val periodicTasks: PeriodicTasks,
    private val batteryOptimizationRequired: BatteryOptimizationRequired,
    private val submittedOnboardingAnalyticsProvider: SubmittedOnboardingAnalyticsProvider
) : ViewModel() {

    private val activityNavigationLiveData = SingleLiveEvent<NavigationTarget>()
    fun onActivityNavigation(): LiveData<NavigationTarget> = activityNavigationLiveData

    fun onExposureNotificationsActive() {
        onboardingCompletedProvider.value = true
        if (submittedOnboardingAnalyticsProvider.value != true) {
            submitOnboardingAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent()
            submittedOnboardingAnalyticsProvider.value = true
        }
        periodicTasks.schedule()

        if (batteryOptimizationRequired()) {
            activityNavigationLiveData.postValue(BATTERY_OPTIMIZATION)
        } else {
            activityNavigationLiveData.postValue(STATUS_ACTIVITY)
        }
    }

    enum class NavigationTarget {
        BATTERY_OPTIMIZATION, STATUS_ACTIVITY
    }
}
