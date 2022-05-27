package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityHowDoYouFeelBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.HowDoYouFeelViewModel.NavigationTarget.Next
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.HowDoYouFeelViewModel.NavigationTarget.Previous
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class HowDoYouFeelActivity : BaseActivity() {

    private lateinit var binding: ActivityHowDoYouFeelBinding

    @Inject
    lateinit var factory: HowDoYouFeelViewModel.Factory

    private val viewModel: HowDoYouFeelViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SYMPTOMS_DATA_KEY)
                ?: throw IllegalStateException("Symptoms data was not available from starting intent")
        )
    }

    private var hasScrolledToError: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityHowDoYouFeelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.how_you_feel_header,
                upIndicator = R.drawable.ic_arrow_back_white
            )

            howDoYouFeelTextStep.contentDescription = getString(R.string.generic_step_label, 2, 3)
            howDoYouFeelTextStep.text = getString(R.string.generic_step_label, 2, 3)

            howDoYouFeelBinaryRadioGroup.setOnValueChangedListener(viewModel::onHowDoYouFeelOptionChecked)

            howDoYouFeelContinueButton.setOnSingleClickListener {
                viewModel.onClickContinue()
            }
        }

        setupObservers()
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun showError(hasError: Boolean) = with(binding) {
        if (hasError) {
            howDoYouFeelErrorView.visible()
            if (!hasScrolledToError) {
                howDoYouFeelScrollViewContainer.smoothScrollToAndThen(0, 0) {
                    howDoYouFeelErrorView.requestFocusFromTouch()
                    howDoYouFeelErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    hasScrolledToError = true
                }
            }
        } else {
            binding.howDoYouFeelErrorView.gone()
        }
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            binding.howDoYouFeelBinaryRadioGroup.selectedOption = viewState.howDoYouFeelSelection
            showError(viewState.hasError)
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is Next -> {
                    startActivity<CheckYourAnswersActivity> {
                        putExtra(CheckYourAnswersActivity.SYMPTOMS_DATA_KEY, navTarget.symptomsCheckerQuestions)
                    }
                    finish()
                }
                is Previous -> {
                    startActivity<YourSymptomsActivity> {
                        putExtra(YourSymptomsActivity.SYMPTOMS_DATA_KEY, navTarget.symptomsCheckerQuestions)
                    }
                    finish()
                }
            }
        }
    }

    companion object {
        const val SYMPTOMS_DATA_KEY = "SYMPTOMS_DATA_KEY"
    }
}
