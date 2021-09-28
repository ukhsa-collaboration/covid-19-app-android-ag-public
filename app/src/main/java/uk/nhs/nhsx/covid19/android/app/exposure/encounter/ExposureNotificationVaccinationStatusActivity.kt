package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.questionsRecyclerView
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
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class ExposureNotificationVaccinationStatusActivity :
    BaseActivity(R.layout.activity_exposure_notification_vaccination_status) {

    @Inject
    lateinit var factory: ViewModelFactory<ExposureNotificationVaccinationStatusViewModel>

    private val viewModel: ExposureNotificationVaccinationStatusViewModel by viewModels { factory }

    private var hasScrolledToError = false

    private lateinit var questionnaireViewAdapter: VaccinationStatusQuestionnaireAdapter

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

        setupAdapter()
        setUpViewModelListeners()
    }

    private fun setupAdapter() {
        questionnaireViewAdapter = VaccinationStatusQuestionnaireAdapter(
            viewModel::onFullyVaccinatedOptionChanged,
            viewModel::onLastDoseDateOptionChanged,
            viewModel::onMedicallyExemptOptionChanged,
            viewModel::onClinicalTrialOptionChanged
        )
        questionsRecyclerView.layoutManager = LinearLayoutManager(this)
        questionsRecyclerView.adapter = questionnaireViewAdapter
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

                val vaccinationStatusQuestions = questions.map { it.toVaccinationStatusQuestion(date) }
                questionnaireViewAdapter.submitList(vaccinationStatusQuestions)
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
