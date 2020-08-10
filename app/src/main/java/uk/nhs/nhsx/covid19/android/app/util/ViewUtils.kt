package uk.nhs.nhsx.covid19.android.app.util

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
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

val Int.pxToDp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)

val Int.dpToPx: Float
    get() = (this * Resources.getSystem().displayMetrics.density)
