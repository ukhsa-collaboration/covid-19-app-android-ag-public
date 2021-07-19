package uk.nhs.nhsx.covid19.android.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.jeroenmols.featureflag.framework.FeatureFlag.SUBMIT_ANALYTICS_VIA_ALARM_MANAGER
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import com.jeroenmols.featureflag.framework.TestSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import timber.log.Timber.DebugTree
import uk.nhs.covid19.config.production
import uk.nhs.covid19.config.qrCodesSignatureKey
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityListener
import uk.nhs.nhsx.covid19.android.app.availability.GooglePlayUpdateProvider
import uk.nhs.nhsx.covid19.android.app.battery.AndroidBatteryOptimizationChecker
import uk.nhs.nhsx.covid19.android.app.di.ApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.DaggerApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.di.module.ViewModelModule
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.GoogleExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.AndroidRandomNonRiskyExposureWindowsLimiter
import uk.nhs.nhsx.covid19.android.app.packagemanager.AndroidPackageManager
import uk.nhs.nhsx.covid19.android.app.permissions.AndroidPermissionsManager
import uk.nhs.nhsx.covid19.android.app.qrcode.AndroidBarcodeDetectorBuilder
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidBluetoothStateProvider
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidLocationStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.additionalInterceptors
import uk.nhs.nhsx.covid19.android.app.status.DateChangeBroadcastReceiver
import uk.nhs.nhsx.covid19.android.app.status.DateChangeReceiver
import uk.nhs.nhsx.covid19.android.app.util.AndroidStrongBoxSupport
import uk.nhs.nhsx.covid19.android.app.util.AndroidUUIDGenerator
import uk.nhs.nhsx.covid19.android.app.util.EncryptedSharedPreferencesUtils
import uk.nhs.nhsx.covid19.android.app.util.EncryptedStorage
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationRetryChecker
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationRetryStorage
import java.time.Clock

open class ExposureApplication : Application(), Configuration.Provider, LifecycleObserver {

    lateinit var appComponent: ApplicationComponent
    lateinit var encryptionUtils: EncryptionUtils
    private var appAvailabilityListener: AppAvailabilityListener? = null

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        if (isTestBuild) {
            Timber.plant(DebugTree())
            Timber.d("onCreate")
        }

        encryptionUtils = EncryptionUtils(AndroidStrongBoxSupport)

        buildAndUseAppComponent(
            NetworkModule(production, additionalInterceptors),
            ViewModelModule(),
            clock = Clock.systemDefaultZone()
        )

        RuntimeBehavior.initialize(this, isTestBuild)

        appComponent.provideRemoteServiceExceptionHandler().initialize()

        migrateIsolationState()

        initializeWorkManager()
        if (!isRunningTest) {
            startPeriodicTasks()
        }
        appComponent.provideExposureNotificationRetryAlarmController().onAppCreated()
        if (RuntimeBehavior.isFeatureEnabled(SUBMIT_ANALYTICS_VIA_ALARM_MANAGER)) {
            appComponent.provideSubmitAnalyticsAlarmController().onAppCreated()
        } else {
            appComponent.provideSubmitAnalyticsAlarmController().cancelIfScheduled()
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (RuntimeBehavior.isFeatureEnabled(TestSetting.STRICT_MODE)) {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onApplicationStart() {
        appComponent.provideApplicationStartAreaRiskUpdater().updateIfNecessary()
    }

    protected fun startPeriodicTasks() {
        appComponent.providePeriodicTasks().schedule(policy = ExistingPeriodicWorkPolicy.KEEP)
    }

    private fun initializeWorkManager() {
        // Google insist we need to initialise WorkManager in onCreate()
        WorkManager.getInstance(this)
    }

    private fun migrateIsolationState() {
        appComponent.provideMigrateIsolationState().invoke()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        val builder = Configuration.Builder()
        builder.setMaxSchedulerLimit(WORK_MANAGER_SCHEDULER_LIMIT)
        if (isTestBuild) {
            builder.setMinimumLoggingLevel(android.util.Log.DEBUG)
        }
        return builder.build()
    }

    fun updateLifecycleListener() {
        appAvailabilityListener?.let {
            unregisterActivityLifecycleCallbacks(it)
        }
        appAvailabilityListener = appComponent.provideAppAvailabilityListener()
        registerActivityLifecycleCallbacks(appAvailabilityListener)
    }

    fun buildAndUseAppComponent(
        networkModule: NetworkModule,
        viewModelModule: ViewModelModule,
        exposureNotificationApi: ExposureNotificationApi = GoogleExposureNotificationApi(this),
        clock: Clock,
        dateChangeReceiver: DateChangeReceiver = DateChangeBroadcastReceiver(),
        applicationContext: Context = this,
    ) {
        val encryptedStorage = createEncryptedStorage()

        appComponent = DaggerApplicationComponent.builder()
            .appModule(
                AppModule(
                    applicationContext,
                    applicationScope,
                    exposureNotificationApi,
                    AndroidBluetoothStateProvider(),
                    AndroidLocationStateProvider(),
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
                    dateChangeReceiver
                )
            )
            .networkModule(networkModule)
            .viewModelModule(viewModelModule)
            .build()

        updateLifecycleListener()
    }

    protected fun createEncryptedStorage(): EncryptedStorage {
        val encryptedSharedPreferencesUtils = EncryptedSharedPreferencesUtils(encryptionUtils)
        val migrationSharedPreferences = encryptedSharedPreferencesUtils.createGenericEncryptedSharedPreferences(
            this,
            encryptionUtils.getDefaultMasterKey(),
            SharedPrefsDelegate.migrationSharedPreferencesFileName
        )
        return EncryptedStorage.from(
            this,
            StrongBoxMigrationRetryChecker(
                StrongBoxMigrationRetryStorage(migrationSharedPreferences)
            ),
            encryptionUtils
        )
    }

    companion object {
        private const val WORK_MANAGER_SCHEDULER_LIMIT = 50
        val isTestBuild = BuildConfig.DEBUG || BuildConfig.FLAVOR == "scenarios"
    }

    protected val isRunningTest: Boolean by lazy {
        try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}

val Context.appComponent: ApplicationComponent
    get() = (applicationContext as ExposureApplication).appComponent

val Context.app: ExposureApplication
    get() = (applicationContext as ExposureApplication)

fun Context.inPortraitMode(): Boolean =
    resources.configuration.orientation == ORIENTATION_PORTRAIT

inline fun <reified T : Activity> Context.startActivity(config: Intent.() -> Unit = {}) =
    startActivity(componentIntent<T>(config))

inline fun <reified T> Context.componentIntent(config: Intent.() -> Unit = {}) =
    Intent(this, T::class.java).apply(config)
