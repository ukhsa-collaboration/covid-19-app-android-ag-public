package uk.nhs.nhsx.covid19.android.app.state

import androidx.annotation.VisibleForTesting
import com.tinder.StateMachine
import com.tinder.StateMachine.Transition
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DeclaredNegativeResultFromDct
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventTracker
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
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleAcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult
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
private object OnDailyContactTestingOptIn : Event()
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
    private val analyticsEventTracker: AnalyticsEventTracker,
    private val exposureNotificationHandler: ExposureNotificationHandler,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val createSelfAssessmentIndexCase: CreateSelfAssessmentIndexCase,
    private val trackTestResultAnalyticsOnReceive: TrackTestResultAnalyticsOnReceive,
    private val trackTestResultAnalyticsOnAcknowledge: TrackTestResultAnalyticsOnAcknowledge,
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

                val exposureDay = it.exposureDate.toLocalDate(ZoneOffset.UTC)
                val potentialContactExpiryDate = exposureDay
                    .plusDays(isolationConfiguration.contactCase.toLong())

                val currentLogicalState = IsolationLogicalState.from(this)
                val contactCase = with(
                    ContactCase(
                        exposureDate = exposureDay,
                        notificationDate = LocalDate.now(clock),
                        expiryDate = potentialContactExpiryDate,
                        dailyContactTestingOptInDate = null
                    )
                ) {
                    this.copy(expiryDate = currentLogicalState.capExpiryDate(this))
                }

                if (contactCase.hasExpired(clock)) {
                    dontTransition()
                } else {
                    val newState = updateHasAcknowledgedEndOfIsolation(
                        currentState = this,
                        newState = copy(contactCase = contactCase)
                    )
                    transitionTo(newState, SendExposedNotification)
                }
            }

            on<OnPositiveSelfAssessment> {
                val isolationLogicalState = IsolationLogicalState.from(this)
                var newState = handlePositiveSelfAssessment(it.onsetDate, isolationLogicalState)
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
                val isolationLogicalState = IsolationLogicalState.from(this)
                val transition = testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationLogicalState,
                    it.testResult,
                    testAcknowledgedDate = Instant.now(clock)
                )
                val sideEffect = HandleAcknowledgedTestResult(isolationLogicalState, it.testResult, transition.keySharingInfo)
                when (transition) {
                    is TransitionDueToTestResult.Transition -> {
                        val newState = updateHasAcknowledgedEndOfIsolation(
                            currentState = this,
                            transition.newState
                        )
                        transitionTo(newState, sideEffect)
                    }
                    is TransitionDueToTestResult.DoNotTransition -> dontTransition(sideEffect)
                }
            }

            on<OnDailyContactTestingOptIn> {
                val isolationLogicalState = IsolationLogicalState.from(this)
                if (isolationLogicalState is PossiblyIsolating &&
                    isolationLogicalState.isActiveContactCaseOnly(clock)
                ) {
                    analyticsEventTracker.track(DeclaredNegativeResultFromDct)
                    var newState = this.copy(
                        contactCase = this.contactCase!!.copy(
                            expiryDate = LocalDate.now(clock),
                            dailyContactTestingOptInDate = LocalDate.now(clock)
                        )
                    )
                    newState = updateHasAcknowledgedEndOfIsolation(
                        currentState = this,
                        newState
                    )
                    transitionTo(newState)
                } else {
                    dontTransition()
                }
            }

            on<OnAcknowledgeIsolationExpiration> {
                when (IsolationLogicalState.from(this)) {
                    is PossiblyIsolating -> transitionTo(this.copy(hasAcknowledgedEndOfIsolation = true))
                    is NeverIsolating -> dontTransition()
                }
            }

            on<OnReset> {
                val isolationConfiguration = isolationConfigurationProvider.durationDays
                val isolationLogicalState = NeverIsolating(isolationConfiguration, negativeTest = null)
                transitionTo(isolationLogicalState.toIsolationState())
            }
        }

        onTransition {
            val validTransition = it as? Transition.Valid ?: return@onTransition

            val currentState = stateStorage.state
            val currentLogicalState = IsolationLogicalState.from(currentState)
            val newState = validTransition.toState
            val newLogicalState = IsolationLogicalState.from(newState)
            val willBeInIsolation = newLogicalState.isActiveIsolation(clock)

            if (newState == currentState) {
                Timber.d("no transition $currentState")
            } else {
                val isInIsolation = currentLogicalState.isActiveIsolation(clock)
                if (!isInIsolation && willBeInIsolation) {
                    analyticsEventTracker.track(StartedIsolation)
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
        return IsolationLogicalState.from(readState())
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

    fun optInToDailyContactTesting() {
        stateMachine.transition(OnDailyContactTestingOptIn)
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

        val currentLogicalState = IsolationLogicalState.from(currentState)
        val isInIsolation = currentLogicalState.isActiveIsolation(clock)

        val newLogicalState = IsolationLogicalState.from(newState)
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
        currentState: IsolationLogicalState
    ): IsolationState? {
        // If it should not be possible to report symptoms or onset date not stated, abort
        if (!currentState.canReportSymptoms(clock) || selectedDate == NotStated) {
            return null
        }

        val indexCase =
            if (currentState is PossiblyIsolating && currentState.hasActivePositiveTestResult(clock)) {
                val selfAssessment = SelfAssessment(
                    selfAssessmentDate = LocalDate.now(clock),
                    onsetDate = if (selectedDate is ExplicitDate) selectedDate.date else null
                )
                val testResult = currentState.toIsolationState().indexInfo!!.testResult!!

                if (selfAssessment.assumedOnsetDate.isAfter(testResult.testEndDate)) {
                    createSelfAssessmentIndexCase(currentState, selfAssessment, discardTestResultIfPresent = false)
                } else currentState.toIsolationState().indexInfo as? IndexCase
            } else {
                when (selectedDate) {
                    NotStated -> null
                    is ExplicitDate -> createSelfAssessmentIndexCase(
                        currentState,
                        onsetDate = selectedDate.date,
                        discardTestResultIfPresent = true
                    )
                    CannotRememberDate -> createSelfAssessmentIndexCase(
                        currentState,
                        onsetDate = null,
                        discardTestResultIfPresent = true
                    )
                }
            }
        return if (indexCase == null || indexCase.hasExpired(clock)) null
        else currentState.toIsolationState().copy(indexInfo = indexCase)
    }

    private fun createSelfAssessmentIndexCase(
        currentState: IsolationLogicalState,
        onsetDate: LocalDate?,
        discardTestResultIfPresent: Boolean
    ): IndexCase =
        createSelfAssessmentIndexCase(
            currentState,
            SelfAssessment(selfAssessmentDate = LocalDate.now(clock), onsetDate),
            discardTestResultIfPresent
        )
}
