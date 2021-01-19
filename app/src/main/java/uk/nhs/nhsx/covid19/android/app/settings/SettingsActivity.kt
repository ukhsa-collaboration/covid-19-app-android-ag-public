package uk.nhs.nhsx.covid19.android.app.settings

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.activity_settings.languageOption
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class SettingsActivity : BaseActivity(R.layout.activity_settings) {

    @Inject
    lateinit var factory: ViewModelFactory<SettingsViewModel>
    private val viewModel: SettingsViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        setNavigateUpToolbar(toolbar, R.string.settings_title, upIndicator = R.drawable.ic_arrow_back_white)

        viewModel.viewState().observe(this) { viewState ->
            languageOption.subtitle = getString(viewState.language.languageName)
        }

        setClickListeners()
        viewModel.loadSettings()
    }

    private fun setClickListeners() {
        languageOption.setOnSingleClickListener {
            startActivity<LanguagesActivity>()
        }
    }
}
