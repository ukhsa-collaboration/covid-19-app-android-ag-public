package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import com.google.android.material.snackbar.Snackbar

fun showSnackBarShort(container: ViewGroup, message: String) {
    Snackbar.make(container, message, Snackbar.LENGTH_SHORT).show()
}

fun View.gone() {
    visibility = View.GONE
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.setOnSingleClickListener(listener: () -> Unit) {
    setOnClickListener(object : OnSingleClickListener() {
        override fun onSingleClick(v: View?) {
            listener()
        }
    })
}

fun NestedScrollView.smoothScrollToAndThen(
    x: Int,
    y: Int,
    scrollDuration: Int = 250,
    runAfterScroll: () -> Unit
) {
    smoothScrollTo(x, y, scrollDuration)
    postDelayed(runAfterScroll, scrollDuration.toLong())
}

val Int.pxToDp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)

val Int.dpToPx: Float
    get() = (this * Resources.getSystem().displayMetrics.density)
