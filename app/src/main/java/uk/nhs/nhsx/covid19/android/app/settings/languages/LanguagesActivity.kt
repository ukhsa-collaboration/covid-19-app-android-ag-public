package uk.nhs.nhsx.covid19.android.app.settings.languages

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_settings_language.languagesRecyclerView
import kotlinx.android.synthetic.main.activity_settings_language.systemLanguage
import kotlinx.android.synthetic.main.item_settings_language.view.languageNativeName
import kotlinx.android.synthetic.main.item_settings_language.view.languageRadio
import kotlinx.android.synthetic.main.item_settings_language.view.languageTranslatedName
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.util.viewutils.mirrorSystemLayoutDirection
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class LanguagesActivity : BaseActivity(R.layout.activity_settings_language) {

    @Inject
    lateinit var factory: ViewModelFactory<LanguagesViewModel>
    private val viewModel: LanguagesViewModel by viewModels { factory }

    private lateinit var languagesViewAdapter: LanguagesViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.language, upIndicator = R.drawable.ic_arrow_back_white)

        languagesRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        setUpLanguagesAdapter()

        setUpSystemLanguage()

        setupViewModelListeners()

        setClickListeners()

        viewModel.loadLanguages(this)
    }

    private fun setUpLanguagesAdapter() {
        languagesViewAdapter = LanguagesViewAdapter { language -> viewModel.selectSupportedLanguage(language) }

        languagesRecyclerView.layoutManager = LinearLayoutManager(this)
        languagesRecyclerView.adapter = languagesViewAdapter
    }

    private fun setUpSystemLanguage() {
        val language = applicationLocaleProvider.getDefaultSystemLanguage()
        systemLanguage.languageNativeName.text = language.nativeLanguageName
        systemLanguage.languageTranslatedName.text = getString(language.languageName)
        systemLanguage.languageRadio.mirrorSystemLayoutDirection()
    }

    private fun setupViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            systemLanguage.languageRadio.isChecked = viewState.isSystemLanguageSelected
            languagesViewAdapter.submitList(viewState.languages)
        }

        viewModel.systemLanguageSelected().observe(this) {
            val language = applicationLocaleProvider.getDefaultSystemLanguage()
            showConfirmationDialog(getString(language.languageName)) { viewModel.switchToSystemLanguage() }
        }

        viewModel.supportedLanguageSelected().observe(this) { language ->
            showConfirmationDialog(language.translatedName) { viewModel.switchToSupportedLanguage(language) }
        }

        viewModel.languageSwitchedTo().observe(this) {
            finish()
            startActivity(intent)
        }
    }

    private fun setClickListeners() {
        systemLanguage.setOnSingleClickListener { viewModel.selectSystemLanguage() }
    }

    private fun showConfirmationDialog(language: String, selectLanguageAction: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.change_language_dialog, language))
        builder.setPositiveButton(R.string.confirm) { dialog, _ ->
            selectLanguageAction()
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
}
