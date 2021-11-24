package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.context
import uk.nhs.nhsx.covid19.android.app.widgets.IconTextView

class IconTextViewMatcher(
    private val text: String,
    @DrawableRes private val expectedDrawableResId: Int
) : TypeSafeMatcher<View?>(View::class.java) {

    constructor(@StringRes stringRes: Int, expectedDrawableResId: Int) : this(context.getString(stringRes), expectedDrawableResId)

    override fun matchesSafely(target: View?): Boolean {
        if (target !is IconTextView) return false

        val verifyStringRes = target.text == text
        val verifyDrawableRes = target.drawableResId == expectedDrawableResId

        return verifyStringRes && verifyDrawableRes
    }

    override fun describeTo(description: Description) {
        description.appendText("with expected text: ")
        description.appendValue(text)
        description.appendText(", and with expectedDrawableResId from resource id: ")
        description.appendValue(expectedDrawableResId)
    }

    companion object {
        fun withIconAndText(
            text: String,
            @DrawableRes drawableResId: Int
        ): Matcher<View?> {
            return IconTextViewMatcher(text, drawableResId)
        }
    }
}
