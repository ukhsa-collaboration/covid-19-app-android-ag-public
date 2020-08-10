package uk.nhs.nhsx.covid19.android.app.availability

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus.Available
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus.NoUpdateAvailable
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus.UpdateError
import kotlin.coroutines.suspendCoroutine

interface UpdateManager {

    suspend fun getAvailableUpdateVersionCode(): AvailableUpdateStatus

    suspend fun startUpdate(activity: AppCompatActivity, requestCode: Int)

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
                    if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
                        continuation.resumeWith(Result.success(Available(result.availableVersionCode())))
                    else
                        continuation.resumeWith(Result.success((NoUpdateAvailable)))
                }
                .addOnFailureListener {
                    continuation.resumeWith(Result.success(UpdateError(it)))
                }
        }

    override suspend fun startUpdate(activity: AppCompatActivity, requestCode: Int) {
        when (getAvailableUpdateVersionCode()) {
            is Available -> {
                updateManager
                    .startUpdateFlowForResult(
                        updateManager.appUpdateInfo.result,
                        activity,
                        AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                        requestCode
                    )
            }
            else -> Unit
        }
    }
}
