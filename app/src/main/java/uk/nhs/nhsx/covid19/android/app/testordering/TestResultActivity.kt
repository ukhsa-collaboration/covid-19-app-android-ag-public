package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_test_result.goodNewsContainer
import kotlinx.android.synthetic.main.activity_test_result.isolationRequestContainer
import kotlinx.android.synthetic.main.view_good_news.goodNewsActionButton
import kotlinx.android.synthetic.main.view_good_news.goodNewsIcon
import kotlinx.android.synthetic.main.view_good_news.goodNewsInfoView
import kotlinx.android.synthetic.main.view_good_news.goodNewsOnlineServiceLink
import kotlinx.android.synthetic.main.view_good_news.goodNewsParagraphContainer
import kotlinx.android.synthetic.main.view_good_news.goodNewsSubtitle
import kotlinx.android.synthetic.main.view_good_news.goodNewsTitle
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestActionButton
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestInfoView
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestOnlineServiceLink
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestParagraphContainer
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestTitle1
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestTitle2
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult.Failure
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult.Success
import uk.nhs.nhsx.covid19.android.app.status.ExposureStatusViewModel.Companion.REQUEST_CODE_SUBMIT_KEYS_PERMISSION
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_IGNORE
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_NEGATIVE_IN_ISOLATION
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_NEGATIVE_NOT_IN_ISOLATION
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_POSITIVE_IN_ISOLATION
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_POSITIVE_NOT_IN_ISOLATION
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.invisible
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.visible
import javax.inject.Inject

class TestResultActivity : BaseActivity(R.layout.activity_test_result) {

    @Inject
    lateinit var factory: ViewModelFactory<TestResultViewModel>

    private val viewModel: TestResultViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        startViewModelListeners()

        goodNewsOnlineServiceLink.setOnClickListener {
            openUrl(R.string.url_nhs_111_online)
        }

        isolationRequestOnlineServiceLink.setOnClickListener {
            openUrl(R.string.url_nhs_111_online)
        }

        viewModel.onCreate()
    }

    private fun startViewModelListeners() {
        viewModel.viewState().observe(
            this,
            Observer { viewState ->
                when (viewState.mainState) {
                    RESULT_NEGATIVE_IN_ISOLATION -> showContinueToSelfIsolationScreenOnNegative(
                        viewState.remainingDaysInIsolation
                    )
                    RESULT_NEGATIVE_NOT_IN_ISOLATION -> showDoNotHaveToSelfIsolateScreenOnNegative()
                    RESULT_POSITIVE_IN_ISOLATION -> showContinueToSelfIsolationScreenOnPositive(
                        viewState.remainingDaysInIsolation
                    )
                    RESULT_POSITIVE_NOT_IN_ISOLATION -> showDoNotHaveToSelfIsolateScreenOnPositive()
                    RESULT_IGNORE -> finish()
                }
            }
        )

        viewModel.keyUploadResult().observe(
            this,
            Observer { result ->
                Timber.d("keyUploadResult: $result")
                when (result) {
                    Success -> finish()
                    is Failure -> {
                        finish()
                    }
                    is ResolutionRequired -> handleSubmitKeysResolution(result.status)
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SUBMIT_KEYS_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.submitKeys()
            } else {
                finish()
            }
        }
    }

    private fun handleSubmitKeysResolution(status: Status) {
        status.startResolutionForResult(this, REQUEST_CODE_SUBMIT_KEYS_PERMISSION)
    }

    private fun showContinueToSelfIsolationScreenOnPositive(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()

        isolationRequestTitle1.text =
            getString(R.string.test_result_positive_continue_self_isolation_title_1)
        isolationRequestTitle2.text = resources.getQuantityString(
            R.plurals.state_isolation_days,
            remainingDaysInIsolation,
            remainingDaysInIsolation
        )
        isolationRequestInfoView.stateText = getString(R.string.state_test_positive_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_positive_continue_self_isolate_explanation),
            getString(R.string.for_further_advice_visit)
        )

        isolationRequestActionButton.text = getString(R.string.continue_button)
        isolationRequestActionButton.setOnClickListener {
            viewModel.submitKeys()
        }
    }

    private fun showContinueToSelfIsolationScreenOnNegative(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()

        isolationRequestTitle1.text =
            getString(R.string.test_result_positive_continue_self_isolation_title_1)
        isolationRequestTitle2.text = resources.getQuantityString(
            R.plurals.state_isolation_days,
            remainingDaysInIsolation,
            remainingDaysInIsolation
        )
        isolationRequestInfoView.stateText = getString(R.string.state_test_negative_info)
        isolationRequestInfoView.stateColor = getColor(R.color.amber)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_negative_continue_self_isolate_explanation),
            getString(R.string.for_further_advice_visit)
        )

        isolationRequestActionButton.text = getString(R.string.back_to_home)
        isolationRequestActionButton.setOnClickListener {
            finish()
        }
    }

    private fun showDoNotHaveToSelfIsolateScreenOnPositive() {
        goodNewsContainer.visible()
        isolationRequestContainer.gone()

        goodNewsIcon.invisible()
        goodNewsTitle.gone()

        goodNewsSubtitle.text = getString(R.string.test_result_positive_no_self_isolation_subtitle)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_positive_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.amber)
        goodNewsParagraphContainer.addAllParagraphs(getString(R.string.for_further_advice_visit))

        goodNewsActionButton.text = getString(R.string.continue_button)
        goodNewsActionButton.setOnClickListener {
            viewModel.submitKeys()
        }
    }

    private fun showDoNotHaveToSelfIsolateScreenOnNegative() {
        goodNewsContainer.visible()
        isolationRequestContainer.gone()

        goodNewsIcon.visible()
        goodNewsTitle.visible()

        goodNewsSubtitle.text = getString(R.string.test_result_negative_no_self_isolation_subtitle)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_negative_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.nhs_button_green)
        goodNewsParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_negative_no_self_isolation_explanation),
            getString(R.string.for_further_advice_visit)
        )

        goodNewsActionButton.text = getString(R.string.back_to_home)
        goodNewsActionButton.setOnClickListener {
            finish()
        }
    }
}
