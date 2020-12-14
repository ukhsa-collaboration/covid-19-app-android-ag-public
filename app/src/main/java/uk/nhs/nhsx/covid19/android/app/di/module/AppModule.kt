package uk.nhs.nhsx.covid19.android.app.di.module

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import uk.nhs.covid19.config.SignatureKey
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationChecker
import uk.nhs.nhsx.covid19.android.app.common.AppInfo
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationWorker
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationWorkerScheduler
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.packagemanager.PackageManager
import uk.nhs.nhsx.covid19.android.app.permissions.PermissionsManager
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.util.AndroidBase64Decoder
import uk.nhs.nhsx.covid19.android.app.util.Base64Decoder
import uk.nhs.nhsx.covid19.android.app.util.EncryptedFileInfo
import uk.nhs.nhsx.covid19.android.app.util.viewutils.DeviceDetection
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModule(
    private val applicationContext: Context,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val bluetoothStateProvider: AvailabilityStateProvider,
    private val locationStateProvider: AvailabilityStateProvider,
    private val encryptedSharedPreferences: SharedPreferences,
    private val encryptedFileInfo: EncryptedFileInfo,
    private val qrCodesSignatureKey: SignatureKey,
    private val applicationLocaleProvider: ApplicationLocaleProvider,
    private val updateManager: UpdateManager,
    private val batteryOptimizationChecker: BatteryOptimizationChecker,
    private val permissionsManager: PermissionsManager,
    private val packageManager: PackageManager
) {
    @Provides
    fun provideContext() = applicationContext

    @Provides
    fun provideAppInfo() = AppInfo()

    @Provides
    fun provideDeviceDetection(): DeviceDetection =
        DeviceDetection(applicationContext)

    @Provides
    fun provideExposureNotificationApi(): ExposureNotificationApi = exposureNotificationApi

    @Provides
    @Named(BLUETOOTH_STATE_NAME)
    fun provideBluetoothStateProvider(): AvailabilityStateProvider = bluetoothStateProvider

    @Provides
    @Named(LOCATION_STATE_NAME)
    fun provideLocationStateProvider(): AvailabilityStateProvider = locationStateProvider

    @Provides
    fun provideEncryptedSharedPreferences(): SharedPreferences = encryptedSharedPreferences

    @Provides
    fun provideEncryptedFileInfo(): EncryptedFileInfo = encryptedFileInfo

    @Provides
    @Singleton
    fun provideNotificationProvider(): NotificationProvider =
        NotificationProvider(applicationContext)

    @Provides
    @Singleton
    fun provideBase64Decoder(): Base64Decoder = AndroidBase64Decoder()

    @Provides
    @Singleton
    fun provideAlarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Provides
    @Singleton
    fun provideAppVersionCodeUpdateProvider(): UpdateManager = updateManager

    @Provides
    @Singleton
    fun provideBatteryOptimizationChecker(): BatteryOptimizationChecker = batteryOptimizationChecker

    @Provides
    fun provideQrCodesSignatureKey(): SignatureKey = qrCodesSignatureKey

    @Provides
    @Singleton
    fun provideApplicationLocaleProvider(): ApplicationLocaleProvider =
        applicationLocaleProvider

    @Provides
    @Singleton
    fun providePermissionsManager(): PermissionsManager =
        permissionsManager

    @Provides
    @Singleton
    fun providePackageManager(): PackageManager =
        packageManager

    @Provides
    @Singleton
    fun provideUtcClock(): Clock = Clock.systemUTC()

    @Provides
    fun provideExposureNotificationWorkerScheduler(): ExposureNotificationWorkerScheduler =
        ExposureNotificationWorker.Companion

    companion object {
        const val BLUETOOTH_STATE_NAME = "BLUETOOTH_STATE"
        const val LOCATION_STATE_NAME = "LOCATION_STATE"
    }
}
