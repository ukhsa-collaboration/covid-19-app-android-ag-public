package uk.nhs.nhsx.covid19.android.app.testordering.unknownresult

import android.os.Bundle
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_unknown_test_result.unknown_test_result_action_button
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.openAppStore
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class UnknownTestResultActivity : BaseActivity(R.layout.activity_unknown_test_result) {

    @Inject
    lateinit var factory: ViewModelFactory<UnknownTestResultViewModel>
    private val viewModel: UnknownTestResultViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setCloseToolbar(toolbar, R.string.empty, R.drawable.ic_close_primary) {
            viewModel.acknowledgeUnknownTestResult()
        }

        unknown_test_result_action_button.setOnSingleClickListener {
            viewModel.acknowledgeUnknownTestResult()
            openAppStore()
            finish()
        }
    }

    override fun onBackPressed() {
        viewModel.acknowledgeUnknownTestResult()

        super.onBackPressed()
    }
}
