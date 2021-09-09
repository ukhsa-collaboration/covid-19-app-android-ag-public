package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import java.time.LocalDate
import javax.inject.Inject

class CalculateContactExpiryDate @Inject constructor() {

    operator fun invoke(contact: Contact, isolationConfiguration: DurationDays): LocalDate =
        contact.optOutOfContactIsolation?.date
            ?: contact.exposureDate.plusDays(isolationConfiguration.contactCase.toLong())
}
