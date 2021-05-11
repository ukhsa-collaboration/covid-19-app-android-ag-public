package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.material.appbar.MaterialToolbar
import uk.nhs.nhsx.covid19.android.app.R

fun Context.smallestScreenWidth(): Int = resources.configuration.smallestScreenWidthDp

fun AppCompatActivity.setCloseToolbar(
    toolbar: MaterialToolbar,
    @StringRes titleResId: Int,
    @DrawableRes closeIndicator: Int = R.drawable.ic_close_white,
    listenerAction: () -> Unit = {}
) {
    setNavigateUpToolbar(
        toolbar = toolbar,
        titleResId = titleResId,
        upContentDescription = R.string.close,
        upIndicator = closeIndicator,
        listenerAction = listenerAction
    )
}

fun AppCompatActivity.setNavigateUpToolbar(
    toolbar: MaterialToolbar,
    @StringRes titleResId: Int,
    @DrawableRes upIndicator: Int = R.drawable.ic_arrow_back_white,
    listenerAction: () -> Unit = {}
) {
    setNavigateUpToolbar(
        toolbar = toolbar,
        titleResId = titleResId,
        upContentDescription = R.string.go_back,
        upIndicator = upIndicator,
        listenerAction = listenerAction
    )
}

private fun AppCompatActivity.setNavigateUpToolbar(
    toolbar: MaterialToolbar,
    @StringRes titleResId: Int,
    @StringRes upContentDescription: Int,
    @DrawableRes upIndicator: Int,
    listenerAction: () -> Unit = {}
) {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setHomeAsUpIndicator(upIndicator)
    supportActionBar?.setHomeActionContentDescription(upContentDescription)
    supportActionBar?.title = getString(titleResId)
    toolbar.setNavigationOnClickListener {
        listenerAction()
        onBackPressed()
    }

    toolbar.getChildAt(0)?.let {
        if (it is TextView) {
            it.setUpAccessibilityHeading()
        }
    }
}

fun AppCompatActivity.setToolbar(
    toolbar: MaterialToolbar,
    @StringRes titleResId: Int
) {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setHomeAsUpIndicator(android.R.color.transparent)
    supportActionBar?.title = getString(titleResId)

    toolbar.getChildAt(0)?.let {
        if (it is TextView) {
            it.setUpAccessibilityHeading()
        }
    }
}

fun AppCompatActivity.setToolbarNoNavigation(
    toolbar: MaterialToolbar,
    @StringRes titleResId: Int
) {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(false)
    supportActionBar?.setHomeAsUpIndicator(android.R.color.transparent)
    supportActionBar?.title = getString(titleResId)

    toolbar.getChildAt(0)?.let {
        if (it is TextView) {
            it.setUpAccessibilityHeading()
        }
    }
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

fun TextView.setUpOpensInBrowserWarning() {
    ViewCompat.setAccessibilityDelegate(
        this,
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription =
                    context.getString(R.string.accessibility_announcement_link, text)
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        context.getString(R.string.open_in_browser_warning)
                    )
                )
            }
        }
    )
}

fun TextView.setUpAccessibilityButton() {
    ViewCompat.setAccessibilityDelegate(
        this,
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription =
                    context.getString(R.string.accessibility_announcement_button, text)
            }
        }
    )
}

fun TextView.setUpAccessibilityHeading(heading: String) {
    ViewCompat.setAccessibilityDelegate(
        this,
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription = heading
                info.isHeading = true
            }
        }
    )
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
