package uk.nhs.nhsx.covid19.android.app.state

import androidx.annotation.VisibleForTesting
import com.tinder.StateMachine
import com.tinder.StateMachine.Transition
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.StorageBasedUserInbox
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleAcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.lang.Long.max
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class Event
data class OnExposedNotification(val exposureDate: Instant) : Event()
data class OnPositiveSelfAssessment(val onsetDate: SelectedDate) : Event()
data class OnTestResult(
    val testResult: ReceivedTestResult,
    val showNotification: Boolean = true,
    val testOrderType: TestOrderType,
) : Event()

data class OnTestResultAcknowledge(
    val testResult: ReceivedTestResult
) : Event()

private object OnReset : Event()
private data class OnOptOutOfContactIsolation(val encounterDate: LocalDate, val reason: OptOutReason) : Event()
private object OnAcknowledgeIsolationExpiration : Event()

sealed class SideEffect {
    object SendExposedNotification : SideEffect()

    data class HandleTestResult(
        val testResult: ReceivedTestResult,
        val showNotification: Boolean = true,
        val testOrderType: TestOrderType,
    ) : SideEffect()

    data class HandleAcknowledgedTestResult(
        val previousIsolation: IsolationLogicalState,
        val testResult: ReceivedTestResult,
        val keySharingInfo: KeySharingInfo?
    ) : SideEffect()
}

data class IsolationInfo(
    val selfAssessment: SelfAssessment? = null,
    val testResult: AcknowledgedTestResult? = null,
    val contact: Contact? = null,
    val hasAcknowledgedEndOfIsolation: Boolean = false
) {
    val assumedOnsetDateForExposureKeys: LocalDate? =
        when {
            testResult != null && testResult.isPositive() &&
                    (selfAssessment == null || testResult.testEndDate.isBefore(selfAssessment.assumedOnsetDate)) ->
                testResult.testEndDate
            selfAssessment != null ->
                selfAssessment.assumedOnsetDate
            else ->
                null
        }
}

@Singleton
class IsolationStateMachine @Inject constructor(
    private val stateStorage: StateStorage,
    private val notificationProvider: NotificationProvider,
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val testResultIsolationHandler: TestResultIsolationHandler,
    private val storageBasedUserInbox: StorageBasedUserInbox,
    private val isolationExpirationAlarmController: IsolationExpirationAlarmController,
    private val clock: Clock,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val exposureNotificationHandler: ExposureNotificationHandler,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val createIsolationLogicalState: CreateIsolationLogicalState,
    private val trackTestResultAnalyticsOnReceive: TrackTestResultAnalyticsOnReceive,
    private val trackTestResultAnalyticsOnAcknowledge: TrackTestResultAnalyticsOnAcknowledge,
    private val createIsolationState: CreateIsolationState
) {
    private var _stateMachine = createStateMachine()
    internal val stateMachine: StateMachine<IsolationInfo, Event, SideEffect>
        get() = _stateMachine

    private fun createStateMachine() = StateMachine.create<IsolationInfo, Event, SideEffect> {
        initialState(stateStorage.state.toIsolationInfo())
        state<IsolationInfo> {
            on<OnExposedNotification> {
                if (!isInterestedInExposureNotifications()) {
                    return@on dontTransition()
                }

                val newInfo = copy(
                    contact = Contact(
                        exposureDate = it.exposureDate.toLocalDate(ZoneOffset.UTC),
                        notificationDate = LocalDate.now(clock),
                        optOutOfContactIsolation = null
                    )
                )

                val newLogicalState = createIsolationLogicalState(newInfo)

                if (newLogicalState.isActiveContactCase(clock)) {
                    val newInfoWithAcknowledgement = updateHasAcknowledgedEndOfIsolation(
                        currentInfo = this,
                        newInfo = newInfo
                    )
                    transitionTo(newInfoWithAcknowledgement, SendExposedNotification)
                } else {
                    dontTransition()
                }
            }

            on<OnPositiveSelfAssessment> {
                var newInfo = handlePositiveSelfAssessment(it.onsetDate, currentInfo = this)
                if (newInfo != null) {
                    newInfo = updateHasAcknowledgedEndOfIsolation(
                        currentInfo = this,
                        newInfo
                    )
                    transitionTo(newInfo)
                } else {
                    dontTransition()
                }
            }

            on<OnTestResult> {
                dontTransition(HandleTestResult(it.testResult, it.showNotification, it.testOrderType))
            }

            on<OnTestResultAcknowledge> {
                val transition = testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    currentState = createIsolationState(this),
                    receivedTestResult = it.testResult,
                    testAcknowledgedDate = Instant.now(clock)
                )
                val sideEffect = HandleAcknowledgedTestResult(
                    previousIsolation = createIsolationLogicalState(this),
                    testResult = it.testResult,
                    keySharingInfo = transition.keySharingInfo
                )
                when (transition) {
                    is TransitionDueToTestResult.Transition -> {
                        val newLogicalState = createIsolationLogicalState(transition.newIsolationInfo)
                        val hasExpiredIsolation = newLogicalState is PossiblyIsolating &&
                                !newLogicalState.isActiveIsolation(clock)
                        val newInfo =
                            transition.newIsolationInfo.copy(hasAcknowledgedEndOfIsolation = hasExpiredIsolation)
                        transitionTo(newInfo, sideEffect)
                    }
                    is TransitionDueToTestResult.DoNotTransition -> dontTransition(sideEffect)
                }
            }

            on<OnOptOutOfContactIsolation> { optOutEvent ->
                if (contact != null && contact.optOutOfContactIsolation == null) {
                    var newInfo = this.copy(
                        contact = contact.copy(
                            optOutOfContactIsolation = OptOutOfContactIsolation(
                                date = optOutEvent.encounterDate,
                                reason = optOutEvent.reason
                            )
                        )
                    )
                    newInfo = updateHasAcknowledgedEndOfIsolation(currentInfo = this, newInfo)
                    transitionTo(newInfo)
                } else {
                    dontTransition()
                }
            }

            on<OnAcknowledgeIsolationExpiration> {
                when (createIsolationLogicalState(this)) {
                    is PossiblyIsolating -> transitionTo(this.copy(hasAcknowledgedEndOfIsolation = true))
                    is NeverIsolating -> dontTransition()
                }
            }

            on<OnReset> {
                transitionTo(IsolationInfo())
            }
        }

        onTransition {
            val validTransition = it as? Transition.Valid ?: return@onTransition

            val currentIsolationInfo = stateStorage.state.toIsolationInfo()
            val currentLogicalState = createIsolationLogicalState(currentIsolationInfo)
            val newIsolationInfo = validTransition.toState
            val newLogicalState = createIsolationLogicalState(newIsolationInfo)
            val willBeInIsolation = newLogicalState.isActiveIsolation(clock)

            if (newIsolationInfo == currentIsolationInfo) {
                Timber.d("no transition $currentIsolationInfo")
            } else {
                val isInIsolation = currentLogicalState.isActiveIsolation(clock)
                if (!isInIsolation && willBeInIsolation) {
                    analyticsEventProcessor.track(StartedIsolation)
                }
                Timber.d("transition from $currentIsolationInfo to $newIsolationInfo")
            }

            stateStorage.state = createIsolationState(newIsolationInfo)

            when (val sideEffect = validTransition.sideEffect) {
                SendExposedNotification -> {
                    exposureNotificationHandler.show()
                }
                is HandleTestResult -> {
                    unacknowledgedTestResultsProvider.add(sideEffect.testResult)
                    if (sideEffect.showNotification) {
                        notificationProvider.showTestResultsReceivedNotification()
                    }
                    storageBasedUserInbox.notifyChanges()
                    trackTestResultAnalyticsOnReceive(sideEffect.testResult, sideEffect.testOrderType)
                }
                is HandleAcknowledgedTestResult -> {
                    trackTestResultAnalyticsOnAcknowledge(sideEffect.previousIsolation, sideEffect.testResult)
                    unacknowledgedTestResultsProvider.remove(sideEffect.testResult)
                    if (sideEffect.keySharingInfo != null) {
                        keySharingInfoProvider.keySharingInfo = sideEffect.keySharingInfo
                    }
                }
            }

            if (willBeInIsolation) {
                isolationExpirationAlarmController.setupExpirationCheck(currentLogicalState, newLogicalState)
            } else {
                isolationExpirationAlarmController.cancelExpirationCheckIfAny()
                exposureNotificationHandler.cancel()
            }
        }
    }

    private fun createIsolationLogicalState(isolationInfo: IsolationInfo): IsolationLogicalState {
        return createIsolationLogicalState(createIsolationState(isolationInfo))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun invalidateStateMachine() {
        _stateMachine = createStateMachine()
    }

    fun readState(): IsolationState = synchronized(this) {
        return createIsolationState(stateMachine.state)
    }

    fun readLogicalState(): IsolationLogicalState = synchronized(this) {
        return createIsolationLogicalState(readState())
    }

    fun processEvent(event: Event): Transition<IsolationInfo, Event, SideEffect> = synchronized(this) {
        return stateMachine.transition(event)
    }

    fun isInterestedInExposureNotifications(): Boolean {
        return when (val state = readLogicalState()) {
            is NeverIsolating -> true
            is PossiblyIsolating ->
                if (state.hasExpired(clock)) true
                else state.isActiveIndexCaseOnly(clock) && !state.hasActiveConfirmedPositiveTestResult(clock)
        }
    }

    fun reset() {
        stateMachine.transition(OnReset)
    }

    fun optOutOfContactIsolation(encounterDate: LocalDate, reason: OptOutReason) {
        stateMachine.transition(OnOptOutOfContactIsolation(encounterDate, reason))
    }

    fun acknowledgeIsolationExpiration() {
        stateMachine.transition(OnAcknowledgeIsolationExpiration)
    }

    fun remainingDaysInIsolation(state: IsolationLogicalState = readLogicalState()): Long {
        return when (state) {
            is NeverIsolating -> 0
            is PossiblyIsolating -> daysUntil(state.expiryDate)
        }
    }

    private fun daysUntil(date: LocalDate) = max(0, ChronoUnit.DAYS.between(LocalDate.now(clock), date))

    private fun updateHasAcknowledgedEndOfIsolation(
        currentInfo: IsolationInfo,
        newInfo: IsolationInfo
    ): IsolationInfo {
        if (currentInfo == newInfo) {
            return newInfo
        }

        val currentLogicalState = createIsolationLogicalState(currentInfo)
        val isInIsolation = currentLogicalState.isActiveIsolation(clock)

        val newLogicalState = createIsolationLogicalState(newInfo)
        val willBeInIsolation = newLogicalState.isActiveIsolation(clock)

        return if (!isInIsolation && willBeInIsolation)
            newInfo.copy(hasAcknowledgedEndOfIsolation = false)
        else if (isInIsolation && !willBeInIsolation)
            newInfo.copy(hasAcknowledgedEndOfIsolation = true)
        else
            newInfo
    }

    private fun handlePositiveSelfAssessment(
        selectedDate: SelectedDate,
        currentInfo: IsolationInfo
    ): IsolationInfo? {
        val currentLogicalState = createIsolationLogicalState(currentInfo)

        // If it should not be possible to report symptoms or onset date not stated, abort
        if (!currentLogicalState.canReportSymptoms(clock) || selectedDate == NotStated) {
            return null
        }

        val selfAssessmentDate = LocalDate.now(clock)

        val newInfo =
            if (currentLogicalState.hasActivePositiveTestResult(clock)) {
                val selfAssessment = SelfAssessment(
                    selfAssessmentDate = selfAssessmentDate,
                    onsetDate = if (selectedDate is ExplicitDate) selectedDate.date else null
                )

                if (selfAssessment.assumedOnsetDate.isAfter(currentInfo.testResult!!.testEndDate)) {
                    currentInfo.copy(selfAssessment = selfAssessment)
                } else {
                    currentInfo
                }
            } else {
                when (selectedDate) {
                    NotStated -> null
                    is ExplicitDate ->
                        currentInfo.copy(
                            selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate = selectedDate.date),
                            testResult = null
                        )
                    CannotRememberDate ->
                        currentInfo.copy(
                            selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate = null),
                            testResult = null
                        )
                }
            }

        if (newInfo == null) {
            return null
        }

        val newLogicalState = createIsolationLogicalState(newInfo)

        return if (newLogicalState.isActiveIndexCase(clock)) newInfo
        else null
    }

    fun isActiveContactCaseOnly(clock: Clock): Boolean = readLogicalState().isActiveContactCaseOnly(clock)
}
