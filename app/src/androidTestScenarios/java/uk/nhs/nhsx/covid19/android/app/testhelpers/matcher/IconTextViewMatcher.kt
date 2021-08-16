package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import uk.nhs.nhsx.covid19.android.app.widgets.IconTextView

class IconTextViewMatcher(
    @StringRes private val expectedStringResId: Int,
    @DrawableRes private val expectedDrawableResId: Int
) :
    TypeSafeMatcher<View?>(View::class.java) {
    var resourceName: String? = null

    override fun matchesSafely(target: View?): Boolean {
        require(target != null) { "Cannot match a null view" }
        require(target is IconTextView) { "Target has to be IconTextView" }

        val verifyStringRes = target.stringResId == expectedStringResId
        val verifyDrawableRes = target.drawableResId == expectedDrawableResId

        return verifyStringRes && verifyDrawableRes
    }

    override fun describeTo(description: Description) {
        description.appendText("with expectedStringResId from resource id: ")
        description.appendValue(expectedStringResId)
        description.appendText(", and with expectedDrawableResId from resource id: ")
        description.appendValue(expectedDrawableResId)
    }

    companion object {
        fun withIconAndText(
            @StringRes stringResId: Int,
            @DrawableRes drawableResId: Int
        ): Matcher<View?> {
            return IconTextViewMatcher(stringResId, drawableResId)
        }
    }
}
