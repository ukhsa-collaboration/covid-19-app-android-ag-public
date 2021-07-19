package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import android.os.Bundle
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar

class WhenNotToPauseContactTracingActivity : BaseActivity(R.layout.activity_when_not_to_pause_contact_tracing) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setNavigateUpToolbar(toolbar, titleResId = R.string.when_not_to_pause_contact_tracing_title)
    }
}
