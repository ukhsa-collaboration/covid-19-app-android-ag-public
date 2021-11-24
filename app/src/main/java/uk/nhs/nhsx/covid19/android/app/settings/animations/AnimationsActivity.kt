package uk.nhs.nhsx.covid19.android.app.settings.animations

import android.app.Dialog
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityAnimationsBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setMultilineTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class AnimationsActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<AnimationsViewModel>
    private val viewModel: AnimationsViewModel by viewModels { factory }
    private var currentDialog: Dialog? = null

    private lateinit var binding: ActivityAnimationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityAnimationsBinding.inflate(layoutInflater)

        with(binding) {

            setContentView(root)

            startListeningToViewState()

            setNavigateUpToolbar(primaryToolbar.toolbar, titleResId = R.string.animations_settings_title)

            optionHomeScreenAnimation.setOnSingleClickListener {
                viewModel.onAnimationToggleClicked()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroy() {
        currentDialog?.setOnDismissListener { }
        // To avoid leaking the window
        currentDialog?.dismiss()
        currentDialog = null

        super.onDestroy()
    }

    private fun startListeningToViewState() {
        viewModel.viewState().observe(this) { viewState ->
            handleAnimationState(viewState.animationsEnabled)
            if (viewState.showDialog) {
                showAnimationDialog()
            }
        }
    }

    private fun handleAnimationState(animationsEnabled: Boolean) = with(binding) {
        homeScreenAnimationSwitch.isChecked = animationsEnabled
        val animationStatusText =
            if (animationsEnabled) {
                R.string.animations_status_on
            } else {
                R.string.animations_status_off
            }
        homeScreenAnimationStatus.text = getString(animationStatusText)
    }

    private fun showAnimationDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMultilineTitle(getString(R.string.animations_dialog_title))
        dialogBuilder.setMessage(R.string.animations_dialog_text)
        dialogBuilder.setPositiveButton(R.string.okay) { dialog, _ ->
            dialog.dismiss()
        }

        dialogBuilder.setOnDismissListener {
            currentDialog = null
            viewModel.onDialogDismissed()
        }

        currentDialog = dialogBuilder.show()
    }
}
