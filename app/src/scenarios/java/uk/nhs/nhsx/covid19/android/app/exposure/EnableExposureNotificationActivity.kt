package uk.nhs.nhsx.covid19.android.app.exposure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityEnableExposureNotificationBinding
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class EnableExposureNotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnableExposureNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnableExposureNotificationBinding.inflate(layoutInflater)
        with(binding) {
            setContentView(root)

            yesButton.setOnSingleClickListener {
                setResult(RESULT_OK)
                (appComponent.provideExposureNotificationApi() as MockExposureNotificationApi).activationResult =
                    Success()
                finish()
            }

            noButton.setOnSingleClickListener {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }
}
