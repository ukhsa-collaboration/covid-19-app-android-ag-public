package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.LocaleProvider
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityCheckYourAnswersBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget.HowYouFeel
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget.Next
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget.YourSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class CheckYourAnswersActivity : BaseActivity() {

    private lateinit var binding: ActivityCheckYourAnswersBinding

    @Inject
    lateinit var localeProvider: LocaleProvider

    @Inject
    lateinit var factory: CheckYourAnswersViewModel.Factory

    private val viewModel: CheckYourAnswersViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SYMPTOMS_DATA_KEY)
                ?: throw IllegalStateException("Symptoms data was not available from starting intent")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityCheckYourAnswersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.check_answers_heading,
                upIndicator = R.drawable.ic_arrow_back_white
            )

            checkYourAnswersStepOf.text = getString(R.string.generic_step_label, 3, 3)
            checkYourAnswersStepOf.contentDescription = getString(R.string.generic_step_label, 3, 3)

            checkYourAnswersYourSymptoms.textReviewYourSymptomAnswer.text = getString(R.string.your_symptoms_title)
            checkYourAnswersHowDoYouFeel.textReviewYourSymptomAnswer.text = getString(R.string.how_you_feel_header)

            setOnClickListeners()
            setupObservers()
        }
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            setText(viewState)
            setAnswers(viewState)
        }
        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is Next -> {
                    startActivity<SymptomCheckerAdviceActivity>() {
                        putExtra(SymptomCheckerAdviceActivity.VALUE_KEY_QUESTIONS, navTarget.symptomsCheckerQuestions)
                        putExtra(SymptomCheckerAdviceActivity.VALUE_KEY_RESULT, navTarget.result)
                    }
                    finish()
                }
                is HowYouFeel -> {
                    startActivity<HowDoYouFeelActivity> {
                        putExtra(SYMPTOMS_DATA_KEY, navTarget.symptomsCheckerQuestions)
                    }
                    finish()
                }
                is YourSymptoms -> {
                    startActivity<YourSymptomsActivity> {
                        putExtra(SYMPTOMS_DATA_KEY, navTarget.symptomsCheckerQuestions)
                    }
                    finish()
                }
            }
        }
    }

    private fun setText(viewState: ViewState) {
        if (viewState.symptomsCheckerQuestions.cardinalSymptom?.title != null &&
            viewState.symptomsCheckerQuestions.nonCardinalSymptoms?.title != null
        ) {
            binding.checkYourAnswersNonCardinalQuestion.text =
                viewState.symptomsCheckerQuestions.nonCardinalSymptoms.title.translate(localeProvider.default())

            binding.checkYourAnswersNonCardinalBulletParagraph.setRawBoldText(
                viewState.symptomsCheckerQuestions.nonCardinalSymptoms.nonCardinalSymptomsText.translate(localeProvider.default()))

            binding.checkYourAnswersCardinalQuestion.text =
                viewState.symptomsCheckerQuestions.cardinalSymptom.title.translate(localeProvider.default())
        }
    }

    private fun setAnswers(viewState: ViewState) {
        if (viewState.symptomsCheckerQuestions.nonCardinalSymptoms?.isChecked != null &&
            viewState.symptomsCheckerQuestions.cardinalSymptom?.isChecked != null &&
            viewState.symptomsCheckerQuestions.howDoYouFeelSymptom?.isChecked != null
        ) {
            setIndividualAnswer(
                viewState.symptomsCheckerQuestions.nonCardinalSymptoms.isChecked,
                binding.checkYourAnswersNonCardinalAnswer.imageSymptomMark,
                binding.checkYourAnswersNonCardinalAnswer.textSymptomMessage
            )

            setIndividualAnswer(
                viewState.symptomsCheckerQuestions.cardinalSymptom.isChecked,
                binding.checkYourAnswersCardinalAnswer.imageSymptomMark,
                binding.checkYourAnswersCardinalAnswer.textSymptomMessage
            )

            setIndividualAnswer(
                viewState.symptomsCheckerQuestions.howDoYouFeelSymptom.isChecked,
                binding.checkYourAnswersHowDoYouFeelAnswer.imageSymptomMark,
                binding.checkYourAnswersHowDoYouFeelAnswer.textSymptomMessage
            )
        }
    }

    private fun setIndividualAnswer(checked: Boolean, imageSymptomMark: ImageView, textSymptomMessage: TextView) {
        if (checked) {
            imageSymptomMark.setImageResource(R.drawable.ic_tick_green)
            textSymptomMessage.text = getString(R.string.check_answers_yes_answer)
        } else {
            imageSymptomMark.setImageResource(R.drawable.ic_cross_red)
            textSymptomMessage.text = getString(R.string.check_answers_no_answer)
        }
    }

    private fun setOnClickListeners() {
        binding.checkYourAnswersContinueButton.setOnSingleClickListener {
            viewModel.onClickSubmitAnswers()
        }

        binding.checkYourAnswersYourSymptoms.textChange.setOnSingleClickListener {
            viewModel.onClickYourSymptomsChange()
        }

        binding.checkYourAnswersHowDoYouFeel.textChange.setOnSingleClickListener {
            viewModel.onClickHowYouFeelChange()
        }
    }

    override fun onBackPressed() {
        viewModel.onClickHowYouFeelChange()
    }

    companion object {
        const val SYMPTOMS_DATA_KEY = "SYMPTOMS_DATA_KEY"
    }
}
