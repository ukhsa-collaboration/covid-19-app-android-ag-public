package uk.nhs.nhsx.covid19.android.app.isolation

import uk.nhs.nhsx.covid19.android.app.state.CalculateContactExpiryDate
import uk.nhs.nhsx.covid19.android.app.state.CalculateIndexExpiryDate
import uk.nhs.nhsx.covid19.android.app.state.CalculateIndexStartDate
import uk.nhs.nhsx.covid19.android.app.state.CreateIsolationLogicalState
import java.time.Clock

/**
 * Create and return a new instance of [CreateIsolationLogicalState] without mocking any dependencies.
 */
fun createIsolationLogicalState(clock: Clock): CreateIsolationLogicalState =
    CreateIsolationLogicalState(
        CalculateContactExpiryDate(),
        CalculateIndexExpiryDate(clock),
        CalculateIndexStartDate()
    )
