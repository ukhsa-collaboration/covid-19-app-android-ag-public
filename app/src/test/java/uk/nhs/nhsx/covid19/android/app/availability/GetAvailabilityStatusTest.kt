package uk.nhs.nhsx.covid19.android.app.availability

import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.AppAvailabilityApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion
import kotlin.test.assertEquals

class GetAvailabilityStatusTest {

    private val appAvailabilityApi = mockk<AppAvailabilityApi>()
    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)

    private val testSubject =
        GetAvailabilityStatus(
            appAvailabilityApi,
            appAvailabilityProvider,
            notificationProvider
        )

    @Test
    fun `saves appAvailability to provider`() = runBlocking {
        val stubResponse = stubResponse()
        coEvery { appAvailabilityApi.getAvailability() } returns stubResponse
        val slot = slot<AppAvailabilityResponse>()
        every { appAvailabilityProvider.appAvailability = capture(slot) } just runs

        testSubject.invoke()

        assertEquals(stubResponse, slot.captured)
    }

    @Test
    fun `display notification if app availability has changed `() = runBlocking {
        val stubResponse = stubResponse()
        coEvery { appAvailabilityApi.getAvailability() } returns stubResponse
        val slot = slot<AppAvailabilityResponse>()
        every { appAvailabilityProvider.appAvailability = capture(slot) } just runs

        every { appAvailabilityProvider.isAppAvailable() } returns true andThen false

        testSubject.invoke()

        assertEquals(stubResponse, slot.captured)
        verify { notificationProvider.showAppIsNotAvailable() }
    }

    private fun stubResponse(minSdkValue: Int = 23, minAppVersionCode: Int = 8) =
        AppAvailabilityResponse(
            minimumAppVersion = MinimumAppVersion(
                description = Translatable(mapOf("en-GB" to "Please Update or Not available")),
                value = minAppVersionCode
            ),
            minimumSdkVersion = MinimumSdkVersion(
                description = Translatable(mapOf("en-GB" to "Not supported")),
                value = minSdkValue
            )
        )
}
