package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_data_and_privacy.buttonAgree
import kotlinx.android.synthetic.main.activity_data_and_privacy.privacyNoticeLink
import kotlinx.android.synthetic.main.activity_data_and_privacy.termsOfUseLink
import kotlinx.android.synthetic.main.activity_data_and_privacy.textNoThanks
import kotlinx.android.synthetic.main.include_onboarding_toolbar.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar

class DataAndPrivacyActivity : BaseActivity(R.layout.activity_data_and_privacy) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setNavigateUpToolbar(toolbar as MaterialToolbar, R.string.empty)

        buttonAgree.setOnClickListener {
            PermissionActivity.start(this)
        }

        textNoThanks.setOnClickListener { finish() }

        privacyNoticeLink.setOnClickListener {
            openUrl(R.string.url_privacy_notice)
        }

        termsOfUseLink.setOnClickListener {
            openUrl(R.string.url_terms_of_use)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DataAndPrivacyActivity::class.java)
    }
}
