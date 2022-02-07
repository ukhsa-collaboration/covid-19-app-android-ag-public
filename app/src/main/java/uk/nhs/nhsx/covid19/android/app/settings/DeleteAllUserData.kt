package uk.nhs.nhsx.covid19.android.app.settings

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.ShouldShowBluetoothSplashScreen
import javax.inject.Inject

class DeleteAllUserData @Inject constructor(
    private val venuesStorage: VisitedVenuesStorage,
    private val stateMachine: IsolationStateMachine,
    private val sharedPreferences: SharedPreferences,
    private val submittedOnboardingAnalyticsProvider: SubmittedOnboardingAnalyticsProvider,
    private val shouldShowBluetoothSplashScreen: ShouldShowBluetoothSplashScreen
) {
    operator fun invoke() {
        val submittedOnboardingAnalytics = submittedOnboardingAnalyticsProvider.value
        sharedPreferences.edit().clear().apply()
        submittedOnboardingAnalyticsProvider.value = submittedOnboardingAnalytics
        stateMachine.reset()
        venuesStorage.removeAllVenueVisits()
        shouldShowBluetoothSplashScreen.setHasBeenShown(false)
    }
}
