package uk.nhs.nhsx.covid19.android.app.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_settings.actionDeleteAllData
import kotlinx.android.synthetic.main.activity_settings.animationsOption
import kotlinx.android.synthetic.main.activity_settings.languageOption
import kotlinx.android.synthetic.main.activity_settings.myAreaOption
import kotlinx.android.synthetic.main.activity_settings.myDataOption
import kotlinx.android.synthetic.main.activity_settings.venueHistoryOption
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.mydata.MyDataActivity
import uk.nhs.nhsx.covid19.android.app.about.VenueHistoryActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.settings.animations.AnimationsActivity
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesActivity
import uk.nhs.nhsx.covid19.android.app.settings.myarea.MyAreaActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class SettingsActivity : BaseActivity(R.layout.activity_settings) {

    @Inject
    lateinit var factory: ViewModelFactory<SettingsViewModel>
    private val viewModel: SettingsViewModel by viewModels { factory }

    /**
     * Dialog currently displayed, or null if none are displayed
     */
    private var currentDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        setNavigateUpToolbar(toolbar, R.string.settings_title, upIndicator = R.drawable.ic_arrow_back_white)

        setupViewModelListeners()
        setClickListeners()
        viewModel.loadSettings()
    }

    private fun setupViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            languageOption.subtitle = getString(viewState.language.languageName)
            if (viewState.showDeleteAllDataDialog) {
                showConfirmDeletingAllDataDialog()
            }
        }

        viewModel.getAllUserDataDeleted().observe(this) {
            handleAllUserDataDeleted()
        }
    }

    private fun setClickListeners() {
        languageOption.setOnSingleClickListener {
            startActivity<LanguagesActivity>()
        }

        myAreaOption.setOnSingleClickListener {
            startActivity<MyAreaActivity>()
        }

        myDataOption.setOnSingleClickListener {
            startActivity<MyDataActivity>()
        }

        venueHistoryOption.setOnSingleClickListener {
            startActivity<VenueHistoryActivity>()
        }

        actionDeleteAllData.setOnSingleClickListener {
            viewModel.onDeleteAllUserDataClicked()
        }

        animationsOption.setOnSingleClickListener {
            startActivity<AnimationsActivity>()
        }
    }

    private fun showConfirmDeletingAllDataDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.about_delete_your_data_title))
        builder.setMessage(R.string.delete_data_explanation)
        builder.setPositiveButton(
            R.string.about_delete_positive_text
        ) { _, _ ->
            viewModel.deleteAllUserData()
        }

        builder.setNegativeButton(
            R.string.cancel
        ) { dialog, _ ->
            dialog.dismiss()
        }

        builder.setOnDismissListener {
            currentDialog = null
            viewModel.onDialogDismissed()
        }

        currentDialog = builder.show()
    }

    private fun handleAllUserDataDeleted() {
        startActivity<MainActivity> {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        finish()
    }
}
