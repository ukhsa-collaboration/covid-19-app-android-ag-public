/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.testhelpers

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.work.WorkManager
import uk.nhs.covid19.config.Configurations
import uk.nhs.covid19.config.qrCodesSignatureKey
import uk.nhs.nhsx.covid19.android.app.ExposureApplication
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.state.Event
import uk.nhs.nhsx.covid19.android.app.state.SideEffect
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResult
import uk.nhs.nhsx.covid19.android.app.util.getPrivateProperty
import java.util.concurrent.atomic.AtomicReference

class TestApplicationContext {

    val app: ExposureApplication = ApplicationProvider.getApplicationContext()

    val virologyTestingApi = MockVirologyTestingApi()

    internal val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val exposureNotificationApi = MockExposureNotificationApi()

    private val bluetoothStateProvider = TestBluetoothStateProvider()

    private val locationStateProvider = TestLocationStateProvider()

    private val sharedPreferences =
        app.createEncryptedSharedPreferences("testEncryptedSharedPreferences")

    private val applicationLocaleProvider = ApplicationLocaleProvider(sharedPreferences)

    private val component: TestAppComponent = DaggerTestAppComponent.builder()
        .appModule(
            AppModule(
                app,
                exposureNotificationApi,
                bluetoothStateProvider,
                locationStateProvider,
                sharedPreferences,
                qrCodesSignatureKey,
                applicationLocaleProvider
            )
        )
        .networkModule(NetworkModule(Configurations.qa))
        .managedApiModule(ManagedApiModule(virologyTestingApi))
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

        setPostCode("CM1")
        setExposureNotificationsEnabled(true)
        setBluetoothEnabled(true)
        setLocationEnabled(true)

        closeNotificationPanel()

        component.provideIsolationStateMachine().reset()
    }

    fun setBluetoothEnabled(isEnabled: Boolean) {
        bluetoothStateProvider.bluetoothStateMutable.postValue(if (isEnabled) ENABLED else DISABLED)
    }

    fun setLocationEnabled(isEnabled: Boolean) {
        locationStateProvider.locationStateMutable.postValue(if (isEnabled) ENABLED else DISABLED)
    }

    fun setExposureNotificationsEnabled(isEnabled: Boolean, canBeChanged: Boolean = true) {
        exposureNotificationApi.setEnabled(isEnabled, canBeChanged)
    }

    fun setPostCode(postCode: String?) {
        component.getPostCodeProvider().value = postCode
    }

    fun setAuthenticated(authenticated: Boolean) {
        component.getAuthenticationCodeProvider().value = authenticated
    }

    fun setVenueVisit(venueId: String) {
        component.getUserInbox().addUserInboxItem(ShowVenueAlert(venueId))
    }

    fun setLatestTestResultProvider(latestTestResult: LatestTestResult) {
        component.getLatestTestResultProvider().latestTestResult = latestTestResult
    }

    fun getLatestTestResultProvider() =
        component.getLatestTestResultProvider().latestTestResult

    fun setState(state: State) {
        val ref = component.provideIsolationStateMachine()
            .stateMachine
            .getPrivateProperty<com.tinder.StateMachine<State, Event, SideEffect>, AtomicReference<State>>(
                "stateRef"
            )
        ref?.set(state)
    }

    fun getCurrentState(): State =
        component.provideIsolationStateMachine().readState()

    fun getVisitedVenuesStorage(): VisitedVenuesStorage {
        return component.provideVisitedVenuesStorage()
    }

    fun temporaryExposureKeyHistoryWasCalled() =
        exposureNotificationApi.temporaryExposureKeyHistoryWasCalled()

    fun getPeriodicTasks(): PeriodicTasks {
        return component.providePeriodicTasks()
    }

    fun getIsolationConfigurationProvider() =
        component.getIsolationConfigurationProvider()

    fun setLocale(languageName: String?) {
        applicationLocaleProvider.language = languageName
    }
}

fun stringFromResId(@StringRes stringRes: Int): String {
    val resources = ApplicationProvider.getApplicationContext<ExposureApplication>().resources
    return resources.getString(stringRes)
}

class TestBluetoothStateProvider : AvailabilityStateProvider {
    val bluetoothStateMutable = MutableLiveData<AvailabilityState>()
    override val availabilityState: LiveData<AvailabilityState> = bluetoothStateMutable
    override fun start(context: Context) {
        bluetoothStateMutable.postValue(bluetoothStateMutable.value)
    }

    override fun stop(context: Context) {
    }
}

class TestLocationStateProvider : AvailabilityStateProvider {
    val locationStateMutable = MutableLiveData<AvailabilityState>()
    override val availabilityState: LiveData<AvailabilityState> = locationStateMutable

    override fun start(context: Context) {
        locationStateMutable.postValue(locationStateMutable.value)
    }

    override fun stop(context: Context) {
    }
}
