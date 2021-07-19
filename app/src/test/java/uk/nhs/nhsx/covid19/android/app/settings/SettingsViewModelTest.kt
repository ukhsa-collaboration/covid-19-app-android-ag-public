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
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
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

    private val contactCaseExposureDate = LocalDate.parse("2020-05-19")
    private val contactCaseNotificationDate = LocalDate.parse("2020-05-20")
    private val dailyContactTestingOptInDate = LocalDate.parse("2020-05-21")
    private val contactCaseExpiryDate = LocalDate.parse("2020-05-24")
    private val selfAssessmentDate = LocalDate.parse("2020-05-15")
    private val symptomsOnsetDate = LocalDate.parse("2020-05-14")
    private val indexCaseExpiryDate = LocalDate.parse("2020-05-23")

    private val contactAndIndexIsolation = IsolationState(
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            exposureDate = contactCaseExposureDate,
            notificationDate = contactCaseNotificationDate,
            expiryDate = contactCaseExpiryDate,
            dailyContactTestingOptInDate = dailyContactTestingOptInDate
        ),
        indexInfo = IndexCase(
            isolationTrigger = SelfAssessment(
                selfAssessmentDate = selfAssessmentDate,
                onsetDate = symptomsOnsetDate
            ),
            expiryDate = indexCaseExpiryDate
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
        every { applicationLocaleProvider.getDefaultSystemLanguage() } returns ENGLISH

        testSubject.loadSettings()

        verify { viewStateObserver.onChanged(ViewState(ENGLISH)) }
    }

    @Test
    fun `delete removes data from storage`() {
        every { stateMachine.readState() } returns contactAndIndexIsolation

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
