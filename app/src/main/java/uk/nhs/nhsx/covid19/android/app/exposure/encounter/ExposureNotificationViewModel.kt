package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeroenmols.featureflag.framework.FeatureFlag.NEW_ENGLAND_CONTACT_CASE_JOURNEY
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.exposure.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.NewEnglandJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.QuestionnaireJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.ExposureNotificationAgeLimit
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.NewEnglandContactAdvice
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.ContinueIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.NEW_ADVICE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ExposureNotificationViewModel @Inject constructor(
    private val getRiskyContactEncounterDate: GetRiskyContactEncounterDate,
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val optOutOfContactIsolation: OptOutOfContactIsolation,
    private val acknowledgeRiskyContact: AcknowledgeRiskyContact,
) : ViewModel() {
    private val contactJourneyLiveData = MutableLiveData<ContactJourney>()
    val contactJourney: LiveData<ContactJourney> = contactJourneyLiveData

    private val navigationTarget = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTarget

    private val finishActivityLiveData = SingleLiveEvent<Void>()
    val finishActivity: LiveData<Void> = finishActivityLiveData

    fun updateViewState() {
        val encounterDate = getRiskyContactEncounterDate()

        if (encounterDate == null) {
            Timber.e("Could not get encounter date")
            finishActivityLiveData.postCall()
            return
        }
        viewModelScope.launch {
            contactJourneyLiveData.postValue(
                if (shouldShowNewEnglandJourney()) {
                    NewEnglandJourney(encounterDate)
                } else {
                    QuestionnaireJourney(
                        encounterDate = encounterDate,
                        shouldShowTestingAndIsolationAdvice = !isolationStateMachine.readLogicalState()
                            .isActiveIndexCase(clock)
                    )
                }
            )
        }
    }

    fun onPrimaryButtonClick() {
        viewModelScope.launch {
            navigationTarget.value = when {
                shouldShowNewEnglandJourney() && isolationStateMachine.readLogicalState().isActiveIndexCase(clock) -> {
                    optOutOfContactIsolation(reason = NEW_ADVICE)
                    acknowledgeRiskyContact()
                    ContinueIsolation
                }
                shouldShowNewEnglandJourney() -> NewEnglandContactAdvice
                else -> ExposureNotificationAgeLimit
            }
        }
    }

    private suspend fun shouldShowNewEnglandJourney(): Boolean =
        withContext(viewModelScope.coroutineContext) {
            RuntimeBehavior.isFeatureEnabled(NEW_ENGLAND_CONTACT_CASE_JOURNEY) && localAuthorityPostCodeProvider.requirePostCodeDistrict() == ENGLAND
        }

    sealed class ContactJourney(open val encounterDate: LocalDate) {
        data class QuestionnaireJourney(
            override val encounterDate: LocalDate,
            val shouldShowTestingAndIsolationAdvice: Boolean
        ) : ContactJourney(encounterDate)

        data class NewEnglandJourney(override val encounterDate: LocalDate) : ContactJourney(encounterDate)
    }

    sealed class NavigationTarget {
        object ExposureNotificationAgeLimit : NavigationTarget()
        object NewEnglandContactAdvice : NavigationTarget()
        object ContinueIsolation : NavigationTarget()
    }
}
