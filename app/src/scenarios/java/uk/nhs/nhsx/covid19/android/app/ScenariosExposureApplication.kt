package uk.nhs.nhsx.covid19.android.app

import android.content.Context
import android.content.SharedPreferences
import timber.log.Timber
import uk.nhs.covid19.config.Configurations
import uk.nhs.covid19.config.EnvironmentConfiguration
import uk.nhs.covid19.config.Remote
import uk.nhs.covid19.config.production
import uk.nhs.covid19.config.qrCodesSignatureKey
import uk.nhs.nhsx.covid19.android.app.DebugActivity.Companion.SELECTED_ENVIRONMENT
import uk.nhs.nhsx.covid19.android.app.DebugActivity.Companion.USE_MOCKED_EXPOSURE_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.availability.GooglePlayUpdateProvider
import uk.nhs.nhsx.covid19.android.app.battery.AndroidBatteryOptimizationChecker
import uk.nhs.nhsx.covid19.android.app.di.DaggerMockApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.GoogleExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.packagemanager.AndroidPackageManager
import uk.nhs.nhsx.covid19.android.app.permissions.AndroidPermissionsManager
import uk.nhs.nhsx.covid19.android.app.qrcode.AndroidBarcodeDetectorBuilder
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidBluetoothStateProvider
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidLocationStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.additionalInterceptors
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils
import java.time.Clock

class ScenariosExposureApplication : ExposureApplication() {

    private lateinit var debugSharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        debugSharedPreferences =
            getSharedPreferences(DebugActivity.DEBUG_PREFERENCES_NAME, Context.MODE_PRIVATE)

        updateDependencyGraph()
        startPeriodicTasks()
    }

    fun updateDependencyGraph() {
        val selectedEnvironmentIndex = debugSharedPreferences.getInt(SELECTED_ENVIRONMENT, 0)
            .coerceIn(0, environments.size - 1)
        val useMockedExposureNotifications =
            debugSharedPreferences.getBoolean(USE_MOCKED_EXPOSURE_NOTIFICATION, false)
        val useMockNetwork = selectedEnvironmentIndex == 0
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

    private fun setApplicationComponent(
        useMockNetwork: Boolean,
        useMockExposureApi: Boolean
    ) {
        Timber.d("setApplicationComponent: useMockNetwork = $useMockNetwork, useMockExposureApi = $useMockExposureApi")
        if (useMockNetwork) {
            useMockApplicationComponent(useMockExposureApi)
        } else {
            useRegularApplicationComponent(useMockExposureApi)
        }
    }

    private fun useRegularApplicationComponent(useMockExposureApi: Boolean) {
        buildAndUseAppComponent(
            NetworkModule(getConfiguration(), additionalInterceptors),
            getExposureNotificationApi(useMockExposureApi)
        )
    }

    private fun useMockApplicationComponent(useMockExposureApi: Boolean) {
        val sharedPreferences = EncryptionUtils.createEncryptedSharedPreferences(this)
        val encryptedFile = EncryptionUtils.createEncryptedFile(this, "venues")
        appComponent =
            DaggerMockApplicationComponent.builder()
                .appModule(
                    AppModule(
                        applicationContext,
                        getExposureNotificationApi(useMockExposureApi),
                        AndroidBluetoothStateProvider(),
                        AndroidLocationStateProvider(),
                        sharedPreferences,
                        encryptedFile,
                        qrCodesSignatureKey,
                        GooglePlayUpdateProvider(this),
                        AndroidBatteryOptimizationChecker(this),
                        AndroidPermissionsManager(),
                        AndroidPackageManager(),
                        AndroidBarcodeDetectorBuilder(this),
                        Clock.systemDefaultZone()
                    )
                )
                .mockApiModule(MockApiModule())
                .networkModule(NetworkModule(getConfiguration(), additionalInterceptors))
                .build()
        updateLifecycleListener()
    }

    private fun getExposureNotificationApi(useMockExposureApi: Boolean) =
        if (useMockExposureApi) MockExposureNotificationApi() else GoogleExposureNotificationApi(
            this
        )

    private fun getConfiguration(): EnvironmentConfiguration {
        val selectedEnvironmentIndex = debugSharedPreferences.getInt(SELECTED_ENVIRONMENT, 0).coerceIn(0, environments.size - 1)
        return environments[selectedEnvironmentIndex]
    }
}

val Context.scenariosApp: ScenariosExposureApplication
    get() = (applicationContext as ScenariosExposureApplication)
