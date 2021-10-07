package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.OptOutResponseAdapter.ViewHolder
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.OptOutResponseView.OptOutResponse
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.AgeLimitQuestionType.IsAdult
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.ClinicalTrial
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import java.time.LocalDate

class OptOutResponseAdapter(
    private val responses: List<OptOutResponseEntry>,
    private val ageLimitDate: LocalDate,
    private val lastDoseDateLimit: LocalDate
) :
    RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = OptOutResponseView(parent.context)
        itemView.layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        return ViewHolder(itemView, ageLimitDate, lastDoseDateLimit)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(responses[position], showDivider = position != itemCount - 1)
    }

    override fun getItemCount(): Int = responses.size

    class ViewHolder(
        private val responseView: OptOutResponseView,
        private val ageLimitDate: LocalDate,
        private val lastDoseDateLimit: LocalDate
    ) : RecyclerView.ViewHolder(responseView) {
        fun bind(data: OptOutResponseEntry, showDivider: Boolean) {
            responseView.setResponse(convert(data))
            responseView.setShouldShowDivider(showDivider)
        }

        private fun convert(optOutResponseEntry: OptOutResponseEntry): OptOutResponse {
            val context = responseView.context
            return when (optOutResponseEntry.questionType) {
                IsAdult -> convertWithDate(context, optOutResponseEntry, ageLimitDate)
                ClinicalTrial, FullyVaccinated, MedicallyExempt -> {
                    val question = context.getString(optOutResponseEntry.questionType.question)
                    val description = context.getString(optOutResponseEntry.contentDescription)
                    val responseText = context.getString(optOutResponseEntry.responseText)
                    OptOutResponse(question, optOutResponseEntry.response, description, responseText)
                }
                DoseDate -> convertWithDate(context, optOutResponseEntry, lastDoseDateLimit)
            }
        }

        private fun convertWithDate(
            context: Context,
            optOutResponseEntry: OptOutResponseEntry,
            date: LocalDate
        ): OptOutResponse {
            val question = context.getString(optOutResponseEntry.questionType.question, date.uiLongFormat(context))
            val description = context.getString(optOutResponseEntry.contentDescription, date.uiLongFormat(context))
            val responseText = context.getString(optOutResponseEntry.responseText)
            return OptOutResponse(question, optOutResponseEntry.response, description, responseText)
        }
    }
}
