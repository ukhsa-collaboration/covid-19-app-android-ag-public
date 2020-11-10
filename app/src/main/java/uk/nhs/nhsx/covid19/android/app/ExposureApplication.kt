package uk.nhs.nhsx.covid19.android.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.work.Configuration
import androidx.work.WorkManager
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import com.jeroenmols.featureflag.framework.TestSetting
import timber.log.Timber
import timber.log.Timber.DebugTree
import uk.nhs.covid19.config.production
import uk.nhs.covid19.config.qrCodesSignatureKey
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityListener
import uk.nhs.nhsx.covid19.android.app.availability.GooglePlayUpdateProvider
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.di.ApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.DaggerApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.GoogleExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidBluetoothStateProvider
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidLocationStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.additionalInterceptors
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils

open class ExposureApplication : Application(), Configuration.Provider {
    lateinit var appComponent: ApplicationComponent

    private var appAvailabilityListener: AppAvailabilityListener? = null

    override fun onCreate() {
        super.onCreate()

        if (isTestBuild) {
            Timber.plant(DebugTree())
            Timber.d("onCreate")
        }

        buildAndUseAppComponent(NetworkModule(production, additionalInterceptors))

        RuntimeBehavior.initialize(this, isTestBuild)

        initializeWorkManager()
        startPeriodicTasks()

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

    protected fun startPeriodicTasks() {
        appComponent.providePeriodicTasks().schedule()
    }

    private fun initializeWorkManager() {
        // Google insist we need to initialise WorkManager in onCreate()
        WorkManager.getInstance(this)
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
        exposureNotificationApi: ExposureNotificationApi = GoogleExposureNotificationApi(this),
        languageCode: String? = null
    ) {
        val encryptedFile = EncryptionUtils.retryOnException {
            EncryptionUtils.createEncryptedFile(this, "venues")
        }
        val sharedPreferences = EncryptionUtils.retryOnException {
            EncryptionUtils.createEncryptedSharedPreferences(this)
        }

        appComponent = DaggerApplicationComponent.builder()
            .appModule(
                AppModule(
                    this,
                    exposureNotificationApi,
                    AndroidBluetoothStateProvider(),
                    AndroidLocationStateProvider(),
                    sharedPreferences,
                    encryptedFile,
                    qrCodesSignatureKey,
                    ApplicationLocaleProvider(sharedPreferences, languageCode),
                    GooglePlayUpdateProvider(this)
                )
            )
            .networkModule(networkModule)
            .build()

        updateLifecycleListener()
    }

    companion object {
        private const val WORK_MANAGER_SCHEDULER_LIMIT = 50
        val isTestBuild = BuildConfig.DEBUG || BuildConfig.FLAVOR == "scenarios"
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
