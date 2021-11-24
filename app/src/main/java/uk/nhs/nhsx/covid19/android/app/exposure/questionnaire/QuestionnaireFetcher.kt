package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.QuestionNode
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.SelectionOutcome.Completion
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.SelectionOutcome.FollowupQuestion
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.ClinicalTrial
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome

interface QuestionnaireFetcher {
    suspend fun create(): QuestionNode
}

class EnglishQuestionnaireFetcher : QuestionnaireFetcher {

    override suspend fun create(): QuestionNode {
        return QuestionNode(
            FullyVaccinated,
            yes = FollowupQuestion(
                QuestionNode(
                    DoseDate,
                    yes = Completion(QuestionnaireOutcome.FullyVaccinated),
                    no = FollowupQuestion(
                        QuestionNode(
                            ClinicalTrial,
                            yes = Completion(QuestionnaireOutcome.FullyVaccinated),
                            no = FollowupQuestion(
                                QuestionNode(
                                    MedicallyExempt,
                                    yes = Completion(QuestionnaireOutcome.MedicallyExempt),
                                    no = Completion(QuestionnaireOutcome.NotExempt)
                                ),
                            )
                        )
                    )
                )
            ),
            no = FollowupQuestion(
                QuestionNode(
                    MedicallyExempt,
                    yes = Completion(QuestionnaireOutcome.MedicallyExempt),
                    no = FollowupQuestion(
                        QuestionNode(
                            ClinicalTrial,
                            yes = Completion(QuestionnaireOutcome.FullyVaccinated),
                            no = Completion(QuestionnaireOutcome.NotExempt)
                        )
                    )
                )
            )
        )
    }
}

class WelshQuestionnaireFetcher : QuestionnaireFetcher {
    override suspend fun create() = QuestionNode(
        FullyVaccinated,
        yes = FollowupQuestion(
            QuestionNode(
                DoseDate,
                yes = Completion(QuestionnaireOutcome.FullyVaccinated),
                no = FollowupQuestion(
                    QuestionNode(
                        ClinicalTrial,
                        yes = Completion(QuestionnaireOutcome.FullyVaccinated),
                        no = Completion(QuestionnaireOutcome.NotExempt)
                    )
                )
            )
        ),
        no = FollowupQuestion(
            QuestionNode(
                ClinicalTrial,
                yes = Completion(QuestionnaireOutcome.FullyVaccinated),
                no = Completion(QuestionnaireOutcome.NotExempt)
            )
        )
    )
}
