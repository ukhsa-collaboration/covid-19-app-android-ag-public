package uk.nhs.nhsx.covid19.android.app.settings.languages

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import java.text.Collator
import javax.inject.Inject

class LanguagesViewModel @Inject constructor(
    private val applicationLocaleProvider: ApplicationLocaleProvider
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private val systemLanguageSelected = MutableLiveData<Unit>()
    fun systemLanguageSelected(): LiveData<Unit> = systemLanguageSelected

    private val supportedLanguageSelected = MutableLiveData<Language>()
    fun supportedLanguageSelected(): LiveData<Language> = supportedLanguageSelected

    private val languageSwitchedTo = MutableLiveData<Unit>()
    fun languageSwitchedTo(): LiveData<Unit> = languageSwitchedTo

    fun loadLanguages(context: Context) {
        val selectedLanguage = applicationLocaleProvider.getUserSelectedLanguage()
        val locale = applicationLocaleProvider.getLocale()

        val collator: Collator = Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
        val languages = SupportedLanguage.values().toList()
            .map { language ->
                Language(
                    language.code,
                    language.nativeLanguageName,
                    translatedName = context.getString(language.languageName),
                    isSelected = language == selectedLanguage
                )
            }
            .sortedWith(compareBy(collator) { it.translatedName })

        viewState.postValue(
            ViewState(
                languages,
                isSystemLanguageSelected = selectedLanguage == null
            )
        )
    }

    fun selectSystemLanguage() {
        viewState.value?.let { currentViewState ->
            if (!currentViewState.isSystemLanguageSelected) {
                systemLanguageSelected.postValue(Unit)
            }
        }
    }

    fun selectSupportedLanguage(language: Language) {
        viewState.value?.let { currentViewState ->
            if (currentViewState.languages.any { it.code == language.code && !it.isSelected }) {
                supportedLanguageSelected.postValue(language)
            }
        }
    }

    fun switchToSystemLanguage() {
        applicationLocaleProvider.languageCode = null

        val languages = viewState.value?.let { currentViewState ->
            currentViewState.languages.map { it.copy(isSelected = false) }
        } ?: emptyList()

        val newViewState = ViewState(
            languages,
            isSystemLanguageSelected = true
        )

        viewState.postValue(newViewState)

        languageSwitchedTo.postValue(Unit)
    }

    fun switchToSupportedLanguage(language: Language) {
        applicationLocaleProvider.languageCode = language.code

        val languages = viewState.value?.let { currentViewState ->
            currentViewState.languages.map { it.copy(isSelected = it.code == language.code) }
        } ?: emptyList()

        val newViewState = ViewState(
            languages,
            isSystemLanguageSelected = false
        )

        viewState.postValue(newViewState)

        languageSwitchedTo.postValue(Unit)
    }

    data class Language(
        val code: String,
        val nativeName: String,
        val translatedName: String,
        val isSelected: Boolean
    )

    data class ViewState(val languages: List<Language>, val isSystemLanguageSelected: Boolean)
}
