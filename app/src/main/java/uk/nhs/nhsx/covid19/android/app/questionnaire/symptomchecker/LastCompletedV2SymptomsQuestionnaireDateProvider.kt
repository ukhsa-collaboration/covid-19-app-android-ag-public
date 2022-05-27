package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import uk.nhs.nhsx.covid19.android.app.util.storage
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class LastCompletedV2SymptomsQuestionnaireDateProvider @Inject constructor(
    private val clock: Clock,
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {
    var lastCompletedV2SymptomsQuestionnaire: LastCompletedV2SymptomsQuestionnaireDate? by storage(
        LAST_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_DATE_KEY
    )
    var lastCompletedV2SymptomsQuestionnaireAndStayAtHome: LastCompletedV2SymptomsQuestionnaireAndStayAtHomeDate? by storage(
        LAST_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_AND_STAY_AT_HOME_DATE_KEY
    )

    companion object {
        const val LAST_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_DATE_KEY =
            "LAST_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_DATE_KEY"
        const val LAST_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_AND_STAY_AT_HOME_DATE_KEY =
            "LAST_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_AND_STAY_AT_HOME_DATE_KEY"
    }

    fun containsCompletedV2SymptomsQuestionnaire(): Boolean {
        val now = LocalDate.now(clock)
        val lastCompletedV2SymptomsQuestionnaire = lastCompletedV2SymptomsQuestionnaire
        return lastCompletedV2SymptomsQuestionnaire != null &&
                now.isEqualOrAfter(lastCompletedV2SymptomsQuestionnaire.latestDate) &&
                now.isBefore(
                    lastCompletedV2SymptomsQuestionnaire.latestDate.plusDays(
                        lastCompletedV2SymptomsQuestionnaire.keepAnalyticsTickDays.toLong()
                    )
                )
    }

    fun containsCompletedV2SymptomsQuestionnaireAndTryToStayAtHomeResult(): Boolean {
        val now = LocalDate.now(clock)
        val lastCompletedV2SymptomsQuestionnaireAndStayAtHome = lastCompletedV2SymptomsQuestionnaireAndStayAtHome
        return lastCompletedV2SymptomsQuestionnaireAndStayAtHome != null &&
                now.isEqualOrAfter(lastCompletedV2SymptomsQuestionnaireAndStayAtHome.latestDate) &&
                now.isBefore(
                    lastCompletedV2SymptomsQuestionnaireAndStayAtHome.latestDate.plusDays(
                        lastCompletedV2SymptomsQuestionnaireAndStayAtHome.keepAnalyticsTickDays.toLong()
                    )
                )
    }
}

@JsonClass(generateAdapter = true)
data class LastCompletedV2SymptomsQuestionnaireDate(
    val latestDate: LocalDate,
    val keepAnalyticsTickDays: Int = 14
)

@JsonClass(generateAdapter = true)
data class LastCompletedV2SymptomsQuestionnaireAndStayAtHomeDate(
    val latestDate: LocalDate,
    val keepAnalyticsTickDays: Int = 14
)
