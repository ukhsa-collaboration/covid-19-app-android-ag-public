package uk.nhs.nhsx.covid19.android.app.about

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
import kotlinx.android.synthetic.main.activity_about_user_data.editLocalAuthority
import kotlinx.android.synthetic.main.activity_about_user_data.editVenueVisits
import kotlinx.android.synthetic.main.activity_about_user_data.encounterDataSection
import kotlinx.android.synthetic.main.activity_about_user_data.lastResultDate
import kotlinx.android.synthetic.main.activity_about_user_data.lastResultValue
import kotlinx.android.synthetic.main.activity_about_user_data.latestResultContainer
import kotlinx.android.synthetic.main.activity_about_user_data.localAuthority
import kotlinx.android.synthetic.main.activity_about_user_data.localAuthorityTitle
import kotlinx.android.synthetic.main.activity_about_user_data.symptomsDataSection
import kotlinx.android.synthetic.main.activity_about_user_data.textEncounterDate
import kotlinx.android.synthetic.main.activity_about_user_data.textViewSymptomsDate
import kotlinx.android.synthetic.main.activity_about_user_data.titleEncounter
import kotlinx.android.synthetic.main.activity_about_user_data.titleLatestResult
import kotlinx.android.synthetic.main.activity_about_user_data.titleSymptoms
import kotlinx.android.synthetic.main.activity_about_user_data.venueHistoryList
import kotlinx.android.synthetic.main.activity_about_user_data.venueVisitsTitle
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.VenueVisitsUiState
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class UserDataActivity : BaseActivity(R.layout.activity_about_user_data) {

    @Inject
    lateinit var factory: ViewModelFactory<UserDataViewModel>

    private val viewModel: UserDataViewModel by viewModels { factory }

    private lateinit var venueVisitsViewAdapter: VenueVisitsViewAdapter

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
        viewModel.loadUserData()
    }

    private fun setupOnClickListeners() {
        actionDeleteAllData.setOnClickListener {
            showConfirmDeletingAllDataDialog()
        }

        editVenueVisits.setOnClickListener {
            viewModel.onEditVenueVisitClicked()
        }

        editLocalAuthority.setOnClickListener {
            EditPostalDistrictActivity.start(this)
        }
    }

    private fun setupViewModelListeners() {
        viewModel.localAuthorityText().observe(this) { localAuthorityText ->
            localAuthority.text = localAuthorityText
        }

        viewModel.getLastStatusMachineState().observe(this) { isolationState ->
            handleStateMachineState(isolationState)
        }

        viewModel.getVenueVisitsUiState().observe(this) { venueVisitsUiState ->
            updateVenueVisitsContainer(venueVisitsUiState)
        }

        viewModel.getReceivedTestResult().observe(this) { latestTestResult ->
            handleShowingLatestTestResult(latestTestResult)
        }

        viewModel.getAllUserDataDeleted().observe(this) {
            handleAllUserDataDeleted()
        }
    }

    private fun handleShowingLatestTestResult(receivedTestResult: ReceivedTestResult?) {
        if (receivedTestResult == null) {
            titleLatestResult.gone()
            latestResultContainer.gone()
        } else {
            val latestTestResultDate: LocalDate =
                LocalDateTime.ofInstant(receivedTestResult.testEndDate, ZoneId.systemDefault()).toLocalDate()
            lastResultValue.text = getTestResultText(receivedTestResult)
            lastResultDate.text = latestTestResultDate.uiFormat(this)

            titleLatestResult.visible()
            latestResultContainer.visible()
        }
    }

    private fun getTestResultText(receivedTestResult: ReceivedTestResult) =
        when (receivedTestResult.testResult) {
            POSITIVE -> getString(R.string.about_positive)
            NEGATIVE -> getString(R.string.about_negative)
            VOID -> getString(R.string.about_void)
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

        builder.show()
    }

    private fun handleAllUserDataDeleted() {
        startActivity<MainActivity> {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        finish()
    }

    private fun handleStateMachineState(it: State) {
        when (it) {
            is Default -> handleStateMachineEmptyStates()
            is Isolation -> handleIsolation(it)
        }
    }

    private fun handleIsolation(isolation: Isolation) {
        if (isolation.contactCase != null) {
            titleEncounter.visible()
            encounterDataSection.visible()

            val encounterDate: LocalDate =
                LocalDateTime.ofInstant(isolation.contactCase.startDate, ZoneId.systemDefault()).toLocalDate()
            textEncounterDate.text = encounterDate.uiFormat(this)
        } else {
            titleEncounter.gone()
            encounterDataSection.gone()
        }
        if (isolation.indexCase != null) {
            titleSymptoms.visible()
            symptomsDataSection.visible()

            textViewSymptomsDate.text = isolation.indexCase.symptomsOnsetDate.uiFormat(this)
        } else {
            titleSymptoms.gone()
            symptomsDataSection.gone()
        }
    }

    private fun handleStateMachineEmptyStates() {
        titleSymptoms.gone()
        symptomsDataSection.gone()

        titleEncounter.gone()
        encounterDataSection.gone()
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

            editVenueVisits.text =
                if (venueVisitsUiState.isInEditMode) getString(R.string.done_button_text) else getString(
                    R.string.edit
                )
        }
    }

    private fun setUpVenueVisitsAdapter(venueVisits: List<VenueVisit>, showDeleteIcon: Boolean) {
        venueVisitsViewAdapter = VenueVisitsViewAdapter(venueVisits, showDeleteIcon) {
            showDeleteVenueVisitConfirmationDialog(it)
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

        builder.show()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, UserDataActivity::class.java)
    }
}
