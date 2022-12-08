package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySelfReportAppWillNotNotifyOtherUsersBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAppWillNotNotifyOtherUsersViewModel.NavigationTarget.TestKit
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAppWillNotNotifyOtherUsersViewModel.NavigationTarget.ShareKeysInfo
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithoutTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class SelfReportAppWillNotNotifyOtherUsersActivity : BaseActivity() {

    @Inject
    lateinit var factory: SelfReportAppWillNotNotifyOtherUsersViewModel.Factory

    private val viewModel: SelfReportAppWillNotNotifyOtherUsersViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    private lateinit var binding: ActivitySelfReportAppWillNotNotifyOtherUsersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySelfReportAppWillNotNotifyOtherUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_app_will_not_notify_other_users_back_button_accessibility_description
        )

        setClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is TestKit -> {
                    startActivity<TestKitTypeActivity> {
                        putExtra(TestKitTypeActivity.SELF_REPORT_QUESTIONS_DATA_KEY, navTarget.selfReportTestQuestions)
                    }
                    finish()
                }
                is ShareKeysInfo -> {
                    startActivity<SelfReportShareKeysInformationActivity> {
                        putExtra(SelfReportShareKeysInformationActivity.SELF_REPORT_QUESTIONS_DATA_KEY, navTarget.selfReportTestQuestions)
                    }
                    finish()
                }
            }
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun setClickListeners() {
        binding.buttonContinue.setOnSingleClickListener {
            viewModel.onClickContinue()
        }
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
    }
}
