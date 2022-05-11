package uk.nhs.nhsx.covid19.android.app.isolation

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Moshi.Builder
import com.squareup.moshi.adapters.EnumJsonAdapter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogStorage
import uk.nhs.nhsx.covid19.android.app.common.ResetIsolationStateIfNeeded
import uk.nhs.nhsx.covid19.android.app.util.adapters.SerializeNulls
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.CalculateIndexExpiryDate
import uk.nhs.nhsx.covid19.android.app.state.CreateIsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.state.StateStorage.Companion.ISOLATION_STATE_KEY
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.WouldTestIsolationEndBeforeOrOnStartOfExistingIsolation
import uk.nhs.nhsx.covid19.android.app.util.adapters.ColorSchemeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.ContentBlockTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalInformationAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalMessageTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.PolicyIconAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.RiskyVenueMessageTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.TranslatableStringAdapter
import uk.nhs.riskscore.ObservationType
import uk.nhs.riskscore.ObservationType.gen
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class IsolationTestContext {

    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxUnitFun = true)
    private val getLatestConfiguration = mockk<GetLatestConfiguration>(relaxUnitFun = true)
    private val sharedPreferences: SharedPreferences = mockk()
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider = mockk()
    private val createIsolationConfiguration: CreateIsolationConfiguration = CreateIsolationConfiguration(localAuthorityPostCodeProvider)

    private val stateStorage =
        StateStorage(isolationConfigurationProvider, provideMoshi(), sharedPreferences, createIsolationConfiguration)

    init {
        val durationDays = DurationDays()
        every { isolationConfigurationProvider.durationDays } returns durationDays
        val countrySpecificConfiguration = mockk<CountrySpecificConfiguration>()
        every { getLatestConfiguration() } returns countrySpecificConfiguration
        every { countrySpecificConfiguration.contactCase } returns durationDays.england.contactCase
        every { countrySpecificConfiguration.pendingTasksRetentionPeriod } returns durationDays.england.pendingTasksRetentionPeriod
        every { sharedPreferences.edit() } returns mockk(relaxed = true)
        every { sharedPreferences.all[ISOLATION_STATE_KEY] } returns null
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND
    }

    private lateinit var localAuthority: String
    val clock: MockClock = MockClock()
    private val isolationStateMachine = IsolationStateMachine(
        stateStorage = stateStorage,
        notificationProvider = mockk(relaxUnitFun = true),
        unacknowledgedTestResultsProvider = mockk(relaxUnitFun = true),
        testResultIsolationHandler = TestResultIsolationHandler(
            CalculateKeySubmissionDateRange(
                getLatestConfiguration,
                clock
            ),
            WouldTestIsolationEndBeforeOrOnStartOfExistingIsolation(
                CalculateIndexExpiryDate(clock),
                clock
            ),
            createIsolationLogicalState(clock),
            clock
        ),
        storageBasedUserInbox = mockk(relaxUnitFun = true),
        isolationExpirationAlarmController = mockk(relaxUnitFun = true),
        clock = clock,
        analyticsEventProcessor = mockk(relaxUnitFun = true),
        exposureNotificationHandler = mockk(relaxUnitFun = true),
        keySharingInfoProvider = mockk(relaxUnitFun = true),
        createIsolationLogicalState = createIsolationLogicalState(clock),
        createIsolationState = createIsolationState(
            stateStorage,
            isolationConfigurationProvider,
            createIsolationConfiguration
        ),
        trackTestResultAnalyticsOnReceive = mockk(relaxUnitFun = true),
        trackTestResultAnalyticsOnAcknowledge = mockk(relaxUnitFun = true),
    )

    private val resetStateIfNeeded = ResetIsolationStateIfNeeded(
        isolationStateMachine,
        unacknowledgedTestResultsProvider = mockk(relaxUnitFun = true),
        getLatestConfiguration,
        clock
    )

    private fun provideMoshi(): Moshi {
        return Builder()
            .add(LocalDateAdapter())
            .add(InstantAdapter())
            .add(TranslatableStringAdapter())
            .add(LocalInformationAdapter())
            .add(PolicyIconAdapter())
            .add(ColorSchemeAdapter())
            .add(RiskyVenueMessageTypeAdapter())
            .add(LocalMessageTypeAdapter())
            .add(ContentBlockTypeAdapter())
            .add(
                ObservationType::class.java,
                EnumJsonAdapter.create(ObservationType::class.java).withUnknownFallback(gen)
            )
            .add(SerializeNulls.jsonAdapterFactory)
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

    fun setStateStringStorage(json: String) {
        every { sharedPreferences.all[ISOLATION_STATE_KEY] } returns json
    }

    fun advanceClock(secondsToAdvance: Long) {
        clock.currentInstant = clock.instant().plusSeconds(secondsToAdvance)
        getCurrentState()

        resetStateIfNeeded()
    }

    fun sendExposureNotification(exposureDate: LocalDate) {
        val sendExposureNotification =
            SendExposureNotification(isolationStateMachine, getLatestConfiguration, clock)
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
