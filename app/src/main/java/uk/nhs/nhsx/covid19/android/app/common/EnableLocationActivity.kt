package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityEdgeCaseBinding
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.LOCATION_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject
import javax.inject.Named

class EnableLocationActivity : BaseActivity() {

    @Inject
    @Named(LOCATION_STATE_NAME)
    lateinit var locationStateProvider: AvailabilityStateProvider

    private lateinit var binding: ActivityEdgeCaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityEdgeCaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationStateProvider.availabilityState.observe(
            this
        ) { isLocationEnabled ->
            if (isLocationEnabled == ENABLED && !isFinishing) {
                finish()
            }
        }

        with(binding) {

            edgeCaseTitle.setText(R.string.enable_location_service_title)
            edgeCaseText.setText(R.string.enable_location_service_rationale)

            takeActionButton.setText(R.string.go_to_your_settings)
            takeActionButton.setOnSingleClickListener {
                startActivity(
                    Intent(ACTION_LOCATION_SOURCE_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    }
                )
            }
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
