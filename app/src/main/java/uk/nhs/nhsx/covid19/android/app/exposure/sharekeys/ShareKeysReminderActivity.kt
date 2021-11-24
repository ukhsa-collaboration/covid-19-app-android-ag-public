package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityShareKeysReminderBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class ShareKeysReminderActivity : ShareKeysBaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<ShareKeysReminderViewModel>
    override val viewModel: ShareKeysReminderViewModel by viewModels { viewModelFactory }

    private lateinit var binding: ActivityShareKeysReminderBinding

    override fun inject() {
        appComponent.inject(this)
    }

    override fun setupBinding() {
        binding = ActivityShareKeysReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setupOnClickListeners() = with(binding) {
        shareResultsButton.setOnSingleClickListener {
            viewModel.onShareKeysButtonClicked()
        }

        doNotShareResultsButton.setOnSingleClickListener {
            viewModel.onDoNotShareKeysClicked()
        }
    }
}
