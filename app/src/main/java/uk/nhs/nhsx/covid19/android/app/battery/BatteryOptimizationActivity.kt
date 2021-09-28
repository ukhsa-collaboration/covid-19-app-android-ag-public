package uk.nhs.nhsx.covid19.android.app.battery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_battery_optimization.batteryOptimizationAllowButton
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.startActivityForResultSafely
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class BatteryOptimizationActivity : BaseActivity(R.layout.activity_battery_optimization) {

    @Inject
    lateinit var factory: ViewModelFactory<BatteryOptimizationViewModel>

    private val viewModel: BatteryOptimizationViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setCloseToolbar(
            toolbar,
            R.string.empty,
            closeIndicator = R.drawable.ic_close_primary
        ) {
            viewModel.onIgnoreBatteryOptimizationAcknowledged()
        }

        batteryOptimizationAllowButton.setOnSingleClickListener {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${this.packageName}")
            )
            startActivityForResultSafely(intent, IGNORE_BATTERY_OPTIMIZATION_REQUEST_CODE)
        }

        viewModel.onAcknowledge().observe(this) {
            StatusActivity.start(this)
            finish()
        }

        viewModel.onCreate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IGNORE_BATTERY_OPTIMIZATION_REQUEST_CODE && resultCode != Activity.RESULT_CANCELED) {
            viewModel.onIgnoreBatteryOptimizationAcknowledged()
        }
    }

    override fun onBackPressed() = Unit

    companion object {
        private const val IGNORE_BATTERY_OPTIMIZATION_REQUEST_CODE = 1337
    }
}
