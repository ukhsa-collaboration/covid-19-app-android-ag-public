package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.content.Intent
import android.os.Bundle
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySelfReportShareKeysInformationBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportShareKeysInformationViewModel.NavigationTarget.DeclinedKeySharing
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportShareKeysInformationViewModel.NavigationTarget.SharedKeys
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportShareKeysInformationViewModel.NavigationTarget.TestType
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithoutTitle
import javax.inject.Inject
 import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class SelfReportShareKeysInformationActivity : BaseActivity() {

    @Inject
    lateinit var factory: SelfReportShareKeysInformationViewModel.Factory

    private val viewModel: SelfReportShareKeysInformationViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    private lateinit var binding: ActivitySelfReportShareKeysInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySelfReportShareKeysInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_submit_keys_information_back_button_accessibility_description
        )

        setClickListeners()
        setupObservers()
    }

    private fun setClickListeners() {
        binding.selfReportShareKeysConfirm.setOnSingleClickListener {
            viewModel.onClickContinue()
        }
    }

    private fun setupObservers() {
        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is DeclinedKeySharing -> {
                    startActivity<SelfReportAppWillNotNotifyOtherUsersActivity> {
                        putExtra(SelfReportAppWillNotNotifyOtherUsersActivity.SELF_REPORT_QUESTIONS_DATA_KEY, navTarget.selfReportTestQuestions)
                    }
                    finish()
                }
                is SharedKeys -> {
                    startActivity<TestKitTypeActivity> {
                        putExtra(TestKitTypeActivity.SELF_REPORT_QUESTIONS_DATA_KEY, navTarget.selfReportTestQuestions)
                    }
                    finish()
                }
                is TestType -> {
                    startActivity<TestTypeActivity> {
                        putExtra(TestTypeActivity.SELF_REPORT_QUESTIONS_DATA_KEY, navTarget.selfReportTestQuestions)
                    }
                    finish()
                }
            }
        }
        viewModel.permissionRequest().observe(this) { permissionRequest ->
            permissionRequest(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("onActivityResult: requestCode = $requestCode resultCode = $resultCode")

        viewModel.onActivityResult(requestCode, resultCode)
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
    }
}
