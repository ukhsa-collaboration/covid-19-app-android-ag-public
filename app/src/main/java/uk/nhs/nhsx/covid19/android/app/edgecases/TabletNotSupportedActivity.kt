package uk.nhs.nhsx.covid19.android.app.edgecases

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_tablet_not_supported.tabletInformationUrl
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.openUrl

class TabletNotSupportedActivity : BaseActivity(R.layout.activity_tablet_not_supported) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tabletInformationUrl.setOnClickListener {
            openUrl(R.string.url_nhs_tablet_device)
        }
    }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, TabletNotSupportedActivity::class.java)
    }
}
