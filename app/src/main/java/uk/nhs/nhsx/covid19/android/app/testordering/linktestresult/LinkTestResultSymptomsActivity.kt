package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_link_test_result_symptoms.linkTestResultSymptomsButtonNo
import kotlinx.android.synthetic.main.activity_link_test_result_symptoms.linkTestResultSymptomsButtonYes
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setToolbarNoNavigation
import javax.inject.Inject

class LinkTestResultSymptomsActivity : BaseActivity(R.layout.activity_link_test_result_symptoms) {

    @Inject
    lateinit var factory: ViewModelFactory<LinkTestResultSymptomsViewModel>

    private val viewModel by viewModels<LinkTestResultSymptomsViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setToolbarNoNavigation(
            toolbar,
            R.string.link_test_result_symptoms_information_title
        )

        intent.getParcelableExtra<ReceivedTestResult>(EXTRA_TEST_RESULT)?.let { receivedTestResult ->
            setupListeners()

            viewModel.onCreate()

            viewModel.confirmSymptoms().observe(this) {
                LinkTestResultOnsetDateActivity.start(this, receivedTestResult)
                finish()
            }
        } ?: finish()
    }

    override fun onBackPressed() = Unit

    private fun setupListeners() {
        linkTestResultSymptomsButtonYes.setOnSingleClickListener {
            viewModel.onConfirmSymptomsClicked()
        }

        linkTestResultSymptomsButtonNo.setOnSingleClickListener {
            finish()
        }
    }

    companion object {
        const val EXTRA_TEST_RESULT = "EXTRA_TEST_RESULT"

        fun start(context: Context, testResult: ReceivedTestResult) =
            context.startActivity(getIntent(context, testResult))

        private fun getIntent(context: Context, testResult: ReceivedTestResult) =
            Intent(context, LinkTestResultSymptomsActivity::class.java)
                .putExtra(EXTRA_TEST_RESULT, testResult)
    }
}
