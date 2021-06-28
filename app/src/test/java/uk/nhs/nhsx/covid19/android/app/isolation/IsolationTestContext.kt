package uk.nhs.nhsx.covid19.android.app.isolation

import com.squareup.moshi.Moshi
import com.squareup.moshi.Moshi.Builder
import com.squareup.moshi.adapters.EnumJsonAdapter
import io.mockk.every
import io.mockk.mockk
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogStorage
import uk.nhs.nhsx.covid19.android.app.common.ResetIsolationStateIfNeeded
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.CreateSelfAssessmentIndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.state.StateStringStorage
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.util.adapters.ColorSchemeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.ContentBlockTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalMessageTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.PolicyIconAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.RiskyVenueMessageTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.TranslatableLocalMessageAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.TranslatableStringAdapter
import uk.nhs.riskscore.ObservationType
import uk.nhs.riskscore.ObservationType.gen
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class IsolationTestContext {
    private val stateStringStorage = InMemoryStateStringStorage()

    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxUnitFun = true)
    private val stateStorage = StateStorage(stateStringStorage, isolationConfigurationProvider, provideMoshi())

    init {
        every { isolationConfigurationProvider.durationDays } returns DurationDays()
    }

    private lateinit var localAuthority: String
    val clock: MockClock = MockClock()
    private val isolationStateMachine = IsolationStateMachine(
        stateStorage,
        notificationProvider = mockk(relaxUnitFun = true),
        isolationConfigurationProvider = isolationConfigurationProvider,
        unacknowledgedTestResultsProvider = mockk(relaxUnitFun = true),
        testResultIsolationHandler = TestResultIsolationHandler(
            CalculateKeySubmissionDateRange(
                isolationConfigurationProvider,
                clock
            ),
            CreateSelfAssessmentIndexCase(),
            clock
        ),
        storageBasedUserInbox = mockk(relaxUnitFun = true),
        isolationExpirationAlarmController = mockk(relaxUnitFun = true),
        clock = clock,
        analyticsEventTracker = mockk(relaxUnitFun = true),
        exposureNotificationHandler = mockk(relaxUnitFun = true),
        keySharingInfoProvider = mockk(relaxUnitFun = true),
        createSelfAssessmentIndexCase = CreateSelfAssessmentIndexCase(),
        trackTestResultAnalyticsOnReceive = mockk(relaxUnitFun = true),
        trackTestResultAnalyticsOnAcknowledge = mockk(relaxUnitFun = true),

    )

    private val resetStateIfNeeded = ResetIsolationStateIfNeeded(
        isolationStateMachine,
        unacknowledgedTestResultsProvider = mockk(relaxUnitFun = true),
        isolationConfigurationProvider,
        clock
    )

    private fun provideMoshi(): Moshi {
        return Builder()
            .add(LocalDateAdapter())
            .add(InstantAdapter())
            .add(TranslatableStringAdapter())
            .add(TranslatableLocalMessageAdapter())
            .add(PolicyIconAdapter())
            .add(ColorSchemeAdapter())
            .add(RiskyVenueMessageTypeAdapter())
            .add(LocalMessageTypeAdapter())
            .add(ContentBlockTypeAdapter())
            .add(
                ObservationType::class.java,
                EnumJsonAdapter.create(ObservationType::class.java).withUnknownFallback(gen)
            )
            .add(AnalyticsLogStorage.analyticsLogItemAdapter)
            .build()
    }

    fun setLocalAuthority(localAuthority: String) {
        this.localAuthority = localAuthority
    }

    fun getIsolationStateMachine(): IsolationStateMachine {
        return isolationStateMachine
    }

    fun getCurrentState(): IsolationState {
        return isolationStateMachine.readState()
    }

    fun getCurrentLogicalState(): IsolationLogicalState {
        return isolationStateMachine.readLogicalState()
    }

    fun getStateStringStorage(): StateStringStorage {
        return stateStringStorage
    }

    fun advanceClock(secondsToAdvance: Long) {
        clock.currentInstant = clock.instant().plusSeconds(secondsToAdvance)
        getCurrentState()

        resetStateIfNeeded()
    }

    fun sendExposureNotification(exposureDate: LocalDate) {
        val sendExposureNotification =
            SendExposureNotification(isolationStateMachine, isolationConfigurationProvider, clock)
        sendExposureNotification(exposureDate)
    }

    companion object {
        const val ENGLISH_LOCAL_AUTHORITY = "E07000063"
    }
}

class MockClock(var currentInstant: Instant? = null) : Clock() {

    override fun instant(): Instant = currentInstant ?: Instant.now()

    override fun withZone(zone: ZoneId?): Clock = this

    override fun getZone(): ZoneId = ZoneOffset.UTC

    fun reset() {
        currentInstant = null
    }
}

class InMemoryStateStringStorage : StateStringStorage {
    override var prefsValue: String? = null
}
