/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R

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

const val URL_INFO = "https://covid19.nhs.uk/"
const val URL_PRIVACY_NOTICE = "https://covid19.nhs.uk/privacy-and-data.html"
const val URL_TERMS_OF_USE = "https://covid19.nhs.uk/our-policies.html"

const val URL_NHS_NOT_SUPPORTED_DEVICE = "https://faq.covid19.nhs.uk/article/KA-01073/en-us"
const val URL_NHS_TABLET_DEVICE = "https://faq.covid19.nhs.uk/article/KA-01079/en-us"

const val URL_NHS_111_ONLINE = "https://111.nhs.uk/"

const val URL_ACCESSIBILITY_STATEMENT = "https://covid19.nhs.uk/accessibility.html"

const val URL_POSTAL_CODE_RISK_MORE_INFO = "https://faq.covid19.nhs.uk/article/KA-01101/en-us"

const val URL_COMMON_QUESTIONS = "https://faq.covid19.nhs.uk/"
const val URL_LATEST_ADVICE = "https://www.gov.uk/coronavirus"
const val URL_ISOLATION_ADVICE = "https://faq.covid19.nhs.uk/article/KA-01099/en-us"
const val URL_ABOUT_THE_APP = "https://covid19.nhs.uk/"
const val URL_ORDER_TEST_PRIVACY = "https://www.gov.uk/government/publications/coronavirus-covid-19-testing-privacy-information/testing-for-coronavirus-privacy-information"
const val URL_ORDER_TEST_FOR_SOMEONE_ELSE = "https://www.nhs.uk/conditions/coronavirus-covid-19/testing-and-tracing/get-a-test-to-check-if-you-have-coronavirus/"
