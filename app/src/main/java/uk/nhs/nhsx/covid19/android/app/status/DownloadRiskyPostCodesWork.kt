package uk.nhs.nhsx.covid19.android.app.status

import androidx.work.ListenableWorker
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.RiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult

class DownloadRiskyPostCodesWork @Inject constructor(
    private val riskyPostCodeApi: RiskyPostDistrictsApi,
    private val postCodeProvider: PostCodeProvider,
    private val riskyPostCodeIndicatorProvider: RiskyPostCodeIndicatorProvider,
    private val notificationProvider: NotificationProvider,
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader
) {

    suspend operator fun invoke(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        runSafely {
            val response = riskyPostCodeApi.fetchRiskyPostCodeDistribution()

            if (response.postDistricts.isEmpty()) throw EmptyPostDistrictsForV2Exception()

            val mainPostCode = postCodeProvider.value
                ?: return@runSafely Success(ListenableWorker.Result.success())

            val localAuthorityId: String? = localAuthorityProvider.value

            val hasLocalAuthorityToRiskLevelMapping =
                localAuthorityId != null && response.localAuthorities?.get(localAuthorityId) != null && RuntimeBehavior.isFeatureEnabled(
                    FeatureFlag.LOCAL_AUTHORITY
                )
            val riskIndicatorId: String? = if (hasLocalAuthorityToRiskLevelMapping) {
                response.localAuthorities!![localAuthorityId]
            } else {
                response.postDistricts[mainPostCode]
            }

            response.riskLevels[riskIndicatorId]?.let { riskIndicator ->
                val currentRiskIndicator = riskyPostCodeIndicatorProvider.riskyPostCodeIndicator

                val hasRiskinessChanged =
                    currentRiskIndicator?.riskLevel?.let { currentRiskLevel -> currentRiskLevel != riskIndicatorId }
                        ?: false

                val localAuthorityName =
                    localAuthorityPostCodesLoader.load()?.localAuthorities?.get(localAuthorityId)?.name
                val localAuthorityRiskTitle =
                    if (riskIndicator.policyData != null && localAuthorityName != null) {
                        riskIndicator.policyData.localAuthorityRiskTitle.replace(
                            "[local authority]",
                            localAuthorityName
                        ).replace("[postcode]", mainPostCode)
                    } else null

                val policyData =
                    if (localAuthorityRiskTitle != null) {
                        riskIndicator.policyData?.copy(localAuthorityRiskTitle = localAuthorityRiskTitle)
                    } else null

                val riskIndicatorName = riskIndicator.name.replace("[postcode]", mainPostCode)

                val updatedRiskIndicator =
                    riskIndicator.copy(name = riskIndicatorName, policyData = policyData)

                storeAndNotifyRiskyPostCodeIndicator(
                    RiskIndicatorWrapper(
                        riskLevel = riskIndicatorId,
                        riskIndicator = updatedRiskIndicator,
                        riskLevelFromLocalAuthority = hasLocalAuthorityToRiskLevelMapping
                    ),
                    hasRiskinessChanged
                )
            } ?: throw PostCodeNotFoundException()

            return@runSafely Success(ListenableWorker.Result.success())
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

    private class PostCodeNotFoundException : Exception()

    private class EmptyPostDistrictsForV2Exception : Exception()
}
