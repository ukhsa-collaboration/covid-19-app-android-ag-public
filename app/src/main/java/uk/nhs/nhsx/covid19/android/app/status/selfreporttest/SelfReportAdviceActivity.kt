package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySelfReportAdviceBinding
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasNotReportedIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasNotReportedNoNeedToIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasReportedIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasReportedNoNeedToIsolate
import uk.nhs.nhsx.covid19.android.app.util.uiFullFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpLinkTypeWithBrowserWarning

class SelfReportAdviceActivity : BaseActivity() {

    @Inject
    lateinit var factory: SelfReportAdviceViewModel.Factory

    private val viewModel: SelfReportAdviceViewModel by assistedViewModel {
        factory.create(
            reportedTest = intent.getBooleanExtra(REPORTED_TEST_DATA_KEY, true)
        )
    }

    private lateinit var binding: ActivitySelfReportAdviceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySelfReportAdviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            when (viewState.resultAdvice) {
                is HasNotReportedIsolate -> setUpHasNotReportedIsolate(viewState.resultAdvice.currentDate, viewState.resultAdvice.isolationEndDate)
                is HasReportedIsolate -> setUpHasReportedIsolate(viewState.resultAdvice.currentDate, viewState.resultAdvice.isolationEndDate)
                is HasNotReportedNoNeedToIsolate -> setUpHasNotReportedNoNeedToIsolate()
                is HasReportedNoNeedToIsolate -> setUpHasReportedNoNeedToIsolate()
            }
            when (viewState.country) {
                ENGLAND -> setUpEnglandLink()
                else -> setUpWalesLink()
            }
        }
    }

    private fun setClickListeners() {
        binding.primaryActionButton.setOnSingleClickListener {
            StatusActivity.start(this)
            finish()
        }
        binding.secondaryActionButton.setOnSingleClickListener {
            StatusActivity.start(this)
            finish()
        }
        binding.primaryLinkActionButton.setOnSingleClickListener {
            openUrl(
                getString(R.string.self_report_advice_primary_link_button_url)
            )
        }
    }

    private fun setUpHasNotReportedIsolate(currentDate: LocalDate, isolationEndDate: LocalDate) {
        val lastDayOfIsolation = isolationEndDate.minusDays(1)
        val daysLeft = ChronoUnit.DAYS.between(currentDate, isolationEndDate).toInt()
        binding.nowReportContainer.selfReportAdviceNotReportedSubTitle.text = resources.getQuantityString(R.plurals.self_report_advice_isolate_subheader,
            daysLeft,
            daysLeft,
            lastDayOfIsolation.uiFullFormat(this)
        )
        setUpWhenIsolate()
        setUpWhenNotReported()
    }

    private fun setUpHasReportedIsolate(currentDate: LocalDate, isolationEndDate: LocalDate) = with(binding) {
        selfReportAdviceImage.setImageResource(R.drawable.ic_isolation_continue)
        val daysLeft = ChronoUnit.DAYS.between(currentDate, isolationEndDate).toInt()
        selfReportAdviceMainTitle.text = resources.getQuantityString(
            R.plurals.self_report_advice_isolate_header,
            daysLeft,
            daysLeft
        )
        setUpWhenIsolate()
        setUpWhenReported()
    }

    private fun setUpHasNotReportedNoNeedToIsolate() = with(binding) {
        nowReportContainer.selfReportAdviceNotReportedSubTitle.text = getString(R.string.self_report_advice_reported_result_out_of_isolation_header)
        setUpWhenNoNeedToIsolate()
        setUpWhenNotReported()
    }

    private fun setUpHasReportedNoNeedToIsolate() = with(binding) {
        selfReportAdviceImage.setImageResource(R.drawable.ic_onboarding_welcome)
        selfReportAdviceMainTitle.text = getString(R.string.self_report_advice_reported_result_out_of_isolation_header)
        setUpWhenNoNeedToIsolate()
        setUpWhenReported()
    }

    private fun setUpWhenReported() = with(binding) {
        selfReportAdviceMainTitle.visible()
        primaryActionButton.visible()
        nowReportContainer.root.gone()
        primaryLinkActionButton.gone()
        secondaryActionButton.gone()
    }

    private fun setUpWhenNotReported() = with(binding) {
        selfReportAdviceImage.setImageResource(R.drawable.ic_isolation_book_test)
        primaryLinkActionButton.setUpLinkTypeWithBrowserWarning(primaryLinkActionButton.text)
        nowReportContainer.root.visible()
        primaryLinkActionButton.visible()
        secondaryActionButton.visible()
        selfReportAdviceMainTitle.gone()
        primaryActionButton.gone()
    }

    private fun setUpWhenIsolate() = with(binding) {
        selfReportIsolateIconBulletSection.root.visible()
        selfReportAdviceNoIsolationInfoBox.gone()
    }

    private fun setUpWhenNoNeedToIsolate() = with(binding) {
        selfReportIsolateIconBulletSection.root.gone()
        selfReportAdviceNoIsolationInfoBox.visible()
    }

    private fun setUpEnglandLink() = with(binding) {
        covidLinkTextView.text = getString(R.string.self_report_advice_read_more_url_label)
        covidLinkTextView.setLinkUrl(R.string.self_report_advice_read_more_url_link)
    }

    private fun setUpWalesLink() = with(binding) {
        covidLinkTextView.text = getString(R.string.self_report_advice_read_more_url_label_wls)
        covidLinkTextView.setLinkUrl(R.string.self_report_advice_read_more_url_link_wls)
    }

    override fun onBackPressed() {
    }

    companion object {
        const val REPORTED_TEST_DATA_KEY = "REPORTED_TEST_DATA_KEY"
    }
}
