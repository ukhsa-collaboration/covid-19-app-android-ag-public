package uk.nhs.nhsx.covid19.android.app.availability

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.AppAvailabilityApi
import javax.inject.Inject

class GetAvailabilityStatus @Inject constructor(
    private val appAvailabilityApi: AppAvailabilityApi,
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val notificationProvider: NotificationProvider
) {
    suspend operator fun invoke(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runSafely {
                val response = appAvailabilityApi.getAvailability()

                val lastAvailability = appAvailabilityProvider.isAppAvailable()

                appAvailabilityProvider.appAvailability = response

                if (lastAvailability != appAvailabilityProvider.isAppAvailable()) {
                    if (appAvailabilityProvider.isAppAvailable())
                        notificationProvider.showAppIsAvailable()
                    else
                        notificationProvider.showAppIsNotAvailable()
                }
            }
        }
}
