package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_about_user_data.actionDeleteAllData
import kotlinx.android.synthetic.main.activity_about_user_data.encounterDataSection
import kotlinx.android.synthetic.main.activity_about_user_data.lastResultDate
import kotlinx.android.synthetic.main.activity_about_user_data.lastResultValue
import kotlinx.android.synthetic.main.activity_about_user_data.latestResultContainer
import kotlinx.android.synthetic.main.activity_about_user_data.postalDistrict
import kotlinx.android.synthetic.main.activity_about_user_data.symptomsDataSection
import kotlinx.android.synthetic.main.activity_about_user_data.textEncounterDate
import kotlinx.android.synthetic.main.activity_about_user_data.textViewSymptomsDate
import kotlinx.android.synthetic.main.activity_about_user_data.titleEncounter
import kotlinx.android.synthetic.main.activity_about_user_data.titleLatestResult
import kotlinx.android.synthetic.main.activity_about_user_data.titleSymptoms
import kotlinx.android.synthetic.main.activity_about_user_data.toolbar
import kotlinx.android.synthetic.main.activity_about_user_data.venueHistoryList
import kotlinx.android.synthetic.main.activity_about_user_data.venueVisitsTitle
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResult
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.visible
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class UserDataActivity : BaseActivity(R.layout.activity_about_user_data) {

    @Inject
    lateinit var factory: ViewModelFactory<UserDataViewModel>

    private val viewModel: UserDataViewModel by viewModels { factory }

    private lateinit var venueVisitsAdapter: VenueVisitsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar,
            R.string.about_manage_my_data,
            R.drawable.ic_arrow_back_white
        )

        viewModel.getPostCode().observe(
            this,
            Observer { postCode ->
                postalDistrict.text = postCode
            }
        )

        viewModel.getLastStatusMachineState().observe(
            this,
            Observer {
                handleStateMachineState(it)
            }
        )

        viewModel.getVenueVisits().observe(
            this,
            Observer { venueVisits ->
                showVenueVisits(venueVisits)
            }
        )

        viewModel.getLatestTestResult().observe(
            this,
            Observer {
                handleShowingLatestTestResult(it)
            }
        )

        viewModel.getAllUserDataDeleted().observe(
            this,
            Observer {
                handleAllUserDataDeleted()
            }
        )

        viewModel.loadUserData()

        actionDeleteAllData.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun handleShowingLatestTestResult(latestTestResult: LatestTestResult?) {
        if (latestTestResult == null) {
            titleLatestResult.gone()
            latestResultContainer.gone()
        } else {
            val dateTime =
                LocalDateTime.ofInstant(latestTestResult.testEndDate, ZoneId.systemDefault())
            lastResultValue.text = getTestResultText(latestTestResult)
            lastResultDate.text = dateTime.uiFormat()

            titleLatestResult.visible()
            latestResultContainer.visible()
        }
    }

    private fun getTestResultText(latestTestResult: LatestTestResult) =
        if (latestTestResult.testResult == POSITIVE) getString(R.string.about_positive) else
            getString(R.string.about_negative)

    private fun showConfirmationDialog() {
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

            val dateTime =
                LocalDateTime.ofInstant(isolation.contactCase.startDate, ZoneId.systemDefault())
            textEncounterDate.text = dateTime.uiFormat()
        } else {
            titleEncounter.gone()
            encounterDataSection.gone()
        }
        if (isolation.indexCase != null) {
            titleSymptoms.visible()
            symptomsDataSection.visible()

            textViewSymptomsDate.text = isolation.indexCase.symptomsOnsetDate.uiFormat()
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

    private fun showVenueVisits(venueVisits: List<VenueVisit>?) {
        if (venueVisits.isNullOrEmpty()) {
            venueVisitsTitle.gone()

            return
        }

        setUpVenueVisitsAdapter(venueVisits)
    }

    private fun setUpVenueVisitsAdapter(venueVisits: List<VenueVisit>) {
        venueVisitsAdapter = VenueVisitsAdapter(venueVisits)
        venueHistoryList.layoutManager = LinearLayoutManager(this)
        venueHistoryList.adapter = venueVisitsAdapter
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, UserDataActivity::class.java)
    }
}
