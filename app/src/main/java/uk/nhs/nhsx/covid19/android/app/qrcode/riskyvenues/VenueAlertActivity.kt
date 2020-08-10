package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_venue_alert.buttonReturnToHomeScreen
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertViewModel.ViewState.UnknownVisit
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import javax.inject.Inject

class VenueAlertActivity : AppCompatActivity(R.layout.activity_venue_alert) {
    @Inject
    lateinit var venueAlertViewModelFactory: ViewModelFactory<VenueAlertViewModel>

    private val venueAlertViewModel: VenueAlertViewModel by viewModels {
        venueAlertViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        venueAlertViewModel.venueVisitState().observe(
            this,
            Observer { viewState ->
                when (viewState) {
                    UnknownVisit -> finish()
                }
            }
        )

        buttonReturnToHomeScreen.setOnClickListener {
            StatusActivity.start(this)
        }

        val venueId = intent.getStringExtra(EXTRA_VENUE_ID)
        if (venueId.isNullOrEmpty()) {
            finish()
        } else {
            venueAlertViewModel.updateVenueVisitState(venueId)
        }
    }

    companion object {
        const val EXTRA_VENUE_ID = "EXTRA_VENUE_ID"
    }
}
