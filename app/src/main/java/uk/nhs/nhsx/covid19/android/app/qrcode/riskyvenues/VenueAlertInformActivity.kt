package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityVenueAlertInformBinding
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertInformViewModel.ViewState.UnknownVisit
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class VenueAlertInformActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<VenueAlertInformViewModel>
    private val viewModel: VenueAlertInformViewModel by viewModels { factory }
    private lateinit var binding: ActivityVenueAlertInformBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityVenueAlertInformBinding.inflate(layoutInflater)

        with(binding) {

            setContentView(root)

            setCloseToolbar(
                primaryToolbar.toolbar,
                titleResId = R.string.empty,
                closeIndicator = R.drawable.ic_close_primary
            )

            viewModel.venueVisitState().observe(
                this@VenueAlertInformActivity,
                { viewState ->
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

            buttonReturnToHomeScreen.setOnSingleClickListener {
                viewModel.acknowledgeVenueAlert()
                StatusActivity.start(this@VenueAlertInformActivity)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.acknowledgeVenueAlert()
    }

    companion object {
        const val EXTRA_VENUE_ID = "EXTRA_VENUE_ID"

        fun start(context: Context, venueId: String) =
            context.startActivity(getIntent(context, venueId))

        private fun getIntent(context: Context, venueId: String) =
            Intent(context, VenueAlertInformActivity::class.java)
                .putExtra(EXTRA_VENUE_ID, venueId)
    }
}
