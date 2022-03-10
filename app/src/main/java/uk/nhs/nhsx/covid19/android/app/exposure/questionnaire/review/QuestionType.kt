package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import uk.nhs.nhsx.covid19.android.app.R

sealed class QuestionType(
    @StringRes val question: Int,
    @StringRes val yesResponseText: Int,
    @StringRes val yesContentDescription: Int,
    @StringRes val noResponseText: Int,
    @StringRes val noContentDescription: Int
) : Parcelable {

    sealed class VaccinationStatusQuestionType(
        @StringRes question: Int,
        @StringRes yesResponseText: Int,
        @StringRes yesContentDescription: Int,
        @StringRes noResponseText: Int,
        @StringRes noContentDescription: Int
    ) : QuestionType(question, yesResponseText, yesContentDescription, noResponseText, noContentDescription) {

        @Parcelize
        object FullyVaccinated : VaccinationStatusQuestionType(
            question = R.string.exposure_notification_vaccination_status_all_doses_question,
            yesResponseText = R.string.exposure_notification_vaccination_status_all_doses_yes,
            yesContentDescription = R.string.exposure_notification_vaccination_status_all_doses_yes_content_description,
            noResponseText = R.string.exposure_notification_vaccination_status_all_doses_no,
            noContentDescription = R.string.exposure_notification_vaccination_status_all_doses_no_content_description
        )

        @Parcelize
        object DoseDate : VaccinationStatusQuestionType(
            question = R.string.exposure_notification_vaccination_status_date_question,
            yesResponseText = R.string.exposure_notification_vaccination_status_date_yes,
            yesContentDescription = R.string.exposure_notification_vaccination_status_date_yes_content_description,
            noResponseText = R.string.exposure_notification_vaccination_status_date_no,
            noContentDescription = R.string.exposure_notification_vaccination_status_date_no_content_description
        )

        @Parcelize
        object ClinicalTrial : VaccinationStatusQuestionType(
            question = R.string.exposure_notification_clinical_trial_question,
            yesResponseText = R.string.exposure_notification_clinical_trial_yes,
            yesContentDescription = R.string.exposure_notification_clinical_trial_yes_content_description,
            noResponseText = R.string.exposure_notification_clinical_trial_no,
            noContentDescription = R.string.exposure_notification_clinical_trial_no_content_description
        )

        @Parcelize
        object MedicallyExempt : VaccinationStatusQuestionType(
            question = R.string.exposure_notification_medically_exempt_question,
            yesResponseText = R.string.exposure_notification_medically_exempt_yes,
            yesContentDescription = R.string.exposure_notification_medically_exempt_yes_content_description,
            noResponseText = R.string.exposure_notification_medically_exempt_no,
            noContentDescription = R.string.exposure_notification_medically_exempt_no_content_description
        )
    }

    sealed class AgeLimitQuestionType(
        @StringRes question: Int,
        @StringRes yesResponseText: Int,
        @StringRes yesContentDescription: Int,
        @StringRes noResponseText: Int,
        @StringRes noContentDescription: Int
    ) : QuestionType(question, yesResponseText, yesContentDescription, noResponseText, noContentDescription) {

        @Parcelize
        object IsAdult : AgeLimitQuestionType(
            question = R.string.exposure_notification_age_subtitle_template,
            yesResponseText = R.string.exposure_notification_age_option1_text,
            yesContentDescription = R.string.exposure_notification_age_yes_content_description_template,
            noResponseText = R.string.exposure_notification_age_option2_text,
            noContentDescription = R.string.exposure_notification_age_no_content_description_template
        )
    }
}
