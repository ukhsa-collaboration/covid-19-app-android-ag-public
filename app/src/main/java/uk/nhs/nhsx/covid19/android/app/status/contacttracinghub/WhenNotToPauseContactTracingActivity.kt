package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityWhenNotToPauseContactTracingBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar

class WhenNotToPauseContactTracingActivity : BaseActivity() {

    private lateinit var binding: ActivityWhenNotToPauseContactTracingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWhenNotToPauseContactTracingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbar(
            binding.primaryToolbar.toolbar,
            titleResId = R.string.when_not_to_pause_contact_tracing_title
        )
    }
}
