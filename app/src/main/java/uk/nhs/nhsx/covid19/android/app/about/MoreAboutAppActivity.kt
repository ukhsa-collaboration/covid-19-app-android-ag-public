package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_more_about_app.textSoftwareDateOfRelease
import kotlinx.android.synthetic.main.activity_more_about_app.textSoftwareName
import kotlinx.android.synthetic.main.activity_more_about_app.textSoftwareVersion
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar

class MoreAboutAppActivity : BaseActivity(R.layout.activity_more_about_app) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setNavigateUpToolbar(toolbar, R.string.about_this_app_title, upIndicator = R.drawable.ic_arrow_back_white)

        textSoftwareName.setText(R.string.app_name)
        textSoftwareVersion.text = BuildConfig.VERSION_NAME
        textSoftwareDateOfRelease.text = getReleaseDate()
    }

    private fun getReleaseDate(): String {
        return BuildConfig.RELEASE_DATE
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, MoreAboutAppActivity::class.java)
    }
}
