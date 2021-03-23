package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_my_data.dailyContactTestingOptInDate
import kotlinx.android.synthetic.main.activity_my_data.dailyContactTestingSection
import kotlinx.android.synthetic.main.activity_my_data.encounterDataSection
import kotlinx.android.synthetic.main.activity_my_data.exposureNotificationDataSection
import kotlinx.android.synthetic.main.activity_my_data.followUpDate
import kotlinx.android.synthetic.main.activity_my_data.followUpState
import kotlinx.android.synthetic.main.activity_my_data.followUpTestDateContainer
import kotlinx.android.synthetic.main.activity_my_data.followUpTestStatusContainer
import kotlinx.android.synthetic.main.activity_my_data.lastDayOfIsolationDate
import kotlinx.android.synthetic.main.activity_my_data.lastDayOfIsolationSection
import kotlinx.android.synthetic.main.activity_my_data.lastResultDate
import kotlinx.android.synthetic.main.activity_my_data.lastResultKitType
import kotlinx.android.synthetic.main.activity_my_data.lastResultValue
import kotlinx.android.synthetic.main.activity_my_data.lastRiskyVenueVisitDate
import kotlinx.android.synthetic.main.activity_my_data.lastRiskyVenueVisitSection
import kotlinx.android.synthetic.main.activity_my_data.latestResultDateContainer
import kotlinx.android.synthetic.main.activity_my_data.latestResultKitTypeContainer
import kotlinx.android.synthetic.main.activity_my_data.latestResultValueContainer
import kotlinx.android.synthetic.main.activity_my_data.noRecordsView
import kotlinx.android.synthetic.main.activity_my_data.symptomsDataSection
import kotlinx.android.synthetic.main.activity_my_data.textEncounterDate
import kotlinx.android.synthetic.main.activity_my_data.textExposureNotificationDate
import kotlinx.android.synthetic.main.activity_my_data.textViewSymptomsDate
import kotlinx.android.synthetic.main.activity_my_data.titleDailyContactTestingOptIn
import kotlinx.android.synthetic.main.activity_my_data.titleExposureNotification
import kotlinx.android.synthetic.main.activity_my_data.titleLastDayOfIsolation
import kotlinx.android.synthetic.main.activity_my_data.titleLastRiskyVenueVisit
import kotlinx.android.synthetic.main.activity_my_data.titleLatestResult
import kotlinx.android.synthetic.main.activity_my_data.titleSymptoms
import kotlinx.android.synthetic.main.activity_my_data.viewContent
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.BaseMyDataViewModel.IsolationState
import uk.nhs.nhsx.covid19.android.app.about.BaseMyDataViewModel.MyDataState
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class MyDataActivity : BaseActivity(R.layout.activity_my_data) {

    @Inject
    lateinit var factory: ViewModelFactory<BaseMyDataViewModel>

    private val viewModel: BaseMyDataViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.settings_my_data, upIndicator = R.drawable.ic_arrow_back_white)

        setupViewModelListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupViewModelListeners() {
        viewModel.myDataState().observe(this) {
            renderViewState(it)
        }
    }

    private fun renderViewState(viewState: MyDataState) {
        if (viewState.isolationState == null &&
            viewState.lastRiskyVenueVisitDate == null &&
            viewState.acknowledgedTestResult == null
        ) {
            noRecordsView.visible()
            viewContent.gone()
        } else {
            noRecordsView.gone()
            viewContent.visible()
            handleStateMachineState(viewState.isolationState)
            updateLastRiskyVenueVisitedSegment(viewState.lastRiskyVenueVisitDate)
            handleShowingLatestTestResult(viewState.acknowledgedTestResult)
        }
    }

    private fun handleShowingLatestTestResult(acknowledgedTestResult: AcknowledgedTestResult?) {
        if (acknowledgedTestResult != null) {
            lastResultDate.text = uiFormat(acknowledgedTestResult.testEndDate)
            lastResultValue.text = getTestResultText(acknowledgedTestResult)

            if (acknowledgedTestResult.testKitType != null) {
                lastResultKitType.text = getTestResultKitTypeText(acknowledgedTestResult.testKitType)
                latestResultKitTypeContainer.visible()
            } else {
                latestResultKitTypeContainer.gone()
            }

            if (acknowledgedTestResult.requiresConfirmatoryTest) {
                if (acknowledgedTestResult.confirmedDate != null) {
                    followUpState.text = getString(R.string.about_test_follow_up_state_complete)
                    followUpDate.text = uiFormat(acknowledgedTestResult.confirmedDate)
                    followUpTestDateContainer.visible()
                } else {
                    followUpState.text = getString(R.string.about_test_follow_up_state_pending)
                    followUpTestDateContainer.gone()
                }
            } else {
                followUpState.text = getString(R.string.about_test_follow_up_state_not_required)
                followUpTestDateContainer.gone()
            }

            titleLatestResult.visible()
            latestResultDateContainer.visible()
            latestResultValueContainer.visible()
        } else {
            titleLatestResult.gone()
            latestResultDateContainer.gone()
            latestResultValueContainer.gone()
            latestResultKitTypeContainer.gone()
            followUpTestDateContainer.gone()
            followUpTestStatusContainer.gone()
        }
    }

    private fun getTestResultText(acknowledgedTestResult: AcknowledgedTestResult) =
        when (acknowledgedTestResult.testResult) {
            POSITIVE -> getString(R.string.about_positive)
            NEGATIVE -> getString(R.string.about_negative)
        }

    private fun getTestResultKitTypeText(testKitType: VirologyTestKitType) =
        when (testKitType) {
            LAB_RESULT -> getString(R.string.about_pcr)
            RAPID_RESULT -> getString(R.string.about_lfd)
            RAPID_SELF_REPORTED -> getString(R.string.about_lfd_self_reported)
        }

    private fun handleStateMachineState(isolationState: IsolationState?) {
        isolationState?.let { handleIsolationState(it) } ?: handleStateMachineEmptyStates()
    }

    private fun handleIsolationState(isolationState: IsolationState) {
        noRecordsView.gone()
        if (isolationState.lastDayOfIsolation != null) {
            lastDayOfIsolationDate.text = isolationState.lastDayOfIsolation.uiFormat(this)
            titleLastDayOfIsolation.visible()
            lastDayOfIsolationSection.visible()
        } else {
            titleLastDayOfIsolation.gone()
            lastDayOfIsolationSection.gone()
        }

        if (isolationState.contactCaseEncounterDate != null) {
            textEncounterDate.text = uiFormat(isolationState.contactCaseEncounterDate)
            titleExposureNotification.visible()
            encounterDataSection.visible()

            if (isolationState.contactCaseNotificationDate != null) {
                textExposureNotificationDate.text = uiFormat(isolationState.contactCaseNotificationDate)
                exposureNotificationDataSection.visible()
            } else {
                exposureNotificationDataSection.gone()
            }
        } else {
            titleExposureNotification.gone()
            encounterDataSection.gone()
            exposureNotificationDataSection.gone()
        }

        if (isolationState.indexCaseSymptomOnsetDate != null) {
            textViewSymptomsDate.text = isolationState.indexCaseSymptomOnsetDate.uiFormat(this)
            titleSymptoms.visible()
            symptomsDataSection.visible()
        } else {
            titleSymptoms.gone()
            symptomsDataSection.gone()
        }

        if (isolationState.dailyContactTestingOptInDate != null) {
            dailyContactTestingOptInDate.text = isolationState.dailyContactTestingOptInDate.uiFormat(this)
            titleDailyContactTestingOptIn.visible()
            dailyContactTestingSection.visible()
        } else {
            titleDailyContactTestingOptIn.gone()
            dailyContactTestingSection.gone()
        }
    }

    private fun handleStateMachineEmptyStates() {
        titleLastDayOfIsolation.gone()
        lastDayOfIsolationSection.gone()

        titleSymptoms.gone()
        symptomsDataSection.gone()

        titleExposureNotification.gone()
        encounterDataSection.gone()
        exposureNotificationDataSection.gone()

        titleDailyContactTestingOptIn.gone()
        dailyContactTestingSection.gone()
    }

    private fun updateLastRiskyVenueVisitedSegment(date: LocalDate?) {
        if (date != null) {
            titleLastRiskyVenueVisit.visible()
            lastRiskyVenueVisitSection.visible()
            lastRiskyVenueVisitDate.text = date.uiFormat(this)
        } else {
            titleLastRiskyVenueVisit.gone()
            lastRiskyVenueVisitSection.gone()
        }
    }

    private fun uiFormat(instant: Instant): String {
        val localDate: LocalDate =
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
        return localDate.uiFormat(this)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, MyDataActivity::class.java)
    }
}
