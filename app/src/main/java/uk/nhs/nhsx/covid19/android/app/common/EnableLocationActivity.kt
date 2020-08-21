package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseText
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.LOCATION_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import javax.inject.Inject
import javax.inject.Named

class EnableLocationActivity : BaseActivity(R.layout.activity_edge_case) {

    @Inject
    @Named(LOCATION_STATE_NAME)
    lateinit var locationStateProvider: AvailabilityStateProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        locationStateProvider.availabilityState.observe(
            this,
            Observer { isLocationEnabled ->
                if (isLocationEnabled == ENABLED && !isFinishing) {
                    finish()
                }
            }
        )

        edgeCaseTitle.setText(R.string.enable_location_service_title)
        edgeCaseText.setText(R.string.enable_location_service_rationale)

        takeActionButton.setText(R.string.go_to_your_settings)
        takeActionButton.setOnClickListener {
            startActivity(
                Intent(ACTION_LOCATION_SOURCE_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        locationStateProvider.start(this)
    }

    override fun onPause() {
        super.onPause()
        locationStateProvider.stop(this)
    }

    override fun onBackPressed() {
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        private fun getIntent(context: Context) =
            Intent(context, EnableLocationActivity::class.java)
    }
}
