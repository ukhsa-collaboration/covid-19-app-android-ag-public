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
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityIsolationExpirationBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openInExternalBrowserForResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpLinkTypeWithBrowserWarning
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
        displayExpirationDescription(it.expired, it.expiryDate, it.showTemperatureNotice, it.isActiveOrPreviousIndexCase)
    }

    private fun displayExpirationDescription(
        expired: Boolean,
        expiryDate: LocalDate,
        showTemperatureNotice: Boolean,
        isActiveOrPreviousIndexCase: Boolean
    ) {
        if (isActiveOrPreviousIndexCase)
            setUpUIIndexCaseWales(expired)
        else
            setUpDefaultUI(expired, expiryDate, showTemperatureNotice)
    }

    private fun setUpUIIndexCaseWales(
        expired: Boolean
    ) {
        val doneIconImageResource =
            if (expired) R.drawable.ic_elbow_bump
            else R.drawable.ic_isolation_continue
        binding.doneIcon.setImageResource(doneIconImageResource)
        binding.expirationDescription.text =
            if (expired) getString(R.string.expiration_notification_description_passed_wales)
            else getString(R.string.your_isolation_are_ending_soon_wales)

        binding.goodNewsTitle.isVisible = false
        binding.expirationDescription.setPadding(
            binding.expirationDescription.paddingStart,
            32.dpToPx.toInt(),
            binding.expirationDescription.paddingEnd,
            binding.expirationDescription.paddingBottom
        )

        if (expired) {
            binding.continueIsolationIfPositiveView.isVisible = true
            binding.temperatureNoticeView.isVisible = false
        } else {
            binding.temperatureNoticeView.isVisible = true
            binding.continueIsolationIfPositiveView.isVisible = false
            binding.temperatureNoticeView.stateText = getString(R.string.expiration_notification_callout_advice_wales)
        }
        binding.WalesTestAdvice.text =
            if (expired) getString(R.string.expiration_notification_testing_advice_wales_after_isolation_ended_wales)
            else getString(R.string.expiration_notification_testing_advice_wales_before_isolation_ended_wales)

        binding.primaryReturnToHomeScreenButton.isVisible = false

        binding.covidGuidanceLinkButton.isVisible = true
        binding.covidGuidanceLinkButton.setOnSingleClickListener {
            openInExternalBrowserForResult(
                getString(R.string.url_latest_government_guidance_wls), READ_COVID_GUIDANCE)
        }
        binding.covidGuidanceLinkButton.setUpLinkTypeWithBrowserWarning(binding.covidGuidanceLinkButton.text)

        binding.secondaryReturnToHomeScreenButton.isVisible = true
        binding.secondaryReturnToHomeScreenButton.setOnSingleClickListener {
            viewModel.acknowledgeIsolationExpiration()
            StatusActivity.start(this)
        }
    }

    private fun setUpDefaultUI(
        expired: Boolean,
        expiryDate: LocalDate,
        showTemperatureNotice: Boolean
    ) {
        val lastDayOfIsolation = expiryDate.minusDays(1)
        val pattern =
            if (expired) R.string.expiration_notification_description_passed else R.string.your_isolation_will_finish
        binding.expirationDescription.text = getString(
            pattern,
            lastDayOfIsolation.uiFormat(this)
        )
        binding.primaryReturnToHomeScreenButton.setOnSingleClickListener {
            viewModel.acknowledgeIsolationExpiration()
            StatusActivity.start(this)
        }

        binding.temperatureNoticeView.isVisible = showTemperatureNotice
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_COVID_GUIDANCE) {
            viewModel.acknowledgeIsolationExpiration()
            navigateToStatusActivity()
        }
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
        finish()
    }

    companion object {
        const val EXTRA_EXPIRY_DATE = "EXTRA_EXPIRY_DATE"
        const val READ_COVID_GUIDANCE = 1351

        fun start(context: Context, expiryDate: String) =
            context.startActivity(getIntent(context, expiryDate))

        private fun getIntent(context: Context, expiryDate: String) =
            Intent(context, IsolationExpirationActivity::class.java)
                .putExtra(EXTRA_EXPIRY_DATE, expiryDate)
    }
}
