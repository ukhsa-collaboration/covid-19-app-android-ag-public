package uk.nhs.nhsx.covid19.android.app.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityBrowserBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class BrowserActivity : BaseActivity() {

    private lateinit var binding: ActivityBrowserBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WebView.setWebContentsDebuggingEnabled(true)

        val url = intent.dataString

        with(binding) {

            if (URLUtil.isValidUrl(url)) {
                webView.visible()
                webView.settings.javaScriptEnabled = true
                webView.webViewClient = WebViewClient()
                url?.let {
                    webView.loadUrl(url)
                }
            } else {
                browserFeedbackText.visible()
                browserFeedbackText.text = url
            }

            browserCloseButton.setOnSingleClickListener {
                finish()
            }

            setCloseToolbar(binding.primaryToolbar.toolbar, R.string.empty)
        }
    }

    companion object {
        fun start(context: Context, url: String) =
            context.startActivity(getIntent(context, url))

        fun startForResult(activity: Activity, url: String, requestId: Int) =
            activity.startActivityForResult(getIntent(activity, url), requestId)

        private fun getIntent(context: Context, url: String) =
            Intent(context, BrowserActivity::class.java)
                .setData(Uri.parse(url))
    }
}
