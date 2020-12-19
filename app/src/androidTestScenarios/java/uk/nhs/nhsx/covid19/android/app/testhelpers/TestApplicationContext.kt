/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.testhelpers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.work.WorkManager
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.tinder.StateMachine
import uk.nhs.covid19.config.Configurations
import uk.nhs.covid19.config.qrCodesSignatureKey
import uk.nhs.nhsx.covid19.android.app.ExposureApplication
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationChecker
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.packagemanager.MockPackageManager
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.permissions.MockPermissionsManager
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenues
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.MockIsolationPaymentApi
import uk.nhs.nhsx.covid19.android.app.remote.MockKeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockQuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.additionalInterceptors
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.state.Event
import uk.nhs.nhsx.covid19.android.app.state.SideEffect
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork
import uk.nhs.nhsx.covid19.android.app.util.EncryptedFileInfo
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.util.getPrivateProperty
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

const val AWAIT_AT_MOST_SECONDS: Long = 10

class TestApplicationContext {

    val app: ExposureApplication = ApplicationProvider.getApplicationContext()

    val riskyVenuesApi = MockRiskyVenuesApi()

    val virologyTestingApi = MockVirologyTestingApi()

    val isolationPaymentApi = MockIsolationPaymentApi()

    val questionnaireApi = MockQuestionnaireApi()

    val keysSubmissionApi = MockKeysSubmissionApi()

    val updateManager = TestUpdateManager()

    val permissionsManager = MockPermissionsManager()

    val packageManager = MockPackageManager()

    val clock = MockClock()

    internal val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val exposureNotificationApi = MockExposureNotificationApi()

    private val bluetoothStateProvider = TestBluetoothStateProvider()

    private val locationStateProvider = TestLocationStateProvider()

    private val batteryOptimizationChecker = TestBatteryOptimizationChecker()

    private val sharedPreferences: SharedPreferences =
        EncryptionUtils.createEncryptedSharedPreferences(
            app,
            EncryptionUtils.getDefaultMasterKey(),
            "testEncryptedSharedPreferences"
        )

    private val encryptedFile: EncryptedFileInfo =
        EncryptionUtils.createEncryptedFile(
            app,
            "venues"
        )

    private val applicationLocaleProvider = ApplicationLocaleProvider(sharedPreferences)

    private val component: TestAppComponent = DaggerTestAppComponent.builder()
        .appModule(
            AppModule(
                app,
                exposureNotificationApi,
                bluetoothStateProvider,
                locationStateProvider,
                sharedPreferences,
                encryptedFile,
                qrCodesSignatureKey,
                applicationLocaleProvider,
                updateManager,
                batteryOptimizationChecker,
                permissionsManager,
                packageManager,
                clock
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
                keysSubmissionApi,
                isolationPaymentApi
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

    fun reset() {
        WorkManager.getInstance(app).cancelAllWork()

        sharedPreferences.edit { clear() }

        setExposureNotificationsEnabled(true)
        exposureNotificationApi.setDeviceSupportsLocationlessScanning(false)
        setOnboardingCompleted(true)
        setBluetoothEnabled(true)
        setLocationEnabled(true)
        setPolicyUpdateAccepted(true)
        FeatureFlagTestHelper.clearFeatureFlags()
        closeNotificationPanel()

        component.provideIsolationStateMachine().reset()
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

    fun getUserInbox() = component.getUserInbox()

    fun getTestResultsProvider() = component.getTestResultsProvider()

    fun setState(state: State) {
        val ref = component.provideIsolationStateMachine()
            .stateMachine
            .getPrivateProperty<StateMachine<State, Event, SideEffect>, AtomicReference<State>>(
                "stateRef"
            )
        ref?.set(state)
    }

    fun getCurrentState(): State =
        component.provideIsolationStateMachine().readState()

    fun getExposureNotificationTokenProvider() =
        component.getExposureNotificationsTokenProvider()

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

    fun setLocale(languageName: String?) {
        applicationLocaleProvider.language = languageName
        updateResources()
    }

    private fun updateResources() {
        val locale = applicationLocaleProvider.getLocale()
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

    fun setKeysSubmissionApiShouldSucceed(shouldSucceed: Boolean) {
        keysSubmissionApi.shouldSucceed = shouldSucceed
    }

    fun setIgnoringBatteryOptimizations(ignoringBatteryOptimizations: Boolean) {
        batteryOptimizationChecker.ignoringBatteryOptimizations = ignoringBatteryOptimizations
    }

    fun getIsolationPaymentTokenStateProvider() =
        component.getIsolationPaymentTokenStateProvider()
}

class MockClock(var currentInstant: Instant? = null) : Clock() {

    override fun instant(): Instant = currentInstant ?: Instant.now()

    override fun withZone(zone: ZoneId?): Clock = this

    override fun getZone(): ZoneId = ZoneOffset.UTC

    fun reset() { currentInstant = null }
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
}

class TestBatteryOptimizationChecker : BatteryOptimizationChecker {

    var ignoringBatteryOptimizations = false

    override fun isIgnoringBatteryOptimizations(): Boolean =
        ignoringBatteryOptimizations
}
