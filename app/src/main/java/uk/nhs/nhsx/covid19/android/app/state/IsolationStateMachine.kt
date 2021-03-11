package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.JsonClass
import com.tinder.StateMachine
import com.tinder.StateMachine.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DeclaredNegativeResultFromDct
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.GLOBAL_SCOPE
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.AcknowledgeTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.ClearAcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransitionButStoreTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.TestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultChecker
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultHandler
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import java.lang.Long.max
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

sealed class State {
    data class Default(val previousIsolation: Isolation? = null) : State()

    data class Isolation(
        val isolationStart: Instant,
        val isolationConfiguration: DurationDays,
        val indexCase: IndexCase? = null,
        val contactCase: ContactCase? = null
    ) : State() {
        @JsonClass(generateAdapter = true)
        data class IndexCase(
            val symptomsOnsetDate: LocalDate,
            val expiryDate: LocalDate,
            val selfAssessment: Boolean
        )

        @JsonClass(generateAdapter = true)
        data class ContactCase(
            val startDate: Instant,
            val notificationDate: Instant?,
            val expiryDate: LocalDate,
            val dailyContactTestingOptInDate: LocalDate? = null,
        )

        fun isContactCaseOnly(): Boolean =
            contactCase != null && indexCase == null

        fun isIndexCaseOnly(): Boolean =
            indexCase != null && contactCase == null

        fun isIndexCase(): Boolean =
            indexCase != null

        fun isContactCase(): Boolean =
            contactCase != null

        fun isBothCases(): Boolean =
            isIndexCase() && isContactCase()

        fun isSelfAssessmentIndexCase(): Boolean =
            indexCase != null && indexCase.selfAssessment

        fun hasExpired(clock: Clock, daysAgo: Int = 0): Boolean {
            val today = LocalDate.now(clock)
            return expiryDate.isBeforeOrEqual(today.minusDays(daysAgo.toLong()))
        }

        fun capExpiryDate(potentialExpiryDate: LocalDate): LocalDate {
            val latestPossibleExpiryDate = isolationStart.plus(
                isolationConfiguration.maxIsolation.toLong(),
                ChronoUnit.DAYS
            ).atZone(ZoneOffset.UTC).toLocalDate()
            return selectEarliest(latestPossibleExpiryDate, potentialExpiryDate)
        }

        val lastDayOfIsolation: LocalDate = expiryDate.minusDays(1)

        val expiryDate: LocalDate
            get() {
                val potentialExpiryDate = when {
                    isBothCases() -> {
                        latestExpiryDate(indexCase!!, contactCase!!)
                    }
                    isIndexCaseOnly() -> {
                        indexCase!!.expiryDate
                    }
                    isContactCaseOnly() -> {
                        contactCase!!.expiryDate
                    }
                    else -> {
                        Timber.e("Unknown expiryDate")
                        LocalDate.now().plusDays(1)
                    }
                }
                return capExpiryDate(potentialExpiryDate)
            }

        private fun latestExpiryDate(
            indexCase: IndexCase,
            contactCase: ContactCase
        ): LocalDate {
            return if (indexCase.expiryDate.isAfter(contactCase.expiryDate)) {
                indexCase.expiryDate
            } else {
                contactCase.expiryDate
            }
        }
    }
}

sealed class Event
data class OnExposedNotification(val exposureDate: Instant) : Event()
data class OnPositiveSelfAssessment(val onsetDate: SelectedDate) : Event()
data class OnTestResult(
    val testResult: ReceivedTestResult,
    val showNotification: Boolean = true
) : Event()

data class OnTestResultAcknowledge(
    val testResult: ReceivedTestResult
) : Event()

private object OnExpired : Event()
private object OnReset : Event()
private object OnPreviousIsolationOutdated : Event()
private object OnDailyContactTestingOptIn : Event()

sealed class SideEffect {
    object SendExposedNotification : SideEffect()

    data class HandleTestResult(
        val testResult: ReceivedTestResult,
        val showNotification: Boolean = true
    ) : SideEffect()

    data class AcknowledgeTestResult(
        val testResult: ReceivedTestResult,
        val testResultStorageOperation: TestResultStorageOperation
    ) : SideEffect()

    object ClearAcknowledgedTestResult : SideEffect()
}

val State.canReportSymptoms
    get() = if (this is Isolation) isContactCaseOnly() else true

@Singleton
class IsolationStateMachine @Inject constructor(
    private val stateStorage: StateStorage,
    private val notificationProvider: NotificationProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val relevantTestResultProvider: RelevantTestResultProvider,
    private val testResultHandler: TestResultHandler,
    private val testResultIsolationHandler: TestResultIsolationHandler,
    private val userInbox: UserInbox,
    private val isolationExpirationAlarmController: IsolationExpirationAlarmController,
    private val clock: Clock,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    @Named(GLOBAL_SCOPE) private val analyticsEventScope: CoroutineScope,
    private val exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController
) {

    fun readState(validateExpiry: Boolean = true): State = synchronized(this) {
        if (validateExpiry) {
            validateExpiry()
        }
        return stateMachine.state
    }

    fun processEvent(event: Event): Transition<State, Event, SideEffect> =
        synchronized(this) {
            validateExpiry()
            return stateMachine.transition(event)
        }

    fun isInterestedInExposureNotifications(): Boolean {
        return when (val state = stateMachine.state) {
            is Default -> true
            is Isolation -> state.isIndexCaseOnly() && !state.hasConfirmedPositiveTestResult(testResultHandler)
        }
    }

    private fun validateExpiry() {
        when (val state = stateMachine.state) {
            is Default -> {
                val expiryDays = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
                if (state.previousIsolation?.hasExpired(clock, daysAgo = expiryDays) == true)
                    stateMachine.transition(OnReset)
            }
            is Isolation ->
                if (state.hasExpired(clock))
                    stateMachine.transition(OnExpired)
        }
    }

    fun reset() {
        stateMachine.transition(OnReset)
    }

    fun clearPreviousIsolation() {
        stateMachine.transition(OnPreviousIsolationOutdated)
    }

    fun optInToDailyContactTesting() {
        stateMachine.transition(OnDailyContactTestingOptIn)
    }

    fun remainingDaysInIsolation(state: State = readState(validateExpiry = true)): Long {
        return when (state) {
            is Default -> 0
            is Isolation -> daysUntil(state.expiryDate)
        }
    }

    private fun daysUntil(date: LocalDate) = max(0, ChronoUnit.DAYS.between(LocalDate.now(clock), date))

    internal val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(stateStorage.state)
        state<Default> {
            on<OnExposedNotification> {
                if (!isInterestedInExposureNotifications()) {
                    return@on dontTransition()
                }

                val contactCaseIsolationDays =
                    getConfigurationDurations().contactCase
                val until = it.exposureDate.atZone(ZoneOffset.UTC).toLocalDate()
                    .plusDays(contactCaseIsolationDays.toLong())
                val now = Instant.now(clock)

                val isolation = Isolation(
                    contactCase = ContactCase(
                        startDate = it.exposureDate,
                        notificationDate = now,
                        expiryDate = until,
                        dailyContactTestingOptInDate = null,
                    ),
                    isolationStart = now,
                    isolationConfiguration = getConfigurationDurations()
                )
                val today = now.atZone(ZoneOffset.UTC).toLocalDate()
                if (isolation.expiryDate.isAfter(today)) {
                    transitionTo(isolation, SendExposedNotification)
                } else {
                    dontTransition()
                }
            }
            on<OnPositiveSelfAssessment> {
                val nextState = handlePositiveSelfAssessment(it.onsetDate, currentIsolation = null)
                if (nextState != null) {
                    transitionTo(nextState, ClearAcknowledgedTestResult)
                } else {
                    dontTransition()
                }
            }
            on<OnTestResult> {
                dontTransition(HandleTestResult(it.testResult, it.showNotification))
            }
            on<OnTestResultAcknowledge> {
                when (
                    val transition =
                        testResultIsolationHandler.computeTransitionWithTestResult(this, it.testResult)
                ) {
                    is TransitionDueToTestResult.TransitionAndStoreTestResult -> transitionTo(
                        transition.newState,
                        AcknowledgeTestResult(it.testResult, transition.testResultStorageOperation)
                    )
                    is DoNotTransitionButStoreTestResult -> dontTransition(
                        AcknowledgeTestResult(it.testResult, transition.testResultStorageOperation)
                    )
                    is Ignore -> dontTransition(
                        AcknowledgeTestResult(it.testResult, TestResultStorageOperation.Ignore)
                    )
                }
            }
            on<OnReset> {
                transitionTo(Default())
            }
            on<OnPreviousIsolationOutdated> {
                transitionTo(Default(previousIsolation = null))
            }
        }
        state<Isolation> {
            on<OnExposedNotification> {
                if (!isInterestedInExposureNotifications()) {
                    dontTransition()
                } else {
                    val expiryDateBasedOnExposure = it.exposureDate.plus(
                        getConfigurationDurations().contactCase.toLong(),
                        ChronoUnit.DAYS
                    ).atZone(ZoneOffset.UTC).toLocalDate()

                    val expiryDate = capExpiryDate(expiryDateBasedOnExposure)

                    val newState = this.copy(
                        contactCase = ContactCase(
                            startDate = it.exposureDate,
                            notificationDate = Instant.now(clock),
                            expiryDate = expiryDate,
                            dailyContactTestingOptInDate = null
                        )
                    )

                    transitionTo(newState, SendExposedNotification)
                }
            }
            on<OnPositiveSelfAssessment> {
                if (isContactCaseOnly()) {
                    val nextState = handlePositiveSelfAssessment(it.onsetDate, currentIsolation = this)
                    if (nextState != null) {
                        transitionTo(nextState, ClearAcknowledgedTestResult)
                    } else {
                        dontTransition()
                    }
                } else {
                    // TODO this shouldn't be possible. Review
                    indexCase?.let { case ->
                        val newState = this.copy(indexCase = case.copy(selfAssessment = true))
                        transitionTo(newState)
                    } ?: dontTransition()
                }
            }
            on<OnTestResult> {
                dontTransition(HandleTestResult(it.testResult, it.showNotification))
            }
            on<OnTestResultAcknowledge> {
                when (
                    val transition =
                        testResultIsolationHandler.computeTransitionWithTestResult(this, it.testResult)
                ) {
                    is TransitionDueToTestResult.TransitionAndStoreTestResult -> transitionTo(
                        transition.newState,
                        AcknowledgeTestResult(it.testResult, transition.testResultStorageOperation)
                    )
                    is DoNotTransitionButStoreTestResult -> dontTransition(
                        AcknowledgeTestResult(it.testResult, transition.testResultStorageOperation)
                    )
                    is Ignore -> dontTransition(
                        AcknowledgeTestResult(it.testResult, TestResultStorageOperation.Ignore)
                    )
                }
            }
            on<OnExpired> {
                transitionTo(Default(previousIsolation = this))
            }
            on<OnReset> {
                transitionTo(Default())
            }
            on<OnDailyContactTestingOptIn> {
                if (this.isContactCaseOnly()) {
                    trackAnalyticsEvent(DeclaredNegativeResultFromDct)
                    transitionTo(
                        Default(
                            previousIsolation = this.copy(
                                contactCase = this.contactCase!!.copy(
                                    expiryDate = LocalDate.now(clock),
                                    dailyContactTestingOptInDate = LocalDate.now(clock)
                                )
                            )
                        )
                    )
                } else {
                    dontTransition()
                }
            }
        }
        onTransition {
            val validTransition = it as? Transition.Valid ?: return@onTransition

            val currentState = stateStorage.state
            val newState = validTransition.toState
            if (newState == currentState) {
                Timber.d("no transition $currentState")
            } else {
                Timber.d("transition from $currentState to $newState")
                if (currentState is Default && newState is Isolation) {
                    trackAnalyticsEvent(StartedIsolation)
                }
            }

            stateStorage.state = newState

            when (val sideEffect = validTransition.sideEffect) {
                SendExposedNotification -> {
                    userInbox.addUserInboxItem(ShowEncounterDetection)
                    notificationProvider.showExposureNotification()
                    exposureNotificationRetryAlarmController.setupNextAlarm()
                }
                is HandleTestResult -> {
                    testResultHandler.onTestResultReceived(sideEffect.testResult)
                    if (sideEffect.showNotification) {
                        notificationProvider.showTestResultsReceivedNotification()
                    }
                    userInbox.notifyChanges()
                    if (sideEffect.testResult.testResult == VirologyTestResult.POSITIVE &&
                        sideEffect.testResult.requiresConfirmatoryTest
                    ) {
                        trackAnalyticsEvent(AnalyticsEvent.ReceivedUnconfirmedPositiveTestResult)
                    }
                }
                is AcknowledgeTestResult -> {
                    testResultHandler.acknowledge(sideEffect.testResult, sideEffect.testResultStorageOperation)
                }
                ClearAcknowledgedTestResult -> {
                    relevantTestResultProvider.clear()
                }
            }

            when (newState) {
                is Isolation -> isolationExpirationAlarmController.setupExpirationCheck(newState.expiryDate)
                is Default -> {
                    isolationExpirationAlarmController.cancelExpirationCheckIfAny()
                    notificationProvider.cancelExposureNotification()
                    userInbox.clearItem(ShowEncounterDetection)
                    exposureNotificationRetryAlarmController.cancel()
                }
            }
        }
    }

    private fun handlePositiveSelfAssessment(
        selectedDate: SelectedDate,
        currentIsolation: Isolation? = null
    ): State? {
        return when (selectedDate) {
            NotStated -> null
            is ExplicitDate -> {
                val onsetDate = selectedDate.date
                val today = LocalDate.now(clock)
                val daysBetween = ChronoUnit.DAYS.between(onsetDate, today)
                val isolationDays = getIndexCaseIsolationDurationSinceOnset()
                if (daysBetween >= isolationDays) {
                    null
                } else {
                    val expiryDate = today.plus(isolationDays - daysBetween, ChronoUnit.DAYS)

                    val finalExpiryDate = selectLatest(expiryDate, currentIsolation?.expiryDate)
                    addIndexCaseToCurrentIsolationOrCreateIsolationWithIndexCase(
                        currentIsolation,
                        onsetDate,
                        finalExpiryDate,
                        selfAssessment = true
                    )
                }
            }
            CannotRememberDate -> {
                val today = LocalDate.now(clock)
                val isolationDays = getIndexCaseIsolationDurationUnknownOnset()
                val expiryDate = today.plusDays(isolationDays.toLong())
                val onsetDate = today.minusDays(2)

                val finalExpiryDate = selectLatest(expiryDate, currentIsolation?.expiryDate)
                addIndexCaseToCurrentIsolationOrCreateIsolationWithIndexCase(
                    currentIsolation,
                    onsetDate,
                    finalExpiryDate,
                    selfAssessment = true
                )
            }
        }
    }

    private fun addIndexCaseToCurrentIsolationOrCreateIsolationWithIndexCase(
        currentIsolation: Isolation?,
        onsetDate: LocalDate,
        expiryDate: LocalDate,
        selfAssessment: Boolean
    ): State {
        return currentIsolation?.copy(
            indexCase = IndexCase(onsetDate, expiryDate, selfAssessment)
        )
            ?: Isolation(
                isolationStart = Instant.now(clock),
                indexCase = IndexCase(onsetDate, expiryDate, selfAssessment),
                isolationConfiguration = getConfigurationDurations()
            )
    }

    private fun selectLatest(
        calculatedExpiryDate: LocalDate,
        previousExpiryDate: LocalDate?
    ): LocalDate {
        return if (previousExpiryDate != null && previousExpiryDate.isAfter(calculatedExpiryDate)) {
            previousExpiryDate
        } else {
            calculatedExpiryDate
        }
    }

    private fun getIndexCaseIsolationDurationUnknownOnset(): Int {
        val isolationDurations = getConfigurationDurations()
        return isolationDurations.indexCaseSinceSelfDiagnosisUnknownOnset
    }

    private fun getIndexCaseIsolationDurationSinceOnset(): Int {
        val isolationDurations = getConfigurationDurations()
        return isolationDurations.indexCaseSinceSelfDiagnosisOnset
    }

    private fun getConfigurationDurations() = isolationConfigurationProvider.durationDays

    private fun trackAnalyticsEvent(event: AnalyticsEvent) {
        analyticsEventScope.launch {
            analyticsEventProcessor.track(event)
        }
    }
}

fun Isolation.testBelongsToIsolation(testResult: TestResult): Boolean =
    isolationStart.isBeforeOrEqual(testResult.testEndDate)

fun Isolation.hasPositiveTestResult(testResultChecker: TestResultChecker): Boolean =
    testResultChecker.hasTestResultMatching { testResult ->
        testResult.isPositive() &&
            testBelongsToIsolation(testResult)
    }

fun Isolation.hasUnconfirmedPositiveTestResult(testResultChecker: TestResultChecker): Boolean =
    testResultChecker.hasTestResultMatching { testResult ->
        testResult.isPositive() &&
            !testResult.isConfirmed() &&
            testBelongsToIsolation(testResult)
    }

fun Isolation.hasConfirmedPositiveTestResult(testResultChecker: TestResultChecker): Boolean =
    testResultChecker.hasTestResultMatching { testResult ->
        testResult.isPositive() &&
            testResult.isConfirmed() &&
            testBelongsToIsolation(testResult)
    }
