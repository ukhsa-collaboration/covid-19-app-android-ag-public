package uk.nhs.nhsx.covid19.android.app.exposure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityAllowShareKeysBinding
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class AllowShareKeysActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllowShareKeysBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllowShareKeysBinding.inflate(layoutInflater)
        with(binding) {

        setContentView(root)

        yesButton.setOnSingleClickListener {
            setResult(RESULT_OK)
            (appComponent.provideExposureNotificationApi() as MockExposureNotificationApi).temporaryExposureKeyHistoryResult = Success()
            finish()
        }

        noButton.setOnSingleClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        }
    }
}
