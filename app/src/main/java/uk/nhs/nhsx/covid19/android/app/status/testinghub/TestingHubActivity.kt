package uk.nhs.nhsx.covid19.android.app.status.testinghub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_testing_hub.itemBookTest
import kotlinx.android.synthetic.main.activity_testing_hub.itemEnterTestResult
import kotlinx.android.synthetic.main.activity_testing_hub.itemFindOutAboutTesting
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ShowFindOutAboutTesting
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ShowFindOutAboutTesting.DoNotShow
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ShowFindOutAboutTesting.Show
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity.Companion.REQUEST_CODE_ORDER_A_TEST
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class TestingHubActivity : BaseActivity(R.layout.activity_testing_hub) {

    @Inject
    lateinit var factory: ViewModelFactory<TestingHubViewModel>
    private val viewModel: TestingHubViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.testing_hub_title, upIndicator = R.drawable.ic_arrow_back_white)

        viewModel.viewState().observe(this) {
            renderViewState(it)
        }

        setUpOnClickListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REQUEST_CODE_ORDER_A_TEST || requestCode == REQUEST_CODE_ENTER_TEST_RESULT) &&
            resultCode == Activity.RESULT_OK
        ) {
            finish()
        }
    }

    private fun renderViewState(viewState: ViewState) {
        itemBookTest.isVisible = viewState.showBookTestButton
        handleFindOutAboutTesting(viewState.showFindOutAboutTestingButton)
    }

    private fun handleFindOutAboutTesting(showFindOutAboutTesting: ShowFindOutAboutTesting) {
        when (showFindOutAboutTesting) {
            is Show -> {
                itemFindOutAboutTesting.visible()
                itemFindOutAboutTesting.setOnSingleClickListener {
                    openUrl(showFindOutAboutTesting.urlResId, useInternalBrowser = true)
                    finish()
                }
            }
            DoNotShow -> itemFindOutAboutTesting.gone()
        }
    }

    private fun setUpOnClickListeners() {
        itemBookTest.setOnSingleClickListener {
            val intent = Intent(this, TestOrderingActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_ORDER_A_TEST)
        }

        itemEnterTestResult.setOnSingleClickListener {
            val intent = Intent(this, LinkTestResultActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_ENTER_TEST_RESULT)
        }
    }

    companion object {
        private const val REQUEST_CODE_ENTER_TEST_RESULT = 1123
    }
}
