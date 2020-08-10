package uk.nhs.nhsx.covid19.android.app.status

import androidx.work.ListenableWorker
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.RiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult
import javax.inject.Inject

class DownloadRiskyPostCodesWork @Inject constructor(
    private val riskyPostCodeApi: RiskyPostDistrictsApi,
    private val postCodeProvider: PostCodeProvider,
    private val riskyPostCodeDetectedPrefs: RiskyPostCodeDetectedProvider,
    private val areaRiskChangedPrefs: AreaRiskChangedProvider,
    private val notificationProvider: NotificationProvider
) {

    suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        if (!RuntimeBehavior.isFeatureEnabled(FeatureFlag.HIGH_RISK_POST_DISTRICTS)) {
            return@withContext ListenableWorker.Result.success()
        }

        runSafely {
            val riskyPostCodes = riskyPostCodeApi.fetchRiskyPostDistricts()

            if (riskyPostCodes.postDistricts.isNullOrEmpty()) return@runSafely ListenableWorker.Result.success()

            val mainPostCode =
                postCodeProvider.value ?: return@runSafely ListenableWorker.Result.success()

            val currentMainPostCodeRiskLevel = riskyPostCodeDetectedPrefs.toRiskLevel()
            val updatedMainPostCodeRiskLevel = riskyPostCodes.postDistricts[mainPostCode]

            val hasRiskinessChanged = currentMainPostCodeRiskLevel != updatedMainPostCodeRiskLevel

            riskyPostCodeDetectedPrefs.setRiskyPostCodeLevel(updatedMainPostCodeRiskLevel)
            areaRiskChangedPrefs.value = hasRiskinessChanged

            if (hasRiskinessChanged && !StatusActivity.isVisible) {
                notificationProvider.showAreaRiskChangedNotification()
            }

            return@runSafely ListenableWorker.Result.success()
        }.toWorkerResult()
    }
}
