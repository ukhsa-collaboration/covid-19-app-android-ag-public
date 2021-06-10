package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import org.hamcrest.core.AllOf.allOf
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlock
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction
import uk.nhs.nhsx.covid19.android.app.testhelpers.clickChildViewWithId
import uk.nhs.nhsx.covid19.android.app.testhelpers.withViewAtPosition

class LocalMessageRobot {
    fun checkActivityIsDisplayed() {
        onView(withId(R.id.localMessageContainer))
            .check(matches(isDisplayed()))
    }

    fun checkTitleIsDisplayed(title: String) {
        onView(withId(R.id.titleLocalMessage))
            .check(matches(withText(title)))
    }

    fun checkContentIsDisplayedAtPosition(contentBlock: ContentBlock, position: Int) {
        val displayedTextMatcher = allOf(
            withId(R.id.localMessageContentDescription),
            withText(contentBlock.text),
            isDisplayed()
        )
        val hiddenTextMatcher = allOf(
            withId(R.id.localMessageContentDescription),
            not(isDisplayed())
        )

        val displayedLinkMatcher = allOf(
            withId(R.id.localMessageContentLinkView),
            withText(contentBlock.linkText ?: contentBlock.link),
            isDisplayed()
        )
        val hiddenLinkMatcher = allOf(
            withId(R.id.localMessageContentLinkView),
            not(isDisplayed())
        )

        onView(withId(R.id.localMessageContentList))
            .check(
                matches(
                    withViewAtPosition(
                        position,
                        allOf(
                            // Description paragraph
                            hasDescendant(
                                if (contentBlock.text != null) {
                                    displayedTextMatcher
                                } else {
                                    hiddenTextMatcher
                                }
                            ),
                            // Link
                            hasDescendant(
                                if (contentBlock.link != null) {
                                    displayedLinkMatcher
                                } else {
                                    hiddenLinkMatcher
                                }
                            )
                        )
                    )
                )
            )
    }

    fun clickLink() {
        onView(withId(R.id.localMessageContentList))
            .perform(
                actionOnItemAtPosition<ViewHolder>(
                    1,
                    clickChildViewWithId(R.id.localMessageContentLinkView)
                )
            )
    }

    fun checkListSize(size: Int) {
        onView(withId(R.id.localMessageContentList))
            .check(matches(hasChildCount(size)))
    }

    fun clickCloseButton() {
        onView(withContentDescription(string.close)).perform(click())
    }

    fun clickBackToHome() {
        onView(withId(R.id.backToHome))
            .perform(NestedScrollViewScrollToAction(), click())
    }
}
