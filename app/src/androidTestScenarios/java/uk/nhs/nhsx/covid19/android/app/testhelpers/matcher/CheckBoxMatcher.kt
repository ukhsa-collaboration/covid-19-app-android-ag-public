package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import android.widget.Checkable
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matchers.isA

fun setChecked(checked: Boolean) = object : ViewAction {
    val checkableViewMatcher = object : BaseMatcher<View>() {
        override fun matches(item: Any?): Boolean = isA(Checkable::class.java).matches(item)
        override fun describeTo(description: Description?) {
            description?.appendText("is Checkable instance ")
        }
    }
    override fun getConstraints(): BaseMatcher<View> = checkableViewMatcher
    override fun getDescription(): String? = null
    override fun perform(uiController: UiController?, view: View) {
        val checkableView: Checkable = view as Checkable
        checkableView.isChecked = checked
    }
}
