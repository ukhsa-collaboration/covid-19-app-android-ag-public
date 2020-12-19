package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.hamcrest.Matchers.not
import org.hamcrest.core.AllOf.allOf
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.clickChildViewWithId
import uk.nhs.nhsx.covid19.android.app.testhelpers.withViewAtPosition

class UserDataRobot {
    fun checkActivityIsDisplayed() {
        onView(withText(R.string.about_manage_my_data))
            .check(matches(isDisplayed()))
    }

    fun userClicksOnDeleteAllDataButton() {
        clickOn(R.id.actionDeleteAllData)
    }

    fun checkDeleteDataConfirmationDialogIsDisplayed() {
        onView(withText(R.string.about_delete_positive_text))
            .check(matches(isDisplayed()))
    }

    fun userClicksDeleteDataOnDialog() {
        onView(withText(R.string.about_delete_positive_text))
            .perform(click())
    }

    fun userClicksEditVenueVisits() {
        clickOn(R.id.editVenueVisits)
    }

    fun userClicksEditPostalDistrict() {
        clickOn(R.id.editLocalAuthority)
    }

    fun checkDeleteIconForFirstVenueVisitIsDisplayed() {
        onView(withId(R.id.venueHistoryList))
            .check(
                matches(
                    withViewAtPosition(
                        0,
                        hasDescendant(allOf(withId(R.id.imageDeleteVenueVisit), isDisplayed()))
                    )
                )
            )
    }

    fun clickDeleteVenueVisitOnFirstPosition() {
        onView(withId(R.id.venueHistoryList)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0,
                clickChildViewWithId(R.id.imageDeleteVenueVisit)
            )
        )
    }

    fun userClicksConfirmOnDialog() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)

        // Search for correct button in the dialog.
        val confirmString = instrumentation.targetContext.getString(R.string.confirm)
        val buttonUpperCase = uiDevice.findObject(UiSelector().text(confirmString.toUpperCase()))
        val button = uiDevice.findObject(UiSelector().text(confirmString))
        if (buttonUpperCase.exists() && buttonUpperCase.isEnabled) {
            buttonUpperCase.click()
        } else if (button.exists() && button.isEnabled) {
            button.click()
        }
    }

    fun confirmDialogIsDisplayed() {
        onView(withId(android.R.id.button1))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    fun editVenueVisitsIsDisplayed() {
        onView(withId(R.id.editVenueVisits))
            .check(matches(withText(R.string.edit)))
    }

    fun checkLastTestResultIsDisplayed() {
        onView(withId(R.id.titleLatestResult))
            .check(matches(isDisplayed()))
        onView(withId(R.id.latestResultContainer))
            .check(matches(isDisplayed()))
    }

    fun checkExposureNotificationIsDisplayed() {
        onView(withId(R.id.titleExposureNotification))
            .check(matches(isDisplayed()))
    }

    fun checkEncounterIsDisplayed() {
        onView(withId(R.id.encounterDataSection))
            .check(matches(isDisplayed()))
    }

    fun checkExposureNotificationDateIsDisplayed() {
        onView(withId(R.id.exposureNotificationDataSection))
            .check(matches(isDisplayed()))
    }

    fun checkExposureNotificationDateIsNotDisplayed() {
        onView(withId(R.id.exposureNotificationDataSection))
            .check(matches(not(isDisplayed())))
    }

    fun checkLastDayOfIsolationIsDisplayed() {
        onView(withId(R.id.titleLastDayOfIsolation))
            .check(matches(isDisplayed()))
        onView(withId(R.id.lastDayOfIsolationSection))
            .check(matches(isDisplayed()))
    }

    fun checkLastDayOfIsolationDisplaysText(expected: String) {
        onView(withId(R.id.lastDayOfIsolationDate))
            .check(matches(withText(expected)))
    }

    fun checkLastDayOfIsolationIsNotDisplayed() {
        onView(withId(R.id.titleLastDayOfIsolation))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.lastDayOfIsolationSection))
            .check(matches(not(isDisplayed())))
    }

    fun checkSymptomsAreDisplayed() {
        onView(withId(R.id.titleSymptoms))
            .check(matches(isDisplayed()))
        onView(withId(R.id.symptomsDataSection))
            .check(matches(isDisplayed()))
    }
}
