package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.Question
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.VaccinationStatusQuestionnaireAdapter.QuestionnaireViewHolder
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.VaccinationStatusQuestionnaireAdapter.VaccinationStatusQuestion
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.VaccinationStatusQuestionnaireAdapter.VaccinationStatusQuestion.ClinicalTrial
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.VaccinationStatusQuestionnaireAdapter.VaccinationStatusQuestion.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.VaccinationStatusQuestionnaireAdapter.VaccinationStatusQuestion.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.VaccinationStatusQuestionnaireAdapter.VaccinationStatusQuestion.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import uk.nhs.nhsx.covid19.android.app.widgets.HeadingTextView
import java.time.LocalDate

class VaccinationStatusQuestionnaireAdapter(
    private val onFullyVaccinatedOptionChanged: (BinaryRadioGroupOption) -> Unit,
    private val onDoseDateOptionChanged: (BinaryRadioGroupOption) -> Unit,
    private val onMedicallyExemptOptionChanged: (BinaryRadioGroupOption) -> Unit,
    private val onClinicalTrialOptionChanged: (BinaryRadioGroupOption) -> Unit
) : ListAdapter<VaccinationStatusQuestion, QuestionnaireViewHolder>(ItemCallback()) {

    class QuestionnaireViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            question: VaccinationStatusQuestion,
            onFullyVaccinatedOptionChanged: (BinaryRadioGroupOption) -> Unit,
            onDoseDateOptionChanged: (BinaryRadioGroupOption) -> Unit,
            onMedicallyExemptOptionChanged: (BinaryRadioGroupOption) -> Unit,
            onClinicalTrialOptionChanged: (BinaryRadioGroupOption) -> Unit
        ) {
            when (question) {
                is FullyVaccinated -> bindFullyVaccinated(itemView, question, onFullyVaccinatedOptionChanged)
                is DoseDate -> bindDoseDate(itemView, question, onDoseDateOptionChanged)
                is MedicallyExempt -> bindMedicallyExempt(itemView, question, onMedicallyExemptOptionChanged)
                is ClinicalTrial -> bindClinicalTrial(itemView, question, onClinicalTrialOptionChanged)
            }
        }

        private fun bindFullyVaccinated(
            itemView: View,
            question: FullyVaccinated,
            onFullyVaccinatedOptionChanged: (BinaryRadioGroupOption) -> Unit
        ) {
            val fullyVaccinatedBinaryRadioGroup = itemView.findViewById<BinaryRadioGroup>(R.id.allDosesBinaryRadioGroup)
            fullyVaccinatedBinaryRadioGroup.selectedOption = question.state
            fullyVaccinatedBinaryRadioGroup.setOnValueChangedListener(onFullyVaccinatedOptionChanged)
        }

        private fun bindDoseDate(
            itemView: View,
            question: DoseDate,
            onDoseDateOptionChanged: (BinaryRadioGroupOption) -> Unit
        ) {
            val lastDoseDateBinaryRadioGroup = itemView.findViewById<BinaryRadioGroup>(R.id.vaccineDateBinaryRadioGroup)
            lastDoseDateBinaryRadioGroup.selectedOption = question.state
            lastDoseDateBinaryRadioGroup.setOnValueChangedListener(onDoseDateOptionChanged)

            val formattedDate = question.date.uiLongFormat(itemView.context)

            val lastDoseDateQuestion = itemView.findViewById<HeadingTextView>(R.id.vaccineDateQuestion)
            lastDoseDateQuestion.text = itemView.context.getString(
                R.string.exposure_notification_vaccination_status_date_question,
                formattedDate
            )

            lastDoseDateBinaryRadioGroup.setOption1Text(
                text = itemView.context.getString(R.string.exposure_notification_vaccination_status_date_yes),
                contentDescription = itemView.context.getString(
                    R.string.exposure_notification_vaccination_status_date_yes_content_description,
                    formattedDate
                )
            )
            lastDoseDateBinaryRadioGroup.setOption2Text(
                text = itemView.context.getString(R.string.exposure_notification_vaccination_status_date_no),
                contentDescription = itemView.context.getString(
                    R.string.exposure_notification_vaccination_status_date_no_content_description,
                    formattedDate
                )
            )
        }

        private fun bindClinicalTrial(
            itemView: View,
            question: ClinicalTrial,
            onMedicallyExemptOptionChanged: (BinaryRadioGroupOption) -> Unit
        ) {
            val clinicalTrialBinaryRadioGroup =
                itemView.findViewById<BinaryRadioGroup>(R.id.clinicalTrialBinaryRadioGroup)
            clinicalTrialBinaryRadioGroup.selectedOption = question.state
            clinicalTrialBinaryRadioGroup.setOnValueChangedListener(onMedicallyExemptOptionChanged)
        }

        private fun bindMedicallyExempt(
            itemView: View,
            question: MedicallyExempt,
            onClinicalTrialOptionChanged: (BinaryRadioGroupOption) -> Unit
        ) {
            val medicallyExemptBinaryRadioGroup =
                itemView.findViewById<BinaryRadioGroup>(R.id.medicallyExemptBinaryRadioGroup)
            medicallyExemptBinaryRadioGroup.selectedOption = question.state
            medicallyExemptBinaryRadioGroup.setOnValueChangedListener(onClinicalTrialOptionChanged)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionnaireViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = when (viewType) {
            VIEW_TYPE_FULLY_VACCINATED -> inflater.inflate(R.layout.binary_question_fully_vaccinated, parent, false)
            VIEW_TYPE_DOSE_DATE -> inflater.inflate(R.layout.binary_question_vaccine_date, parent, false)
            VIEW_TYPE_CLINICAL_TRIAL -> inflater.inflate(R.layout.binary_question_clinical_trial, parent, false)
            VIEW_TYPE_MEDICALLY_EXEMPT -> inflater.inflate(R.layout.binary_question_medically_exempt, parent, false)
            else -> throw Exception("Unknown ViewType")
        }
        return QuestionnaireViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: QuestionnaireViewHolder, position: Int) =
        holder.bind(
            getItem(position),
            onFullyVaccinatedOptionChanged,
            onDoseDateOptionChanged,
            onMedicallyExemptOptionChanged,
            onClinicalTrialOptionChanged
        )

    override fun getItemViewType(position: Int) =
        when (getItem(position)) {
            is FullyVaccinated -> VIEW_TYPE_FULLY_VACCINATED
            is DoseDate -> VIEW_TYPE_DOSE_DATE
            is ClinicalTrial -> VIEW_TYPE_CLINICAL_TRIAL
            is MedicallyExempt -> VIEW_TYPE_MEDICALLY_EXEMPT
        }

    class ItemCallback : DiffUtil.ItemCallback<VaccinationStatusQuestion>() {
        override fun areItemsTheSame(oldItem: VaccinationStatusQuestion, newItem: VaccinationStatusQuestion) =
            oldItem::class == newItem::class

        override fun areContentsTheSame(oldItem: VaccinationStatusQuestion, newItem: VaccinationStatusQuestion) =
            oldItem == newItem

        override fun getChangePayload(oldItem: VaccinationStatusQuestion, newItem: VaccinationStatusQuestion): Any {
            return Unit
        }
    }

    sealed class VaccinationStatusQuestion {
        data class FullyVaccinated(val state: BinaryRadioGroupOption?) : VaccinationStatusQuestion()
        data class DoseDate(val state: BinaryRadioGroupOption?, val date: LocalDate) : VaccinationStatusQuestion()
        data class MedicallyExempt(val state: BinaryRadioGroupOption?) : VaccinationStatusQuestion()
        data class ClinicalTrial(val state: BinaryRadioGroupOption?) : VaccinationStatusQuestion()
    }

    companion object {
        private const val VIEW_TYPE_FULLY_VACCINATED = 0
        private const val VIEW_TYPE_DOSE_DATE = 1
        private const val VIEW_TYPE_CLINICAL_TRIAL = 2
        private const val VIEW_TYPE_MEDICALLY_EXEMPT = 3
    }
}

fun Question.toVaccinationStatusQuestion(date: LocalDate): VaccinationStatusQuestion =
    when (questionType) {
        VaccinationStatusQuestionType.ClinicalTrial -> ClinicalTrial(state)
        VaccinationStatusQuestionType.DoseDate -> DoseDate(state, date)
        VaccinationStatusQuestionType.FullyVaccinated -> FullyVaccinated(state)
        VaccinationStatusQuestionType.MedicallyExempt -> MedicallyExempt(state)
    }
