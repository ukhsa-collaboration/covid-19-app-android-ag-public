package uk.nhs.nhsx.covid19.android.app.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.ShouldShowBluetoothSplashScreen

class DeleteAllUserDataTest {
    private val stateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val sharedPreferences = mockk<SharedPreferences>()
    private val sharedPreferencesEditor = mockk<Editor>()
    private val sharedPreferencesDeletedDataEditor = mockk<Editor>(relaxUnitFun = true)
    private val submittedOnboardingAnalyticsProvider = mockk<SubmittedOnboardingAnalyticsProvider>(relaxUnitFun = true)
    private val venuesStorage = mockk<VisitedVenuesStorage>(relaxUnitFun = true)
    private val shouldShowBluetoothSplashScreen = mockk<ShouldShowBluetoothSplashScreen>(relaxUnitFun = true)

    private val testSubject = DeleteAllUserData(
        venuesStorage,
        stateMachine,
        sharedPreferences,
        submittedOnboardingAnalyticsProvider,
        shouldShowBluetoothSplashScreen
    )

    @Test
    fun `delete removes data from storage`() {
        coEvery { venuesStorage.getVisits() } returns listOf()
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.clear() } returns sharedPreferencesDeletedDataEditor
        every { submittedOnboardingAnalyticsProvider.value } returns true

        testSubject()

        verifySequence {
            submittedOnboardingAnalyticsProvider getProperty "value"
            sharedPreferencesEditor.clear()
            sharedPreferencesDeletedDataEditor.apply()
            submittedOnboardingAnalyticsProvider setProperty "value" value eq(true)
            stateMachine.reset()
            venuesStorage.removeAllVenueVisits()
            shouldShowBluetoothSplashScreen.setHasBeenShown(false)
        }
    }
}
