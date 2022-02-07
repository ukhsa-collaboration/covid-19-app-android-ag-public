package uk.nhs.nhsx.covid19.android.app.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.ENGLISH
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.WELSH
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.settings.SettingsViewModel.ViewState

class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val applicationLocaleProvider = mockk<ApplicationLocaleProvider>(relaxUnitFun = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val deleteAllUserData = mockk<DeleteAllUserData>(relaxUnitFun = true)
    private val allUserDataDeletedObserver = mockk<Observer<Unit>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.getAllUserDataDeleted().observeForever(allUserDataDeletedObserver)
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns ENGLISH
    }

    private val testSubject = SettingsViewModel(
        applicationLocaleProvider,
        deleteAllUserData
    )

    @Test
    fun `load settings with user language`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH

        testSubject.loadSettings()

        verifySequence { viewStateObserver.onChanged(ViewState(WELSH)) }
    }

    @Test
    fun `load settings without user language`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns null
        every { applicationLocaleProvider.getDefaultSystemLanguage() } returns ENGLISH

        testSubject.loadSettings()

        verifySequence { viewStateObserver.onChanged(ViewState(ENGLISH)) }
    }

    @Test
    fun `dataDeletionConfirmed removes data from storage`() {
        testSubject.loadSettings()
        testSubject.dataDeletionConfirmed()

        verifySequence {
            deleteAllUserData()
            allUserDataDeletedObserver.onChanged(null)
        }
    }
}
