package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.QuestionNode
import javax.inject.Inject

class QuestionnaireFactory @Inject constructor(
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider
) {
    suspend fun create(): QuestionNode {
        val fetcher: QuestionnaireFetcher = if (localAuthorityPostCodeProvider.isWelshDistrict()) {
            WelshQuestionnaireFetcher()
        } else {
            EnglishQuestionnaireFetcher()
        }

        return fetcher.create()
    }
}
