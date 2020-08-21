package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_more_about_app.linkAccessibilityStatement
import kotlinx.android.synthetic.main.activity_more_about_app.linkCommonQuestions
import kotlinx.android.synthetic.main.activity_more_about_app.linkManageData
import kotlinx.android.synthetic.main.activity_more_about_app.linkPrivacyNotice
import kotlinx.android.synthetic.main.activity_more_about_app.linkTermsOfUse
import kotlinx.android.synthetic.main.activity_more_about_app.textSoftwareInformation
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar

class MoreAboutAppActivity : BaseActivity(R.layout.activity_more_about_app) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setNavigateUpToolbar(toolbar, R.string.about_this_app_title, R.drawable.ic_arrow_back_white)

        val appName = getString(R.string.app_name)
        val appVersion = BuildConfig.VERSION_NAME
        val dateOfRelease = getReleaseDate()
        textSoftwareInformation.text =
            getString(R.string.about_software_information_text, appName, appVersion, dateOfRelease)

        setupListeners()
    }

    private fun getReleaseDate(): String {
        return "08/2020" // TODO Implement it when get enough info
    }

    private fun setupListeners() {
        linkCommonQuestions.setOnClickListener {
            openUrl(R.string.url_common_questions)
        }

        linkTermsOfUse.setOnClickListener {
            openUrl(R.string.url_terms_of_use)
        }

        linkPrivacyNotice.setOnClickListener {
            openUrl(R.string.url_privacy_notice)
        }

        linkAccessibilityStatement.setOnClickListener {
            openUrl(R.string.url_accessibility_statement)
        }

        linkManageData.setOnClickListener {
            UserDataActivity.start(this)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, MoreAboutAppActivity::class.java)
    }
}
