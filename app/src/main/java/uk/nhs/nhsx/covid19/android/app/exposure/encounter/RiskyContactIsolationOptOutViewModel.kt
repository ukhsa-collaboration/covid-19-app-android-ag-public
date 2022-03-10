package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.exposure.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.NEW_ADVICE
import javax.inject.Inject

class RiskyContactIsolationOptOutViewModel @Inject constructor(
    private val optOutOfContactIsolation: OptOutOfContactIsolation,
    private val acknowledgeRiskyContact: AcknowledgeRiskyContact,
) : ViewModel() {

    fun acknowledgeAndOptOutContactIsolation() {
        optOutOfContactIsolation(reason = NEW_ADVICE)
        acknowledgeRiskyContact()
    }
}
