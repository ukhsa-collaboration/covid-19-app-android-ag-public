package uk.nhs.nhsx.covid19.android.app.availability

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import javax.inject.Inject

class AppAvailabilityListener @Inject constructor(
    private val appAvailabilityProvider: AppAvailabilityProvider
) : ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity) {}
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
        }
    }

    fun shouldShowAvailabilityScreen(): Boolean {
        return !appAvailabilityProvider.isAppAvailable()
    }
}
