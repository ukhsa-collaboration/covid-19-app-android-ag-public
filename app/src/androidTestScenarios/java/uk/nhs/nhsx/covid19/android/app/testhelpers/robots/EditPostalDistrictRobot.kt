package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo
import com.schibsted.spain.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import uk.nhs.nhsx.covid19.android.app.R
import java.lang.Thread.sleep

class EditPostalDistrictRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.continuePostCode))
            .check(matches(isDisplayed()))
    }

    fun clickSavePostDistrictCode() {
        clickOn(R.id.continuePostCode)
    }

    fun checkErrorContainerForNotSupportedPostCodeIsDisplayed() {
        assertDisplayed(R.id.errorTextTitle, R.string.postcode_not_supported)
    }

    fun checkInvalidPostDistrictErrorIsDisplayed() {
        assertDisplayed(R.string.post_code_invalid_title)
    }

    fun enterPostDistrictCode(postDistrictCode: String) {
        writeTo(R.id.postCodeEditText, postDistrictCode)
        closeKeyboard()
        sleep(500)
    }
}
