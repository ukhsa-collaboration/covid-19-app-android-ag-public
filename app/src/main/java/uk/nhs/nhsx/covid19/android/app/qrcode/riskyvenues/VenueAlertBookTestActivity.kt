package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_venue_alert_book_test.buttonBookTest
import kotlinx.android.synthetic.main.activity_venue_alert_book_test.buttonReturnToHomeScreen
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.ViewState.UnknownVisit
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class VenueAlertBookTestActivity : BaseActivity(R.layout.activity_venue_alert_book_test) {

    @Inject
    lateinit var factory: ViewModelFactory<VenueAlertBookTestViewModel>
    private val viewModel: VenueAlertBookTestViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        viewModel.venueVisitState().observe(
            this,
            Observer { viewState ->
                if (viewState == UnknownVisit) {
                    finish()
                }
            }
        )

        val extraVenueId = intent.getStringExtra(EXTRA_VENUE_ID)
        if (extraVenueId.isNullOrEmpty()) {
            finish()
            return
        } else {
            viewModel.updateVenueVisitState(extraVenueId)
        }

        buttonBookTest.setOnSingleClickListener {
            viewModel.acknowledgeVenueAlert()
            startActivityForResult(
                TestOrderingActivity.getIntent(this),
                REQUEST_CODE_ORDER_A_TEST
            )
        }

        buttonReturnToHomeScreen.setOnSingleClickListener {
            viewModel.acknowledgeVenueAlert()
            finish()
        }
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
