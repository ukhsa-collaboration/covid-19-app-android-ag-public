package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.setChecked
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo

class ExposureNotificationVaccinationStatusRobot {

    private val dosesYesButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption1),
            isDescendantOfA(withId(R.id.allDosesBinaryRadioGroup))
        )
    )

    private val dosesNoButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption2),
            isDescendantOfA(withId(R.id.allDosesBinaryRadioGroup))
        )
    )

    private val dateYesButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption1),
            isDescendantOfA(withId(R.id.vaccineDateBinaryRadioGroup))
        )
    )

    private val dateNoButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption2),
            isDescendantOfA(withId(R.id.vaccineDateBinaryRadioGroup))
        )
    )

    private val clinicalTrialYesButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption1),
            isDescendantOfA(withId(R.id.clinicalTrialBinaryRadioGroup))
        )
    )

    private val clinicalTrialNoButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption2),
            isDescendantOfA(withId(R.id.clinicalTrialBinaryRadioGroup))
        )
    )

    private val medicallyExemptYesButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption1),
            isDescendantOfA(withId(R.id.medicallyExemptBinaryRadioGroup))
        )
    )

    private val medicallyExemptNoButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption2),
            isDescendantOfA(withId(R.id.medicallyExemptBinaryRadioGroup))
        )
    )

    fun checkActivityIsDisplayed() {
        onView(withText(R.string.exposure_notification_vaccination_status_title))
            .check(matches(isDisplayed()))
    }

    fun checkErrorVisible(shouldBeVisible: Boolean) {
        onView(withId(R.id.vaccinationStatusErrorView))
            .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
    }

    fun clickDosesYesButton() {
        dosesYesButton.perform(nestedScrollTo(), setChecked(true))
    }

    fun clickDosesNoButton() {
        dosesNoButton.perform(nestedScrollTo(), setChecked(true))
    }

    fun checkDosesNothingSelected() {
        dosesYesButton.check(matches(isNotChecked()))
        dosesNoButton.check(matches(isNotChecked()))
    }

    fun checkMedicallyExemptNothingSelected() {
        medicallyExemptYesButton.check(matches(isNotChecked()))
        medicallyExemptNoButton.check(matches(isNotChecked()))
    }

    fun checkMedicallyExemptYesSelected() {
        medicallyExemptYesButton.check(matches(isChecked()))
        medicallyExemptNoButton.check(matches(isNotChecked()))
    }

    fun checkDosesYesSelected() {
        dosesYesButton
            .perform(nestedScrollTo())
            .check(matches(isChecked()))
        dosesNoButton
            .perform(nestedScrollTo())
            .check(matches(isNotChecked()))
    }

    fun checkDosesNoSelected() {
        dosesNoButton
            .perform(nestedScrollTo())
            .check(matches(isChecked()))
        dosesYesButton
            .perform(nestedScrollTo())
            .check(matches(isNotChecked()))
    }

    fun clickDateYesButton() {
        dateYesButton.perform(nestedScrollTo(), click())
    }

    fun clickDateNoButton() {
        dateNoButton.perform(nestedScrollTo(), click())
    }

    fun checkDateNothingSelected() {
        dateYesButton
            .perform(nestedScrollTo())
            .check(matches(isNotChecked()))
        dateNoButton
            .perform(nestedScrollTo())
            .check(matches(isNotChecked()))
    }

    fun checkDateNoSelected() {
        dateNoButton
            .perform(nestedScrollTo())
            .check(matches(isChecked()))
        dateYesButton
            .perform(nestedScrollTo())
            .check(matches(isNotChecked()))
    }

    fun clickContinueButton() {
        onView(withId(R.id.vaccinationStatusContinueButton))
            .perform(nestedScrollTo(), click())
    }

    fun checkDosesDateQuestionContainerDisplayed(displayed: Boolean) {
        onView(withId(R.id.vaccineDateQuestionContainer)).apply {
            if (displayed) {
                perform(nestedScrollTo())
            }
        }
            .check(if (displayed) matches(isDisplayed()) else doesNotExist())
    }

    fun checkDosesDateQuestionDisplayedWithDate(expectedDate: String) {
        onView(withId(R.id.vaccineDateQuestion))
            .perform(nestedScrollTo())
            .check(
                matches(
                    withText(
                        context.getString(R.string.exposure_notification_vaccination_status_date_question, expectedDate)
                    )
                )
            )
    }

    fun clickApprovedVaccinesLink() {
        onView(withId(R.id.approvedVaccinesLink))
            .perform(nestedScrollTo(), click())
    }

    fun checkClinicalTrialQuestionContainerDisplayed(displayed: Boolean) {
        onView(withText(R.string.exposure_notification_clinical_trial_question)).apply {
            if (displayed) {
                perform(nestedScrollTo())
            }
        }
            .check(if (displayed) matches(isDisplayed()) else doesNotExist())
    }

    fun checkClinicalTrialNoSelected() {
        clinicalTrialNoButton
            .perform(nestedScrollTo())
            .check(matches(isChecked()))
        clinicalTrialYesButton
            .perform(nestedScrollTo())
            .check(matches(isNotChecked()))
    }

    fun checkMedicallyExemptQuestionContainerDisplayed(displayed: Boolean) {
        onView(withId(R.id.medicallyExemptQuestionContainer)).apply {
            if (displayed) {
                perform(nestedScrollTo())
            }
        }
            .check(if (displayed) matches(isDisplayed()) else doesNotExist())
    }

    fun clickClinicalTrialYesButton() {
        clinicalTrialYesButton.perform(nestedScrollTo(), click())
    }

    fun clickClinicalTrialNoButton() {
        clinicalTrialNoButton.perform(nestedScrollTo(), click())
    }

    fun clickMedicallyExemptYesButton() {
        medicallyExemptYesButton.perform(nestedScrollTo(), click())
    }

    fun clickMedicallyExemptNoButton() {
        medicallyExemptNoButton.perform(nestedScrollTo(), click())
    }

    fun checkSubtitleDisplayed(displayed: Boolean) {
        onView(withId(R.id.vaccinationStatusSubtitle)).apply {
            if (displayed) perform(nestedScrollTo())
        }
            .check(matches(if (displayed) isDisplayed() else not(isDisplayed())))
    }
}
