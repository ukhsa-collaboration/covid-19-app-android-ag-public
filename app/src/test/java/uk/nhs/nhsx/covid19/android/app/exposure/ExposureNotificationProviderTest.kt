package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes.RESOLUTION_REQUIRED
import com.google.android.gms.common.api.Status
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ExposureNotificationProviderTest {

    private val exposureNotificationApi = mockk<ExposureNotificationApi>(relaxed = true)

    private val testSubject = ExposureNotificationManager(
        exposureNotificationApi
    )

    @Before
    fun setUp() {
        coEvery { exposureNotificationApi.isEnabled() } returns true
    }

    @Test
    fun `start exposure notifications and set hasOnboarded true`() = runBlockingTest {

        val result = testSubject.startExposureNotifications()

        assertThat(result).isEqualTo(ExposureNotificationActivationResult.Success)
    }

    @Test
    fun `start exposure notifications requires resolution`() = runBlockingTest {
        val status = mockk<Status>()
        every { status.hasResolution() } returns true
        every { status.statusCode } returns RESOLUTION_REQUIRED
        every { status.statusMessage } returns "Message"

        coEvery { exposureNotificationApi.start() } throws ApiException(status)

        val result = testSubject.startExposureNotifications()

        assertThat(result).isInstanceOf(ExposureNotificationActivationResult.ResolutionRequired::class.java)
    }

    @Test
    fun `start exposure notifications return error`() = runBlockingTest {

        coEvery { exposureNotificationApi.start() } throws Exception()

        val result = testSubject.startExposureNotifications()

        assertThat(result).isInstanceOf(ExposureNotificationActivationResult.Error::class.java)
    }

    @Test
    fun `exposure notification is enabled returns true`() = runBlockingTest {
        val result = testSubject.isEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun `exposure notification is disabled returns false`() = runBlockingTest {
        coEvery { exposureNotificationApi.isEnabled() } returns false

        val result = testSubject.isEnabled()

        assertThat(result).isFalse()
    }
}
