package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.JsonClass
import com.tinder.StateMachine
import com.tinder.StateMachine.Transition
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendTestResultNotification
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import java.lang.Long.max
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class State {
    data class Default(val previousIsolation: Isolation? = null) : State()

    data class Isolation(
        val isolationStart: Instant,
        val expiryDate: LocalDate,
        val indexCase: IndexCase? = null,
        val contactCase: ContactCase? = null
    ) : State() {
        @JsonClass(generateAdapter = true)
        data class IndexCase(
            val symptomsOnsetDate: LocalDate,
            val testResult: TestResult? = null
        )

        @JsonClass(generateAdapter = true)
        data class ContactCase(val startDate: Instant)

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
data class OnPositiveTestResult(val testDate: Instant) : Event()
data class OnNegativeTestResult(val testDate: Instant) : Event()
private object OnExpired : Event()
private object OnReset : Event()

sealed class SideEffect {
    object SendExposedNotification : SideEffect()
    object SendTestResultNotification : SideEffect()
}

fun IsolationStateMachine.remainingDaysInIsolation(): Long {
    return when (val state = readState(validateExpiry = true)) {
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
    private val clock: Clock,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val userInbox: UserInbox,
    private val isolationExpirationAlarmController: IsolationExpirationAlarmController
) {

    @Inject
    constructor(
        stateStorage: StateStorage,
        notificationProvider: NotificationProvider,
        isolationConfigurationProvider: IsolationConfigurationProvider,
        userInbox: UserInbox,
        isolationExpirationAlarmController: IsolationExpirationAlarmController
    ) : this(
        stateStorage,
        notificationProvider,
        Clock.systemDefaultZone(),
        isolationConfigurationProvider,
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
        val hasExpired = when (val state = stateMachine.state) {
            is Default -> false
            is Isolation -> hasExpired(state.expiryDate)
        }

        if (hasExpired) {
            stateMachine.transition(OnExpired)
        }
    }

    private fun hasExpired(until: LocalDate): Boolean {
        return !LocalDate.now(clock).isBefore(until)
    }

    fun reset() {
        stateMachine.transition(OnReset)
    }

    internal val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(stateStorage.state)
        val now = Instant.now(clock)
        state<Default> {
            on<OnExposedNotification> {
                val contactCaseIsolationDays =
                    getConfigurationDurations().contactCase
                val until = it.exposureDate.atZone(ZoneOffset.UTC).toLocalDate()
                    .plusDays(contactCaseIsolationDays.toLong())
                transitionTo(
                    Isolation(
                        contactCase = ContactCase(now),
                        isolationStart = now,
                        expiryDate = until
                    ),
                    SendExposedNotification
                )
            }
            on<OnPositiveSelfAssessment> {
                val nextState = handlePositiveSelfAssessment(it.onsetDate, previousIsolation = null)
                if (nextState != null) {
                    transitionTo(nextState)
                } else {
                    dontTransition()
                }
            }
            on<OnPositiveTestResult> {
                val updatedDefaultState = this.withUpdatedTestResult(it.testDate, POSITIVE)
                transitionTo(updatedDefaultState, SendTestResultNotification)
            }
            on<OnNegativeTestResult> {
                val updatedDefaultState = this.withUpdatedTestResult(it.testDate, NEGATIVE)
                transitionTo(updatedDefaultState, SendTestResultNotification)
            }
            on<OnReset> {
                transitionTo(Default())
            }
        }
        state<Isolation> {
            on<OnExposedNotification> {
                if (isIndexCaseOnly() && indexCase?.testResult?.result != POSITIVE) {
                    val newExpiryDate = selectEarliest(
                        isolationStart.plus(
                            getConfigurationDurations().maxIsolation.toLong(),
                            ChronoUnit.DAYS
                        ),
                        it.exposureDate.plus(
                            getConfigurationDurations().contactCase.toLong(),
                            ChronoUnit.DAYS
                        )
                    ).atZone(ZoneOffset.UTC).toLocalDate()

                    val newState = this.copy(
                        contactCase = ContactCase(now),
                        expiryDate = newExpiryDate
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
                    dontTransition()
                }
            }
            on<OnPositiveTestResult> { event ->
                val newState = this.withUpdatedTestResult(event.testDate, POSITIVE)
                transitionTo(newState, SendTestResultNotification)
            }
            on<OnNegativeTestResult> { event ->
                if (isIndexCaseOnly()) {
                    val lastIsolation = this.withUpdatedTestResult(event.testDate, NEGATIVE)
                    transitionTo(
                        Default(previousIsolation = lastIsolation),
                        SendTestResultNotification
                    )
                } else {
                    val updatedIsolation = this.withUpdatedTestResult(event.testDate, NEGATIVE)
                    transitionTo(updatedIsolation, SendTestResultNotification)
                }
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

            val newState = validTransition.toState
            stateStorage.state = newState

            when (validTransition.sideEffect) {
                SendExposedNotification -> {
                    notificationProvider.showExposureNotification()
                    userInbox.addUserInboxItem(ShowEncounterDetection)
                }
                SendTestResultNotification -> {
                    notificationProvider.showTestResultsReceivedNotification()
                    userInbox.addUserInboxItem(ShowTestResult)
                }
            }

            when (newState) {
                is Isolation -> isolationExpirationAlarmController.setupExpirationCheck(newState.expiryDate)
                is Default -> isolationExpirationAlarmController.cancelExpirationCheckIfAny()
            }
        }
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
                        finalExpiryDate
                    )
                }
            }
            CannotRememberDate -> {
                getConfigurationDurations().indexCaseSinceSelfDiagnosisUnknownOnset
                val today = LocalDate.now(clock)
                val isolationDays = getIndexCaseIsolationDurationUnknownOnset()
                val expiryDate = today.plusDays(isolationDays.toLong())
                val onsetDate = today.minusDays(2)

                val finalExpiryDate = selectLatest(expiryDate, previousIsolation?.expiryDate)
                addIndexCaseToCurrentIsolationOrCreateIsolationWithIndexCase(
                    previousIsolation,
                    onsetDate,
                    finalExpiryDate
                )
            }
        }
    }

    private fun addIndexCaseToCurrentIsolationOrCreateIsolationWithIndexCase(
        previousIsolation: Isolation?,
        onsetDate: LocalDate,
        expiryDate: LocalDate
    ): State? {
        return previousIsolation?.copy(
            indexCase = IndexCase(onsetDate),
            expiryDate = expiryDate
        )
            ?: Isolation(
                isolationStart = Instant.now(clock),
                indexCase = IndexCase(onsetDate),
                expiryDate = expiryDate
            )
    }

    private fun State.withUpdatedTestResult(
        testEndDate: Instant,
        virologyTestResult: VirologyTestResult
    ): State {
        return when (this) {
            is Default -> copy(
                previousIsolation = previousIsolation?.withUpdatedTestResult(
                    testEndDate,
                    virologyTestResult
                )
            )
            is Isolation -> withUpdatedTestResult(testEndDate, virologyTestResult)
        }
    }

    private fun Isolation.withUpdatedTestResult(
        testEndDate: Instant,
        virologyTestResult: VirologyTestResult
    ): Isolation {
        val indexCaseWithTestResult = indexCase?.copy(
            testResult = TestResult(testEndDate, virologyTestResult)
        )
        return copy(indexCase = indexCaseWithTestResult)
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
