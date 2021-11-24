package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.children
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.widgets.ParagraphsContainer

class ParagraphsContainerContainsStringResourceMatcher(@field:StringRes @param:StringRes private val expectedId: Int) :
    TypeSafeMatcher<View?>(View::class.java) {
    var resourceName: String? = null

    protected override fun matchesSafely(target: View?): Boolean {
        require(target != null) { "Cannot match a null view" }

        if (target !is ParagraphsContainer) {
            return false
        }

        if (target.childCount == 0) return false

        target.children
            .filter { it is LinearLayout }
            .map { it.findViewById<TextView>(R.id.paragraphText) }
            .forEach { textView ->
                if (textView.text.contains(target.context.resources.getString(expectedId)))
                    return true
            }

        return false
    }

    override fun describeTo(description: Description) {
        description.appendText("with text from resource id: ")
        description.appendValue(expectedId)
        if (resourceName != null) {
            description.appendText("[")
            description.appendText(resourceName)
            description.appendText("]")
        }
    }
}

fun paragraphsContainerContainsStringResource(@StringRes resourceId: Int): Matcher<View?> {
    return ParagraphsContainerContainsStringResourceMatcher(resourceId)
}
