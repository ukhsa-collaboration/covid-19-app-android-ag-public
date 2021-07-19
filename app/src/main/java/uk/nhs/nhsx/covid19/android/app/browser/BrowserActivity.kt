package uk.nhs.nhsx.covid19.android.app.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_browser.browserCloseButton
import kotlinx.android.synthetic.main.activity_browser.webView
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class BrowserActivity : BaseActivity(R.layout.activity_browser) {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WebView.setWebContentsDebuggingEnabled(true)

        val url = intent.getStringExtra(EXTRA_URL)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        url?.let {
            webView.loadUrl(url)
        }

        browserCloseButton.setOnSingleClickListener {
            finish()
        }

        setCloseToolbar(toolbar, R.string.test_ordering_title)
    }

    companion object {
        private const val EXTRA_URL = "EXTRA_URL"

        fun start(context: Context, url: String) =
            context.startActivity(getIntent(context, url))

        fun startForResult(activity: Activity, url: String, requestId: Int) =
            activity.startActivityForResult(getIntent(activity, url), requestId)

        private fun getIntent(context: Context, url: String) =
            Intent(context, BrowserActivity::class.java)
                .putExtra(EXTRA_URL, url)
    }
}
