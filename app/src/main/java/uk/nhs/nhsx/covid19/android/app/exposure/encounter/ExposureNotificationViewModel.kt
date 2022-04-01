package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_ENGLAND_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.exposure.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.NewEnglandJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.NewWalesJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ContactJourney.QuestionnaireJourney
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.ExposureNotificationAgeLimit
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.NavigationTarget.NewContactJourney
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
                when {
                    shouldShowNewContactJourneyEngland() -> NewEnglandJourney(encounterDate)
                    shouldShowNewContactJourneyWales() -> NewWalesJourney(encounterDate)
                    else -> QuestionnaireJourney(
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
                shouldShowNewContactJourneyEngland() && isolationStateMachine.readLogicalState().isActiveIndexCase(clock) -> {
                    optOutOfContactIsolation(reason = NEW_ADVICE)
                    acknowledgeRiskyContact()
                    ContinueIsolation
                }
                shouldShowNewContactJourneyWales() && isolationStateMachine.readLogicalState().isActiveIndexCase(clock) -> {
                    optOutOfContactIsolation(reason = NEW_ADVICE)
                    acknowledgeRiskyContact()
                    ContinueIsolation
                }
                shouldShowNewContactJourneyEngland() -> NewContactJourney
                shouldShowNewContactJourneyWales() -> NewContactJourney
                else -> ExposureNotificationAgeLimit
            }
        }
    }

    private suspend fun shouldShowNewContactJourneyEngland(): Boolean =
        withContext(viewModelScope.coroutineContext) {
            !RuntimeBehavior.isFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) &&
            localAuthorityPostCodeProvider.requirePostCodeDistrict() == ENGLAND
        }

    private suspend fun shouldShowNewContactJourneyWales(): Boolean =
        withContext(viewModelScope.coroutineContext) {
            !RuntimeBehavior.isFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) &&
                    localAuthorityPostCodeProvider.requirePostCodeDistrict() == WALES
        }

    sealed class ContactJourney(open val encounterDate: LocalDate) {
        data class QuestionnaireJourney(
            override val encounterDate: LocalDate,
            val shouldShowTestingAndIsolationAdvice: Boolean
        ) : ContactJourney(encounterDate)

        data class NewEnglandJourney(override val encounterDate: LocalDate) : ContactJourney(encounterDate)

        data class NewWalesJourney(override val encounterDate: LocalDate) : ContactJourney(encounterDate)
    }

    sealed class NavigationTarget {
        object ExposureNotificationAgeLimit : NavigationTarget()
        object NewContactJourney : NavigationTarget()
        object ContinueIsolation : NavigationTarget()
    }
}
