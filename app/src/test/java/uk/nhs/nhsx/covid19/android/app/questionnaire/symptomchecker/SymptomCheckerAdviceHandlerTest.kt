package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.CONTINUE_NORMAL_ACTIVITIES
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.TRY_TO_STAY_AT_HOME
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SymptomCheckerAdviceHandlerTest {

    private val testSubject = SymptomCheckerAdviceHandler()

    @Test
    fun `when non-cardinal YES and cardinal YES and how do you feel YES return try to stay at home`() {
        val expectedResult = TRY_TO_STAY_AT_HOME
        val actual = testSubject.invoke(
            symptomsCheckerQuestions.copy(
                nonCardinalSymptoms = symptomsCheckerQuestions.nonCardinalSymptoms?.copy(isChecked = true),
                cardinalSymptom = symptomsCheckerQuestions.cardinalSymptom?.copy(isChecked = true),
                howDoYouFeelSymptom = symptomsCheckerQuestions.howDoYouFeelSymptom?.copy(isChecked = true)
            )
        )

        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when non-cardinal YES and cardinal YES and how do you feel NO return try to stay at home`() {
        val expectedResult = TRY_TO_STAY_AT_HOME
        val actual = testSubject.invoke(
            symptomsCheckerQuestions.copy(
                nonCardinalSymptoms = symptomsCheckerQuestions.nonCardinalSymptoms?.copy(isChecked = true),
                cardinalSymptom = symptomsCheckerQuestions.cardinalSymptom?.copy(isChecked = true),
                howDoYouFeelSymptom = symptomsCheckerQuestions.howDoYouFeelSymptom?.copy(isChecked = false)
            )
        )

        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when non-cardinal YES and cardinal NO and how do you feel YES return continue normal activities`() {
        val expectedResult = CONTINUE_NORMAL_ACTIVITIES
        val actual = testSubject.invoke(
            symptomsCheckerQuestions.copy(
                nonCardinalSymptoms = symptomsCheckerQuestions.nonCardinalSymptoms?.copy(isChecked = true),
                cardinalSymptom = symptomsCheckerQuestions.cardinalSymptom?.copy(isChecked = false),
                howDoYouFeelSymptom = symptomsCheckerQuestions.howDoYouFeelSymptom?.copy(isChecked = true)
            )
        )

        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when non-cardinal YES and cardinal NO and how do you feel NO return try to stay at home`() {
        val expectedResult = TRY_TO_STAY_AT_HOME
        val actual = testSubject.invoke(
            symptomsCheckerQuestions.copy(
                nonCardinalSymptoms = symptomsCheckerQuestions.nonCardinalSymptoms?.copy(isChecked = true),
                cardinalSymptom = symptomsCheckerQuestions.cardinalSymptom?.copy(isChecked = false),
                howDoYouFeelSymptom = symptomsCheckerQuestions.howDoYouFeelSymptom?.copy(isChecked = false)
            )
        )

        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when non-cardinal NO, cardinal symptom YES and how do you feel YES return try to stay at home`() {
        val expectedResult = TRY_TO_STAY_AT_HOME
        val actual = testSubject.invoke(
            symptomsCheckerQuestions.copy(
                nonCardinalSymptoms = symptomsCheckerQuestions.nonCardinalSymptoms?.copy(isChecked = false),
                cardinalSymptom = symptomsCheckerQuestions.cardinalSymptom?.copy(isChecked = true),
                howDoYouFeelSymptom = symptomsCheckerQuestions.howDoYouFeelSymptom?.copy(isChecked = true)
            )
        )

        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when non-cardinal NO, cardinal symptom YES and how do you feel NO return try to stay at home`() {
        val expectedResult = TRY_TO_STAY_AT_HOME
        val actual = testSubject.invoke(
            symptomsCheckerQuestions.copy(
                nonCardinalSymptoms = symptomsCheckerQuestions.nonCardinalSymptoms?.copy(isChecked = false),
                cardinalSymptom = symptomsCheckerQuestions.cardinalSymptom?.copy(isChecked = true),
                howDoYouFeelSymptom = symptomsCheckerQuestions.howDoYouFeelSymptom?.copy(isChecked = false)
            )
        )

        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when non-cardinal NO and not cardinal NO and how do you feel YES return continue normal activities`() {
        val expectedResult = CONTINUE_NORMAL_ACTIVITIES
        val actual = testSubject.invoke(
            symptomsCheckerQuestions.copy(
                nonCardinalSymptoms = symptomsCheckerQuestions.nonCardinalSymptoms?.copy(isChecked = false),
                cardinalSymptom = symptomsCheckerQuestions.cardinalSymptom?.copy(isChecked = false),
                howDoYouFeelSymptom = symptomsCheckerQuestions.howDoYouFeelSymptom?.copy(isChecked = true)
            )
        )

        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when non-cardinal NO and not cardinal NO and how do you feel NO return try to stay at home`() {
        val expectedResult = TRY_TO_STAY_AT_HOME
        val actual = testSubject.invoke(
            symptomsCheckerQuestions.copy(
                nonCardinalSymptoms = symptomsCheckerQuestions.nonCardinalSymptoms?.copy(isChecked = false),
                cardinalSymptom = symptomsCheckerQuestions.cardinalSymptom?.copy(isChecked = false),
                howDoYouFeelSymptom = symptomsCheckerQuestions.howDoYouFeelSymptom?.copy(isChecked = false)
            )
        )

        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when any symptoms missing return null `() {
        val symptomsCheckerQuestionsFilled = SymptomsCheckerQuestions(
            nonCardinalSymptoms = NonCardinalSymptoms(
                nonCardinalSymptomsText = TranslatableString(mapOf()),
                title = TranslatableString(mapOf()), isChecked = true
            ),
            cardinalSymptom = CardinalSymptom(title = TranslatableString(mapOf()), isChecked = true),
            howDoYouFeelSymptom = HowDoYouFeelSymptom(true)
        )

        assertNull(
            testSubject.invoke(
                symptomsCheckerQuestions.copy(
                    nonCardinalSymptoms = symptomsCheckerQuestionsFilled.nonCardinalSymptoms?.copy(
                        isChecked = null
                    )
                )
            )
        )

        assertNull(
            testSubject.invoke(
                symptomsCheckerQuestions.copy(
                    cardinalSymptom = symptomsCheckerQuestionsFilled.cardinalSymptom?.copy(
                        isChecked = null
                    )
                )
            )
        )

        assertNull(
            testSubject.invoke(
                symptomsCheckerQuestions.copy(
                    howDoYouFeelSymptom = symptomsCheckerQuestionsFilled.howDoYouFeelSymptom?.copy(
                        isChecked = null
                    )
                )
            )
        )
    }

    private val symptomsCheckerQuestions = SymptomsCheckerQuestions(
        nonCardinalSymptoms = NonCardinalSymptoms(
            nonCardinalSymptomsText = TranslatableString(mapOf()),
            title = TranslatableString(mapOf()), isChecked = null
        ),
        cardinalSymptom = CardinalSymptom(title = TranslatableString(mapOf()), isChecked = null),
        howDoYouFeelSymptom = HowDoYouFeelSymptom(null)
    )
}
