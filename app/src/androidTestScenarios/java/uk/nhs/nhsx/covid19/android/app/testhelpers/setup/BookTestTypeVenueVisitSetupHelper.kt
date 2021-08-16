package uk.nhs.nhsx.covid19.android.app.testhelpers.setup

import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.HasTestAppContext
import java.time.LocalDate

interface BookTestTypeVenueVisitSetupHelper : HasTestAppContext {
    fun givenNoBookTestTypeVenueVisitStored() {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = null
    }

    fun givenBookTestTypeVenueVisitStored() {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue =
            LastVisitedBookTestTypeVenueDate(
                LocalDate.now(testAppContext.clock),
                RiskyVenueConfigurationDurationDays()
            )
    }
}
