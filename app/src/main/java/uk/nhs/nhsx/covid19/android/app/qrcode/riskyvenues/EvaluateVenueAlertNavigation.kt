package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget.BookATest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget.SymptomsAfterRiskyVenue
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import javax.inject.Inject

class EvaluateVenueAlertNavigation @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock
) {
    operator fun invoke(): NavigationTarget =
        if (isolationStateMachine.readLogicalState().isActiveIndexCase(clock)) {
            BookATest
        } else {
            SymptomsAfterRiskyVenue
        }

    sealed class NavigationTarget {
        object BookATest : NavigationTarget()
        object SymptomsAfterRiskyVenue : NavigationTarget()
        object Finish : NavigationTarget()
    }
}
