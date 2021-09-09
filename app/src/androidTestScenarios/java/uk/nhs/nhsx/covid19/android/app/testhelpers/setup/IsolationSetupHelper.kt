package uk.nhs.nhsx.covid19.android.app.testhelpers.setup

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.HasTestAppContext

interface IsolationSetupHelper : HasTestAppContext {
    val isolationHelper: IsolationHelper

    fun givenNeverInIsolation() {
        testAppContext.setState(isolationHelper.neverInIsolation())
    }

    fun givenContactIsolation() {
        testAppContext.setState(isolationHelper.contact().asIsolation())
    }

    fun givenSelfAssessmentIsolation() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())
    }

    fun givenSelfAssessmentAndContactIsolation() {
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                selfAssessment = isolationHelper.selfAssessment(),
                contact = isolationHelper.contact()
            )
        )
    }
}
