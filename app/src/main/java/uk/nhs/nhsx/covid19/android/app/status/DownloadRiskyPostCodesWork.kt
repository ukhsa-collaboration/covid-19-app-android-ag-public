package uk.nhs.nhsx.covid19.android.app.status

import androidx.work.ListenableWorker
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.RiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult
import javax.inject.Inject

class DownloadRiskyPostCodesWork @Inject constructor(
    private val riskyPostCodeApi: RiskyPostDistrictsApi,
    private val postCodeProvider: PostCodeProvider,
    private val riskyPostCodeIndicatorProvider: RiskyPostCodeIndicatorProvider,
    private val notificationProvider: NotificationProvider
) {

    suspend operator fun invoke(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        if (!RuntimeBehavior.isFeatureEnabled(FeatureFlag.HIGH_RISK_POST_DISTRICTS)) {
            return@withContext ListenableWorker.Result.success()
        }

        runCatching {
            val riskyPostCodeDistribution = riskyPostCodeApi.fetchRiskyPostCodeDistribution()

            if (riskyPostCodeDistribution.postDistricts.isEmpty()) throw EmptyPostDistrictsForV2Exception()

            val mainPostCode = postCodeProvider.value ?: return@runCatching Success(ListenableWorker.Result.success())

            val updatedMainPostCodeRiskIndicatorKey = riskyPostCodeDistribution.postDistricts[mainPostCode]
            val updatedMainPostCodeRiskIndicator =
                riskyPostCodeDistribution.riskLevels[updatedMainPostCodeRiskIndicatorKey]
            updatedMainPostCodeRiskIndicator?.let {
                val currentMainPostCodeRiskIndicator = riskyPostCodeIndicatorProvider.riskyPostCodeIndicator

                val hasRiskinessChanged =
                    currentMainPostCodeRiskIndicator?.riskLevel?.let { it != updatedMainPostCodeRiskIndicatorKey }
                        ?: false

                storeAndNotifyRiskyPostCodeIndicator(
                    RiskIndicatorWrapper(
                        riskLevel = updatedMainPostCodeRiskIndicatorKey,
                        riskIndicator = replaceRiskIndicatorPostCodePlaceholders(
                            updatedMainPostCodeRiskIndicator,
                            mainPostCode
                        )
                    ),
                    hasRiskinessChanged
                )
            } ?: throw PostCodeNotFoundException()

            return@runCatching Success(ListenableWorker.Result.success())
        }.getOrElse {
            runSafely {
                val riskyPostCodes = riskyPostCodeApi.fetchRiskyPostDistricts()

                if (riskyPostCodes.postDistricts.isNullOrEmpty()) return@runSafely ListenableWorker.Result.success()

                val mainPostCode = postCodeProvider.value ?: return@runSafely ListenableWorker.Result.success()

                val updatedMainPostCodeRiskLevel = riskyPostCodes.postDistricts[mainPostCode]
                updatedMainPostCodeRiskLevel?.let {
                    val currentMainPostCodeRiskIndicator = riskyPostCodeIndicatorProvider.riskyPostCodeIndicator

                    val hasRiskinessChanged = currentMainPostCodeRiskIndicator?.oldRiskLevel?.let {
                        currentMainPostCodeRiskIndicator.oldRiskLevel != updatedMainPostCodeRiskLevel
                    } ?: false

                    storeAndNotifyRiskyPostCodeIndicator(
                        RiskIndicatorWrapper(oldRiskLevel = it),
                        hasRiskinessChanged
                    )
                }

                return@runSafely ListenableWorker.Result.success()
            }
        }.toWorkerResult()
    }

    private fun storeAndNotifyRiskyPostCodeIndicator(
        riskyPostCodeIndicatorWrapper: RiskIndicatorWrapper,
        hasRiskinessChanged: Boolean
    ) {
        riskyPostCodeIndicatorProvider.riskyPostCodeIndicator = riskyPostCodeIndicatorWrapper

        if (hasRiskinessChanged && !StatusActivity.isVisible) {
            notificationProvider.showAreaRiskChangedNotification()
        }
    }

    private fun replaceRiskIndicatorPostCodePlaceholders(
        riskIndicator: RiskIndicator,
        mainPostCode: String
    ): RiskIndicator {
        return riskIndicator.copy(
            name = riskIndicator.name.copy(
                translations = riskIndicator.name.replace("[postcode]", mainPostCode)
            )
        )
    }

    private class PostCodeNotFoundException : Exception()

    private class EmptyPostDistrictsForV2Exception : Exception()
}
