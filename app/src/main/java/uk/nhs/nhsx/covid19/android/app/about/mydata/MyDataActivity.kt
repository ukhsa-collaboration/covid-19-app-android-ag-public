package uk.nhs.nhsx.covid19.android.app.about.mydata

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel.IsolationViewState
import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel.MyDataState
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityMyDataBinding
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
import java.time.LocalDate
import javax.inject.Inject

class MyDataActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<BaseMyDataViewModel>

    private val viewModel: BaseMyDataViewModel by viewModels { factory }

    private lateinit var binding: ActivityMyDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityMyDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbar(
            binding.primaryToolbar.toolbar,
            R.string.settings_my_data,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        setupViewModelListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        checkSectionItemOrientation()
    }

    private fun setupViewModelListeners() {
        viewModel.myDataState().observe(this) {
            renderViewState(it)
        }
    }

    private fun renderViewState(viewState: MyDataState) = with(binding) {
        if (viewState.isolationState == null &&
            viewState.lastRiskyVenueVisitDate == null &&
            viewState.acknowledgedTestResult == null
        ) {
            noRecordsView.visible()
            viewContent.gone()
        } else {
            noRecordsView.gone()
            viewContent.visible()
            handleAcknowledgedTestResult(viewState.acknowledgedTestResult)
            handleIsolationState(viewState.isolationState)
            handleLastRiskyVenueVisitDate(viewState.lastRiskyVenueVisitDate)
        }
    }

    private fun handleAcknowledgedTestResult(acknowledgedTestResult: AcknowledgedTestResult?) = with(binding) {
        lastTestResultSection.clear()
        if (acknowledgedTestResult != null) {
            lastTestResultSection.addItems(
                testEndDateSectionItem(acknowledgedTestResult.testEndDate),
                acknowledgeDateSectionItem(acknowledgedTestResult.acknowledgedDate),
                aboutTestResultSectionItem(getTestResultText(acknowledgedTestResult))
            )

            if (acknowledgedTestResult.testKitType != null) {
                lastTestResultSection.addItems(testKitTypeSectionItem(acknowledgedTestResult.testKitType))
            }

            if (acknowledgedTestResult.requiresConfirmatoryTest) {
                if (acknowledgedTestResult.confirmedDate != null) {
                    lastTestResultSection.addItems(
                        followUpDateSectionItem(acknowledgedTestResult.confirmedDate),
                        followUpStatusSectionItem(getString(R.string.about_test_follow_up_state_complete))
                    )
                } else {
                    lastTestResultSection.addItems(
                        followUpStatusSectionItem(getString(R.string.about_test_follow_up_state_pending))
                    )
                }
            } else {
                lastTestResultSection.addItems(
                    followUpStatusSectionItem(getString(R.string.about_test_follow_up_state_not_required))
                )
            }
        }
    }

    private fun handleIsolationState(isolationViewState: IsolationViewState?) = with(binding) {
        selfIsolationSection.clear()
        symptomsInformationSection.clear()
        exposureNotificationSection.clear()

        if (isolationViewState?.lastDayOfIsolation != null) {
            selfIsolationSection.addItems(
                lastDayOfIsolationDateSectionItem(isolationViewState.lastDayOfIsolation)
            )
        }
        if (isolationViewState?.indexCaseSymptomOnsetDate != null) {
            symptomsInformationSection.addItems(
                symptomsOnsetDateSectionItem(isolationViewState.indexCaseSymptomOnsetDate)
            )
        }
        if (isolationViewState?.contactCaseEncounterDate != null) {
            exposureNotificationSection.addItems(
                encounterDateSectionItem(isolationViewState.contactCaseEncounterDate)
            )
            if (isolationViewState.contactCaseNotificationDate != null) {
                exposureNotificationSection.addItems(
                    exposureNotificationDateSectionItem(isolationViewState.contactCaseNotificationDate)
                )
            }
            if (isolationViewState.optOutOfContactIsolationDate != null) {
                exposureNotificationSection.addItems(
                    optOutOfContactIsolationDateSectionItem(isolationViewState.optOutOfContactIsolationDate)
                )
            }
        }
    }

    private fun handleLastRiskyVenueVisitDate(date: LocalDate?) = with(binding) {
        riskyVenueSection.clear()
        if (date != null) {
            riskyVenueSection.addItems(riskyVenueSectionItem(date))
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

    private fun checkSectionItemOrientation() = with(binding) {
        isFontScalingIncreased().also {
            lastTestResultSection.setSectionItemStackVertically(it)
            selfIsolationSection.setSectionItemStackVertically(it)
            symptomsInformationSection.setSectionItemStackVertically(it)
            exposureNotificationSection.setSectionItemStackVertically(it)
            riskyVenueSection.setSectionItemStackVertically(it)
        }
    }

    private fun isFontScalingIncreased(): Boolean = resources.configuration.fontScale > 1.0f

    private fun testEndDateSectionItem(date: LocalDate) =
        dateSectionItem(getString(R.string.user_data_test_end), date)

    private fun acknowledgeDateSectionItem(date: LocalDate) =
        dateSectionItem(getString(R.string.user_data_acknowledged_date), date)

    private fun aboutTestResultSectionItem(testResult: String) =
        MyDataSectionItem(getString(R.string.about_test_result), value = testResult)

    private fun testKitTypeSectionItem(testKitType: VirologyTestKitType): MyDataSectionItem =
        MyDataSectionItem(
            title = getString(R.string.about_test_kit_type),
            value = getTestResultKitTypeText(testKitType)
        )

    private fun followUpDateSectionItem(date: LocalDate) =
        dateSectionItem(getString(R.string.about_test_follow_up_date), date)

    private fun followUpStatusSectionItem(status: String) =
        MyDataSectionItem(getString(R.string.about_test_follow_up_status), value = status)

    private fun lastDayOfIsolationDateSectionItem(date: LocalDate) =
        dateSectionItem(getString(R.string.about_my_data_last_day_of_isolation), date)

    private fun symptomsOnsetDateSectionItem(date: LocalDate) =
        dateSectionItem(getString(R.string.about_my_data_symptom_onset_date), date)

    private fun exposureNotificationDateSectionItem(date: LocalDate) =
        dateSectionItem(getString(R.string.about_notification_date), date)

    private fun encounterDateSectionItem(date: LocalDate) =
        dateSectionItem(getString(R.string.about_encounter_date), date)

    private fun optOutOfContactIsolationDateSectionItem(date: LocalDate) =
        dateSectionItem(getString(R.string.about_contact_isolation_opt_out_date), date)

    private fun riskyVenueSectionItem(date: LocalDate) =
        dateSectionItem(getString(R.string.about_my_data_last_visited), date)

    private fun dateSectionItem(title: String, date: LocalDate) =
        MyDataSectionItem(title, value = date.uiFormat(this))

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, MyDataActivity::class.java)
    }
}
