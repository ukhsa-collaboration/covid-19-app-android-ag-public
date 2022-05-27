package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.Lce.Error
import uk.nhs.nhsx.covid19.android.app.common.Lce.Loading
import uk.nhs.nhsx.covid19.android.app.common.Lce.Success
import uk.nhs.nhsx.covid19.android.app.common.LocaleProvider
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityYourSymptomsBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.YourSymptomsViewModel.NavigationTarget.Next
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.YourSymptomsViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class YourSymptomsActivity : BaseActivity() {
    private lateinit var binding: ActivityYourSymptomsBinding

    @Inject
    lateinit var factory: ViewModelFactory<YourSymptomsViewModel>

    @Inject
    lateinit var localeProvider: LocaleProvider

    private val viewModel: YourSymptomsViewModel by viewModels { factory }

    private var hasScrolledToError: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityYourSymptomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.your_symptoms_title,
                upIndicator = R.drawable.ic_arrow_back_white
            )

            nonCardinalBinaryRadioGroup.setOnValueChangedListener(viewModel::onNonCardinalOptionChecked)
            cardinalBinaryRadioGroup.setOnValueChangedListener(viewModel::onCardinalOptionChecked)

            yourSymptomsContinueButton.setOnSingleClickListener {
                viewModel.onClickContinue()
            }
        }

        setupListeners()
        setupObservers()

        val questionnaireData = intent.getParcelableExtra<SymptomsCheckerQuestions>(SYMPTOMS_DATA_KEY)
        viewModel.loadQuestionnaire(questionnaireData)
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) {
            when (it) {
                is Success -> handleSuccess(it.data)
                is Error -> showErrorState()
                is Loading -> showLoadingSpinner()
            }
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is Next -> {
                    startActivity<HowDoYouFeelActivity> {
                        putExtra(HowDoYouFeelActivity.SYMPTOMS_DATA_KEY, navTarget.symptomsCheckerQuestions)
                    }
                    finish()
                }
            }
        }
    }

    private fun handleSuccess(viewState: ViewState) {
        showYourSymptoms(viewState)
        binding.nonCardinalBinaryRadioGroup.selectedOption = viewState.nonCardinalSymptomsSelection
        binding.cardinalBinaryRadioGroup.selectedOption = viewState.cardinalSymptomSelection
        checkForErrors(viewState)
    }

    private fun showLoadingSpinner() = with(binding) {
        yourSymptomsLoadingContainer.visible()
        yourSymptomsErrorStateContainer.gone()
        yourSymptomsScrollViewContainer.gone()

        setAccessibilityTitle(R.string.loading)
    }

    private fun showErrorState() = with(binding) {
        yourSymptomsErrorStateContainer.visible()
        yourSymptomsLoadingContainer.gone()
        yourSymptomsScrollViewContainer.gone()

        setAccessibilityTitle("${textErrorTitle.text}. ${textErrorMessage.text}")
    }

    private fun showYourSymptoms(viewState: ViewState) = with(binding) {
        setAccessibilityTitle(R.string.your_symptoms_title)

        yourSymptomsScrollViewContainer.visible()
        yourSymptomsLoadingContainer.gone()
        yourSymptomsErrorStateContainer.gone()

        yourSymptomsStepOf.text = getString(R.string.generic_step_label, 1, 3)
        yourSymptomsStepOf.contentDescription = getString(R.string.generic_step_label, 1, 3)
        textNonCardinalSymptomsTitle.text = viewState.nonCardinal.title.translate(localeProvider.default())
        nonCardinalBulletParagraph.setRawText(viewState.nonCardinal.description.translate(localeProvider.default()))
        textCardinalSymptomTitle.text = viewState.cardinal.title.translate(localeProvider.default())
    }

    private fun checkForErrors(
        viewState: ViewState
    ) {
        when {
            viewState.hasDidNotCheckAnyQuestionError -> showAndScrollToError(getString(R.string.your_symptoms_no_questions_answerd_error_description))
            viewState.hasDidNotCheckNonCardinalSymptomsError -> showAndScrollToError(
                getString(
                    R.string.your_symptoms_error_description,
                    viewState.nonCardinal.title.translate(localeProvider.default())
                )
            )
            viewState.hasDidNotCheckCardinalSymptomsError -> showAndScrollToError(
                getString(
                    R.string.your_symptoms_error_description,
                    viewState.cardinal.title.translate(localeProvider.default())
                )
            )
            else -> binding.yourSymptomsErrorView.gone()
        }
    }

    private fun showAndScrollToError(errorDescription: String) = with(binding) {
        yourSymptomsErrorView.errorDescription = errorDescription
        yourSymptomsErrorView.visible()
        if (!hasScrolledToError) {
            yourSymptomsScrollViewContainer.smoothScrollToAndThen(0, 0) {
                yourSymptomsErrorView.requestFocusFromTouch()
                yourSymptomsErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                hasScrolledToError = true
            }
        }
    }

    private fun setupListeners() = with(binding) {
        buttonTryAgain.setOnSingleClickListener {
            viewModel.loadQuestionnaire()
        }
    }

    companion object {
        const val SYMPTOMS_DATA_KEY = "SYMPTOMS_DATA_KEY"
    }
}
