package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.LastCompletedV2SymptomsQuestionnaireDateProvider.Companion.LAST_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_DATE_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.OBJECT_TO_JSON
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.reflect.KProperty1
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LastCompletedV2SymptomsQuestionnaireDateProviderTest :
    ProviderTest<LastCompletedV2SymptomsQuestionnaireDateProvider, LastCompletedV2SymptomsQuestionnaireDate?>() {
    private val fixedClock = Clock.fixed(Instant.parse("2022-05-01T10:00:00Z"), ZoneOffset.UTC)

    override val getTestSubject: (Moshi, SharedPreferences) -> LastCompletedV2SymptomsQuestionnaireDateProvider =
        { moshi, sharedPreferences ->
            LastCompletedV2SymptomsQuestionnaireDateProvider(fixedClock, moshi, sharedPreferences)
        }
    override val property: KProperty1<LastCompletedV2SymptomsQuestionnaireDateProvider, LastCompletedV2SymptomsQuestionnaireDate?>
        get() = LastCompletedV2SymptomsQuestionnaireDateProvider::lastCompletedV2SymptomsQuestionnaire
    override val key: String
        get() = LAST_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_DATE_KEY
    override val defaultValue: LastCompletedV2SymptomsQuestionnaireDate? = null
    override val expectations: List<ProviderTestExpectation<LastCompletedV2SymptomsQuestionnaireDate?>> = listOf(
        ProviderTestExpectation(json = TEST_DATE_JSON, objectValue = TEST_DATE),
        ProviderTestExpectation(json = null, objectValue = null, direction = OBJECT_TO_JSON)
    )

    @Test
    fun `does not contain completed questionnaire when latest date is null`() {
        sharedPreferencesReturns(null)

        val actual = testSubject.containsCompletedV2SymptomsQuestionnaire()

        assertFalse(actual)
    }

    @Test
    fun `contains questionnaire completed when current date is 14 days after stored latest date`() {
        sharedPreferencesReturns(TEST_DATE_JSON)

        val fixedClock = Clock.fixed(Instant.parse("2022-05-14T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = LastCompletedV2SymptomsQuestionnaireDateProvider(
            fixedClock,
            moshi,
            sharedPreferences
        )

        val actual = testSubject.containsCompletedV2SymptomsQuestionnaire()

        assertTrue(actual)
    }

    @Test
    fun `does not contain questionnaire completed when current date is after 14 days last stored date`() {
        sharedPreferencesReturns(TEST_DATE_JSON)

        val fixedClock = Clock.fixed(Instant.parse("2022-05-16T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = LastCompletedV2SymptomsQuestionnaireDateProvider(
            fixedClock,
            moshi,
            sharedPreferences
        )

        val actual = testSubject.containsCompletedV2SymptomsQuestionnaire()

        assertFalse(actual)
    }

    companion object {
        private val TEST_DATE_JSON =
            """
            {"latestDate":"2022-05-01","keepAnalyticsTickDays":14}
            """.trimIndent()

        private val TEST_DATE = LastCompletedV2SymptomsQuestionnaireDate(
            LocalDate.parse("2022-05-01")
        )
    }
}
