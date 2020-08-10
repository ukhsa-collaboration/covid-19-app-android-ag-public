package uk.nhs.nhsx.covid19.android.app.di.module

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import uk.nhs.covid19.config.SignatureKey
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager
import uk.nhs.nhsx.covid19.android.app.availability.GooglePlayUpdateProvider
import uk.nhs.nhsx.covid19.android.app.notifications.AndroidUserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.util.AndroidBase64Decoder
import uk.nhs.nhsx.covid19.android.app.util.Base64Decoder
import uk.nhs.nhsx.covid19.android.app.util.DeviceDetection
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModule(
    private val applicationContext: Context,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val bluetoothStateProvider: AvailabilityStateProvider,
    private val locationStateProvider: AvailabilityStateProvider,
    private val encryptedSharedPreferences: SharedPreferences,
    private val qrCodesSignatureKey: SignatureKey
) {
    @Provides
    fun provideContext() = applicationContext

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
    @Singleton
    fun provideNotificationProvider(): NotificationProvider =
        NotificationProvider(applicationContext)

    @Provides
    @Singleton
    fun provideBase64Decoder(): Base64Decoder = AndroidBase64Decoder()

    @Provides
    @Singleton
    fun providerUserInbox(sharedPreferences: SharedPreferences): UserInbox =
        AndroidUserInbox(sharedPreferences)

    @Provides
    @Singleton
    fun provideAlarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Provides
    @Singleton
    fun provideAppVersionCodeUpdateProvider(): UpdateManager =
        GooglePlayUpdateProvider(
            applicationContext
        )

    @Provides
    fun provideQrCodesSignatureKey(): SignatureKey = qrCodesSignatureKey

    companion object {
        const val BLUETOOTH_STATE_NAME = "BLUETOOTH_STATE"
        const val LOCATION_STATE_NAME = "LOCATION_STATE"
    }
}
