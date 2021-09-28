package uk.nhs.nhsx.covid19.android.app.testhelpers.setup

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
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
                isolationConfiguration = DurationDays(),
                selfAssessment = isolationHelper.selfAssessment(),
                contact = isolationHelper.contact(exposureDate = exposureDate)
            )
        )
    }
}
