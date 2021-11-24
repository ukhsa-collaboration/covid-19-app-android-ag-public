package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.allOf
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.OptOutResponseEntry
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.AgeLimitQuestionType.IsAdult
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.withViewAtPosition
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import java.time.LocalDate

class ExposureNotificationReviewRobot(val testAppContext: TestApplicationContext) : HasActivity {

    override val containerId: Int
        get() = R.id.reviewScrollView

    fun clickSubmitButton() {
        onView(withId(R.id.submitExposureQuestionnaire))
            .perform(nestedScrollTo(), click())
    }

    fun verifyReviewViewState(ageResponse: Boolean = true, vaccinationStatusResponses: List<OptOutResponseEntry>) {
        runBlocking {
            verifyViewState(
                ageResponse = ageResponse,
                vaccinationStatusResponses = vaccinationStatusResponses,
                ageLimitDate = testAppContext.getAgeLimitBeforeEncounter()!!,
                lastDoseDateLimit = testAppContext.getLastDoseDateLimit()!!
            )
        }
    }

    fun verifyViewState(
        ageResponse: Boolean,
        vaccinationStatusResponses: List<OptOutResponseEntry>,
        ageLimitDate: LocalDate,
        lastDoseDateLimit: LocalDate
    ) {
        verifyAgeLimitGroup(ageResponse, ageLimitDate)
        if (vaccinationStatusResponses.isEmpty()) {
            verifyVaccinationStatusGroupNotDisplayed()
        } else {
            verifyVaccinationStatusGroup(vaccinationStatusResponses, lastDoseDateLimit)
        }
    }

    private fun verifyAgeLimitGroup(response: Boolean, ageLimitDate: LocalDate) {
        val ageResponse = OptOutResponseEntry(IsAdult, response)
        onView(
            allOf(
                withId(R.id.responseGroupTitle),
                isDescendantOfA(withId(R.id.reviewYourAgeGroup))
            )
        ).check(
            matches(withText(R.string.exposure_notification_age_heading))
        )

        onView(
            allOf(
                withId(R.id.responseGroupUserInput),
                isDescendantOfA(withId(R.id.reviewYourAgeGroup))
            )
        )
            .check(
                matches(
                    allOf(
                        withViewAtPosition(
                            position = 0,
                            ageResponse.verify(ageLimitDate)
                        )
                    )
                )
            )
    }

    private fun OptOutResponseEntry.verify(
        dateParameter: LocalDate
    ) = allOf(
        withContentDescription(
            context.getString(
                contentDescription,
                dateParameter.uiLongFormat(context)
            )
        ),
        hasDescendant(withText(context.getString(questionType.question, dateParameter.uiLongFormat(context)))),
        hasDescendant(withResponse(response))
    )

    private fun OptOutResponseEntry.verify() = allOf(
        withContentDescription(context.getString(contentDescription)),
        hasDescendant(withText(context.getString(questionType.question))),
        hasDescendant(withResponse(response))
    )

    private fun verifyVaccinationStatusGroupNotDisplayed() {
        onView(withId(R.id.reviewYourVaccinationStatusGroup)).check(matches(not(isDisplayed())))
    }

    private fun verifyVaccinationStatusGroup(
        vaccinationStatusResponses: List<OptOutResponseEntry>,
        lastDoseDateLimit: LocalDate
    ) {
        onView(
            allOf(
                withId(R.id.responseGroupTitle),
                isDescendantOfA(withId(R.id.reviewYourVaccinationStatusGroup))
            )
        ).check(
            matches(withText(R.string.exposure_notification_vaccination_status_heading))
        )

        vaccinationStatusResponses.forEachIndexed { index, entry ->
            val verifier = when (entry.questionType) {
                DoseDate -> entry.verify(lastDoseDateLimit)
                else -> entry.verify()
            }

            onView(
                allOf(
                    withId(R.id.responseGroupUserInput),
                    isDescendantOfA(withId(R.id.reviewYourVaccinationStatusGroup))
                )
            )
                .check(
                    matches(
                        allOf(
                            withViewAtPosition(
                                position = index,
                                verifier
                            )
                        )
                    )
                )
        }
    }

    private fun withResponse(response: Boolean) =
        if (response) withText(R.string.exposure_notification_age_option1_text)
        else withText(R.string.exposure_notification_age_option2_text)

    fun clickChangeAge() {
        onView(
            allOf(
                withId(R.id.responseGroupChanger),
                isDescendantOfA(withId(R.id.reviewYourAgeGroup))
            )
        ).perform(nestedScrollTo(), click())
    }

    fun clickChangeVaccinationStatus() {
        onView(
            allOf(
                withId(R.id.responseGroupChanger),
                isDescendantOfA(withId(R.id.reviewYourVaccinationStatusGroup))
            )
        ).perform(nestedScrollTo(), click())
    }
}
