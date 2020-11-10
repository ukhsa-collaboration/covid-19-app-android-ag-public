package uk.nhs.nhsx.covid19.android.app.availability

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.AppAvailabilityApi
import javax.inject.Inject

class GetAvailabilityStatus @Inject constructor(
    private val appAvailabilityApi: AppAvailabilityApi,
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val notificationProvider: NotificationProvider,
    private val lastRecommendedNotificationAppVersionProvider: LastRecommendedNotificationAppVersionProvider
) {
    suspend operator fun invoke(): Result<Unit> =
        withContext(Dispatchers.IO) {
            val sdkVersion = Build.VERSION.SDK_INT
            val appVersionCode = BuildConfig.VERSION_CODE
            Timber.d("sdk = $sdkVersion version code = $appVersionCode")

            runSafely {
                val response = appAvailabilityApi.getAvailability()

                val lastAvailability = appAvailabilityProvider.isAppAvailable()

                appAvailabilityProvider.appAvailability = response

                if (lastAvailability != appAvailabilityProvider.isAppAvailable()) {
                    if (appAvailabilityProvider.isAppAvailable())
                        notificationProvider.showAppIsAvailable()
                    else
                        notificationProvider.showAppIsNotAvailable()
                } else {
                    if (appAvailabilityProvider.isUpdateRecommended()) {
                        appAvailabilityProvider.appAvailability?.let {
                            val recommendedAppVersion = it.recommendedAppVersion.value
                            val lastRecommendedAppVersion = lastRecommendedNotificationAppVersionProvider.value ?: 0
                            if (recommendedAppVersion > lastRecommendedAppVersion) {
                                lastRecommendedNotificationAppVersionProvider.value = recommendedAppVersion
                                notificationProvider.showRecommendedAppUpdateIsAvailable()
                            }
                        }
                    }
                }
            }
        }
}
