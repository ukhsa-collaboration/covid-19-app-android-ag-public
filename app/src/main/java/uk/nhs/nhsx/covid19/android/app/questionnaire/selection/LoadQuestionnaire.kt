package uk.nhs.nhsx.covid19.android.app.questionnaire.selection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.QuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.data.QuestionnaireResponse
import javax.inject.Inject

class LoadQuestionnaire @Inject constructor(private val questionnaireApi: QuestionnaireApi) {
    suspend operator fun invoke(): Result<QuestionnaireResponse> = withContext(Dispatchers.IO) {
        runSafely {
            questionnaireApi.fetchQuestionnaire()
        }
    }
}
