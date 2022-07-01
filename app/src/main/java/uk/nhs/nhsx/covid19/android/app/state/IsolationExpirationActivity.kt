package uk.nhs.nhsx.covid19.android.app.state

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityIsolationExpirationBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import java.time.LocalDate
import javax.inject.Inject

class IsolationExpirationActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<IsolationExpirationViewModel>

    private val viewModel: IsolationExpirationViewModel by viewModels { factory }

    private lateinit var binding: ActivityIsolationExpirationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityIsolationExpirationBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun registerViewModelListeners() = viewModel.viewState().observe(this) {
        when (it.country) {
            ENGLAND -> setUpUIEngland(it.expired, it.expiryDate, it.showTemperatureNotice)
            else -> setUpUIWales(it.expired, it.expiryDate, it.showTemperatureNotice)
        }
    }

    private fun setUpUIEngland(
        expired: Boolean,
        expiryDate: LocalDate,
        showTemperatureNotice: Boolean
    ) {
        setUpUI(
            expired = expired,
            expiryDate = expiryDate,
            showTemperatureNotice = showTemperatureNotice,
            goodNewsHeader = R.string.expiration_notification_title,
            expiredSubHeader = R.string.expiration_notification_description_passed,
            endingSoonSubHeader = R.string.your_isolation_will_finish,
            infoBoxText = R.string.expiration_temperature_hint,
            recommendationText = R.string.expiration_notification_recommendation,
            linkText = R.string.nhs_111_online_service
        )
    }

    private fun setUpUIWales(
        expired: Boolean,
        expiryDate: LocalDate,
        showTemperatureNotice: Boolean
    ) {
        setUpUI(
            expired = expired,
            expiryDate = expiryDate,
            showTemperatureNotice = showTemperatureNotice,
            goodNewsHeader = R.string.expiration_notification_title_wales,
            expiredSubHeader = R.string.expiration_notification_description_passed_wales,
            endingSoonSubHeader = R.string.your_isolation_will_finish_wales,
            infoBoxText = R.string.expiration_temperature_hint_wales,
            recommendationText = R.string.expiration_notification_recommendation_wales,
            linkText = R.string.nhs_111_online_service_wales
        )
    }

    private fun setUpUI(
        expired: Boolean,
        expiryDate: LocalDate,
        showTemperatureNotice: Boolean,
        goodNewsHeader: Int,
        expiredSubHeader: Int,
        endingSoonSubHeader: Int,
        infoBoxText: Int,
        recommendationText: Int,
        linkText: Int
    ) {
        binding.goodNewsTitle.text = getString(goodNewsHeader)
        val lastDayOfIsolation = expiryDate.minusDays(1)
        val pattern =
            if (expired) expiredSubHeader else endingSoonSubHeader
        binding.expirationDescription.text = getString(
            pattern,
            lastDayOfIsolation.uiFormat(this)
        )

        binding.temperatureNoticeView.isVisible = showTemperatureNotice
        binding.temperatureNoticeView.stateText = getString(infoBoxText)

        binding.expirationRecommendation.text = getString(recommendationText)
        binding.onlineServiceLinkTextView.text = getString(linkText)

        binding.primaryReturnToHomeScreenButton.setOnSingleClickListener {
            viewModel.acknowledgeIsolationExpiration()
            StatusActivity.start(this)
        }
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
