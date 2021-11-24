package uk.nhs.nhsx.covid19.android.app.testordering.unknownresult

import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.openAppStore
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityUnknownTestResultBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class UnknownTestResultActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<UnknownTestResultViewModel>
    private val viewModel: UnknownTestResultViewModel by viewModels { factory }
    private lateinit var binding: ActivityUnknownTestResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityUnknownTestResultBinding.inflate(layoutInflater)

        with(binding) {

            setContentView(root)

            setCloseToolbar(primaryToolbar.toolbar, R.string.empty, R.drawable.ic_close_primary) {
                viewModel.acknowledgeUnknownTestResult()
            }

            unknownTestResultActionButton.setOnSingleClickListener {
                viewModel.acknowledgeUnknownTestResult()
                openAppStore()
                finish()
            }
        }
    }

    override fun onBackPressed() {
        viewModel.acknowledgeUnknownTestResult()

        super.onBackPressed()
    }
}
