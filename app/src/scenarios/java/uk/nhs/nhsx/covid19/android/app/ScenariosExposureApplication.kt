package uk.nhs.nhsx.covid19.android.app

import android.content.Context
import android.content.SharedPreferences
import co.lokalise.android.sdk.LokaliseSDK
import co.lokalise.android.sdk.core.LokaliseContextWrapper
import timber.log.Timber
import uk.nhs.covid19.config.Configurations
import uk.nhs.covid19.config.EnvironmentConfiguration
import uk.nhs.covid19.config.LokaliseSettings
import uk.nhs.covid19.config.Remote
import uk.nhs.covid19.config.production
import uk.nhs.covid19.config.qrCodesSignatureKey
import uk.nhs.nhsx.covid19.android.app.DebugActivity.Companion.OFFSET_DAYS
import uk.nhs.nhsx.covid19.android.app.DebugActivity.Companion.SELECTED_ENVIRONMENT
import uk.nhs.nhsx.covid19.android.app.DebugActivity.Companion.USE_MOCKED_EXPOSURE_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.availability.GooglePlayUpdateProvider
import uk.nhs.nhsx.covid19.android.app.battery.AndroidBatteryOptimizationChecker
import uk.nhs.nhsx.covid19.android.app.di.ApplicationClock
import uk.nhs.nhsx.covid19.android.app.di.DaggerMockApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.di.module.ViewModelModule
import uk.nhs.nhsx.covid19.android.app.exposure.GoogleExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.AndroidRandomNonRiskyExposureWindowsLimiter
import uk.nhs.nhsx.covid19.android.app.packagemanager.AndroidPackageManager
import uk.nhs.nhsx.covid19.android.app.permissions.AndroidPermissionsManager
import uk.nhs.nhsx.covid19.android.app.qrcode.AndroidBarcodeDetectorBuilder
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidBluetoothStateProvider
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidLocationStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.additionalInterceptors
import uk.nhs.nhsx.covid19.android.app.status.ScenariosDateChangeBroadcastReceiver
import uk.nhs.nhsx.covid19.android.app.util.AndroidUUIDGenerator
import uk.nhs.nhsx.covid19.android.app.util.isEmulator
import uk.nhs.nhsx.covid19.android.app.util.workarounds.ConcurrentModificationExceptionWorkaround
import java.time.Clock

class ScenariosExposureApplication : ExposureApplication() {

    private lateinit var debugSharedPreferences: SharedPreferences

    override fun onCreate() {
        ConcurrentModificationExceptionWorkaround.init(this)
        super.onCreate()
        LokaliseSDK.init(LokaliseSettings.apiKey, LokaliseSettings.projectId, this)
        LokaliseSDK.setPreRelease(true)

        Timber.d("onCreate")
        debugSharedPreferences =
            getSharedPreferences(DebugActivity.DEBUG_PREFERENCES_NAME, Context.MODE_PRIVATE)

        updateDependencyGraph()
        if (!isRunningTest) {
            startPeriodicTasks()
        }
    }

    fun updateDependencyGraph() {
        val selectedEnvironmentIndex = debugSharedPreferences.getInt(SELECTED_ENVIRONMENT, 0)
            .coerceIn(0, environments.size - 1)
        val useMockedExposureNotifications =
            debugSharedPreferences.getBoolean(USE_MOCKED_EXPOSURE_NOTIFICATION, isEmulator())
        val useMockNetwork = selectedEnvironmentIndex == mockEnvironmentIndex
        setApplicationComponent(useMockNetwork, useMockedExposureNotifications)
    }

    val environments = mutableListOf<EnvironmentConfiguration>().apply {
        add(
            EnvironmentConfiguration(
                name = "Mock",
                distributedRemote = Remote("localhost", path = null),
                apiRemote = Remote("localhost", path = null)
            )
        )
        addAll(Configurations.allConfigs)
        add(production)
    }.toList()

    val mockEnvironmentIndex: Int
        get() = environments.indexOf(environments.first { it.name == "Mock" })

    private fun setApplicationComponent(
        useMockNetwork: Boolean,
        useMockExposureApi: Boolean
    ) {
        Timber.d("setApplicationComponent: useMockNetwork = $useMockNetwork, useMockExposureApi = $useMockExposureApi")
        val offsetDays = debugSharedPreferences.getLong(OFFSET_DAYS, 0L)
        val clock = ApplicationClock(offsetDays)
        if (useMockNetwork) {
            useMockApplicationComponent(useMockExposureApi, clock)
        } else {
            useRegularApplicationComponent(useMockExposureApi, clock)
        }
    }

    private fun useRegularApplicationComponent(useMockExposureApi: Boolean, clock: Clock) {
        buildAndUseAppComponent(
            NetworkModule(getConfiguration(), additionalInterceptors),
            ViewModelModule(),
            getExposureNotificationApi(useMockExposureApi, clock),
            clock,
            ScenariosDateChangeBroadcastReceiver(),
            applicationContext = LokaliseContextWrapper(applicationContext)
        )
    }

    private fun useMockApplicationComponent(useMockExposureApi: Boolean, clock: Clock) {
        val encryptedStorage = createEncryptedStorage()

        appComponent =
            DaggerMockApplicationComponent.builder()
                .appModule(
                    AppModule(
                        LokaliseContextWrapper(applicationContext),
                        applicationScope,
                        getExposureNotificationApi(useMockExposureApi, clock),
                        AndroidBluetoothStateProvider(),
                        AndroidLocationStateProvider(this),
                        encryptedStorage.sharedPreferences,
                        encryptedStorage.encryptedFile,
                        qrCodesSignatureKey,
                        GooglePlayUpdateProvider(this),
                        AndroidBatteryOptimizationChecker(this),
                        AndroidPermissionsManager(),
                        AndroidPackageManager(),
                        AndroidBarcodeDetectorBuilder(this),
                        AndroidRandomNonRiskyExposureWindowsLimiter(),
                        AndroidUUIDGenerator(),
                        clock,
                        ScenariosDateChangeBroadcastReceiver()
                    )
                )
                .mockApiModule(MockApiModule())
                .networkModule(NetworkModule(getConfiguration(), additionalInterceptors))
                .build()
        updateLifecycleListener()
    }

    private fun getExposureNotificationApi(useMockExposureApi: Boolean, clock: Clock) =
        if (useMockExposureApi) MockExposureNotificationApi(
            this,
            clock,
            simulateGoogleEN = true,
            sharedPreferences = getSharedPreferences(DebugActivity.DEBUG_PREFERENCES_NAME, Context.MODE_PRIVATE)
        ) else GoogleExposureNotificationApi(
            this
        )

    private fun getConfiguration(): EnvironmentConfiguration {
        val selectedEnvironmentIndex =
            debugSharedPreferences.getInt(SELECTED_ENVIRONMENT, 0).coerceIn(0, environments.size - 1)
        return environments[selectedEnvironmentIndex]
    }
}

val Context.scenariosApp: ScenariosExposureApplication
    get() = (applicationContext as ScenariosExposureApplication)
