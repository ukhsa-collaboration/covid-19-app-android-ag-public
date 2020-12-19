package uk.nhs.nhsx.covid19.android.app.availability

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.covid19.android.app.availability.AppState.InBackground
import uk.nhs.nhsx.covid19.android.app.availability.AppState.InForeground
import uk.nhs.nhsx.covid19.android.app.availability.AppState.Starting
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.minutesUntilNow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class AppAvailabilityListener @Inject constructor(
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val clock: Clock
) : ActivityLifecycleCallbacks {

    private var appState: AppState = Starting

    override fun onActivityPaused(activity: Activity) {
        appState = InBackground(start = Instant.now(clock))
    }
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {
        if (shouldShowAvailabilityScreen() && activity !is AppAvailabilityActivity) {
            (activity as AppCompatActivity).applicationContext.startActivity<AppAvailabilityActivity> {
                flags = FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        } else {
            if (shouldShowRecommendationScreen() && activity !is UpdateRecommendedActivity) {
                (activity as AppCompatActivity).startActivity<UpdateRecommendedActivity>()
            }
        }
        appState = InForeground
    }
    fun shouldShowAvailabilityScreen(): Boolean {
        return !appAvailabilityProvider.isAppAvailable()
    }

    private fun shouldShowRecommendationScreen(): Boolean {
        return if (appAvailabilityProvider.isUpdateRecommended()) {
            when (val currentAppState = appState) {
                Starting -> true
                is InBackground -> currentAppState.start.minutesUntilNow(clock) > 5
                InForeground -> false
            }
        } else false
    }
}

sealed class AppState {
    object Starting : AppState()
    data class InBackground(val start: Instant) : AppState()
    object InForeground : AppState()
}
