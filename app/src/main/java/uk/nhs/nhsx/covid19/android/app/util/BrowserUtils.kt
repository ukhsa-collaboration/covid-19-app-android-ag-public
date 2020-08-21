/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R

fun Activity.openUrl(@StringRes urlStringId: Int, useInternalBrowser: Boolean = true) {
    val url = getString(urlStringId)
    openUrl(url, useInternalBrowser)
}

fun Activity.openUrl(url: String, useInternalBrowser: Boolean = true) {
    try {
        if (useInternalBrowser) openInInternalBrowser(url)
        else openInExternalBrowser(url)
    } catch (t: Throwable) {
        Timber.e(t, "Error opening url")
    }
}

private fun Activity.openInInternalBrowser(url: String) {
    CustomTabsIntent.Builder()
        .addDefaultShareMenuItem()
        .setToolbarColor(getColor(R.color.links_toolbar_color))
        .build()
        .launchUrl(this, Uri.parse(url))
}

private fun Activity.openInExternalBrowser(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
