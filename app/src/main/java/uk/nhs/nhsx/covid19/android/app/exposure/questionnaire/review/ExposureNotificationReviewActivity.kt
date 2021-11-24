package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityExposureNotificationReviewBinding
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget.AgeLimit
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget.IsolationAdvice
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget.VaccinationStatus
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.Minor
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.NotExempt
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class ExposureNotificationReviewActivity : BaseActivity() {

    @Inject
    lateinit var factory: ExposureNotificationReviewViewModel.Factory

    private val viewModel: ExposureNotificationReviewViewModel by assistedViewModel {
        factory.create(intent.getParcelableExtra(EXTRA_REVIEW_DATA)!!)
    }

    private lateinit var binding: ActivityExposureNotificationReviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityExposureNotificationReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbar(
            binding.primaryToolbar.toolbar,
            R.string.contact_case_summary_title,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        startListeningToViewState()

        setUpOnClickListeners()
    }

    private fun startListeningToViewState() {
        viewModel.navigationTarget.observe(this) { navigationTarget ->
            when (navigationTarget) {
                is IsolationAdvice -> navigateToIsolationAdvice(navigationTarget.questionnaireOutcome)
                AgeLimit -> ExposureNotificationAgeLimitActivity.start(this, clearTop = true)
                VaccinationStatus -> ExposureNotificationVaccinationStatusActivity.start(this, clearTop = true)
            }
        }

        viewModel.viewState.observe(this) {
            with(binding) {

                reviewYourAgeGroup.setResponses(
                    listOf(it.ageLimitResponse),
                    ageLimitDate = it.ageLimitDate,
                    lastDoseDateLimit = it.lastDoseDateLimit
                )
                reviewYourVaccinationStatusGroup.isVisible = it.vaccinationStatusResponse.isNotEmpty()
                reviewYourVaccinationStatusGroup.setResponses(
                    it.vaccinationStatusResponse,
                    ageLimitDate = it.ageLimitDate,
                    lastDoseDateLimit = it.lastDoseDateLimit
                )
            }
        }
    }

    private fun navigateToIsolationAdvice(questionnaireOutcome: QuestionnaireOutcome) {
        when (questionnaireOutcome) {
            FullyVaccinated -> RiskyContactIsolationAdviceActivity.startAsFullyVaccinated(this)
            MedicallyExempt -> RiskyContactIsolationAdviceActivity.startAsMedicallyExempt(this)
            Minor -> RiskyContactIsolationAdviceActivity.startAsMinor(this)
            is NotExempt -> RiskyContactIsolationAdviceActivity.start(this)
        }
    }

    private fun setUpOnClickListeners() = with(binding) {
        submitExposureQuestionnaire.setOnSingleClickListener {
            viewModel.onSubmitClicked()
        }

        reviewYourAgeGroup.onChangeListener = {
            viewModel.onChangeAgeLimitResponseClicked()
        }

        reviewYourVaccinationStatusGroup.onChangeListener = {
            viewModel.onChangeVaccinationStatusResponseClicked()
        }
    }

    companion object {
        private const val EXTRA_REVIEW_DATA = "EXTRA_REVIEW_DATA"

        fun start(context: Context, reviewData: ReviewData) =
            context.startActivity(getIntent(context, reviewData))

        private fun getIntent(context: Context, reviewData: ReviewData) =
            Intent(context, ExposureNotificationReviewActivity::class.java)
                .putExtra(EXTRA_REVIEW_DATA, reviewData)
    }
}
