package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySelfReportCheckAnswersBinding
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.SubmitAndContinue
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.SymptomsOnset
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.TestKitType
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.TestOrigin
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.ThankYou
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithoutTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.format.DateTimeFormatter
import javax.inject.Inject
 import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class SelfReportCheckAnswersActivity : BaseActivity() {

    @Inject
    lateinit var factory: SelfReportCheckAnswersViewModel.Factory

    private val viewModel: SelfReportCheckAnswersViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    private lateinit var binding: ActivitySelfReportCheckAnswersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySelfReportCheckAnswersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_check_answers_back_button_accessibility_description
        )

        setObservers()
        setNavigationObservers()
        setClickListeners()
    }

    private fun setClickListeners() {
        binding.selfReportCheckAnswersContinueButton.setOnSingleClickListener {
            viewModel.onClickSubmitAndContinue()
        }
    }

    private fun setObservers() {
        viewModel.viewState().observe(this) { viewState ->
            viewState.selfReportTestQuestions.testKitType?.let {
                setupTestKitTypeAnswer(it)
            }
            viewState.selfReportTestQuestions.isNHSTest?.let {
                setupTestOriginAnswer(it)
            }
            viewState.selfReportTestQuestions.testEndDate?.let {
                setupTestTestDateAnswer(it)
            }
            viewState.selfReportTestQuestions.hadSymptoms?.let {
                setupSymptomsAnswer(it)
            }
            viewState.selfReportTestQuestions.symptomsOnsetDate?.let {
                setupSymptomsDateAnswer(it)
            }
            viewState.selfReportTestQuestions.hasReportedResult?.let {
                setupReportedTestAnswer(it)
            }
        }
    }

    private fun setNavigationObservers() {
        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is TestKitType -> {
                    startActivity<TestKitTypeActivity> {
                        putExtra(
                            TestKitTypeActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is TestOrigin -> {
                    startActivity<TestOriginActivity> {
                        putExtra(
                            TestOriginActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is TestDate -> {
                    startActivity<SelectTestDateActivity> {
                        putExtra(
                            SelectTestDateActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is Symptoms -> {
                    startActivity<SelfReportSymptomsActivity> {
                        putExtra(
                            SelfReportSymptomsActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is SymptomsOnset -> {
                    startActivity<SelfReportSymptomsOnsetActivity> {
                        putExtra(
                            SelfReportSymptomsOnsetActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is ReportedTest -> {
                    startActivity<ReportedTestActivity> {
                        putExtra(
                            ReportedTestActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is SubmitAndContinue -> {
                    startActivity<SelfReportSubmitTestResultAndKeysProgressActivity> {
                        putExtra(
                            SelfReportSubmitTestResultAndKeysProgressActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is ThankYou -> {
                    startActivity<SelfReportThankYouActivity> {
                        putExtra(SelfReportThankYouActivity.SHARING_SUCCESSFUL, navTarget.hasSharedSuccessfully)
                        putExtra(SelfReportThankYouActivity.HAS_REPORTED, navTarget.hasReported)
                    }
                    finish()
                }
            }
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun setupTestKitTypeAnswer(testKitType: VirologyTestKitType) = with(binding.selfReportCheckAnswersTestKitType) {
        checkAnswerQuestionSubtitle.text = getString(R.string.self_report_test_kit_type_header)
        checkAnswerQuestionAnswer.text = when (testKitType) {
            LAB_RESULT -> getString(R.string.self_report_test_kit_type_radio_button_option_pcr)
            else -> getString(R.string.self_report_test_kit_type_radio_button_option_lfd)
        }
        checkAnswerQuestionChangeButton.contentDescription = getString(R.string.self_report_check_answers_change_test_kit_type)
        checkAnswerQuestionChangeButton.setOnSingleClickListener {
            viewModel.changeTestKitTypeClicked()
        }
    }

    private fun setupTestOriginAnswer(isNHSTest: Boolean) = with(binding.selfReportCheckAnswersTestOrigin) {
        checkAnswersElementContainer.visible()
        checkAnswerQuestionSubtitle.text = getString(R.string.self_report_test_origin_header)
        checkAnswerQuestionAnswer.text = when (isNHSTest) {
            true -> getString(R.string.self_report_test_origin_radio_button_option_yes)
            else -> getString(R.string.self_report_test_origin_radio_button_option_no)
        }
        checkAnswerQuestionChangeButton.contentDescription = getString(R.string.self_report_check_answers_change_test_origin)
        checkAnswerQuestionChangeButton.setOnSingleClickListener {
            viewModel.changeTestOriginClicked()
        }
    }

    private fun setupTestTestDateAnswer(testDate: ChosenDate) = with(binding.selfReportCheckAnswersTestDate) {
        checkAnswerQuestionSubtitle.text = getString(R.string.self_report_test_date_header)
        checkAnswerQuestionAnswer.text = when (testDate.rememberedDate) {
            true -> DateTimeFormatter.ofPattern(DATE_FORMAT).format(testDate.date)
            else -> getString(R.string.self_report_test_date_no_date)
        }
        checkAnswerQuestionChangeButton.contentDescription = getString(R.string.self_report_check_answers_change_test_date)
        checkAnswerQuestionChangeButton.setOnSingleClickListener {
            viewModel.changeTestDateClicked()
        }
    }

    private fun setupSymptomsAnswer(symptoms: Boolean) = with(binding.selfReportCheckAnswersSymptoms) {
        checkAnswersElementContainer.visible()
        checkAnswerQuestionSubtitle.text = getString(R.string.self_report_symptoms_header)
        checkAnswerQuestionBulletedParagraph.visible()
        checkAnswerQuestionBulletedParagraph.setRawText(getString(R.string.self_report_symptoms_bullets_content))
        checkAnswerQuestionAnswer.text = when (symptoms) {
            true -> getString(R.string.self_report_symptoms_radio_button_option_yes)
            else -> getString(R.string.self_report_symptoms_radio_button_option_no)
        }
        checkAnswerQuestionChangeButton.contentDescription = getString(R.string.self_report_check_answers_change_symptoms)
        checkAnswerQuestionChangeButton.setOnSingleClickListener {
            viewModel.changeSymptomsClicked()
        }
    }

    private fun setupSymptomsDateAnswer(symptomsDate: ChosenDate) = with(binding.selfReportCheckAnswersSymptomsOnset) {
        checkAnswersElementContainer.visible()
        checkAnswerQuestionSubtitle.text = getString(R.string.self_report_symptoms_date_header)
        checkAnswerQuestionAnswer.text = when (symptomsDate.rememberedDate) {
            true -> DateTimeFormatter.ofPattern(DATE_FORMAT).format(symptomsDate.date)
            else -> getString(R.string.self_report_symptoms_date_no_date)
        }
        checkAnswerQuestionChangeButton.contentDescription = getString(R.string.self_report_check_answers_change_symptoms_onset)
        checkAnswerQuestionChangeButton.setOnSingleClickListener {
            viewModel.changeSymptomsOnsetClicked()
        }
    }

    private fun setupReportedTestAnswer(reportedTest: Boolean) = with(binding.selfReportCheckAnswersReportedTest) {
        checkAnswersElementContainer.visible()
        checkAnswerQuestionSubtitle.text = getString(R.string.self_report_reported_test_header)
        checkAnswerQuestionAnswer.text = when (reportedTest) {
            true -> getString(R.string.self_report_reported_test_radio_button_option_yes)
            else -> getString(R.string.self_report_reported_test_radio_button_option_no)
        }
        checkAnswerQuestionChangeButton.contentDescription = getString(R.string.self_report_check_answers_change_reported_test)
        checkAnswerQuestionChangeButton.setOnSingleClickListener {
            viewModel.changeReportedTestClicked()
        }
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
        const val DATE_FORMAT = "d MMM yyyy"
    }
}
