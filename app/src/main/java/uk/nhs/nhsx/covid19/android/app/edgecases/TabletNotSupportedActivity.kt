package uk.nhs.nhsx.covid19.android.app.edgecases

import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity

class TabletNotSupportedActivity : BaseActivity(R.layout.activity_tablet_not_supported) {
    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, TabletNotSupportedActivity::class.java)
    }
}
