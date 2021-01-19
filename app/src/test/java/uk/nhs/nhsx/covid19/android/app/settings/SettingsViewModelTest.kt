package uk.nhs.nhsx.covid19.android.app.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.ENGLISH
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.WELSH
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.settings.SettingsViewModel.ViewState

class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val applicationLocaleProvider = mockk<ApplicationLocaleProvider>(relaxed = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)

    private val testSubject = SettingsViewModel(
        applicationLocaleProvider
    )

    @Test
    fun `load settings with user language`() {
        testSubject.viewState().observeForever(viewStateObserver)

        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH

        testSubject.loadSettings()

        verify { viewStateObserver.onChanged(ViewState(WELSH)) }
    }

    @Test
    fun `load settings without user language`() {
        testSubject.viewState().observeForever(viewStateObserver)

        every { applicationLocaleProvider.getUserSelectedLanguage() } returns null
        every { applicationLocaleProvider.getSystemLanguage() } returns ENGLISH

        testSubject.loadSettings()

        verify { viewStateObserver.onChanged(ViewState(ENGLISH)) }
    }
}
