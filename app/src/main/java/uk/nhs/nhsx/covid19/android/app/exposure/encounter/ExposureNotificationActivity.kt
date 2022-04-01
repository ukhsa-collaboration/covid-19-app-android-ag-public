package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R.color
import uk.nhs.nhsx.covid19.android.app.R.layout
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityExposureNotificationBinding
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.NewEnglandJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.NewWalesJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.QuestionnaireJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.ExposureNotificationAgeLimit
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.NewContactJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.ContinueIsolation
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class ExposureNotificationActivity : BaseActivity() {
    @Inject
    lateinit var factory: ViewModelFactory<ExposureNotificationViewModel>

    private val viewModel: ExposureNotificationViewModel by viewModels { factory }

    private lateinit var binding: ActivityExposureNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityExposureNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.updateViewState()

        binding.primaryActionButton.setOnSingleClickListener {
            viewModel.onPrimaryButtonClick()
        }

        viewModel.contactJourney.observe(this) { contactJourney ->
            renderViewState(contactJourney)
        }

        viewModel.finishActivity.observe(this) {
            finish()
        }

        viewModel.navigationTarget().observe(this) {
            when (it) {
                ExposureNotificationAgeLimit -> ExposureNotificationAgeLimitActivity.start(this)
                NewContactJourney -> startActivity<RiskyContactIsolationOptOutActivity>()
                ContinueIsolation -> RiskyContactIsolationAdviceActivity.start(this)
            }
        }
    }

    private fun renderViewState(contactJourney: ContactJourney) {
        val formattedDate = contactJourney.encounterDate.uiLongFormat(this@ExposureNotificationActivity)
        when (contactJourney) {
            is NewEnglandJourney -> renderViewForNewEnglandJourney(formattedDate)
            is NewWalesJourney -> renderViewForNewWalesJourney(formattedDate)
            is QuestionnaireJourney -> renderViewForQuestionnaireJourney(
                formattedDate,
                contactJourney.shouldShowTestingAndIsolationAdvice
            )
        }
    }

    private fun renderViewForNewEnglandJourney(formattedDate: String) = with(binding) {
        exposureNotificationHeading.text = getString(string.contact_case_exposure_info_screen_title_england)
        closeContactDate.text =
            getString(string.contact_case_exposure_info_screen_exposure_date_england, formattedDate)
        closeContactAccordionButtonView.title =
            getString(string.contact_case_exposure_info_screen_how_close_contacts_are_calculated_heading_england)
        closeContactAccordionButtonView.content = layout.accordion_how_we_calculate_close_contact_england
        primaryActionButton.text = getString(string.contact_case_exposure_info_screen_continue_button_england)
        selfIsolationWarning.apply {
            stateText = getString(string.contact_case_exposure_info_screen_information_england)
            stateColor = ContextCompat.getColor(context, color.amber)
        }
        testingInformationContainer.gone()
    }

    private fun renderViewForNewWalesJourney(formattedDate: String) = with(binding) {
        exposureNotificationHeading.text = getString(string.contact_case_exposure_info_screen_title_wales)
        closeContactDate.text =
            getString(string.contact_case_exposure_info_screen_exposure_date_wales, formattedDate)
        closeContactAccordionButtonView.title =
            getString(string.contact_case_exposure_info_screen_how_close_contacts_are_calculated_heading_wales)
        closeContactAccordionButtonView.content = layout.accordion_how_we_calculate_close_contact_wales
        primaryActionButton.text = getString(string.contact_case_exposure_info_screen_continue_button_wales)
        selfIsolationWarning.apply {
            stateText = getString(string.contact_case_exposure_info_screen_information_wales)
            stateColor = ContextCompat.getColor(context, color.amber)
        }
        testingInformationContainer.gone()
    }

    private fun renderViewForQuestionnaireJourney(formattedDate: String, shouldShowTestingAndIsolationAdvice: Boolean) =
        with(binding) {
            exposureNotificationHeading.text = getString(string.exposure_notification_title)
            closeContactDate.text =
                getString(string.contact_case_exposure_info_screen_exposure_date, formattedDate)
            closeContactAccordionButtonView.title =
                getString(string.contact_case_exposure_info_screen_how_close_contacts_are_calculated_heading)
            closeContactAccordionButtonView.content = layout.accordion_how_we_calculate_close_contact
            primaryActionButton.text = getString(string.continue_button)
            selfIsolationWarning.apply {
                stateText = getString(string.exposure_notification_warning)
                stateColor = ContextCompat.getColor(context, color.amber)
            }
            selfIsolationWarning.isVisible = shouldShowTestingAndIsolationAdvice
            testingInformationContainer.isVisible = shouldShowTestingAndIsolationAdvice
        }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ExposureNotificationActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
    }
}
