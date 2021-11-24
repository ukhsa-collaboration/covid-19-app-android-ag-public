package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.widgets.ParagraphsContainer

class ContainsStringResourceAtMatcher(
    @field:StringRes @param:StringRes private val expectedId: Int,
    private val expectedIndex: Int
) :
    TypeSafeMatcher<View?>(View::class.java) {
    var resourceName: String? = null

    protected override fun matchesSafely(target: View?): Boolean {
        require(target != null) { "Cannot match a null view" }

        if (target !is ParagraphsContainer) {
            return false
        }

        if (target.childCount == 0) return false
        if (expectedIndex > target.childCount - 1) return false

        with(target.getChildAt(expectedIndex)) {
            return this is LinearLayout &&
                    this.findViewById<TextView>(R.id.paragraphText).text.contains(
                        target.context.resources.getString(
                            expectedId
                        )
                    )
        }
    }

    override fun describeTo(description: Description) {
        description.appendText("with text from resource id: ")
        description.appendValue(expectedId)
        if (resourceName != null) {
            description.appendText("[")
            description.appendText(resourceName)
            description.appendText("]")
        }
        description.appendText("at index: ")
        description.appendValue(expectedIndex)
    }
}

fun containsStringResourceAt(@StringRes resourceId: Int, index: Int): Matcher<View?> {
    return ContainsStringResourceAtMatcher(resourceId, index)
}
