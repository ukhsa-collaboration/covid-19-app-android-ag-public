package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import androidx.lifecycle.LiveData
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.ProgressActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSubmitTestResultAndKeysProgressViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSubmitTestResultAndKeysProgressViewModel.NavigationTarget.ThankYou
import javax.inject.Inject

class SelfReportSubmitTestResultAndKeysProgressActivity : ProgressActivity<Unit>() {

    @Inject
    lateinit var factory: SelfReportSubmitTestResultAndKeysProgressViewModel.Factory

    private val viewModel: SelfReportSubmitTestResultAndKeysProgressViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setupNavigationObservers()
    }

    override fun startAction() {
        viewModel.submitTestResultAndKeys()
    }

    override fun viewModelLiveData(): LiveData<Lce<Unit>> {
        return viewModel.submitKeysResult()
    }

    override fun onSuccess(result: Unit) {
        viewModel.onSuccess()
    }

    private fun setupNavigationObservers() {
        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
               is ThankYou -> {
                   startActivity<SelfReportThankYouActivity> {
                       putExtra(SelfReportThankYouActivity.SHARING_SUCCESSFUL, navTarget.hasSharedSuccessfully)
                       putExtra(SelfReportThankYouActivity.HAS_REPORTED, navTarget.hasReported)
                   }
                   finish()
               }
                is Finish -> { finish() }
            }
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
    }
}
