package uk.nhs.nhsx.covid19.android.app.status.testinghub

import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.status.testinghub.EvaluateBookTestNavigation.NavigationTarget.BookPcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.EvaluateBookTestNavigation.NavigationTarget.SymptomsAfterRiskyVenue
import javax.inject.Inject

class EvaluateBookTestNavigation @Inject constructor(
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val evaluateVenueAlertNavigation: EvaluateVenueAlertNavigation
) {
    operator fun invoke(): NavigationTarget =
        if (lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk()) {
            evaluateVenueAlertNavigation().toBookATestNavigationTarget()
        } else {
            BookPcrTest
        }

    private fun EvaluateVenueAlertNavigation.NavigationTarget.toBookATestNavigationTarget() =
        when (this) {
            EvaluateVenueAlertNavigation.NavigationTarget.BookATest -> BookPcrTest
            EvaluateVenueAlertNavigation.NavigationTarget.SymptomsAfterRiskyVenue -> SymptomsAfterRiskyVenue
        }

    sealed class NavigationTarget {
        object BookPcrTest : NavigationTarget()
        object SymptomsAfterRiskyVenue : NavigationTarget()
    }
}
