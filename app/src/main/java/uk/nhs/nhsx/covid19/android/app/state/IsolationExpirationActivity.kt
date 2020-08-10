package uk.nhs.nhsx.covid19.android.app.state

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_isolation_expiration.buttonReturnToHomeScreen
import kotlinx.android.synthetic.main.activity_isolation_expiration.expirationDescription
import kotlinx.android.synthetic.main.activity_isolation_expiration.temperatureNoticeView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import java.time.LocalDate
import javax.inject.Inject

class IsolationExpirationActivity : AppCompatActivity(R.layout.activity_isolation_expiration) {

    @Inject
    lateinit var factory: ViewModelFactory<IsolationExpirationViewModel>

    private val viewModel: IsolationExpirationViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        buttonReturnToHomeScreen.setOnClickListener {
            StatusActivity.start(this)
        }

        registerViewModelListeners()

        val isolationExpiryDateString = intent.getStringExtra(EXTRA_EXPIRY_DATE)
        if (isolationExpiryDateString.isNullOrEmpty()) {
            finish()
        } else {
            viewModel.checkState(isolationExpiryDateString)
        }
    }

    private fun registerViewModelListeners() {
        viewModel.viewState().observe(
            this,
            Observer {
                displayExpirationDescription(it.expired, it.expiryDate, it.showTemperatureNotice)
            }
        )
    }

    private fun displayExpirationDescription(
        expired: Boolean,
        expiryDate: LocalDate,
        showTemperatureNotice: Boolean
    ) {
        val lastDayOfIsolation = expiryDate.minusDays(1)
        val pattern =
            if (expired) R.string.expiration_notification_description_passed else R.string.your_isolation_will_finish
        expirationDescription.text = resources.getString(
            pattern,
            lastDayOfIsolation.uiFormat()
        )

        temperatureNoticeView.isVisible = showTemperatureNotice
    }

    companion object {
        const val EXTRA_EXPIRY_DATE = "EXTRA_EXPIRY_DATE"
    }
}
