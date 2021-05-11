package uk.nhs.nhsx.covid19.android.app.state

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_isolation_expiration.buttonReturnToHomeScreen
import kotlinx.android.synthetic.main.activity_isolation_expiration.expirationDescription
import kotlinx.android.synthetic.main.activity_isolation_expiration.temperatureNoticeView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import java.time.LocalDate
import javax.inject.Inject

class IsolationExpirationActivity : BaseActivity(R.layout.activity_isolation_expiration) {

    @Inject
    lateinit var factory: ViewModelFactory<IsolationExpirationViewModel>

    private val viewModel: IsolationExpirationViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        buttonReturnToHomeScreen.setOnSingleClickListener {
            viewModel.acknowledgeIsolationExpiration()
            StatusActivity.start(this)
        }

        registerViewModelListeners()

        val isolationExpiryDateString = intent.getStringExtra(EXTRA_EXPIRY_DATE)
        if (isolationExpiryDateString.isNullOrEmpty()) {
            viewModel.acknowledgeIsolationExpiration()
            finish()
        } else {
            viewModel.checkState(isolationExpiryDateString)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.acknowledgeIsolationExpiration()
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
        expirationDescription.text = getString(
            pattern,
            lastDayOfIsolation.uiFormat(this)
        )

        temperatureNoticeView.isVisible = showTemperatureNotice
    }

    companion object {
        const val EXTRA_EXPIRY_DATE = "EXTRA_EXPIRY_DATE"

        fun start(context: Context, expiryDate: String) =
            context.startActivity(getIntent(context, expiryDate))

        private fun getIntent(context: Context, expiryDate: String) =
            Intent(context, IsolationExpirationActivity::class.java)
                .putExtra(EXTRA_EXPIRY_DATE, expiryDate)
    }
}
