package uk.nhs.nhsx.covid19.android.app.settings

import android.content.SharedPreferences
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.DecommissioningNotificationSentProvider
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.ShouldShowBluetoothSplashScreen
import javax.inject.Inject

class DeleteAllUserData @Inject constructor(
    private val venuesStorage: VisitedVenuesStorage,
    private val stateMachine: IsolationStateMachine,
    private val sharedPreferences: SharedPreferences,
    private val submittedOnboardingAnalyticsProvider: SubmittedOnboardingAnalyticsProvider,
    private val shouldShowBluetoothSplashScreen: ShouldShowBluetoothSplashScreen,
    private val applicationLocaleProvider: ApplicationLocaleProvider,
    private val decommissioningNotificationSentProvider: DecommissioningNotificationSentProvider
) {

    private val lock = Object()

    operator fun invoke(shouldKeepLanguage: Boolean = false) = synchronized(lock) {
        Timber.d("Deleting app data started")
        val submittedOnboardingAnalytics = submittedOnboardingAnalyticsProvider.value
        val languageCode = applicationLocaleProvider.languageCode
        sharedPreferences.edit().clear().apply()
        submittedOnboardingAnalyticsProvider.value = submittedOnboardingAnalytics
        if (shouldKeepLanguage) {
            applicationLocaleProvider.languageCode = languageCode
        }
        decommissioningNotificationSentProvider.value = true
        stateMachine.reset()
        venuesStorage.removeAllVenueVisits()
        shouldShowBluetoothSplashScreen.setHasBeenShown(false)
        Timber.d("Deleting app data finished")
    }
}
