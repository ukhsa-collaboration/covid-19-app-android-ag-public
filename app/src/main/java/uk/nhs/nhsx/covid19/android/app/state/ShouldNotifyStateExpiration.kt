package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.DoNotNotify
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.Notify
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class ShouldNotifyStateExpiration @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val calculateExpirationNotificationTime: CalculateExpirationNotificationTime,
    private val clock: Clock
) {
    operator fun invoke(): ShouldNotifyStateExpirationResult {
        val currentState = isolationStateMachine.readLogicalState()
        return if (currentState is PossiblyIsolating && !currentState.hasAcknowledgedEndOfIsolation)
            shouldNotifyIsolationExpiration(currentState.expiryDate)
        else DoNotNotify
    }

    private fun shouldNotifyIsolationExpiration(expiryDate: LocalDate): ShouldNotifyStateExpirationResult {
        val notificationTime = calculateExpirationNotificationTime(expiryDate)
        val now = Instant.now(clock)

        return if (now.isEqualOrAfter(notificationTime)) Notify(expiryDate)
        else DoNotNotify
    }

    sealed class ShouldNotifyStateExpirationResult {
        data class Notify(val expiryDate: LocalDate) : ShouldNotifyStateExpirationResult()
        object DoNotNotify : ShouldNotifyStateExpirationResult()
    }
}
