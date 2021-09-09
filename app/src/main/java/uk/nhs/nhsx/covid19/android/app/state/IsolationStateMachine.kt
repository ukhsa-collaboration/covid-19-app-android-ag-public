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
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleAcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubReminderAlarmController
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.ScheduleIsolationHubReminder
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
private data class OnOptOutOfContactIsolation(val encounterDate: LocalDate) : Event()
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

@Singleton
class IsolationStateMachine @Inject constructor(
    private val stateStorage: StateStorage,
    private val notificationProvider: NotificationProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
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
    private val scheduleIsolationHubReminder: ScheduleIsolationHubReminder,
    private val isolationHubReminderAlarmController: IsolationHubReminderAlarmController,
) {
    private var _stateMachine = createStateMachine()
    internal val stateMachine: StateMachine<IsolationState, Event, SideEffect>
        get() = _stateMachine

    private fun createStateMachine() = StateMachine.create<IsolationState, Event, SideEffect> {
        initialState(stateStorage.state)
        state<IsolationState> {
            on<OnExposedNotification> {
                if (!isInterestedInExposureNotifications()) {
                    return@on dontTransition()
                }

                val newState = copy(
                    contact = Contact(
                        exposureDate = it.exposureDate.toLocalDate(ZoneOffset.UTC),
                        notificationDate = LocalDate.now(clock),
                        optOutOfContactIsolation = null
                    )
                )
                val newLogicalState = createIsolationLogicalState(newState)

                if (newLogicalState.isActiveContactCase(clock)) {
                    val newStateWithAcknowledgement = updateHasAcknowledgedEndOfIsolation(
                        currentState = this,
                        newState = newState
                    )
                    transitionTo(newStateWithAcknowledgement, SendExposedNotification)
                } else {
                    dontTransition()
                }
            }

            on<OnPositiveSelfAssessment> {
                var newState = handlePositiveSelfAssessment(it.onsetDate, currentState = this)
                if (newState != null) {
                    newState = updateHasAcknowledgedEndOfIsolation(
                        currentState = this,
                        newState
                    )
                    transitionTo(newState)
                } else {
                    dontTransition()
                }
            }

            on<OnTestResult> {
                dontTransition(HandleTestResult(it.testResult, it.showNotification, it.testOrderType))
            }

            on<OnTestResultAcknowledge> {
                val transition = testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    currentState = this,
                    it.testResult,
                    testAcknowledgedDate = Instant.now(clock)
                )
                val sideEffect = HandleAcknowledgedTestResult(
                    createIsolationLogicalState(this),
                    it.testResult,
                    transition.keySharingInfo
                )
                when (transition) {
                    is TransitionDueToTestResult.Transition -> {
                        val newLogicalState = createIsolationLogicalState(transition.newState)
                        val hasExpiredIsolation = newLogicalState is PossiblyIsolating &&
                                !newLogicalState.isActiveIsolation(clock)
                        val newState = transition.newState.copy(hasAcknowledgedEndOfIsolation = hasExpiredIsolation)
                        transitionTo(newState, sideEffect)
                    }
                    is TransitionDueToTestResult.DoNotTransition -> dontTransition(sideEffect)
                }
            }

            on<OnOptOutOfContactIsolation> {
                var newState = this.copy(
                    contact = this.contact!!.copy(
                        optOutOfContactIsolation = OptOutOfContactIsolation(date = it.encounterDate)
                    )
                )
                newState = updateHasAcknowledgedEndOfIsolation(currentState = this, newState)
                transitionTo(newState)
            }

            on<OnAcknowledgeIsolationExpiration> {
                when (createIsolationLogicalState(this)) {
                    is PossiblyIsolating -> transitionTo(this.copy(hasAcknowledgedEndOfIsolation = true))
                    is NeverIsolating -> dontTransition()
                }
            }

            on<OnReset> {
                val isolationConfiguration = isolationConfigurationProvider.durationDays
                val isolationState = IsolationState(isolationConfiguration)
                transitionTo(isolationState)
            }
        }

        onTransition {
            val validTransition = it as? Transition.Valid ?: return@onTransition

            val currentState = stateStorage.state
            val currentLogicalState = createIsolationLogicalState(currentState)
            val newState = validTransition.toState
            val newLogicalState = createIsolationLogicalState(newState)
            val willBeInIsolation = newLogicalState.isActiveIsolation(clock)

            if (newState == currentState) {
                Timber.d("no transition $currentState")
            } else {
                val isInIsolation = currentLogicalState.isActiveIsolation(clock)
                if (!isInIsolation && willBeInIsolation) {
                    analyticsEventProcessor.track(StartedIsolation)
                    /*
                    If the user has started isolation as an index case, they have already acknowledged the start of
                    the isolation so we schedule the reminder. For contact case, they are put into isolation immediately
                    after receiving the exposure notification - they haven't acknowledged the start of the isolation yet
                    so in that case we do not schedule the reminder.
                     */
                    if (newLogicalState.isActiveIndexCase(clock)) {
                        scheduleIsolationHubReminder()
                    }
                }
                Timber.d("transition from $currentState to $newState")
            }

            stateStorage.state = newState

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
                isolationHubReminderAlarmController.cancel()
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun invalidateStateMachine() {
        _stateMachine = createStateMachine()
    }

    fun readState(): IsolationState = synchronized(this) {
        return stateMachine.state
    }

    fun readLogicalState(): IsolationLogicalState = synchronized(this) {
        return createIsolationLogicalState(readState())
    }

    fun processEvent(event: Event): Transition<IsolationState, Event, SideEffect> = synchronized(this) {
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

    fun optOutOfContactIsolation(encounterDate: LocalDate) {
        stateMachine.transition(OnOptOutOfContactIsolation(encounterDate))
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
        currentState: IsolationState,
        newState: IsolationState
    ): IsolationState {
        if (currentState == newState) {
            return newState
        }

        val currentLogicalState = createIsolationLogicalState(currentState)
        val isInIsolation = currentLogicalState.isActiveIsolation(clock)

        val newLogicalState = createIsolationLogicalState(newState)
        val willBeInIsolation = newLogicalState.isActiveIsolation(clock)

        return if (!isInIsolation && willBeInIsolation)
            newState.copy(hasAcknowledgedEndOfIsolation = false)
        else if (isInIsolation && !willBeInIsolation)
            newState.copy(hasAcknowledgedEndOfIsolation = true)
        else
            newState
    }

    private fun handlePositiveSelfAssessment(
        selectedDate: SelectedDate,
        currentState: IsolationState
    ): IsolationState? {
        val currentLogicalState = createIsolationLogicalState(currentState)

        // If it should not be possible to report symptoms or onset date not stated, abort
        if (!currentLogicalState.canReportSymptoms(clock) || selectedDate == NotStated) {
            return null
        }

        val selfAssessmentDate = LocalDate.now(clock)

        val newState =
            if (currentLogicalState.hasActivePositiveTestResult(clock)) {
                val selfAssessment = SelfAssessment(
                    selfAssessmentDate = selfAssessmentDate,
                    onsetDate = if (selectedDate is ExplicitDate) selectedDate.date else null
                )

                if (selfAssessment.assumedOnsetDate.isAfter(currentState.testResult!!.testEndDate)) {
                    currentState.copy(selfAssessment = selfAssessment)
                } else {
                    currentState
                }
            } else {
                when (selectedDate) {
                    NotStated -> null
                    is ExplicitDate ->
                        currentState.copy(
                            selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate = selectedDate.date),
                            testResult = null
                        )
                    CannotRememberDate ->
                        currentState.copy(
                            selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate = null),
                            testResult = null
                        )
                }
            }

        if (newState == null) {
            return null
        }

        val newLogicalState = createIsolationLogicalState(newState)

        return if (newLogicalState.isActiveIndexCase(clock)) newState
        else null
    }
}
