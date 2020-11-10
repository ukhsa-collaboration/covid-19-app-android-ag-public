package uk.nhs.nhsx.covid19.android.app.availability

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus
import kotlin.coroutines.suspendCoroutine

interface UpdateManager {

    suspend fun getAvailableUpdateVersionCode(): AvailableUpdateStatus

    sealed class AvailableUpdateStatus {
        data class Available(val versionCode: Int) : AvailableUpdateStatus()
        object NoUpdateAvailable : AvailableUpdateStatus()
        data class UpdateError(val exception: Exception) : AvailableUpdateStatus()
    }
}

class GooglePlayUpdateProvider(context: Context) :
    UpdateManager {

    private val updateManager by lazy { AppUpdateManagerFactory.create(context) }

    override suspend fun getAvailableUpdateVersionCode(): AvailableUpdateStatus =
        suspendCoroutine { continuation ->
            updateManager
                .appUpdateInfo
                .addOnSuccessListener { result: AppUpdateInfo ->
                    if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        Timber.d(result.toString())
                        continuation.resumeWith(
                            Result.success(
                                AvailableUpdateStatus.Available(
                                    result.availableVersionCode()
                                )
                            )
                        )
                    } else
                        continuation.resumeWith(Result.success((AvailableUpdateStatus.NoUpdateAvailable)))
                }
                .addOnFailureListener {
                    continuation.resumeWith(Result.success(AvailableUpdateStatus.UpdateError(it)))
                }
        }
}
