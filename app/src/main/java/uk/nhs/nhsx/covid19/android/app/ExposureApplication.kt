package uk.nhs.nhsx.covid19.android.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import com.jeroenmols.featureflag.framework.TestSetting
import timber.log.Timber
import timber.log.Timber.DebugTree
import uk.nhs.covid19.config.production
import uk.nhs.covid19.config.qrCodesSignatureKey
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalyticsWorker
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityListener
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityWorker
import uk.nhs.nhsx.covid19.android.app.di.ApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.DaggerApplicationComponent
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.GoogleExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidBluetoothStateProvider
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidLocationStateProvider
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate

class ExposureApplication : Application() {
    lateinit var appComponent: ApplicationComponent

    private var appAvailabilityListener: AppAvailabilityListener? = null

    override fun onCreate() {
        super.onCreate()

        buildAndUseAppComponent(NetworkModule(production))

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        RuntimeBehavior.initialize(this, BuildConfig.DEBUG)

        SubmitAnalyticsWorker.schedule(this)

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

    fun updateLifecycleListener() {
        appAvailabilityListener?.let {
            unregisterActivityLifecycleCallbacks(it)
        }
        AppAvailabilityWorker.schedule(this)
        appAvailabilityListener = appComponent.provideAppAvailabilityListener()
        registerActivityLifecycleCallbacks(appAvailabilityListener)
    }

    fun buildAndUseAppComponent(
        networkModule: NetworkModule,
        exposureNotificationApi: ExposureNotificationApi = GoogleExposureNotificationApi(this)
    ) {
        appComponent = DaggerApplicationComponent.builder()
            .appModule(
                AppModule(
                    this,
                    exposureNotificationApi,
                    AndroidBluetoothStateProvider(),
                    AndroidLocationStateProvider(),
                    createEncryptedSharedPreferences(),
                    qrCodesSignatureKey
                )
            )
            .networkModule(networkModule)
            .build()

        updateLifecycleListener()
    }

    fun createEncryptedSharedPreferences(fileName: String = SharedPrefsDelegate.fileName) =
        EncryptedSharedPreferences.create(
            fileName,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}

val Context.appComponent: ApplicationComponent
    get() = (applicationContext as ExposureApplication).appComponent

val Context.app: ExposureApplication
    get() = (applicationContext as ExposureApplication)

inline fun <reified T : Activity> Context.startActivity(config: Intent.() -> Unit = {}) =
    startActivity(componentIntent<T>(config))

inline fun <reified T> Context.componentIntent(config: Intent.() -> Unit = {}) =
    Intent(this, T::class.java).apply(config)
