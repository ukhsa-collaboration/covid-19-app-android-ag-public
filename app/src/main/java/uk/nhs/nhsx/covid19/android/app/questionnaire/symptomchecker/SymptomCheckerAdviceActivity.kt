package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySymptomsCheckerAdviceBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.NavigationTarget.BackToHome
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.NavigationTarget.BackToQuestionnaire
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.ViewState.ContinueNormalActivities
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.ViewState.TryToStayAtHome
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class SymptomCheckerAdviceActivity : BaseActivity() {

    @Inject
    lateinit var factory: SymptomCheckerAdviceViewModel.Factory

    private val viewModel: SymptomCheckerAdviceViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(VALUE_KEY_QUESTIONS)
                ?: throw IllegalStateException("Missing symptom checker questions data"),
            result = intent.getSerializableExtra(VALUE_KEY_RESULT) as? SymptomCheckerAdviceResult
                ?: throw IllegalStateException("Missing symptom checker result data")
        )
    }

    private lateinit var binding: ActivitySymptomsCheckerAdviceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySymptomsCheckerAdviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            setNavigateUpToolbar(toolbar = primaryToolbar.toolbar, R.string.empty, R.drawable.ic_arrow_back_primary)
            symptomsCheckerAdviceFinishButton.setOnSingleClickListener {
                viewModel.onFinishPressed()
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            when (viewState) {
                TryToStayAtHome -> {
                    with(binding) {
                        symptomsCheckerAdviceTitle.text = getString(R.string.symptom_checker_advice_stay_at_home_header)
                        symptomsCheckerAdviceNoticeLink.setDisplayText(R.string.symptom_checker_advice_notice_stay_at_home_link_text)
                        symptomsCheckerAdviceNoticeLink.setOnSingleClickListener {
                            openUrl(
                                getString(R.string.symptom_checker_advice_notice_stay_at_home_link_url)
                            )
                        }
                        symptomsCheckerAdviceImage.setImageResource(R.drawable.ic_isolation_book_test)
                    }
                }
                ContinueNormalActivities -> {
                    with(binding) {
                        symptomsCheckerAdviceTitle.text =
                            getString(R.string.symptom_checker_advice_continue_normal_activities_header)
                        symptomsCheckerAdviceNoticeLink.setDisplayText(R.string.symptom_checker_advice_notice_continue_normal_activities_link_text)
                        symptomsCheckerAdviceNoticeLink.setOnSingleClickListener {
                            openUrl(
                                getString(R.string.symptom_checker_advice_notice_continue_normal_activities_link_url)
                            )
                        }
                        symptomsCheckerAdviceImage.setImageResource(R.drawable.ic_onboarding_welcome)
                    }
                }
            }
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is BackToHome -> {
                    StatusActivity.start(this@SymptomCheckerAdviceActivity)
                    finish()
                }
                is BackToQuestionnaire -> {
                    startActivity<CheckYourAnswersActivity> {
                        putExtra(CheckYourAnswersActivity.SYMPTOMS_DATA_KEY, navTarget.symptomsCheckerQuestions)
                    }
                    finish()
                }
            }
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    companion object {
        const val VALUE_KEY_QUESTIONS = "SYMPTOM_CHECKER_QUESTIONS_KEY"
        const val VALUE_KEY_RESULT = "SYMPTOM_CHECKER_ADVICE_RESULT_KEY"
    }
}
