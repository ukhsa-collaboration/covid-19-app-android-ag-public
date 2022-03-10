package uk.nhs.nhsx.covid19.android.app.testhelpers.setup

import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.HasTestAppContext
import java.time.LocalDate

interface IsolationSetupHelper : HasTestAppContext {
    val isolationHelper: IsolationHelper

    fun givenNeverInIsolation() {
        testAppContext.setState(isolationHelper.neverInIsolation())
    }

    fun givenContactIsolation(exposureDaysAgo: Long = 2) {
        val exposureDate = LocalDate.now(testAppContext.clock).minusDays(exposureDaysAgo)
        testAppContext.setState(isolationHelper.contact(exposureDate = exposureDate).asIsolation())
    }

    fun givenSelfAssessmentIsolation() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())
    }

    fun givenSelfAssessmentAndContactIsolation(exposureDaysAgo: Long = 2) {
        val exposureDate = LocalDate.now(testAppContext.clock).minusDays(exposureDaysAgo)
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = IsolationConfiguration(),
                selfAssessment = isolationHelper.selfAssessment(),
                contact = isolationHelper.contact(exposureDate = exposureDate)
            )
        )
    }

    fun givenOptedOutOfContactIsolation(
        exposureDaysAgo: Long = 2,
        optedOutDaysAgo: Long = 1,
        optOutReason: OptOutReason
    ) {
        val exposureDate = LocalDate.now(testAppContext.clock).minusDays(exposureDaysAgo)
        val optedOutDate = LocalDate.now(testAppContext.clock).minusDays(optedOutDaysAgo)
        testAppContext.setState(
            isolationHelper.contactWithOptOutDate(
                exposureDate = exposureDate,
                optOutOfContactIsolation = optedOutDate,
                optOutReason = optOutReason
            ).asIsolation()
        )
    }
}
