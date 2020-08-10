package uk.nhs.nhsx.covid19.android.app.testhelpers

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

fun getCurrentActivity(): Activity {
    getInstrumentation().waitForIdleSync()
    var currentActivity: Activity? = null
    getInstrumentation().runOnMainSync {
        run {
            currentActivity =
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                    Stage.RESUMED
                ).elementAtOrNull(0)
        }
    }
    return currentActivity!!
}

fun assertBrowserIsOpened(url: String, block: () -> Unit) {
    Intents.init()
    val expectedIntent = CoreMatchers.allOf(
        IntentMatchers.hasAction(Intent.ACTION_VIEW),
        IntentMatchers.hasData(url)
    )
    Intents.intending(expectedIntent).respondWith(Instrumentation.ActivityResult(0, null))

    block()
    try {
        Intents.intended(expectedIntent)
    } finally {
        Intents.release()
    }
}

class NestedScrollViewScrollToAction(
    private val original: ScrollToAction = ScrollToAction()
) : ViewAction by original {

    override fun getConstraints(): Matcher<View> = CoreMatchers.anyOf(
        CoreMatchers.allOf(
            ViewMatchers.withEffectiveVisibility(VISIBLE),
            ViewMatchers.isDescendantOfA(ViewMatchers.isAssignableFrom(NestedScrollView::class.java))
        ),
        original.constraints
    )
}

fun ViewInteraction.isDisplayed(): Boolean =
    runCatching {
        check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        true
    }.getOrElse { false }
