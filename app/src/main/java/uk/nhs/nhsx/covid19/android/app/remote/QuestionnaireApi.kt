package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.QuestionnaireResponse

interface QuestionnaireApi {
    @GET("distribution/symptomatic-questionnaire")
    suspend fun fetchQuestionnaire(): QuestionnaireResponse
}
