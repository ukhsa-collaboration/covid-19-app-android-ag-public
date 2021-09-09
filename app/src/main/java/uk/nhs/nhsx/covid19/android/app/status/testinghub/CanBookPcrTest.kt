package uk.nhs.nhsx.covid19.android.app.status.testinghub

import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class CanBookPcrTest @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val clock: Clock
) {
    operator fun invoke(): Boolean =
        lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() ||
                isolationStateMachine.readLogicalState().isActiveIsolation(clock) ||
                isWithinOptedOutContactIsolationPeriod()

    private fun isWithinOptedOutContactIsolationPeriod(): Boolean {
        val isolationState = isolationStateMachine.readState()
        val optOutDate = isolationState.contact?.optOutOfContactIsolation?.date ?: return false

        val contactCaseIsolationDuration = isolationState.isolationConfiguration.contactCase.toLong()
        val firstDayWhereCannotBookPcrTest = optOutDate
            .plusDays(contactCaseIsolationDuration)
        return firstDayWhereCannotBookPcrTest.isAfter(LocalDate.now(clock))
    }
}
