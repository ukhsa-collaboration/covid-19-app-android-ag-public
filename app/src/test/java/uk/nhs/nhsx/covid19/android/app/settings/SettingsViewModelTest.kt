package uk.nhs.nhsx.covid19.android.app.settings

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.ENGLISH
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.WELSH
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.settings.SettingsViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import java.time.Instant
import java.time.LocalDate

class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val applicationLocaleProvider = mockk<ApplicationLocaleProvider>(relaxed = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>()
    private val sharedPreferencesEditor = mockk<SharedPreferences.Editor>()
    private val sharedPreferencesDeletedDataEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val submittedOnboardingAnalyticsProvider = mockk<SubmittedOnboardingAnalyticsProvider>(relaxed = true)
    private val venuesStorage = mockk<VisitedVenuesStorage>(relaxed = true)
    private val allUserDataDeletedObserver = mockk<Observer<Unit>>(relaxed = true)

    private val contactCaseEncounterDate = Instant.parse("2020-05-19T12:00:00Z")
    private val contactCaseNotificationDate = Instant.parse("2020-05-20T12:00:00Z")
    private val dailyContactTestingOptInDate = LocalDate.now().plusDays(5)
    private val contactCaseOnlyIsolation = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            startDate = contactCaseEncounterDate,
            notificationDate = contactCaseNotificationDate,
            expiryDate = LocalDate.now().plusDays(5),
            dailyContactTestingOptInDate = dailyContactTestingOptInDate
        )
    )

    @Before
    fun setUp() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.getAllUserDataDeleted().observeForever(allUserDataDeletedObserver)

        coEvery { venuesStorage.getVisits() } returns listOf()
    }

    private val testSubject = SettingsViewModel(
        applicationLocaleProvider,
        venuesStorage,
        stateMachine,
        sharedPreferences,
        submittedOnboardingAnalyticsProvider
    )

    @Test
    fun `load settings with user language`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH

        testSubject.loadSettings()

        verify { viewStateObserver.onChanged(ViewState(WELSH)) }
    }

    @Test
    fun `load settings without user language`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns null
        every { applicationLocaleProvider.getSystemLanguage() } returns ENGLISH

        testSubject.loadSettings()

        verify { viewStateObserver.onChanged(ViewState(ENGLISH)) }
    }

    @Test
    fun `delete removes data from storage`() {
        every { stateMachine.readState() } returns contactCaseOnlyIsolation

        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.clear() } returns sharedPreferencesDeletedDataEditor
        every { submittedOnboardingAnalyticsProvider.value } returns true

        testSubject.loadSettings()
        testSubject.deleteAllUserData()

        verify { venuesStorage.removeAllVenueVisits() }
        verify { sharedPreferencesEditor.clear() }
        verify { sharedPreferencesDeletedDataEditor.apply() }
        verify { submittedOnboardingAnalyticsProvider setProperty "value" value eq(true) }
        verify { allUserDataDeletedObserver.onChanged(Unit) }
    }
}
