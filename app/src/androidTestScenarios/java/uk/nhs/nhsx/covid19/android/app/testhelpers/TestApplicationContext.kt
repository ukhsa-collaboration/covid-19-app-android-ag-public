/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.testhelpers

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.work.WorkManager
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.tinder.StateMachine
import kotlinx.coroutines.test.TestCoroutineScope
import uk.nhs.covid19.config.Configurations
import uk.nhs.covid19.config.SignatureKey
import uk.nhs.nhsx.covid19.android.app.ExposureApplication
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationChecker
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTask
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationBroadcastReceiver
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.MockRandomNonRiskyExposureWindowsLimiter
import uk.nhs.nhsx.covid19.android.app.flow.analytics.awaitSuccess
import uk.nhs.nhsx.covid19.android.app.packagemanager.MockPackageManager
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.permissions.MockPermissionsManager
import uk.nhs.nhsx.covid19.android.app.qrcode.MockBarcodeDetectorBuilder
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenues
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.MockAnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockEpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.MockIsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.MockIsolationPaymentApi
import uk.nhs.nhsx.covid19.android.app.remote.MockKeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockLocalMessagesApi
import uk.nhs.nhsx.covid19.android.app.remote.MockQuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.additionalInterceptors
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.state.Event
import uk.nhs.nhsx.covid19.android.app.state.IsolationInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.MigrateIsolationState
import uk.nhs.nhsx.covid19.android.app.state.SideEffect
import uk.nhs.nhsx.covid19.android.app.status.DateChangeBroadcastReceiver
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork
import uk.nhs.nhsx.covid19.android.app.util.AndroidStrongBoxSupport
import uk.nhs.nhsx.covid19.android.app.util.EncryptedSharedPreferencesUtils
import uk.nhs.nhsx.covid19.android.app.util.EncryptedStorage
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils
import uk.nhs.nhsx.covid19.android.app.util.MockUUIDGenerator
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationRetryChecker
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationRetryStorage
import uk.nhs.nhsx.covid19.android.app.util.getPrivateProperty
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

const val AWAIT_AT_MOST_SECONDS: Long = 10

class TestApplicationContext {

    val app: ExposureApplication = ApplicationProvider.getApplicationContext()
    val riskyVenuesApi = MockRiskyVenuesApi()

    val clock = MockClock()

    val virologyTestingApi = MockVirologyTestingApi(clock)

    val isolationPaymentApi = MockIsolationPaymentApi()

    val questionnaireApi = MockQuestionnaireApi()

    val keysSubmissionApi = MockKeysSubmissionApi()

    val analyticsApi = MockAnalyticsApi()

    val localMessagesApi = MockLocalMessagesApi()

    val updateManager = TestUpdateManager()

    val permissionsManager = MockPermissionsManager()

    val packageManager = MockPackageManager()

    val barcodeDetectorProvider = MockBarcodeDetectorBuilder()

    val epidemiologyDataApi = MockEpidemiologyDataApi()

    val randomNonRiskyExposureWindowsLimiter = MockRandomNonRiskyExposureWindowsLimiter()

    val uuidGenerator = MockUUIDGenerator()

    val mockIsolationConfigurationApi = MockIsolationConfigurationApi()

    internal val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val exposureNotificationApi = MockExposureNotificationApi(app, clock, simulateGoogleEN = false)

    private val bluetoothStateProvider = TestBluetoothStateProvider()

    private val locationStateProvider = TestLocationStateProvider()

    private val batteryOptimizationChecker = TestBatteryOptimizationChecker()

    private val encryptionUtils = EncryptionUtils(AndroidStrongBoxSupport)
    private val encryptedSharedPreferencesUtils = EncryptedSharedPreferencesUtils(encryptionUtils)
    internal val encryptedStorage = EncryptedStorage.from(
        app,
        StrongBoxMigrationRetryChecker(
            StrongBoxMigrationRetryStorage(
                encryptedSharedPreferencesUtils.createGenericEncryptedSharedPreferences(
                    app,
                    encryptionUtils.getDefaultMasterKey(),
                    SharedPrefsDelegate.migrationSharedPreferencesFileName
                )
            )
        ),
        encryptionUtils
    )

    private val signatureKey = SignatureKey(
        id = "3",
        pemRepresentation =
        """
            -----BEGIN PUBLIC KEY-----
            MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEVs/o5+uQbTjL3chynL4wXgUg2R9
            q9UU8I5mEovUf86QZ7kOBIjJwqnzD1omageEHWwHdBO6B+dFabmdT9POxg==
            -----END PUBLIC KEY-----
            """.trimIndent()
    )

    private val component: TestAppComponent = DaggerTestAppComponent.builder()
        .appModule(
            AppModule(
                app,
                applicationScope = TestCoroutineScope(),
                exposureNotificationApi,
                bluetoothStateProvider,
                locationStateProvider,
                encryptedStorage.sharedPreferences,
                encryptedStorage.encryptedFile,
                signatureKey,
                updateManager,
                batteryOptimizationChecker,
                permissionsManager,
                packageManager,
                barcodeDetectorProvider,
                randomNonRiskyExposureWindowsLimiter,
                uuidGenerator,
                clock,
                DateChangeBroadcastReceiver()
            )
        )
        .networkModule(
            NetworkModule(
                Configurations.qa,
                additionalInterceptors
            )
        )
        .managedApiModule(
            ManagedApiModule(
                riskyVenuesApi,
                virologyTestingApi,
                questionnaireApi,
                isolationPaymentApi,
                keysSubmissionApi,
                analyticsApi,
                epidemiologyDataApi,
                localMessagesApi,
                mockIsolationConfigurationApi
            )
        )
        .build()

    init {
        app.appComponent = component
    }

    private fun closeNotificationPanel() {
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        app.baseContext.sendBroadcast(it)
    }

    fun reset(resetLocale: Boolean = true) {
        WorkManager.getInstance(app).cancelAllWork()

        encryptedStorage.sharedPreferences.edit(commit = true) { clear() }

        setExposureNotificationsEnabled(true)
        exposureNotificationApi.setDeviceSupportsLocationlessScanning(false)
        setOnboardingCompleted(true)
        setBluetoothEnabled(true)
        setLocationEnabled(true)
        setPolicyUpdateAccepted(true)
        FeatureFlagTestHelper.clearFeatureFlags()
        hideNotifications()
        closeNotificationPanel()

        if (resetLocale) {
            setLocale("en")
        }

        setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
        component.provideIsolationStateMachine().reset()
    }

    private fun hideNotifications() {
        NotificationManagerCompat.from(app).cancelAll()
    }

    fun setBluetoothEnabled(isEnabled: Boolean) {
        bluetoothStateProvider.bluetoothStateMutable.postValue(if (isEnabled) ENABLED else DISABLED)
    }

    fun setLocationEnabled(isEnabled: Boolean) {
        locationStateProvider.locationStateMutable.postValue(if (isEnabled) ENABLED else DISABLED)
    }

    fun setExposureNotificationsEnabled(isEnabled: Boolean) {
        exposureNotificationApi.setEnabled(isEnabled)
    }

    fun getExposureNotificationApi(): MockExposureNotificationApi {
        return exposureNotificationApi
    }

    fun setPostCode(postCode: String?) {
        component.getPostCodeProvider().value = postCode
    }

    fun setLocalAuthority(localAuthority: String?) {
        component.getLocalAuthorityProvider().value = localAuthority
    }

    fun setIsolationPaymentToken(token: String?) {
        component.getIsolationPaymentTokenStateProvider().tokenState = if (token != null) {
            IsolationPaymentTokenState.Token(token)
        } else {
            IsolationPaymentTokenState.Unresolved
        }
    }

    fun setAnimations(isEnabled: Boolean) {
        component.getAnimationsProvider().inAppAnimationEnabled = isEnabled
    }

    fun getSubmitAnalyticsAlarmController() = component.getSubmitAnalyticsAlarmController()

    fun getUserInbox() = component.getUserInbox()

    fun getUnacknowledgedTestResultsProvider() = component.getUnacknowledgedTestResultsProvider()

    fun getReceivedUnknownTestResultProvider() = component.getReceivedUnknownTestResultProvider()

    fun getRelevantTestResultProvider() = component.getRelevantTestResultProvider()

    fun getTestOrderingTokensProvider() = component.getTestOrderingTokensProvider()

    fun getKeySharingInfoProvider() = component.getKeySharingInfoProvider()

    fun setState(state: IsolationState) {
        val stateStorage = component.getStateStorage()
        stateStorage.state = state
        val ref = getIsolationStateMachine()
            .stateMachine
            .getPrivateProperty<StateMachine<IsolationInfo, Event, SideEffect>, AtomicReference<IsolationInfo>>(
                "stateRef"
            )
        ref?.set(state.toIsolationInfo())
    }

    fun getRemainingDaysInIsolation(): Int =
        getIsolationStateMachine().remainingDaysInIsolation().toInt()

    fun getIsolationStateMachine(): IsolationStateMachine =
        component.provideIsolationStateMachine()

    fun getCurrentState(): IsolationState =
        component.provideIsolationStateMachine().readState()

    fun getCurrentLogicalState(): IsolationLogicalState =
        component.provideIsolationStateMachine().readLogicalState()

    fun getMigrateIsolationState(): MigrateIsolationState =
        component.provideMigrateIsolationState()

    fun getExposureCircuitBreakerInfoProvider() =
        component.getExposureCircuitBreakerInfoProvider()

    fun getVisitedVenuesStorage(): VisitedVenuesStorage {
        return component.provideVisitedVenuesStorage()
    }

    fun getDownloadAndProcessRiskyVenues(): DownloadAndProcessRiskyVenues {
        return component.getDownloadAndProcessRiskyVenues()
    }

    fun getDownloadVirologyTestResultWork(): DownloadVirologyTestResultWork {
        return component.getDownloadVirologyTestResultWork()
    }

    fun temporaryExposureKeyHistoryWasCalled() =
        exposureNotificationApi.temporaryExposureKeyHistoryWasCalled()

    fun getPeriodicTasks(): PeriodicTasks {
        return component.providePeriodicTasks()
    }

    fun getDisplayStateExpirationNotification() =
        component.provideDisplayStateExpirationNotification()

    fun getIsolationConfigurationProvider() =
        component.getIsolationConfigurationProvider()

    fun setPolicyUpdateAccepted(accepted: Boolean) {
        component.getPolicyUpdateStorage().value = if (accepted) Int.MAX_VALUE.toString() else null
    }

    fun setLocale(languageCode: String?) {
        component.provideApplicationLocaleProvider().languageCode = languageCode
        updateResources()
    }

    fun getApplicationLocaleProvider() = component.provideApplicationLocaleProvider()

    private fun updateResources() {
        val locale = component.provideApplicationLocaleProvider().getLocale()
        Locale.setDefault(locale)
        val res: Resources = app.baseContext.resources
        val config = Configuration(res.configuration)
        config.locale = locale
        res.updateConfiguration(config, res.displayMetrics)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        component.provideOnboardingCompleted().value = completed
    }

    fun setAppAvailability(appAvailability: AppAvailabilityResponse) {
        component.getAppAvailabilityProvider().appAvailability = appAvailability
    }

    fun setIgnoringBatteryOptimizations(ignoringBatteryOptimizations: Boolean) {
        batteryOptimizationChecker.ignoringBatteryOptimizations = ignoringBatteryOptimizations
    }

    fun getIsolationPaymentTokenStateProvider() =
        component.getIsolationPaymentTokenStateProvider()

    fun getLastVisitedBookTestTypeVenueDateProvider() =
        component.getLastVisitedBookTestTypeVenueDateProvider()

    fun getAlarmManager() =
        component.getAlarmManager()

    fun getRiskyVenueAlertProvider() =
        component.getRiskyVenueAlertProvider()

    fun getShouldShowEncounterDetectionActivityProvider() =
        component.getShouldShowEncounterDetectionActivityProvider()

    fun getLocalMessagesProvider() =
        component.getLocalMessagesProvider()

    fun executeWhileOffline(action: () -> Unit) {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL
        action()
        MockApiModule.behaviour.responseType = ALWAYS_SUCCEED
    }

    fun runBackgroundTasks() {
        getPeriodicTasks().schedule()
        WorkManager.getInstance(app)
            .getWorkInfosForUniqueWorkLiveData(PeriodicTask.PERIODIC_TASKS.workName)
            .awaitSuccess()
    }

    fun sendExposureStateUpdatedBroadcast() {
        val intent = Intent(ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED)
        val broadcastReceiver = ExposureNotificationBroadcastReceiver()
        broadcastReceiver.onReceive(app, intent)
    }

    fun advanceClock(secondsToAdvance: Long) {
        clock.currentInstant = clock.instant().plusSeconds(secondsToAdvance)
        getCurrentState()
        runBackgroundTasks()
    }

    suspend fun getAgeLimitBeforeEncounter(): LocalDate? = component.getAgeLimitBeforeEncounter().invoke()

    fun getLastDoseDateLimit(): LocalDate? = component.getLastDoseDateLimit().invoke()

    fun getShouldShowBluetoothSplashScreen() = component.getShouldShowBluetoothSplashScreen()

    companion object {
        const val ENGLISH_LOCAL_AUTHORITY = "E07000063"
        const val WELSH_LOCAL_AUTHORITY = "W06000008"
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

fun stringFromResId(@StringRes stringRes: Int): String {
    val resources = ApplicationProvider.getApplicationContext<ExposureApplication>().resources
    return resources.getString(stringRes)
}

class TestBluetoothStateProvider : AvailabilityStateProvider {
    val bluetoothStateMutable = SingleLiveEvent<AvailabilityState>()
    override val availabilityState: LiveData<AvailabilityState> =
        distinctUntilChanged(bluetoothStateMutable)

    override fun start(context: Context) {
        bluetoothStateMutable.postValue(bluetoothStateMutable.value)
    }

    override fun stop(context: Context) {
    }

    override fun getState(isDebug: Boolean): AvailabilityState {
        return bluetoothStateMutable.value!!
    }
}

class TestLocationStateProvider : AvailabilityStateProvider {
    val locationStateMutable = MutableLiveData<AvailabilityState>()
    override val availabilityState: LiveData<AvailabilityState> =
        distinctUntilChanged(locationStateMutable)

    override fun start(context: Context) {
        locationStateMutable.postValue(locationStateMutable.value)
    }

    override fun stop(context: Context) {
    }

    override fun getState(isDebug: Boolean): AvailabilityState {
        return locationStateMutable.value!!
    }
}

class TestBatteryOptimizationChecker : BatteryOptimizationChecker {

    var ignoringBatteryOptimizations = false

    override fun isIgnoringBatteryOptimizations(): Boolean =
        ignoringBatteryOptimizations
}
