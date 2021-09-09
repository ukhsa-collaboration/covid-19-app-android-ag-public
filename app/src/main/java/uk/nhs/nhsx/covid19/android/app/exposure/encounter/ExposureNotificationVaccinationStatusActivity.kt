package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.questionsContainer
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccinationStatusContinueButton
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccinationStatusErrorView
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccinationStatusScrollView
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccinationStatusSubtitle
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Isolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.QuestionType.CLINICAL_TRIAL
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.QuestionType.DOSE_DATE
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.QuestionType.FULLY_VACCINATED
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.QuestionType.MEDICALLY_EXEMPT
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import uk.nhs.nhsx.covid19.android.app.widgets.AccessibilityTextView
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup
import javax.inject.Inject

class ExposureNotificationVaccinationStatusActivity :
    BaseActivity(R.layout.activity_exposure_notification_vaccination_status) {
    @Inject
    lateinit var factory: ViewModelFactory<ExposureNotificationVaccinationStatusViewModel>

    private val viewModel: ExposureNotificationVaccinationStatusViewModel by viewModels { factory }

    private var hasScrolledToError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar,
            R.string.exposure_notification_vaccination_status_title,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        vaccinationStatusContinueButton.setOnSingleClickListener {
            hasScrolledToError = false
            viewModel.onClickContinue()
        }

        setUpViewModelListeners()
    }

    private fun setUpViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            with(viewState) {
                if (showError) {
                    vaccinationStatusErrorView.visible()
                    if (!hasScrolledToError) {
                        vaccinationStatusScrollView.smoothScrollToAndThen(0, 0) {
                            vaccinationStatusErrorView.requestFocusFromTouch()
                            vaccinationStatusErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                            hasScrolledToError = true
                        }
                    }
                } else {
                    vaccinationStatusErrorView.gone()
                }
                vaccinationStatusSubtitle.isVisible = showSubtitle
            }

            questionsContainer.removeAllViews()
            viewState.questions.forEach { question ->
                when (question.questionType) {
                    FULLY_VACCINATED -> {
                        val view = layoutInflater.inflate(
                            R.layout.binary_question_fully_vaccinated,
                            questionsContainer,
                            true
                        )
                        val fullyVaccinatedBinaryRadioGroup =
                            view.findViewById<BinaryRadioGroup>(R.id.allDosesBinaryRadioGroup)
                        fullyVaccinatedBinaryRadioGroup.selectedOption = question.state
                        fullyVaccinatedBinaryRadioGroup.setOnValueChangedListener(viewModel::onFullyVaccinatedOptionChanged)
                    }
                    DOSE_DATE -> {
                        val view = layoutInflater.inflate(
                            R.layout.binary_question_vaccine_date,
                            questionsContainer,
                            true
                        )
                        val lastDoseDateBinaryRadioGroup =
                            view.findViewById<BinaryRadioGroup>(R.id.vaccineDateBinaryRadioGroup)
                        lastDoseDateBinaryRadioGroup.selectedOption = question.state
                        lastDoseDateBinaryRadioGroup.setOnValueChangedListener(viewModel::onLastDoseDateOptionChanged)

                        val formattedDate =
                            viewState.date.uiLongFormat(this@ExposureNotificationVaccinationStatusActivity)

                        val lastDoseDateQuestion =
                            view.findViewById<AccessibilityTextView>(R.id.vaccineDateQuestion)
                        lastDoseDateQuestion.text = getString(
                            R.string.exposure_notification_vaccination_status_date_question,
                            formattedDate
                        )

                        lastDoseDateBinaryRadioGroup.setOption1Text(
                            text = getString(R.string.exposure_notification_vaccination_status_date_yes),
                            contentDescription = getString(
                                R.string.exposure_notification_vaccination_status_date_yes_content_description,
                                formattedDate
                            )
                        )
                        lastDoseDateBinaryRadioGroup.setOption2Text(
                            text = getString(R.string.exposure_notification_vaccination_status_date_no),
                            contentDescription = getString(
                                R.string.exposure_notification_vaccination_status_date_no_content_description,
                                formattedDate
                            )
                        )
                    }
                    MEDICALLY_EXEMPT -> {
                        val view = layoutInflater.inflate(
                            R.layout.binary_question_medically_exempt,
                            questionsContainer,
                            true
                        )
                        val medicallyExemptBinaryRadioGroup =
                            view.findViewById<BinaryRadioGroup>(R.id.medicallyExemptBinaryRadioGroup)
                        medicallyExemptBinaryRadioGroup.selectedOption = question.state
                        medicallyExemptBinaryRadioGroup.setOnValueChangedListener(viewModel::onMedicallyExemptOptionChanged)
                    }
                    CLINICAL_TRIAL -> {
                        val view = layoutInflater.inflate(
                            R.layout.binary_question_clinical_trial,
                            questionsContainer,
                            true
                        )
                        val clinicalTrialBinaryRadioGroup =
                            view.findViewById<BinaryRadioGroup>(R.id.clinicalTrialBinaryRadioGroup)
                        clinicalTrialBinaryRadioGroup.selectedOption = question.state
                        clinicalTrialBinaryRadioGroup.setOnValueChangedListener(viewModel::onClinicalTrialOptionChanged)
                    }
                }
            }
        }

        viewModel.navigate().observe(this) { navigationTarget ->
            when (navigationTarget) {
                FullyVaccinated -> RiskyContactIsolationAdviceActivity.startAsFullyVaccinated(this)
                Isolating -> RiskyContactIsolationAdviceActivity.start(this)
                MedicallyExempt -> RiskyContactIsolationAdviceActivity.startAsMedicallyExempt(this)
                Finish -> finish()
            }
        }
    }
}
