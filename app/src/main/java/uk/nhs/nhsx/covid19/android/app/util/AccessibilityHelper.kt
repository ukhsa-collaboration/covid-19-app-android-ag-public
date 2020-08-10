package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.ScrollView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.appbar.MaterialToolbar
import uk.nhs.nhsx.covid19.android.app.R

fun Context.smallestScreenWidth(): Int = resources.configuration.smallestScreenWidthDp

fun AppCompatActivity.setNavigateUpToolbar(
    toolbar: MaterialToolbar,
    @StringRes title: Int,
    @DrawableRes homeIndicator: Int = R.drawable.ic_arrow_back_primary
) {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setHomeAsUpIndicator(homeIndicator)
    supportActionBar?.setHomeActionContentDescription(R.string.go_back)
    supportActionBar?.title = getString(title)
    toolbar.setNavigationOnClickListener { onBackPressed() }

    toolbar.getChildAt(0)?.setUpAccessibilityHeading()
}

/**
 * Reads headings for devices with api 19+.
 * This is handling accessibility headings in a better way
 * than just using the xml attribute => (android:accessibilityHeading="true")
 * because this attribute only works for api 28+.
 */
fun View.setUpAccessibilityHeading() {
    ViewCompat.setAccessibilityDelegate(
        this,
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View?,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.isHeading = true
            }
        }
    )
}

fun ScrollView.scrollToView(view: View) {
    post { smoothScrollTo(0, view.top) }
}

fun Context.announce(@StringRes textToAnnounceRes: Int) {
    val text = getString(textToAnnounceRes)
    announce(text)
}

fun Context.announce(textToAnnounce: String) {
    val accessibilityEvent = AccessibilityEvent.obtain().apply {
        eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
        text.add(textToAnnounce)
    }
    val accessibilityManager =
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    if (accessibilityManager?.isEnabled == true) {
        accessibilityManager.sendAccessibilityEvent(
            accessibilityEvent
        )
    }
}
