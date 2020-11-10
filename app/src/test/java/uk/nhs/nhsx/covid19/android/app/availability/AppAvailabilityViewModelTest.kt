package uk.nhs.nhsx.covid19.android.app.availability

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.AppVersionNotSupported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.DeviceSdkIsNotSupported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.Supported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.UpdateAvailable
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus.Available
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus.NoUpdateAvailable
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus.UpdateError
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.RecommendedAppVersion

class AppAvailabilityViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val updateManager = mockk<UpdateManager>()
    private val observer = mockk<Observer<AppAvailabilityState>>(relaxed = true)

    private val testSubject =
        AppAvailabilityViewModel(
            appAvailabilityProvider,
            updateManager
        )

    private val appAvailableResponse = AppAvailabilityResponse(
        MinimumAppVersion(
            Translatable(mapOf("en" to "en")),
            BuildConfig.VERSION_CODE + 1
        ),
        MinimumSdkVersion(
            Translatable(
                mapOf()
            ),
            Build.VERSION.SDK_INT
        ),
        RecommendedAppVersion(
            Translatable(
                mapOf()
            ),
            BuildConfig.VERSION_CODE,
            title = Translatable(
                mapOf()
            )
        )
    )

    @Before
    fun setUp() {
        testSubject.appAvailabilityState().observeForever(observer)
    }

    @Test
    fun `sdk and version are supported`() {
        every { appAvailabilityProvider.appAvailability } returns stubResponse(
            minSdkValue = 23,
            minAppVersionCode = 10
        )
        testSubject.checkAvailability(deviceSdkVersion = 23, appVersionCode = 10)

        verify { observer.onChanged(Supported()) }
    }

    @Test
    fun `device sdk version is lower than min sdk`() {
        every { appAvailabilityProvider.appAvailability } returns stubResponse(
            minSdkValue = 23,
            minAppVersionCode = 10
        )
        testSubject.checkAvailability(deviceSdkVersion = 22, appVersionCode = 10)

        verify { observer.onChanged(DeviceSdkIsNotSupported("Please Update Device")) }
    }

    @Test
    fun `app version is lower than min app version and update is available`() = runBlocking {
        every { appAvailabilityProvider.appAvailability } returns stubResponse(
            minSdkValue = 23,
            minAppVersionCode = 10
        )
        coEvery { updateManager.getAvailableUpdateVersionCode() } returns Available(10)

        testSubject.checkAvailability(deviceSdkVersion = 23, appVersionCode = 9)

        verify { observer.onChanged(UpdateAvailable("Please Update App")) }
    }

    @Test
    fun `app version is lower than min app version and update is not available`() = runBlocking {
        every { appAvailabilityProvider.appAvailability } returns stubResponse(
            minSdkValue = 23,
            minAppVersionCode = 10
        )
        coEvery { updateManager.getAvailableUpdateVersionCode() } returns NoUpdateAvailable

        testSubject.checkAvailability(deviceSdkVersion = 23, appVersionCode = 9)

        verify { observer.onChanged(AppVersionNotSupported("Please Update App")) }
    }

    @Test
    fun `app version is lower than min app version and cannot check update availability`() =
        runBlocking {
            every { appAvailabilityProvider.appAvailability } returns stubResponse(
                minSdkValue = 23,
                minAppVersionCode = 10
            )
            coEvery { updateManager.getAvailableUpdateVersionCode() } returns UpdateError(
                RuntimeException()
            )

            testSubject.checkAvailability(deviceSdkVersion = 23, appVersionCode = 9)

            verify { observer.onChanged(AppVersionNotSupported("Please Update App")) }
        }

    @Test
    fun `minimum app version bigger than possibleUpdate version code`() = runBlocking {
        every { appAvailabilityProvider.appAvailability } returns appAvailableResponse
        coEvery { updateManager.getAvailableUpdateVersionCode() } returns Available(BuildConfig.VERSION_CODE)

        testSubject.checkAvailability()

        verify {
            observer.onChanged(
                AppVersionNotSupported(
                    appAvailableResponse.minimumAppVersion.description.translate()
                )
            )
        }
    }

    @Test
    fun `appAvailability is null state is Supported`() = runBlocking {
        every { appAvailabilityProvider.appAvailability } returns null

        testSubject.checkAvailability(deviceSdkVersion = 23, appVersionCode = 9)

        verify { observer.onChanged(Supported()) }
    }

    private fun stubResponse(minSdkValue: Int = 23, minAppVersionCode: Int = 8) =
        AppAvailabilityResponse(
            minimumAppVersion = MinimumAppVersion(
                description = Translatable(mapOf("en-GB" to "Please Update App")),
                value = minAppVersionCode
            ),
            minimumSdkVersion = MinimumSdkVersion(
                description = Translatable(mapOf("en-GB" to "Please Update Device")),
                value = minSdkValue
            ),
            recommendedAppVersion = RecommendedAppVersion(
                description = Translatable(mapOf("en-GB" to "Please Update App")),
                value = minAppVersionCode,
                title = Translatable(mapOf("en-GB" to "Please Update App"))
            )
        )
}
