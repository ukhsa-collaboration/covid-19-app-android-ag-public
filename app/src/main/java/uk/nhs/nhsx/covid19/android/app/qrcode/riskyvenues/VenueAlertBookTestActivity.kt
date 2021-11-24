package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityVenueAlertBookTestBinding
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget.BookATest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget.SymptomsAfterRiskyVenue
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.NavigationTarget.BookTest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.ViewState.UnknownVisit
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class VenueAlertBookTestActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<VenueAlertBookTestViewModel>
    private val viewModel: VenueAlertBookTestViewModel by viewModels { factory }
    private lateinit var binding: ActivityVenueAlertBookTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityVenueAlertBookTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setCloseToolbar(
            binding.primaryToolbar.toolbar,
            titleResId = R.string.empty,
            closeIndicator = R.drawable.ic_close_primary
        )

        viewModel.venueVisitState().observe(this) { viewState ->
            if (viewState == UnknownVisit) {
                finish()
            }
        }

        viewModel.navigationEvent().observe(this) {
            when (it) {
                is BookTest -> when (it.navigationTarget) {
                    BookATest -> navigateToBookATest()
                    SymptomsAfterRiskyVenue -> navigateToSymptomsConfirmation()
                }
                Finish -> finish()
            }
        }

        val extraVenueId = intent.getStringExtra(EXTRA_VENUE_ID)
        if (extraVenueId.isNullOrEmpty()) {
            finish()
            return
        } else {
            viewModel.updateVenueVisitState(extraVenueId)
        }

        with(binding) {

            buttonBookTest.setOnSingleClickListener {
                viewModel.onBookATestClicked()
            }

            buttonReturnToHomeScreen.setOnSingleClickListener {
                viewModel.onReturnToHomeClicked()
            }
        }
    }

    private fun navigateToBookATest() {
        startActivityForResult(
            TestOrderingActivity.getIntent(this),
            REQUEST_CODE_ORDER_A_TEST
        )
    }

    private fun navigateToSymptomsConfirmation() {
        SymptomsAfterRiskyVenueActivity.start(this, shouldShowCancelConfirmationDialogOnCancelButtonClick = true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.acknowledgeVenueAlert()
    }

    companion object {
        const val REQUEST_CODE_ORDER_A_TEST = 1340
        const val EXTRA_VENUE_ID = "EXTRA_VENUE_ID"

        fun start(context: Context, venueId: String) =
            context.startActivity(getIntent(context, venueId))

        private fun getIntent(context: Context, venueId: String) =
            Intent(context, VenueAlertBookTestActivity::class.java)
                .putExtra(EXTRA_VENUE_ID, venueId)
    }
}
