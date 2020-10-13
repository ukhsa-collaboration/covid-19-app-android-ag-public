package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Yes
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.PotentialExposureExplanationHandler.ExplanationAction.HIDE_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.PotentialExposureExplanationHandler.ExplanationAction.SHOW_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import javax.inject.Inject

class PotentialExposureExplanationHandler @Inject constructor(val notificationProvider: NotificationProvider) {

    enum class ExplanationAction {
        SHOW_NOTIFICATION, HIDE_NOTIFICATION
    }

    private var finalAction: ExplanationAction? = null

    fun addResult(result: Result<InitialCircuitBreakerResult>) {
        if (getAction(result) == HIDE_NOTIFICATION) {
            finalAction = HIDE_NOTIFICATION
        } else if (finalAction == null) {
            finalAction = SHOW_NOTIFICATION
        }
    }

    fun showNotificationIfNeeded() {
        if (finalAction == SHOW_NOTIFICATION) {
            notificationProvider.showPotentialExposureExplanationNotification()
        } else if (finalAction == HIDE_NOTIFICATION) {
            notificationProvider.hidePotentialExposureExplanationNotification()
        }
        finalAction = null
    }

    private fun getAction(result: Result<InitialCircuitBreakerResult>) =
        if (result is Success && result.value is Yes) {
            HIDE_NOTIFICATION
        } else {
            SHOW_NOTIFICATION
        }
}
