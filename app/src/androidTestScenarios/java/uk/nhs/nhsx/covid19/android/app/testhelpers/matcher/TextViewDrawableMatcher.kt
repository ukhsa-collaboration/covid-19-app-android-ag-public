package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description

class TextViewDrawableMatcher private constructor(private val expectedId: Int) :
    BoundedMatcher<View?, TextView>(TextView::class.java) {
    override fun matchesSafely(textView: TextView): Boolean {
        return drawableMatches(textView, getDrawable(textView), expectedId)
    }

    override fun describeTo(description: Description) {
        if (expectedId == NO_DRAWABLE) {
            description.appendText("with no ")
                .appendText(" drawable")
        } else {
            description.appendText("with ")
                .appendText(" drawable from resource id: ")
                .appendValue(expectedId)
        }
    }

    private fun drawableMatches(view: View, drawable: Drawable?, @DrawableRes expectedId: Int): Boolean {
        val isVisible = view.visibility == VISIBLE
        return when (expectedId) {
            NO_DRAWABLE -> {
                isVisible && drawable == null
            }
            HAS_DRAWABLE -> {
                isVisible && drawable != null
            }
            else -> isVisible && checkNotNull(
                drawable!!.constantState
            ) { "constantState == null" } == ContextCompat.getDrawable(view.context, expectedId)!!.constantState
        }
    }

    @SuppressLint("NewApi")
    fun getDrawable(textView: TextView): Drawable? {
        val drawables = textView.compoundDrawablesRelative
        return drawables[INDEX_END]
    }

    companion object {
        const val NO_DRAWABLE = -1
        const val HAS_DRAWABLE = -2
        const val INDEX_END = 2

        @CheckResult
        fun withTextViewNoDrawable(): TextViewDrawableMatcher {
            return TextViewDrawableMatcher(NO_DRAWABLE)
        }

        @CheckResult
        fun withTextViewHasDrawableEnd(): TextViewDrawableMatcher {
            return TextViewDrawableMatcher(HAS_DRAWABLE)
        }
    }
}
