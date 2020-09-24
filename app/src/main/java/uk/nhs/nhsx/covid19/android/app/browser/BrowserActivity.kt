package uk.nhs.nhsx.covid19.android.app.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import kotlinx.android.synthetic.main.activity_browser.browserCloseButton
import kotlinx.android.synthetic.main.activity_browser.webView
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar

class BrowserActivity : BaseActivity(R.layout.activity_browser) {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WebView.setWebContentsDebuggingEnabled(true)

        val url = intent.getStringExtra(EXTRA_URL)
        webView.settings.javaScriptEnabled = true
        url?.let {
            webView.loadUrl(url)
        }

        browserCloseButton.setOnClickListener {
            finish()
        }

        setNavigateUpToolbar(toolbar, R.string.test_ordering_title, R.drawable.ic_close_white)
    }

    companion object {
        private const val EXTRA_URL = "EXTRA_URL"

        fun start(context: Context, url: String) =
            context.startActivity(getIntent(context, url))

        private fun getIntent(context: Context, url: String) =
            Intent(context, BrowserActivity::class.java)
                .putExtra(EXTRA_URL, url)
    }
}
