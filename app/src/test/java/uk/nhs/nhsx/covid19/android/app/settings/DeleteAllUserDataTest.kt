package uk.nhs.nhsx.covid19.android.app.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.DecommissioningNotificationSentProvider
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
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
    private val applicationLocaleProvider = mockk<ApplicationLocaleProvider>(relaxed = true)
    private val decommissioningNotificationSentProvider = mockk<DecommissioningNotificationSentProvider>(relaxUnitFun = true)

    private val testSubject = DeleteAllUserData(
        venuesStorage,
        stateMachine,
        sharedPreferences,
        submittedOnboardingAnalyticsProvider,
        shouldShowBluetoothSplashScreen,
        applicationLocaleProvider,
        decommissioningNotificationSentProvider
    )

    @Test
    fun `delete removes data from storage`() {
        coEvery { venuesStorage.getVisits() } returns listOf()
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.clear() } returns sharedPreferencesDeletedDataEditor
        every { submittedOnboardingAnalyticsProvider.value } returns true
        every { applicationLocaleProvider.languageCode } returns "en"

        testSubject()

        verifySequence {
            submittedOnboardingAnalyticsProvider getProperty "value"
            applicationLocaleProvider getProperty "languageCode"
            sharedPreferencesEditor.clear()
            sharedPreferencesDeletedDataEditor.apply()
            submittedOnboardingAnalyticsProvider setProperty "value" value eq(true)
            decommissioningNotificationSentProvider setProperty "value" value eq(true)
            stateMachine.reset()
            venuesStorage.removeAllVenueVisits()
            shouldShowBluetoothSplashScreen.setHasBeenShown(false)
        }

        verify(exactly = 0) { applicationLocaleProvider setProperty "languageCode" value eq("en") }
    }

    @Test
    fun `delete removes data from storage, but resets language after deletion`() {
        coEvery { venuesStorage.getVisits() } returns listOf()
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.clear() } returns sharedPreferencesDeletedDataEditor
        every { submittedOnboardingAnalyticsProvider.value } returns true
        every { applicationLocaleProvider.languageCode } returns "en"

        testSubject(shouldKeepLanguage = true)

        verifySequence {
            submittedOnboardingAnalyticsProvider getProperty "value"
            applicationLocaleProvider getProperty "languageCode"
            sharedPreferencesEditor.clear()
            sharedPreferencesDeletedDataEditor.apply()
            submittedOnboardingAnalyticsProvider setProperty "value" value eq(true)
            applicationLocaleProvider setProperty "languageCode" value eq("en")
            decommissioningNotificationSentProvider setProperty "value" value eq(true)
            stateMachine.reset()
            venuesStorage.removeAllVenueVisits()
            shouldShowBluetoothSplashScreen.setHasBeenShown(false)
        }
    }
}
