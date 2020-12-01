package uk.nhs.nhsx.covid19.android.app.state

import androidx.annotation.VisibleForTesting
import com.squareup.moshi.JsonClass
import com.tinder.StateMachine
import com.tinder.StateMachine.Transition
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.AcknowledgeTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import java.lang.Long.max
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val indexCaseOnsetDateBeforeTestResultDate: Long = 3
private const val indexCaseExpiryDateAfterTestResultDate: Long = 10

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
            val expiryDate: LocalDate
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

        fun hasExpired(clock: Clock, daysAgo: Int = 0): Boolean =
            !expiryDate.isAfter(LocalDate.now(clock).minusDays(daysAgo.toLong()))

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

                val isolationDayAtMax = isolationStart.plus(
                    isolationConfiguration.maxIsolation.toLong(),
                    ChronoUnit.DAYS
                ).atZone(ZoneOffset.UTC).toLocalDate()

                return selectEarliest(
                    isolationDayAtMax,
                    potentialExpiryDate!!
                )
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

        private fun selectEarliest(
            localDate1: LocalDate,
            localDate2: LocalDate
        ): LocalDate {
            return if (localDate1.isBefore(localDate2)) {
                localDate1
            } else {
                localDate2
            }
        }
    }
}

fun State.newStateWithTestResult(
    testResultsProvider: TestResultsProvider,
    isolationConfigurationProvider: IsolationConfigurationProvider,
    testResult: ReceivedTestResult,
    clock: Clock
): State {
    return if (this is Isolation && testResult.testResult == NEGATIVE) {
        when {
            testResultsProvider.isLastTestResultPositive() -> this
            this.isIndexCaseOnly() -> Default(previousIsolation = this)
            this.isBothCases() -> this.copy(indexCase = null)
            else -> this
        }
    } else if (this is Default && testResult.testResult == POSITIVE) {
        when {
            testResultsProvider.isLastTestResultNegative() -> tryCreateIndexCaseWhenOnsetDataIsNotProvided(
                this,
                isolationConfigurationProvider,
                testResult.testEndDate,
                false,
                clock
            )
            previousIsolationIsIndexCase() -> this
            else ->
                tryCreateIndexCaseWhenOnsetDataIsNotProvided(
                    this,
                    isolationConfigurationProvider,
                    testResult.testEndDate,
                    false,
                    clock
                )
        }
    } else {
        this
    }
}

fun Default.previousIsolationIsIndexCase() =
    this.previousIsolation != null && this.previousIsolation.isIndexCase()

private fun tryCreateIndexCaseWhenOnsetDataIsNotProvided(
    currentState: State,
    isolationConfigurationProvider: IsolationConfigurationProvider,
    testResultEndDate: Instant,
    selfAssessment: Boolean,
    clock: Clock
): State {
    val testResultDate = LocalDateTime.ofInstant(testResultEndDate, ZoneId.systemDefault()).toLocalDate()
    val isolation = Isolation(
        isolationStart = testResultEndDate,
        isolationConfiguration = isolationConfigurationProvider.durationDays,
        indexCase = IndexCase(
            symptomsOnsetDate = testResultDate.minusDays(indexCaseOnsetDateBeforeTestResultDate),
            expiryDate = testResultDate.plusDays(indexCaseExpiryDateAfterTestResultDate),
            selfAssessment = selfAssessment
        )
    )
    return if (isolation.hasExpired(clock)) {
        currentState
    } else {
        isolation
    }
}

@JsonClass(generateAdapter = true)
data class TestResult(
    val testEndDate: Instant,
    val result: VirologyTestResult
)

sealed class Event
data class OnExposedNotification(val exposureDate: Instant) : Event()
data class OnPositiveSelfAssessment(val onsetDate: SelectedDate) : Event()
data class OnTestResult(
    val testResult: ReceivedTestResult,
    val showNotification: Boolean = true
) : Event()

data class OnTestResultAcknowledge(
    val testResult: ReceivedTestResult,
    val removeTestResult: Boolean = false
) : Event()

private object OnExpired : Event()
private object OnReset : Event()

sealed class SideEffect {
    object SendExposedNotification : SideEffect()
    data class HandleTestResult(
        val testResult: ReceivedTestResult,
        val showNotification: Boolean = true
    ) : SideEffect()

    data class AcknowledgeTestResult(
        val testResult: ReceivedTestResult,
        val removeTestResult: Boolean = false
    ) : SideEffect()
}

fun IsolationStateMachine.remainingDaysInIsolation(state: State = readState(validateExpiry = true)): Long {
    return when (state) {
        is Default -> 0
        is Isolation -> daysUntil(state.expiryDate)
    }
}

val State.canOrderTest
    get() = if (this is Isolation) isIndexCase() else false

val State.canReportSymptoms
    get() = if (this is Isolation) isContactCaseOnly() else true

private fun daysUntil(date: LocalDate) = max(0, ChronoUnit.DAYS.between(LocalDate.now(), date))

@Singleton
class IsolationStateMachine(
    private val stateStorage: StateStorage,
    private val notificationProvider: NotificationProvider,
    private var clock: Clock,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val testResultsProvider: TestResultsProvider,
    private val userInbox: UserInbox,
    private val isolationExpirationAlarmController: IsolationExpirationAlarmController
) {

    @Inject
    constructor(
        stateStorage: StateStorage,
        notificationProvider: NotificationProvider,
        isolationConfigurationProvider: IsolationConfigurationProvider,
        testResultsProvider: TestResultsProvider,
        userInbox: UserInbox,
        isolationExpirationAlarmController: IsolationExpirationAlarmController
    ) : this(
        stateStorage,
        notificationProvider,
        Clock.systemDefaultZone(),
        isolationConfigurationProvider,
        testResultsProvider,
        userInbox,
        isolationExpirationAlarmController
    )

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

    @VisibleForTesting
    fun setClock(testClock: Clock) {
        clock = testClock
    }

    internal val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(stateStorage.state)
        state<Default> {
            on<OnExposedNotification> {
                val contactCaseIsolationDays =
                    getConfigurationDurations().contactCase
                val until = it.exposureDate.atZone(ZoneOffset.UTC).toLocalDate()
                    .plusDays(contactCaseIsolationDays.toLong())
                val now = Instant.now(clock)

                val isolation = Isolation(
                    contactCase = ContactCase(it.exposureDate, until),
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
                val nextState = handlePositiveSelfAssessment(it.onsetDate, previousIsolation = null)
                if (nextState != null) {
                    transitionTo(nextState)
                } else {
                    dontTransition()
                }
            }
            on<OnTestResult> {
                dontTransition(HandleTestResult(it.testResult, it.showNotification))
            }
            on<OnTestResultAcknowledge> {
                val updatedDefaultState = this.newStateWithTestResult(
                    testResultsProvider,
                    isolationConfigurationProvider,
                    it.testResult,
                    clock
                )
                transitionTo(
                    updatedDefaultState,
                    AcknowledgeTestResult(it.testResult, it.removeTestResult)
                )
            }
            on<OnReset> {
                transitionTo(Default())
            }
        }
        state<Isolation> {
            on<OnExposedNotification> {
                if (isIndexCaseOnly() && !hasPositiveResultAfter(this.indexCase!!.symptomsOnsetDate)) {
                    val expiryDate = it.exposureDate.plus(
                        getConfigurationDurations().contactCase.toLong(),
                        ChronoUnit.DAYS
                    ).atZone(ZoneOffset.UTC).toLocalDate()

                    val newState = this.copy(
                        contactCase = ContactCase(it.exposureDate, expiryDate)
                    )
                    transitionTo(newState, SendExposedNotification)
                } else {
                    dontTransition()
                }
            }
            on<OnPositiveSelfAssessment> {
                if (isContactCaseOnly()) {
                    val nextState =
                        handlePositiveSelfAssessment(it.onsetDate, previousIsolation = this)
                    if (nextState != null) {
                        transitionTo(nextState)
                    } else {
                        dontTransition()
                    }
                } else {
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
                val updatedDefaultState = this.newStateWithTestResult(
                    testResultsProvider,
                    isolationConfigurationProvider,
                    it.testResult,
                    clock
                )
                transitionTo(
                    updatedDefaultState,
                    AcknowledgeTestResult(it.testResult, it.removeTestResult)
                )
            }
            on<OnExpired> {
                transitionTo(Default(previousIsolation = this))
            }
            on<OnReset> {
                transitionTo(Default())
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
            }

            stateStorage.state = newState

            when (val sideEffect = validTransition.sideEffect) {
                SendExposedNotification -> {
                    notificationProvider.showExposureNotification()
                    userInbox.addUserInboxItem(ShowEncounterDetection)
                }
                is HandleTestResult -> {
                    testResultsProvider.add(sideEffect.testResult)
                    if (sideEffect.showNotification) {
                        notificationProvider.showTestResultsReceivedNotification()
                    }
                    userInbox.notifyChanges()
                }
                is AcknowledgeTestResult -> {
                    when (sideEffect.removeTestResult) {
                        true -> testResultsProvider.remove(sideEffect.testResult)
                        false -> testResultsProvider.acknowledge(sideEffect.testResult)
                    }
                }
            }

            when (newState) {
                is Isolation -> isolationExpirationAlarmController.setupExpirationCheck(newState.expiryDate)
                is Default -> {
                    isolationExpirationAlarmController.cancelExpirationCheckIfAny()

                    notificationProvider.cancelExposureNotification()
                    userInbox.clearItem(ShowEncounterDetection)
                }
            }
        }
    }

    private fun hasPositiveResultAfter(symptomsOnsetDate: LocalDate): Boolean {
        return testResultsProvider.testResults.values
            .filter {
                it.testEndDate.isAfter(
                    symptomsOnsetDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            }
            .any { it.testResult == POSITIVE }
    }

    private fun handlePositiveSelfAssessment(
        selectedDate: SelectedDate,
        previousIsolation: Isolation? = null
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

                    val finalExpiryDate = selectLatest(expiryDate, previousIsolation?.expiryDate)
                    addIndexCaseToCurrentIsolationOrCreateIsolationWithIndexCase(
                        previousIsolation,
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

                val finalExpiryDate = selectLatest(expiryDate, previousIsolation?.expiryDate)
                addIndexCaseToCurrentIsolationOrCreateIsolationWithIndexCase(
                    previousIsolation,
                    onsetDate,
                    finalExpiryDate,
                    selfAssessment = true
                )
            }
        }
    }

    private fun addIndexCaseToCurrentIsolationOrCreateIsolationWithIndexCase(
        previousIsolation: Isolation?,
        onsetDate: LocalDate,
        expiryDate: LocalDate,
        selfAssessment: Boolean
    ): State {
        return previousIsolation?.copy(
            indexCase = IndexCase(onsetDate, expiryDate, selfAssessment)
        )
            ?: Isolation(
                isolationStart = Instant.now(clock),
                indexCase = IndexCase(onsetDate, expiryDate, selfAssessment),
                isolationConfiguration = getConfigurationDurations()
            )
    }

    private fun selectEarliest(first: Instant, second: Instant): Instant =
        if (first.isBefore(second)) first else second

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
}
