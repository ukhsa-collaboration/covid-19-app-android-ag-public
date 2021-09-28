package uk.nhs.nhsx.covid19.android.app.settings.languages

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.BENGALI
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.WELSH
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesViewModel.Language
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesViewModel.ViewState

class LanguagesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val applicationLocaleProvider = mockk<ApplicationLocaleProvider>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val systemLanguageSelectedObserver = mockk<Observer<Unit>>(relaxed = true)
    private val supportedLanguageSelectedObserver = mockk<Observer<Language>>(relaxed = true)
    private val languageSwitchedToObserver = mockk<Observer<Unit>>(relaxed = true)

    private val testSubject = LanguagesViewModel(
        applicationLocaleProvider
    )

    @Before
    fun setUp() {
        every { context.getString(R.string.default_language) } returns "Default"
        every { context.getString(R.string.english) } returns "English"
        every { context.getString(R.string.bengali) } returns "Bengali"
        every { context.getString(R.string.urdu) } returns "Urdu"
        every { context.getString(R.string.punjabi) } returns "Punjabi"
        every { context.getString(R.string.gujarati) } returns "Gujarati"
        every { context.getString(R.string.welsh) } returns "Welsh"
        every { context.getString(R.string.arabic) } returns "Arabic"
        every { context.getString(R.string.chinese) } returns "Chinese (Simplified)"
        every { context.getString(R.string.romanian) } returns "Romanian"
        every { context.getString(R.string.turkish) } returns "Turkish"
        every { context.getString(R.string.polish) } returns "Polish"
        every { context.getString(R.string.somali) } returns "Somali"
    }

    @Test
    fun `load languages when no language is selected`() {
        setUpObservers()

        every { applicationLocaleProvider.getUserSelectedLanguage() } returns null

        testSubject.loadLanguages(context)

        val expected = ViewState(
            LANGUAGES_IN_ENGLISH_NOTHING_SELECTED,
            isSystemLanguageSelected = true
        )

        verify { viewStateObserver.onChanged(expected) }
        verify { systemLanguageSelectedObserver wasNot Called }
        verify { supportedLanguageSelectedObserver wasNot Called }
        verify { languageSwitchedToObserver wasNot Called }
    }

    @Test
    fun `load languages when welsh is selected`() {
        setUpObservers()

        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH

        testSubject.loadLanguages(context)

        val expected = ViewState(
            LANGUAGES_IN_ENGLISH_NOTHING_SELECTED
                .map { if (it.code == WELSH.code) it.copy(isSelected = true) else it },
            isSystemLanguageSelected = false
        )

        verify { viewStateObserver.onChanged(expected) }
        verify { systemLanguageSelectedObserver wasNot Called }
        verify { supportedLanguageSelectedObserver wasNot Called }
        verify { languageSwitchedToObserver wasNot Called }
    }

    @Test
    fun `select system language but not confirm`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH
        testSubject.loadLanguages(context)

        setUpObservers()

        testSubject.selectSystemLanguage()

        val expected = ViewState(
            LANGUAGES_IN_ENGLISH_NOTHING_SELECTED
                .map { if (it.code == WELSH.code) it.copy(isSelected = true) else it },
            isSystemLanguageSelected = false
        )

        verify { viewStateObserver.onChanged(expected) }
        verify { systemLanguageSelectedObserver.onChanged(Unit) }
        verify(exactly = 0) { supportedLanguageSelectedObserver.onChanged(any()) }
        verify(exactly = 0) { applicationLocaleProvider.languageCode = null }
        verify { languageSwitchedToObserver wasNot Called }
    }

    @Test
    fun `select system language and confirm`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH
        testSubject.loadLanguages(context)

        setUpObservers()

        testSubject.selectSystemLanguage()
        testSubject.switchToSystemLanguage()

        val expected = ViewState(
            LANGUAGES_IN_ENGLISH_NOTHING_SELECTED,
            isSystemLanguageSelected = true
        )

        verify { viewStateObserver.onChanged(expected) }
        verify { systemLanguageSelectedObserver.onChanged(Unit) }
        verify(exactly = 0) { supportedLanguageSelectedObserver.onChanged(any()) }
        verify { applicationLocaleProvider.languageCode = null }
        verify { languageSwitchedToObserver.onChanged(Unit) }
    }

    @Test
    fun `select system language without loading languages first`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH

        setUpObservers()

        testSubject.selectSystemLanguage()

        verify(exactly = 0) { viewStateObserver.onChanged(any()) }
        verify(exactly = 0) { systemLanguageSelectedObserver.onChanged(null) }
        verify(exactly = 0) { supportedLanguageSelectedObserver.onChanged(any()) }
        verify(exactly = 0) { applicationLocaleProvider.languageCode = null }
        verify(exactly = 0) { languageSwitchedToObserver.onChanged(null) }
    }

    @Test
    fun `select system language while it is already selected`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns null
        testSubject.loadLanguages(context)

        setUpObservers()

        testSubject.selectSystemLanguage()

        val expected = ViewState(
            LANGUAGES_IN_ENGLISH_NOTHING_SELECTED,
            isSystemLanguageSelected = true
        )

        verify { viewStateObserver.onChanged(expected) }
        verify(exactly = 0) { systemLanguageSelectedObserver.onChanged(null) }
        verify(exactly = 0) { supportedLanguageSelectedObserver.onChanged(any()) }
        verify(exactly = 0) { applicationLocaleProvider.languageCode = null }
        verify { languageSwitchedToObserver wasNot Called }
    }

    @Test
    fun `select supported language but not confirm`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH
        testSubject.loadLanguages(context)

        setUpObservers()

        testSubject.selectSupportedLanguage(BENGALI_IN_ENGLISH_NOT_SELECTED)

        val expected = ViewState(
            LANGUAGES_IN_ENGLISH_NOTHING_SELECTED
                .map { if (it.code == WELSH.code) it.copy(isSelected = true) else it },
            isSystemLanguageSelected = false
        )

        verify { viewStateObserver.onChanged(expected) }
        verify(exactly = 0) { systemLanguageSelectedObserver.onChanged(null) }
        verify { supportedLanguageSelectedObserver.onChanged(BENGALI_IN_ENGLISH_NOT_SELECTED) }
        verify(exactly = 0) { applicationLocaleProvider setProperty "languageCode" value any<String>() }
        verify { languageSwitchedToObserver wasNot Called }
    }

    @Test
    fun `select supported language and confirm`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH
        testSubject.loadLanguages(context)

        setUpObservers()

        testSubject.selectSupportedLanguage(BENGALI_IN_ENGLISH_NOT_SELECTED)
        testSubject.switchToSupportedLanguage(BENGALI_IN_ENGLISH_NOT_SELECTED)

        val expected = ViewState(
            LANGUAGES_IN_ENGLISH_NOTHING_SELECTED
                .map { if (it.code == BENGALI.code) it.copy(isSelected = true) else it },
            isSystemLanguageSelected = false
        )

        verify { viewStateObserver.onChanged(expected) }
        verify(exactly = 0) { systemLanguageSelectedObserver.onChanged(null) }
        verify { supportedLanguageSelectedObserver.onChanged(BENGALI_IN_ENGLISH_NOT_SELECTED) }
        verify { applicationLocaleProvider setProperty "languageCode" value eq(BENGALI.code!!) }
        verify { languageSwitchedToObserver.onChanged(Unit) }
    }

    @Test
    fun `select supported language and confirm without loading languages first`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns WELSH

        setUpObservers()

        testSubject.selectSupportedLanguage(BENGALI_IN_ENGLISH_NOT_SELECTED)

        verify(exactly = 0) { viewStateObserver.onChanged(any()) }
        verify(exactly = 0) { systemLanguageSelectedObserver.onChanged(null) }
        verify(exactly = 0) { supportedLanguageSelectedObserver.onChanged(BENGALI_IN_ENGLISH_NOT_SELECTED) }
        verify(exactly = 0) { applicationLocaleProvider setProperty "languageCode" value any<String>() }
        verify(exactly = 0) { languageSwitchedToObserver.onChanged(null) }
    }

    @Test
    fun `select supported language while it is already selected`() {
        every { applicationLocaleProvider.getUserSelectedLanguage() } returns BENGALI
        testSubject.loadLanguages(context)

        setUpObservers()

        testSubject.selectSupportedLanguage(BENGALI_IN_ENGLISH_NOT_SELECTED)

        val expected = ViewState(
            LANGUAGES_IN_ENGLISH_NOTHING_SELECTED
                .map { if (it.code == BENGALI.code) it.copy(isSelected = true) else it },
            isSystemLanguageSelected = false
        )

        verify { viewStateObserver.onChanged(expected) }
        verify(exactly = 0) { systemLanguageSelectedObserver.onChanged(null) }
        verify(exactly = 0) { supportedLanguageSelectedObserver.onChanged(any()) }
        verify(exactly = 0) { applicationLocaleProvider setProperty "languageCode" value any<String>() }
        verify { languageSwitchedToObserver wasNot Called }
    }

    private fun setUpObservers() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.systemLanguageSelected().observeForever(systemLanguageSelectedObserver)
        testSubject.supportedLanguageSelected().observeForever(supportedLanguageSelectedObserver)
        testSubject.languageSwitchedTo().observeForever(languageSwitchedToObserver)
    }

    companion object {

        private val BENGALI_IN_ENGLISH_NOT_SELECTED = Language(
            "bn",
            "বাংলা",
            "Bengali",
            false
        )

        private val LANGUAGES_IN_ENGLISH_NOTHING_SELECTED = listOf(
            Language(
                "ar",
                "العربية",
                "Arabic",
                false
            ),
            BENGALI_IN_ENGLISH_NOT_SELECTED,
            Language(
                "zh",
                "中文（简体）",
                "Chinese (Simplified)",
                false
            ),
            Language(
                "en",
                "English (UK)",
                "English",
                false
            ),
            Language(
                "gu",
                "ગુજરાતી",
                "Gujarati",
                false
            ),
            Language(
                "pl",
                "Polski",
                "Polish",
                false
            ),
            Language(
                "pa",
                "ਪੰਜਾਬੀ",
                "Punjabi",
                false
            ),
            Language(
                "ro",
                "Română",
                "Romanian",
                false
            ),
            Language(
                "so",
                "Soomaali",
                "Somali",
                false
            ),
            Language(
                "tr",
                "Türkçe",
                "Turkish",
                false
            ),
            Language(
                "ur",
                "اردو",
                "Urdu",
                false
            ),
            Language(
                "cy",
                "Cymraeg",
                "Welsh",
                false
            )
        )
    }
}
