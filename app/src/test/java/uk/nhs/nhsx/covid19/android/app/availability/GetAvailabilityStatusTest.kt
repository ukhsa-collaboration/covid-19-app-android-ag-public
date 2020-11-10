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
import uk.nhs.nhsx.covid19.android.app.app
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.AppAvailabilityApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.RecommendedAppVersion
import kotlin.test.assertEquals

class GetAvailabilityStatusTest {

    private val appAvailabilityApi = mockk<AppAvailabilityApi>()
    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val lastRecommendedNotificationAppVersionProvider = mockk<LastRecommendedNotificationAppVersionProvider>(relaxed = true)

    private val testSubject =
        GetAvailabilityStatus(
            appAvailabilityApi,
            appAvailabilityProvider,
            notificationProvider,
            lastRecommendedNotificationAppVersionProvider
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
        verify(exactly = 0) { appAvailabilityProvider.isUpdateRecommended() }
    }

    @Test
    fun `display recommended app update notification if recommended app version for first time`() = runBlocking {
        val stubResponse = stubResponse()
        coEvery { appAvailabilityApi.getAvailability() } returns stubResponse
        every { appAvailabilityProvider.appAvailability } returns stubResponse
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityProvider.isUpdateRecommended() } returns true
        every { lastRecommendedNotificationAppVersionProvider.value } returns null

        testSubject.invoke()

        verify { lastRecommendedNotificationAppVersionProvider setProperty "value" value eq(10) }
        verify { notificationProvider.showRecommendedAppUpdateIsAvailable() }
    }

    @Test
    fun `display recommended app update notification if recommended app version increased`() = runBlocking {
        val stubResponse = stubResponse()
        coEvery { appAvailabilityApi.getAvailability() } returns stubResponse
        every { appAvailabilityProvider.appAvailability } returns stubResponse
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityProvider.isUpdateRecommended() } returns true
        every { lastRecommendedNotificationAppVersionProvider.value } returns 9

        testSubject.invoke()

        verify { lastRecommendedNotificationAppVersionProvider setProperty "value" value eq(10) }
        verify { notificationProvider.showRecommendedAppUpdateIsAvailable() }
    }

    @Test
    fun `don't display recommended app update notification if recommended app version decreased`() = runBlocking {
        val stubResponse = stubResponse()
        coEvery { appAvailabilityApi.getAvailability() } returns stubResponse
        every { appAvailabilityProvider.appAvailability } returns stubResponse
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityProvider.isUpdateRecommended() } returns true
        every { lastRecommendedNotificationAppVersionProvider.value } returns 11

        testSubject.invoke()

        verify(exactly = 0) { notificationProvider.showRecommendedAppUpdateIsAvailable() }
    }

    @Test
    fun `don't display recommended app update notification for second time if recommended app version changed`() = runBlocking {
        val stubResponse = stubResponse()
        coEvery { appAvailabilityApi.getAvailability() } returns stubResponse
        every { appAvailabilityProvider.appAvailability } returns stubResponse
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityProvider.isUpdateRecommended() } returns true
        every { lastRecommendedNotificationAppVersionProvider.value } returns 10

        testSubject.invoke()

        verify(exactly = 0) { notificationProvider.showRecommendedAppUpdateIsAvailable() }
    }

    private fun stubResponse(minSdkValue: Int = 23, minAppVersionCode: Int = 8, recommendedAppVersion: Int = 10) =
        AppAvailabilityResponse(
            minimumAppVersion = MinimumAppVersion(
                description = Translatable(mapOf("en-GB" to "Please Update or Not available")),
                value = minAppVersionCode
            ),
            minimumSdkVersion = MinimumSdkVersion(
                description = Translatable(mapOf("en-GB" to "Not supported")),
                value = minSdkValue
            ),
            recommendedAppVersion = RecommendedAppVersion(
                description = Translatable(mapOf("en-GB" to "Not supported")),
                value = recommendedAppVersion,
                title = Translatable(mapOf("en-GB" to "Not supported"))
            )
        )
}
