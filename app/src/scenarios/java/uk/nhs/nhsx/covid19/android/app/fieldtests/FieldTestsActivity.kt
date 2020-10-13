package uk.nhs.nhsx.covid19.android.app.fieldtests

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.fieldtests.ui.main.JoinFragment
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar

class FieldTestsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestIgnoreBatteryOptimizations()
        setContentView(R.layout.activity_field_tests)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    JoinFragment()
                )
                .commitNow()
        }

        setNavigateUpToolbar(
            toolbar = findViewById(R.id.fieldTestToolbar),
            titleResId = R.string.field_tests,
            homeIndicator = R.drawable.ic_arrow_back_primary
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id: Int = item.itemId
        return if (id == R.id.action_settings) {
            settingsAction()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun settingsAction() {
        val intent = Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS)
        try {
            startActivity(intent)
        } catch (exception: Exception) {
            //
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        // TODO: ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS will be blocked by Google
        val intent = Intent()
        val packageName = packageName
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName))
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        else {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:" + packageName)
        }
        startActivity(intent)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, FieldTestsActivity::class.java)
    }
}
