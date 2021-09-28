package uk.nhs.nhsx.covid19.android.app.exposure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.scenarios.activity_enable_exposure_notification.noButton
import kotlinx.android.synthetic.scenarios.activity_enable_exposure_notification.yesButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class EnableExposureNotificationActivity : AppCompatActivity(R.layout.activity_enable_exposure_notification) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        yesButton.setOnSingleClickListener {
            setResult(RESULT_OK)
            (appComponent.provideExposureNotificationApi() as MockExposureNotificationApi).activationResult = Success()
            finish()
        }

        noButton.setOnSingleClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}
