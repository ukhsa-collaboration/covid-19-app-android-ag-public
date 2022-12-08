package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySelfReportThankYouBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.NavigationTarget.Advice
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class SelfReportThankYouActivity : BaseActivity() {
    @Inject
    lateinit var factory: SelfReportThankYouViewModel.Factory

    private val viewModel: SelfReportThankYouViewModel by assistedViewModel {
        factory.create(
            sharingSuccessful = intent.getBooleanExtra(SHARING_SUCCESSFUL, false),
            hasReported = intent.getBooleanExtra(HAS_REPORTED, true)
        )
    }

    private lateinit var binding: ActivitySelfReportThankYouBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySelfReportThankYouBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setClickListeners()
        setupObservers()
    }

    private fun setClickListeners() {
        binding.buttonContinue.setOnSingleClickListener {
            viewModel.onClickContinue()
        }
    }

    override fun onBackPressed() {
    }

    private fun setupObservers() {
        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is Advice -> {
                    startActivity<SelfReportAdviceActivity> {
                        putExtra(SelfReportAdviceActivity.REPORTED_TEST_DATA_KEY, navTarget.hasReported)
                    }
                    finish()
                }
            }
        }

        viewModel.viewState().observe(this) { viewState ->
            when (viewState.flowCase) {
                SuccessfullySharedKeysAndNoNeedToReportTest -> {
                    binding.thankYouHeader.setText(R.string.self_report_thank_you_successfully_shared_header)
                    binding.paragraphText.setText(R.string.self_report_thank_you_para_successfully_shared_keys_and_no_need_to_report_test)
                    binding.eligibleInfoView.gone()
                }
                SuccessfullySharedKeysAndNHSTestNotReported -> {
                    binding.thankYouHeader.setText(R.string.self_report_thank_you_successfully_shared_header)
                    binding.paragraphText.setText(R.string.self_report_thank_you_para_sucessfully_shared_keys_and_nhs_test_not_reported)
                    binding.eligibleInfoView.visible()
                }
                UnsuccessfullySharedKeysAndNoNeedToReportTest -> {
                    binding.thankYouHeader.setText(R.string.self_report_thank_you_did_not_share_header)
                    binding.paragraphText.setText(R.string.self_report_thank_you_para_unsuccessfully_shared_keys_and_no_need_to_report_test)
                    binding.eligibleInfoView.gone()
                }
                UnsuccessfullySharedKeysAndNHSTestNotReported -> {
                    binding.thankYouHeader.setText(R.string.self_report_thank_you_did_not_share_header)
                    binding.paragraphText.setText(R.string.self_report_thank_you_para_unsuccessfully_shared_keys_and_nhs_test_not_reported)
                    binding.eligibleInfoView.visible()
                }
            }
        }
    }

    companion object {
        const val SHARING_SUCCESSFUL = "SHARING_SUCCESSFUL"
        const val HAS_REPORTED = "HAS_REPORTED"
    }
}
