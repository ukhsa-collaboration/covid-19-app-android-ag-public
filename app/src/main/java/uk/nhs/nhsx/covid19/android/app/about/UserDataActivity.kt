package uk.nhs.nhsx.covid19.android.app.about

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeroenmols.featureflag.framework.FeatureFlag.LOCAL_AUTHORITY
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.android.synthetic.main.activity_about_user_data.actionDeleteAllData
import kotlinx.android.synthetic.main.activity_about_user_data.dailyContactTestingOptInDate
import kotlinx.android.synthetic.main.activity_about_user_data.dailyContactTestingSection
import kotlinx.android.synthetic.main.activity_about_user_data.editLocalAuthority
import kotlinx.android.synthetic.main.activity_about_user_data.editVenueVisits
import kotlinx.android.synthetic.main.activity_about_user_data.encounterDataSection
import kotlinx.android.synthetic.main.activity_about_user_data.exposureNotificationDataSection
import kotlinx.android.synthetic.main.activity_about_user_data.followUpDate
import kotlinx.android.synthetic.main.activity_about_user_data.followUpState
import kotlinx.android.synthetic.main.activity_about_user_data.followUpTestContainer
import kotlinx.android.synthetic.main.activity_about_user_data.lastDayOfIsolationDate
import kotlinx.android.synthetic.main.activity_about_user_data.lastDayOfIsolationSection
import kotlinx.android.synthetic.main.activity_about_user_data.lastResultDate
import kotlinx.android.synthetic.main.activity_about_user_data.lastResultKitType
import kotlinx.android.synthetic.main.activity_about_user_data.lastResultValue
import kotlinx.android.synthetic.main.activity_about_user_data.latestResultDateContainer
import kotlinx.android.synthetic.main.activity_about_user_data.latestResultKitTypeContainer
import kotlinx.android.synthetic.main.activity_about_user_data.latestResultValueContainer
import kotlinx.android.synthetic.main.activity_about_user_data.localAuthority
import kotlinx.android.synthetic.main.activity_about_user_data.localAuthorityTitle
import kotlinx.android.synthetic.main.activity_about_user_data.symptomsDataSection
import kotlinx.android.synthetic.main.activity_about_user_data.textEncounterDate
import kotlinx.android.synthetic.main.activity_about_user_data.textExposureNotificationDate
import kotlinx.android.synthetic.main.activity_about_user_data.textViewSymptomsDate
import kotlinx.android.synthetic.main.activity_about_user_data.titleDailyContactTestingOptIn
import kotlinx.android.synthetic.main.activity_about_user_data.titleExposureNotification
import kotlinx.android.synthetic.main.activity_about_user_data.titleLastDayOfIsolation
import kotlinx.android.synthetic.main.activity_about_user_data.titleLatestResult
import kotlinx.android.synthetic.main.activity_about_user_data.titleSymptoms
import kotlinx.android.synthetic.main.activity_about_user_data.venueHistoryList
import kotlinx.android.synthetic.main.activity_about_user_data.venueVisitsTitle
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.DialogType
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.DialogType.ConfirmDeleteAllData
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.DialogType.ConfirmDeleteVenueVisit
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.IsolationState
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.UserDataState
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.VenueVisitsUiState
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class UserDataActivity : BaseActivity(R.layout.activity_about_user_data) {

    @Inject
    lateinit var factory: ViewModelFactory<UserDataViewModel>

    private val viewModel: UserDataViewModel by viewModels { factory }

    private lateinit var venueVisitsViewAdapter: VenueVisitsViewAdapter

    /**
     * Dialog currently displayed, or null if none are displayed
     */
    private var currentDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.about_manage_my_data, upIndicator = R.drawable.ic_arrow_back_white)

        venueHistoryList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        setupViewModelListeners()

        setupOnClickListeners()

        setLocalAuthorityTitle()
    }

    private fun setLocalAuthorityTitle() {
        val localAuthorityTitleResId =
            if (RuntimeBehavior.isFeatureEnabled(LOCAL_AUTHORITY)) R.string.about_local_authority else R.string.postal_district
        localAuthorityTitle.text = getString(localAuthorityTitleResId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupOnClickListeners() {
        actionDeleteAllData.setOnSingleClickListener {
            viewModel.onDeleteAllUserDataClicked()
        }

        editVenueVisits.setOnSingleClickListener {
            viewModel.onEditVenueVisitClicked()
        }

        editLocalAuthority.setOnSingleClickListener {
            EditPostalDistrictActivity.start(this)
        }
    }

    private fun setupViewModelListeners() {
        viewModel.userDataState().observe(this) {
            renderViewState(it)
        }

        viewModel.venueVisitsEditModeChanged().observe(this) { isInEditMode ->
            onVenueVisitsEditModeChanged(isInEditMode)
        }

        viewModel.getAllUserDataDeleted().observe(this) {
            handleAllUserDataDeleted()
        }
    }

    private fun renderViewState(viewState: UserDataState) {
        localAuthority.text = viewState.localAuthority
        handleStateMachineState(viewState.isolationState)
        updateVenueVisitsContainer(viewState.venueVisitsUiState)
        handleShowingLatestTestResult(viewState.acknowledgedTestResult)
        viewState.showDialog?.let { dialogType ->
            handleShowDialog(dialogType)
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
                    followUpDate.visible()
                } else {
                    followUpState.text = getString(R.string.about_test_follow_up_state_pending)
                    followUpDate.gone()
                }
            } else {
                followUpState.text = getString(R.string.about_test_follow_up_state_not_required)
            }

            titleLatestResult.visible()
            latestResultDateContainer.visible()
            latestResultValueContainer.visible()
        } else {
            titleLatestResult.gone()
            latestResultDateContainer.gone()
            latestResultValueContainer.gone()
            latestResultKitTypeContainer.gone()
            followUpTestContainer.gone()
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

    private fun showConfirmDeletingAllDataDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.about_delete_your_data_title))
        builder.setMessage(R.string.delete_data_explanation)
        builder.setPositiveButton(
            R.string.about_delete_positive_text
        ) { _, _ ->
            viewModel.deleteAllUserData()
        }

        builder.setNegativeButton(
            R.string.cancel
        ) { dialog, _ ->
            dialog.dismiss()
        }

        builder.setOnDismissListener {
            currentDialog = null
            viewModel.onDialogDismissed()
        }

        currentDialog = builder.show()
    }

    private fun handleShowDialog(dialogType: DialogType) {
        when (dialogType) {
            ConfirmDeleteAllData -> showConfirmDeletingAllDataDialog()
            is ConfirmDeleteVenueVisit -> showDeleteVenueVisitConfirmationDialog(dialogType.venueVisitPosition)
        }
    }

    private fun handleAllUserDataDeleted() {
        startActivity<MainActivity> {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        finish()
    }

    private fun handleStateMachineState(isolationState: IsolationState?) {
        isolationState?.let { handleIsolationState(it) } ?: handleStateMachineEmptyStates()
    }

    private fun handleIsolationState(isolationState: IsolationState) {
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

    private fun updateVenueVisitsContainer(venueVisitsUiState: VenueVisitsUiState) {
        if (venueVisitsUiState.venueVisits.isNullOrEmpty()) {
            venueVisitsTitle.gone()
            editVenueVisits.gone()
            venueHistoryList.gone()
        } else {
            venueVisitsTitle.visible()
            editVenueVisits.visible()
            venueHistoryList.visible()

            setUpVenueVisitsAdapter(venueVisitsUiState.venueVisits, venueVisitsUiState.isInEditMode)

            editVenueVisits.text = getEditVenueVisitsText(venueVisitsUiState.isInEditMode)
            editVenueVisits.contentDescription = getEditVenueVisitsContentDescription(venueVisitsUiState.isInEditMode)
        }
    }

    private fun onVenueVisitsEditModeChanged(isInEditMode: Boolean) {
        val announcement = getEditVenueVisitsContentDescription(isInEditMode)
        editVenueVisits.announceForAccessibility(announcement)
    }

    private fun getEditVenueVisitsText(isInEditMode: Boolean): String =
        if (isInEditMode) getString(R.string.done_button_text)
        else getString(R.string.edit)

    private fun getEditVenueVisitsContentDescription(isInEditMode: Boolean): String =
        if (isInEditMode) getString(R.string.venue_history_editing_done)
        else getString(R.string.venue_history_edit)

    private fun setUpVenueVisitsAdapter(venueVisits: List<VenueVisit>, showDeleteIcon: Boolean) {
        venueVisitsViewAdapter = VenueVisitsViewAdapter(venueVisits, showDeleteIcon) { position ->
            viewModel.onVenueVisitDataClicked(position)
        }
        venueHistoryList.layoutManager = LinearLayoutManager(this)
        venueHistoryList.adapter = venueVisitsViewAdapter
    }

    private fun showDeleteVenueVisitConfirmationDialog(deletedVenueVisitPosition: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.delete_single_venue_visit_title))
        builder.setMessage(R.string.delete_single_venue_visit_text)
        builder.setPositiveButton(
            R.string.confirm
        ) { dialog, _ ->
            viewModel.deleteVenueVisit(deletedVenueVisitPosition)
            dialog.dismiss()
        }

        builder.setNegativeButton(
            R.string.cancel
        ) { dialog, _ ->
            dialog.dismiss()
        }

        builder.setOnDismissListener {
            currentDialog = null
            viewModel.onDialogDismissed()
        }

        currentDialog = builder.show()
    }

    override fun onDestroy() {
        currentDialog?.setOnDismissListener { }
        // To avoid leaking the window
        currentDialog?.dismiss()
        currentDialog = null

        super.onDestroy()
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
            Intent(context, UserDataActivity::class.java)
    }
}
