package uk.nhs.nhsx.covid19.android.app.exposure

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MockExposureNotificationActivationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = intent.getIntExtra(EXPOSURE_NOTIFICATION_ACTIVATION_RESULT_EXTRA, Activity.RESULT_OK)
        setResult(result)

        finish()
    }

    companion object {
        const val EXPOSURE_NOTIFICATION_ACTIVATION_RESULT_EXTRA = "EXPOSURE_NOTIFICATION_ACTIVATION_RESULT_EXTRA"
    }
}
