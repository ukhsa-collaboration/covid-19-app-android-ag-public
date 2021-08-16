package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.allDosesBinaryRadioGroup
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccinationStatusContinueButton
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccinationStatusErrorView
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccinationStatusScrollView
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccineDateBinaryRadioGroup
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccineDateQuestion
import kotlinx.android.synthetic.main.activity_exposure_notification_vaccination_status.vaccineDateQuestionContainer
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Isolating
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES

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

        allDosesBinaryRadioGroup.setOnValueChangedListener(viewModel::onAllDosesOptionChanged)
        vaccineDateBinaryRadioGroup.setOnValueChangedListener(viewModel::onDoseDateOptionChanged)

        vaccinationStatusContinueButton.setOnSingleClickListener {
            hasScrolledToError = false
            viewModel.onClickContinue()
        }

        setUpViewModelListeners()
    }

    private fun setUpViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            if (viewState.showError) {
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

            if (viewState.allDosesSelection == YES) {
                setUpDoseDateQuestion()
                vaccineDateQuestionContainer.visible()
            } else {
                vaccineDateQuestionContainer.gone()
            }

            allDosesBinaryRadioGroup.selectedOption = viewState.allDosesSelection
            vaccineDateBinaryRadioGroup.selectedOption = viewState.doseDateSelection
        }

        viewModel.navigate().observe(this) { navigationTarget ->
            when (navigationTarget) {
                FullyVaccinated -> RiskyContactIsolationAdviceActivity.startAsFullyVaccinated(this)
                Isolating -> RiskyContactIsolationAdviceActivity.start(this)
            }
        }
    }

    private fun setUpDoseDateQuestion() {
        val formattedDate = viewModel.lastDoseDateLimit().uiLongFormat(this)
        vaccineDateQuestion.text =
            getString(string.exposure_notification_vaccination_status_date_question, formattedDate)
        vaccineDateBinaryRadioGroup.setOptionContentDescriptions(
            option1ContentDescription = getString(
                string.exposure_notification_vaccination_status_date_yes_content_description,
                formattedDate
            ),
            option2ContentDescription = getString(
                string.exposure_notification_vaccination_status_date_no_content_description,
                formattedDate
            )
        )
    }
}
